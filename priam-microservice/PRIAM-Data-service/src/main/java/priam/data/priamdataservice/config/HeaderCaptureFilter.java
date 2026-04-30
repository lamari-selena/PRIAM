package priam.data.priamdataservice.config;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class HeaderCaptureFilter implements Filter {

    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    public static String getUsername() {
        return usernameHolder.get();
    }

    public static String getToken() {
        return tokenHolder.get();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        usernameHolder.set(req.getHeader("x-username"));
        tokenHolder.set(req.getHeader("authtoken"));
        try {
            chain.doFilter(request, response);
        } finally {
            usernameHolder.remove();
            tokenHolder.remove();
        }
    }
}

