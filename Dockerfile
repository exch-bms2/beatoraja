FROM eclipse-temurin:17-jdk

ENV OPENJFX_VERSION 21.0.11
ENV ANT_VERSION 1.10.17

RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN cd /tmp && \
    curl -fsSL -o openjfx.zip https://download2.gluonhq.com/openjfx/${OPENJFX_VERSION}/openjfx-${OPENJFX_VERSION}_linux-x64_bin-sdk.zip && \
    unzip openjfx.zip && \
    mv javafx-sdk-${OPENJFX_VERSION} /opt/openjfx && \
    rm openjfx.zip

RUN cd /tmp && \
    curl -fsSL -o apache-ant.tar.gz https://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant.tar.gz

ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin

WORKDIR /usr/src/app

CMD ["ant", "-lib", "/opt/openjfx/lib", "create_run_jar"]
