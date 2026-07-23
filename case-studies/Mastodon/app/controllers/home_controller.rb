# frozen_string_literal: true

class HomeController < ApplicationController
  include WebAppControllerConcern

  before_action :redirect_to_priam_consent!, if: :user_signed_in?

  def index
    expires_in(15.seconds, public: true, stale_while_revalidate: 30.seconds, stale_if_error: 1.day) unless user_signed_in?
  end

  private

  # §4bis forced-consent redirect: fires at most once, since
  # has_pending_consent_decision? flips to false as soon as a decision
  # exists (granted or refused) - no redirect loop on the next page load.
  def redirect_to_priam_consent!
    return unless Priam.has_pending_consent_decision?(current_account.username, Priam::PUSH_NOTIFICATIONS_PROCESSING)

    redirect_to Priam.consent_url
  end
end
