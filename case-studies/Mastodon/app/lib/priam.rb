# frozen_string_literal: true

require 'net/http'
require 'json'

# Bridge to the PRIAM GDPR rights/consent platform
# (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4/§4bis). Every PRIAM_*/KEYCLOAK_*
# variable is unset by default (fail-open/disabled), so this module is a
# no-op unless explicitly wired up in .env.production /
# case-studies/Mastodon/docker-compose.yml.
module Priam
  # Databases/db_insertion_script.sql: priam-actor.data_subject_category(1) = 'Mastodon User'.
  DATA_SUBJECT_CATEGORY_ID = 1
  # Databases/db_insertion_script.sql: priam-data.data(data_id) per field group.
  USER_DATA_IDS = [1, 2, 3, 4, 5, 6].freeze
  STATUS_DATA_IDS = [7, 8, 9, 10].freeze
  PUSH_SUBSCRIPTION_DATA_IDS = [11, 12, 13, 14].freeze
  PUSH_NOTIFICATIONS_PROCESSING = 'Push Notifications'

  # Wider than the 3s used by prior case studies' equivalents: this
  # environment's first request to a cold Spring Boot service (JIT/
  # connection-pool warmup under Docker Desktop's Windows virtualization
  # layer) was observed taking >3s in real testing, causing a false-negative
  # timeout on an otherwise successful call (see priam-integration/
  # INTEGRATION-REPORT.md).
  TIMEOUT_SECONDS = 8

  class << self
    # CEP (§4): consent for an OPTIONAL processing. Fail-open if PRIAM is
    # not configured, fail-closed (deny) if PRIAM is configured but
    # unreachable/erroring.
    def get_consent(id_ref, processing_name)
      cdp_url = ENV.fetch('PRIAM_CDP_URL', nil)
      return true if cdp_url.blank?

      response = http_get("#{cdp_url}/api/decision/#{ERB::Util.url_encode(processing_name)}", idRefList: id_ref)
      return false unless response.is_a?(Net::HTTPSuccess)

      JSON.parse(response.body)[id_ref] == true
    rescue StandardError => e
      Rails.logger.warn("[Priam] get_consent(#{id_ref}, #{processing_name}) failed: #{e.message}")
      false
    end

    # §4bis: register every new user as a PRIAM data_subject. Idempotent
    # (upsert by idRef on the Actor-service side) - never raises, never
    # blocks sign-up.
    def register_data_subject(id_ref)
      actor_url = ENV.fetch('PRIAM_ACTOR_URL', nil)
      return if actor_url.blank?

      http_post("#{actor_url}/api/DataSubject", idRef: id_ref, dataSubjectCategoryId: DATA_SUBJECT_CATEGORY_ID)
    rescue StandardError => e
      Rails.logger.warn("[Priam] register_data_subject(#{id_ref}) failed: #{e.message}")
    end

    # §4bis: "is there already a consent decision at all" (Consent
    # Information Point) - distinct from get_consent's "is it granted".
    def has_pending_consent_decision?(id_ref, processing_name)
      cdp_url = ENV.fetch('PRIAM_CDP_URL', nil)
      return false if cdp_url.blank? || id_ref.blank?

      response = http_get("#{cdp_url}/api/contract/list/consents/#{ERB::Util.url_encode(id_ref)}/#{ERB::Util.url_encode(processing_name)}")
      return false unless response.is_a?(Net::HTTPSuccess)

      decisions = JSON.parse(response.body)
      decisions.is_a?(Array) && decisions.empty?
    rescue StandardError
      false
    end

    # §4bis: report which annotated data_ids a subject now holds a record
    # of (bookkeeping for the Access Request page - §8.1.b). Must be
    # called at every point a personal record is created, not just at
    # sign-up (§4bis, "the most frequently forgotten point").
    def report_processed_data(id_ref, data_ids)
      actor_url = ENV.fetch('PRIAM_ACTOR_URL', nil)
      data_url = ENV.fetch('PRIAM_DATA_URL', nil)
      return if actor_url.blank? || data_url.blank? || data_ids.blank?

      id_response = http_get("#{actor_url}/api/DataSubjectId/#{ERB::Util.url_encode(id_ref)}")
      return unless id_response.is_a?(Net::HTTPSuccess)

      data_subject_id = id_response.body.strip
      http_post("#{data_url}/api/processed-data/add?subjectId=#{data_subject_id}", data_ids)
    rescue StandardError => e
      Rails.logger.warn("[Priam] report_processed_data(#{id_ref}) failed: #{e.message}")
    end

    # §4bis "Automatic Keycloak identity provisioning": Mastodon has its
    # own local sign-up, so nothing else would ever create a matching
    # Keycloak account. Covers local sign-up only - a sign-up through an
    # OmniAuth social/OIDC provider has no plaintext password to
    # synchronize (documented limitation, not silently ignored).
    def provision_keycloak_user(id_ref, email, password)
      admin_url = ENV.fetch('KEYCLOAK_ADMIN_URL', nil)
      return if admin_url.blank? || email.blank? || password.blank?

      token = keycloak_admin_token(admin_url)
      return if token.blank?

      realm = ENV.fetch('KEYCLOAK_REALM', 'priam-realm')
      # Keycloak username = email, not the Mastodon handle (§4bis/§8.8): a
      # Mastodon username can be as short as 1 character, below Keycloak's
      # 3-char minimum. firstName/lastName are required by the realm's
      # User Profile - reused from email since Mastodon has no separate
      # first/last name fields.
      response = http_post(
        "#{admin_url}/admin/realms/#{realm}/users",
        {
          username: email,
          email: email,
          enabled: true,
          emailVerified: true,
          firstName: email,
          lastName: email,
          credentials: [{ type: 'password', value: password, temporary: false }],
          attributes: { idReference: [id_ref] },
        },
        { 'Authorization' => "Bearer #{token}" }
      )
      Rails.logger.info("[Priam] provision_keycloak_user(#{id_ref}): unexpected status #{response.code}") unless ['201', '409'].include?(response.code)
    rescue StandardError => e
      Rails.logger.warn("[Priam] provision_keycloak_user(#{id_ref}) failed: #{e.message}")
    end

    def frontend_url
      ENV.fetch('PRIAM_FRONTEND_URL', nil).presence
    end

    def consent_url
      frontend_url && "#{frontend_url}/consent"
    end

    private

    def keycloak_admin_token(admin_url)
      uri = URI("#{admin_url}/realms/master/protocol/openid-connect/token")
      response = Net::HTTP.start(uri.host, uri.port, open_timeout: TIMEOUT_SECONDS, read_timeout: TIMEOUT_SECONDS) do |http|
        request = Net::HTTP::Post.new(uri)
        request.set_form_data(
          'grant_type' => 'password',
          'client_id' => 'admin-cli',
          'username' => ENV.fetch('KEYCLOAK_ADMIN_USERNAME', 'admin'),
          'password' => ENV.fetch('KEYCLOAK_ADMIN_PASSWORD', 'admin')
        )
        http.request(request)
      end
      return nil unless response.is_a?(Net::HTTPSuccess)

      JSON.parse(response.body)['access_token']
    end

    def http_get(url, params = nil)
      uri = URI(url)
      uri.query = URI.encode_www_form(params) if params
      Net::HTTP.start(uri.host, uri.port, open_timeout: TIMEOUT_SECONDS, read_timeout: TIMEOUT_SECONDS) { |http| http.get(uri) }
    end

    def http_post(url, payload, headers = {})
      uri = URI(url)
      Net::HTTP.start(uri.host, uri.port, open_timeout: TIMEOUT_SECONDS, read_timeout: TIMEOUT_SECONDS) do |http|
        request = Net::HTTP::Post.new(uri)
        request['Content-Type'] = 'application/json'
        headers.each { |k, v| request[k] = v }
        request.body = payload.to_json
        http.request(request)
      end
    end
  end
end
