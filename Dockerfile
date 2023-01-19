#FROM maven:3-jdk-8-slim
#FROM maven:3-jdk-9-slim
#FROM maven:3-jdk-10-slim
#FROM maven:3-jdk-11-slim
FROM maven:3
#FROM frekele/maven

RUN mkdir /repository
ENV SECRET_SEED ""
ENV FULL_BLOCKCHAIN true
ENV CLEAN_BLOCKCHAIN false
ENV INNER_PEER false

ENV EON_NETWORK ""

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libjffi-jni \
        libsodium-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pom.xml files
COPY pom.xml /app/pom.xml

COPY json-rpc/pom.xml /app/json-rpc/pom.xml
COPY peer-core/pom.xml /app/peer-core/pom.xml
COPY peer-eon/pom.xml /app/peer-eon/pom.xml
COPY peer-eon-app/pom.xml /app/peer-eon-app/pom.xml
COPY peer-crypto/pom.xml /app/peer-crypto/pom.xml
COPY peer-eon-tx-builders/pom.xml /app/peer-eon-tx-builders/pom.xml

# Cache depedences
RUN mvn -Dmaven.repo.local=/repository install jetty:help

# Copy additional files
COPY peer-eon-app/jetty.xml /app/peer-eon-app/jetty.xml
#COPY db /app/db
RUN mkdir /app/db

# Copy sources
COPY json-rpc/src /app/json-rpc/src
COPY peer-eon-app/src /app/peer-eon-app/src
COPY peer-core/src /app/peer-core/src
COPY peer-eon/src /app/peer-eon/src
COPY peer-crypto/src /app/peer-crypto/src
COPY peer-eon-tx-builders/src /app/peer-eon-tx-builders/src

# Compile files (and check formatting)
RUN mvn -Dmaven.repo.local=/repository package install -DskipTests

# Run tests
RUN mvn -Dmaven.repo.local=/repository test

VOLUME /app/peer-eon-app/src/main/webapp/WEB-INF
EXPOSE 9443

ENV MAVEN_OPTS "-XX:+HeapDumpOnOutOfMemoryError -verbose:gc -XX:+PrintGCDetails -XX:+PrintFlagsFinal -Xmx350m -Dlog4j.formatMsgNoLookups=true"
ENTRYPOINT mvn -o -Dmaven.repo.local=/repository jetty:run -DSECRET_SEED=$SECRET_SEED -Dblockchain.full=$FULL_BLOCKCHAIN -Dblockchain.clean=$CLEAN_BLOCKCHAIN -Dhost.inner=$INNER_PEER

# For debug
# ENTRYPOINT /bin/bash
