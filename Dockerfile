FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# sbt stage produces a self-contained directory — copy it in
COPY target/universal/stage /app

EXPOSE 9000

ENV APP_SECRET=""
ENV APP_HOST="0.0.0.0"

ENTRYPOINT ["/app/bin/dand-d-template-parser", \
  "-Dplay.http.secret.key=${APP_SECRET}", \
  "-Dplay.server.http.address=0.0.0.0"]
