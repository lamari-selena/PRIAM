package tools.descartes.teastore.webui.servlet;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import java.io.File;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.WebServlet;

import java.io.IOException;

/**
 * Servlet exposant les métriques Prometheus sur /metrics
 */
@WebServlet(urlPatterns = "/metrics")
public class PrometheusMetricsServlet extends HttpServlet {

    private static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    static {
        // Enregistrer les métriques système et JVM
        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);
        new ProcessorMetrics().bindTo(prometheusRegistry);
        new FileDescriptorMetrics().bindTo(prometheusRegistry);
        new UptimeMetrics().bindTo(prometheusRegistry);
        /*new DiskSpaceMetrics(new File("/")).bindTo(prometheusRegistry);
        new LogbackMetrics().bindTo(prometheusRegistry); // si tu as Logback*/

    }

    public static PrometheusMeterRegistry getRegistry() {
        return prometheusRegistry;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String response = prometheusRegistry.scrape();
        resp.setContentType("text/plain; version=0.0.4; charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(response);
    }
}
