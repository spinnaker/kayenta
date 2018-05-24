FROM openjdk:8-jdk

MAINTAINER delivery-engineering@netflix.com

COPY . workdir/

WORKDIR workdir

RUN GRADLE_USER_HOME=cache ./gradlew buildDeb -x test && \
  dpkg -i ./kayenta-web/build/distributions/*.deb && \
  cd .. && \
  rm -rf workdir

EXPOSE 8090
ENV services.redis.baseUrl=redis://localhost:6379
ENV redis.connection=redis://localhost:6379

CMD ["/opt/kayenta/bin/kayenta"]
