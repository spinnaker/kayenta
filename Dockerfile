#
# Builder Image
#
FROM gradle:5.4-jdk8 AS builder

# Prep build environment
ENV GRADLE_USER_HOME=cache
COPY . /opt/workdir
WORKDIR /opt/workdir

# Build kayenta
RUN gradle build

#
# Release Image
#
FROM openjdk:8-alpine

MAINTAINER delivery-engineering@netflix.com

# Set where to look for config from
ENV KAYENTA_OPTS=-Dspring.config.location=file:/opt/kayenta/config/kayenta.yml

# Copy/unpack from builder image
COPY --from=builder /opt/workdir/kayenta-web/build/distributions/kayenta.tar /tmp/kayenta.tar
RUN tar -xf /tmp/kayenta.tar -C /opt

CMD ["/opt/kayenta/bin/kayenta"]
