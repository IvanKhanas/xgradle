FROM registry.altlinux.org/sisyphus/alt:latest AS base

ARG APP_NAME=xgradle
ARG USER_NAME=$APP_NAME
ARG UID=1000
ARG GID=1000

RUN apt-get update && \
    apt-get dist-upgrade -y && \
    apt-get install -y \
        glibc-pthread \
        java-17-openjdk-devel \
        gradle \
        junit5 \
        apiguardian \
        apache-commons-io \
        apache-commons-cli \
        google-gson \
	shadow-gradle-plugin \
        git && \
    apt-get clean

RUN groupadd -r -g $GID $USER_NAME && useradd -r -m -u $UID -g $USER_NAME $USER_NAME

FROM base AS builder

ARG APP_NAME=xgradle
ARG USER_NAME=$APP_NAME

WORKDIR /app

COPY --chown=$USER_NAME:$USER_NAME . .
RUN chown -R $USER_NAME:$USER_NAME .

USER $USER_NAME

RUN gradle build \
    -Dmaven.poms.dir=/usr/share/maven-poms \
    -Djava.library.dir=/usr/share/java \
    -Prelease

USER 0

RUN apt-get remove -y \
        java-17-openjdk-devel \
        junit5 \
        apiguardian \
        apache-commons-io \
        apache-commons-cli \
        google-gson \
	shadow-gradle-plugin && \
    rm -rf /home/$USER_NAME/.m2 \
    /home/$USER_NAME/.gradle

FROM registry.altlinux.org/sisyphus/alt:latest AS integration-test

ARG APP_NAME=xgradle
ARG CORE_NAME=${APP_NAME}-core
ARG CLI_NAME=${APP_NAME}-tool
ARG USER_NAME=$APP_NAME
ARG UID=1000
ARG GID=1000

RUN apt-get update && \
    apt-get install -y \
        glibc \
        glibc-pthread \
        gradle \
        maven-local \
        java-21-openjdk-devel \
        git \
        biz-aQute-bnd-gradle-plugins \
        junit5 && \
    apt-get clean

RUN groupadd -g $GID $USER_NAME && useradd -m -u $UID -g $GID $USER_NAME

WORKDIR /test

COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CLI_NAME/build/dist/$CLI_NAME /usr/share/java/$APP_NAME/
COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CLI_NAME/build/dist/$CLI_NAME.jar /usr/share/java/$APP_NAME/
COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CORE_NAME/build/dist/$CORE_NAME.jar /usr/share/gradle/$APP_NAME/
COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CORE_NAME/build/dist/$APP_NAME-plugin.gradle /usr/share/gradle/init.d/
COPY docker/patches/* /test/patches/

RUN ln -s /usr/share/java/$APP_NAME/$CLI_NAME /usr/bin/$CLI_NAME && \
    chmod +x /usr/share/java/$APP_NAME/$CLI_NAME && \
    chown -R $USER_NAME:$USER_NAME /usr/share/java/$APP_NAME/ /usr/share/gradle/$APP_NAME/ /usr/share/gradle/init.d/ /test

USER $USER_NAME

RUN git clone https://github.com/hamcrest/JavaHamcrest.git && \
    cd JavaHamcrest && \
    git checkout v3.0 && \
    git apply /test/patches/0001-Disable-checkstyle-plugin-alt-patch.patch && \
    git apply /test/patches/0002-Add-java-plugin-alt-patch.patch && \
    gradle publishToMavenLocal \
    -Dmaven.poms.dir=/usr/share/maven-poms \
    -Djava.library.dir=/usr/share/java \
    --offline

RUN git clone https://github.com/cbeust/jcommander.git && \
    cd jcommander && \
    git checkout 2.0 && \
    git apply /test/patches/0001-Disable-signing-with-key-alt-patch.patch && \
    gradle publishToMavenLocal \
    -Dmaven.poms.dir=/usr/share/maven-poms \
    -Djava.library.dir=/usr/share/java \
    --offline

USER 0

RUN apt-get remove -y \
        glibc \
        glibc-pthread \
        gradle \
        maven-local \
        java-21-openjdk-devel \
        git \
        biz-aQute-bnd-gradle-plugins \
        junit5 && \
    rm -rf /home/$USER_NAME/.m2 \
    /home/$USER_NAME/.gradle \
    /test \
    /var/lib/apt/lists/*

FROM registry.altlinux.org/sisyphus/alt:latest AS runtime

ARG APP_NAME=xgradle
ARG CORE_NAME=$APP_NAME-core
ARG CLI_NAME=$APP_NAME-tool
ARG USER_NAME=$APP_NAME
ARG UID=1000
ARG GID=1000

RUN apt-get update && \
    apt-get install -y \
        glibc \
        glibc-pthread \
        java-21-openjdk-headless \
        gradle \
        git && \
    apt-get clean

RUN groupadd -r -g $GID $USER_NAME && useradd -r -g $USER_NAME $USER_NAME

WORKDIR /app

COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CLI_NAME/build/dist/$CLI_NAME /usr/share/java/$APP_NAME/
COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CLI_NAME/build/dist/$CLI_NAME.jar /usr/share/java/$APP_NAME/
COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CORE_NAME/build/dist/$CORE_NAME.jar /usr/share/gradle/$APP_NAME/
COPY --from=builder --chown=$USER_NAME:$USER_NAME /app/$CORE_NAME/build/dist/$APP_NAME-plugin.gradle /usr/share/gradle/init.d/

RUN ln -s /usr/share/java/$APP_NAME/$CLI_NAME /usr/bin/$CLI_NAME && \
    chmod +x /usr/share/java/$APP_NAME/$CLI_NAME && \
    chown -R $USER_NAME:$USER_NAME /usr/share/java/$APP_NAME/ \
    /usr/share/gradle/$APP_NAME/ \
    /usr/share/gradle/init.d/

USER $USER_NAME

RUN $CLI_NAME --version
