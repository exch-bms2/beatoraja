FROM airdock/oraclejdk:1.8

# Installs Ant
ENV ANT_VERSION 1.9.7
RUN cd && \
    curl -O http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant-${ANT_VERSION}-bin.tar.gz
ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin
VOLUME /usr/src/app
WORKDIR /usr/src/app
CMD ["ant", "create_run_jar"]
