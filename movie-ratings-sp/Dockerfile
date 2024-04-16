FROM openjdk:21-jdk-buster
EXPOSE 9464
WORKDIR /
COPY build/libs/movie-ratings-sp-all-0.0.1.jar /
COPY libs/opentelemetry-javaagent.jar /
COPY configuration /
CMD ["java", "-javaagent:opentelemetry-javaagent.jar", "-Dotel.exporter.otlp.traces.endpoint=http://simplest-agent.observability.svc.cluster.local:4318/v1/traces", "-Dotel.service.name=movie-ratings-sp", "-Dotel.metrics.exporter=prometheus", "-jar", "movie-ratings-sp-all-0.0.1.jar", "dev.properties"]