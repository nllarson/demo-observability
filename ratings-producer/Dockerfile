FROM openjdk:21-jdk-buster
EXPOSE 9464
WORKDIR /
COPY build/libs/ratings-producer-standalone-0.0.1.jar /
COPY libs/opentelemetry-javaagent.jar /
COPY configuration /
CMD ["java", "-javaagent:opentelemetry-javaagent.jar", "-Dotel.exporter.otlp.traces.endpoint=http://simplest-agent.observability.svc.cluster.local:4318/v1/traces", "-Dotel.service.name=ratings-producer", "-Dotel.metrics.exporter=prometheus", "-jar", "ratings-producer-standalone-0.0.1.jar", "dev.properties"]