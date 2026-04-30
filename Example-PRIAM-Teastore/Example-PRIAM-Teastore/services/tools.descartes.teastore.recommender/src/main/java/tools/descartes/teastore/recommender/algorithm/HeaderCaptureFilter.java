package tools.descartes.teastore.recommender.algorithm;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import tools.descartes.teastore.entities.message.SessionBlob;

/**
 * Simple ThreadLocal filter pour capturer les headers x-username et authtoken.
 */
public class HeaderCaptureFilter implements Filter {

    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    public static String getUsername() {
        return usernameHolder.get();
    }

    public static String getToken() {
        return tokenHolder.get();
    }

    public static void setUsername(String username) {
        usernameHolder.set(username);
    }

    public static void setToken(String token) {
        tokenHolder.set(token);
    }

    public static void clear() {
        usernameHolder.remove();
        tokenHolder.remove();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Rien à initialiser
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            usernameHolder.set(httpReq.getHeader("x-username"));
            tokenHolder.set(httpReq.getHeader("authtoken"));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Nettoyage pour éviter les fuites mémoire
            usernameHolder.remove();
            tokenHolder.remove();
        }
    }

    @Override
    public void destroy() {
        // Rien à nettoyer
    }
}

