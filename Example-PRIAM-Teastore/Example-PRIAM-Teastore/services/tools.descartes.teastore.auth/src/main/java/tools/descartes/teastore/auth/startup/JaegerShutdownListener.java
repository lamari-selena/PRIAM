package tools.descartes.teastore.auth.startup;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class JaegerShutdownListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Tracer tracer = Configuration.fromEnv().getTracer();
        if (tracer instanceof io.jaegertracing.internal.JaegerTracer) {
            ((io.jaegertracing.internal.JaegerTracer) tracer).close();  // shuts down reporter threads
        }
    }
}


