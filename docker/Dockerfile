# Runtime image for a single service.
#
# The jar is built on the host and copied in, rather than compiled inside the
# image. That keeps the image honest about what it contains: whichever jar you
# built is exactly what runs. Build the jar with -Pdev to get a BootUI-enabled
# development image, or without it for a production image containing no BootUI.
FROM eclipse-temurin:25-jre-alpine

ARG JAR_FILE
ARG SERVICE_PORT=8080

RUN addgroup -S app && adduser -S -G app app
USER app

WORKDIR /app
COPY --chown=app:app ${JAR_FILE} app.jar

EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
