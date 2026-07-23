# frozen_string_literal: true

# One-off backfill for Mastodon users created before the PRIAM sign-up hook
# (app/models/user.rb `register_with_priam`) existed, or created while
# PRIAM_ACTOR_URL was unset (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4bis, last
# point). Safe to re-run: register_data_subject is idempotent (upsert by
# idRef on the Actor-service side) and report_processed_data just reports
# the data_ids currently held, not an additive counter per call.
#
# Run inside the `web`/`sidekiq` container, which already has the real
# PRIAM_* env vars and the full Rails app/ActiveRecord loaded (reuses the
# app's own ORM directly, not a separate database-to-database access):
#   docker compose exec web bin/rails runner priam-integration/backfill-data-subjects.rb

User.includes(:account).find_each do |user|
  next if user.account.nil? || user.account.domain.present? # local accounts only

  id_ref = user.account.username
  Priam.register_data_subject(id_ref)
  Priam.report_processed_data(id_ref, Priam::USER_DATA_IDS)

  status_count = user.account.statuses.count
  Priam.report_processed_data(id_ref, Priam::STATUS_DATA_IDS) if status_count.positive?

  subscription_count = Web::PushSubscription.where(user_id: user.id).count
  Priam.report_processed_data(id_ref, Priam::PUSH_SUBSCRIPTION_DATA_IDS) if subscription_count.positive?

  puts "Backfilled #{id_ref} (#{status_count} statuses, #{subscription_count} push subscriptions)"
end
