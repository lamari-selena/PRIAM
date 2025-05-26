FROM descartesresearch/teastore-base:latest
MAINTAINER Chair of Software Engineering <se2-it@informatik.uni-wuerzburg.de>

COPY target/*.war /usr/local/tomcat/webapps/
RUN mkdir -p /usr/local/tomcat/webapps/ROOT

CMD java -jar /usr/local/tomcat/bin/dockermemoryconfigurator.jar ${TOMCAT_HEAP_MEM_PERCENTAGE}; /usr/local/tomcat/bin/start.sh && echo '<% response.sendRedirect("/tools.descartes.teastore.webui/"); %>' > /usr/local/tomcat/webapps/ROOT/index.jsp && /usr/local/tomcat/bin/catalina.sh run


# Download the OpenTelemetry agent
RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -O /otel.jar


# Copy your .war file into Tomcat
COPY target/*.war /usr/local/tomcat/webapps/

# Add JVM options to Tomcat to activate the agent
ENV JAVA_TOOL_OPTIONS="-javaagent:/otel.jar \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=http://jaeger:4317 \
  -Dotel.exporter.otlp.protocol=grpc \
  -Dotel.service.name=teastore-webui-service"
