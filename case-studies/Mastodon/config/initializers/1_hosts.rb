# frozen_string_literal: true

port     = ENV.fetch('PORT') { 3000 }
host     = ENV.fetch('LOCAL_DOMAIN') { "localhost:#{port}" }
web_host = ENV.fetch('WEB_DOMAIN') { host }

alternate_domains = ENV.fetch('ALTERNATE_DOMAINS') { '' }.split(/\s*,\s*/)

Rails.application.configure do
  https = Rails.env.production? || ENV['LOCAL_HTTPS'] == 'true'

  config.x.local_domain = host
  config.x.web_domain   = web_host
  config.x.use_https    = https
  config.x.use_s3       = ENV['S3_ENABLED'] == 'true'
  config.x.use_swift    = ENV['SWIFT_ENABLED'] == 'true'

  config.x.alternate_domains = alternate_domains

  config.action_mailer.default_url_options = { host: web_host, protocol: https ? 'https://' : 'http://', trailing_slash: false }

  config.x.streaming_api_base_url = ENV.fetch('STREAMING_API_BASE_URL') do
    if Rails.env.production?
      "ws#{'s' if https}://#{web_host}"
    else
      "ws://#{host.split(':').first}:4000"
    end
  end

  unless Rails.env.test?
    config.hosts << host if host.present?
    config.hosts << web_host if web_host.present?
    config.hosts.concat(alternate_domains) if alternate_domains.present?
    # PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2): called
    # machine-to-machine by PRIAM-Gateway via its internal Docker service
    # name/IP (Host header e.g. "web:3000" or an ephemeral container IP),
    # neither of which matches LOCAL_DOMAIN - ActionDispatch::HostAuthorization
    # otherwise blocks every call with 403 "Blocked hosts" (a real bug hit
    # during this integration, see priam-integration/INTEGRATION-REPORT.md).
    # Excluding by path (like /health below) is robust regardless of the
    # caller's ephemeral container IP, unlike adding it to config.hosts.
    config.host_authorization = { exclude: ->(request) { request.path == '/health' || request.path.start_with?('/api/dataAccessRight', '/api/rectification', '/api/erasure', '/api/dataValue') } }
  end
end
