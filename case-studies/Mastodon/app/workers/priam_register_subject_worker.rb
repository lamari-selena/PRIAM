# frozen_string_literal: true

# Runs the PRIAM registration chain (Docs/PRIAM-INTEGRATION-PLAYBOOK.md
# §4bis/§8.6) in the background so a slow/unreachable PRIAM never delays
# sign-up. register_data_subject is explicitly awaited (sequential calls,
# not parallel) before report_processed_data, since the latter resolves
# idRef -> dataSubjectId internally and would race the former otherwise
# (§8.6). No plaintext password is ever passed here - Keycloak provisioning
# (which needs it) runs synchronously in Auth::RegistrationsController
# instead, the only place the plaintext is still available in memory.
class PriamRegisterSubjectWorker
  include Sidekiq::Worker

  def perform(user_id)
    user = User.find_by(id: user_id)
    return if user.nil?

    id_ref = user.account.username
    Priam.register_data_subject(id_ref)
    Priam.report_processed_data(id_ref, Priam::USER_DATA_IDS)
  end
end
