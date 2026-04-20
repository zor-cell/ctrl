FROM maven:3.9-eclipse-temurin-21

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        docker.io \
        iptables \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh /usr/local/bin/start-docker-and-run
RUN chmod +x /usr/local/bin/start-docker-and-run

ENTRYPOINT ["/usr/local/bin/start-docker-and-run"]
CMD ["mvn", "-v"]

