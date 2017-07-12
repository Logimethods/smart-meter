# https://github.com/nats-io/nats-docker/blob/master/Dockerfile
FROM nats:1.0.0 as nats

### MAIN FROM ###

#FROM logimethods/eureka:entrypoint
FROM entrypoint_exp

# http://nats.io/documentation/server/gnatsd-config/
COPY --from=nats gnatsd /gnatsd
COPY /gnatsd.conf /

# Expose client, management, and cluster ports
EXPOSE 4222 8222 6222

ENTRYPOINT ["/entrypoint.sh", "/gnatsd"]
CMD ["-c", "gnatsd.conf"]

ENV READY_WHEN="Server is ready"
