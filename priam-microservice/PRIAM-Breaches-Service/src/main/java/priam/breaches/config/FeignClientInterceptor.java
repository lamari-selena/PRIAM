import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String username = attrs.getRequest().getHeader("x-username");
            String authToken = attrs.getRequest().getHeader("authtoken");
            if (username != null) {
                template.header("x-username", username);
            }
            if (authToken != null) {
                template.header("authtoken", authToken);
            }
        }
    }
}

