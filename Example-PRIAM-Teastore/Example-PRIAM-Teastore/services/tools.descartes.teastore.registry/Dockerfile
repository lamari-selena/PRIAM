FROM descartesresearch/teastore-base:latest
MAINTAINER Chair of Software Engineering <se2-it@informatik.uni-wuerzburg.de>

COPY target/*.war /usr/local/tomcat/webapps/

# Download the OpenTelemetry agent
RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -O /otel.jar


# Copy your .war file into Tomcat
COPY target/*.war /usr/local/tomcat/webapps/

