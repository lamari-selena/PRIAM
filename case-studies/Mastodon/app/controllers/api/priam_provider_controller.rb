# frozen_string_literal: true

# PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2). Mounted on
# bare /api (config/routes/api.rb), no auth - machine-to-machine, called
# only by PRIAM-Right-service/PRIAM-Frontend-Provider. Pattern mirrors
# Api::V2::InstancesController, the existing no-auth controller in this
# codebase.
class Api::PriamProviderController < Api::BaseController
  skip_before_action :require_authenticated_user!
  skip_around_action :set_locale

  # idRef = accounts.username (local accounts only). "User" merges the
  # Devise auth row (users) and the ActivityPub profile row (accounts),
  # joined 1:1 via users.account_id - a single logical identity from a
  # GDPR standpoint even though it is physically two tables (playbook §1
  # point 5 pattern, same as prior case studies' "User" DataType).
  ACCOUNT_FIELDS = %w(display_name note).freeze

  READABLE = {
    'User' => %w(email sign_up_ip locale time_zone display_name note),
    'Status' => %w(id text spoiler_text language),
    'PushSubscription' => %w(subscriptionId endpoint key_p256dh key_auth),
  }.freeze

  # Deliberately narrower than READABLE (see
  # Databases/db_insertion_script.sql "Scope decisions" comment): `email`
  # is not erasable (Devise requires it), `sign_up_ip` is read-only, the
  # primary-key columns (`id`/`subscriptionId`) are neither, and
  # PushSubscription's fields are only ever created/read/erased, never
  # rectified (nothing about a push endpoint is meant to be hand-edited).
  RECTIFIABLE = {
    'User' => %w(email locale time_zone display_name note),
    'Status' => %w(text spoiler_text language),
    'PushSubscription' => [].freeze,
  }.freeze

  ERASABLE = {
    'User' => %w(locale time_zone display_name note),
    'Status' => %w(text spoiler_text language),
    'PushSubscription' => %w(endpoint key_p256dh key_auth),
  }.freeze

  # GET /api/dataAccessRight?idRef=...&dataTypeName=...&attributes=a,b,c
  # Always answers with a JSON array (§2), one element per row of
  # dataTypeName held by idRef (a single element for User, one per row for
  # Status/PushSubscription).
  def data_access_right
    id_ref = params[:idRef].to_s
    data_type_name = params[:dataTypeName].to_s
    attributes = params[:attributes].to_s.split(',').map(&:strip).reject(&:blank?)

    unless id_ref.present? && READABLE.key?(data_type_name)
      render json: []
      return
    end

    allowed = attributes.select { |attr| READABLE[data_type_name].include?(attr) }
    account = find_account(id_ref)
    records = account ? load_records(account, data_type_name) : []
    render json: records.map { |record| pick_attributes(record, allowed) }
  end

  # POST /api/rectification  body: {idRef, dataTypeName, dataName, newValue, primaryKeys}
  def rectification
    id_ref, data_type_name, data_name, new_value, primary_keys = write_params
    return render_bad_request unless RECTIFIABLE.fetch(data_type_name, []).include?(data_name)

    account = find_account(id_ref)
    record = account && record_for(account, data_type_name, data_name, primary_keys)
    return render_not_found unless record

    record.update_column(data_name, new_value.to_s)
    render json: { success: true }
  end

  # POST /api/erasure  body: {idRef, dataTypeName, dataName, primaryKeys}
  def erasure
    id_ref, data_type_name, data_name, = write_params
    primary_keys = params[:primaryKeys] || {}
    return render_bad_request unless ERASABLE.fetch(data_type_name, []).include?(data_name)

    account = find_account(id_ref)
    record = account && record_for(account, data_type_name, data_name, primary_keys)
    return render_not_found unless record

    record.update_column(data_name, '')
    render json: { success: true }
  end

  # POST /api/dataValue  body: {idRef, dataName, primaryKeys}
  # No dataTypeName (§2/§8.2.f) - inferred from dataName's whitelist, which
  # is why every DataType in READABLE above uses a distinct field name
  # (Status.id vs PushSubscription.subscriptionId).
  def data_value
    id_ref = params[:idRef].to_s
    data_name = params[:dataName].to_s
    primary_keys = params[:primaryKeys] || {}
    data_type_name = infer_data_type_name(data_name)
    return render_not_found unless data_type_name

    account = find_account(id_ref)
    record = account && record_for(account, data_type_name, data_name, primary_keys)
    return render_not_found unless record

    render json: { value: physical_value(record, data_type_name, data_name).to_s }
  end

  private

  def write_params
    [params[:idRef].to_s, params[:dataTypeName].to_s, params[:dataName].to_s, params[:newValue].to_s, params[:primaryKeys] || {}]
  end

  def find_account(id_ref)
    Account.find_by(username: id_ref, domain: nil)
  end

  def infer_data_type_name(data_name)
    READABLE.find { |_type, fields| fields.include?(data_name) }&.first
  end

  def record_for(account, data_type_name, data_name, primary_keys)
    case data_type_name
    when 'User'
      ACCOUNT_FIELDS.include?(data_name) ? account : account.user
    when 'Status'
      account.statuses.find_by(id: primary_keys['id'])
    when 'PushSubscription'
      account.user && Web::PushSubscription.find_by(id: primary_keys['subscriptionId'], user_id: account.user.id)
    end
  end

  # PushSubscription.subscriptionId is a Provider-bridge-only alias for the
  # physical `id` column (see READABLE's comment above).
  def physical_value(record, data_type_name, data_name)
    attr = data_type_name == 'PushSubscription' && data_name == 'subscriptionId' ? 'id' : data_name
    record.public_send(attr)
  end

  def load_records(account, data_type_name)
    case data_type_name
    when 'User'
      user = account.user
      return [] unless user

      [{
        'email' => user.email,
        'sign_up_ip' => user.sign_up_ip&.to_s,
        'locale' => user.locale,
        'time_zone' => user.time_zone,
        'display_name' => account.display_name,
        'note' => account.note,
      }]
    when 'Status'
      account.statuses.order(:id).map do |status|
        { 'id' => status.id.to_s, 'text' => status.text, 'spoiler_text' => status.spoiler_text, 'language' => status.language }
      end
    when 'PushSubscription'
      return [] unless account.user

      Web::PushSubscription.where(user_id: account.user.id).order(:id).map do |sub|
        { 'subscriptionId' => sub.id.to_s, 'endpoint' => sub.endpoint, 'key_p256dh' => sub.key_p256dh, 'key_auth' => sub.key_auth }
      end
    else
      []
    end
  end

  def pick_attributes(record, allowed)
    keys = allowed.presence || record.keys
    record.slice(*keys)
  end

  def render_not_found
    render json: { error: 'Record not found' }, status: 404
  end

  def render_bad_request
    render json: { error: 'Unknown or non-writable field' }, status: 400
  end
end
