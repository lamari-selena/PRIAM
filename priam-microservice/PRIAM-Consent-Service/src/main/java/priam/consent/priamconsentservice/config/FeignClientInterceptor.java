package priam.consent.priamconsentservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String username = HeaderCaptureFilter.getUsername();
        String token = HeaderCaptureFilter.getToken();

        if (username != null) {
            template.header("x-username", username);
        }
        if (token != null) {
            template.header("authtoken", token);
        }
    }
}

