FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && \
    apt-get install -y ant openjfx && \
    rm -rf /var/lib/apt/lists/*
ENV JAVAFX_LIB_PATH=/usr/share/openjfx/lib
VOLUME /usr/src/app
WORKDIR /usr/src/app
CMD ["ant", "create_run_jar"]
