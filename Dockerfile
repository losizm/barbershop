################################################################################
# Instructions:
#
# (1) Build application package:
#     sbt packageZipTarball
#
# (2) Build Docker image:
#     docker image build --build-arg VERSION=x.y.z -t losizm/barbershop:x.y.z .
################################################################################
FROM ubuntu:20.04
COPY --from=eclipse-temurin:8 /opt/java/openjdk /opt/jdk8
ENV JAVA_HOME=/opt/jdk8 \
    PATH="/opt/jdk8/bin:$PATH"
ARG VERSION=0.0.0
ADD ["/target/universal/barbershop-${VERSION}.tgz", "/opt"]
RUN cd /opt && \
    ln -s "barbershop-${VERSION}" barbershop && \
    chmod +x barbershop/bin/*
ENV BARBERSHOP_VERSION="$VERSION" \
    BARBERSHOP_HOME=/opt/barbershop \
    PATH="/opt/barbershop/bin:$PATH"
WORKDIR $BARBERSHOP_HOME
EXPOSE 9999
ENTRYPOINT ["barbershop"]
