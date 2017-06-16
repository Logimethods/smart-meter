# https://docs.docker.com/engine/userguide/eng-image/multistage-build/#use-multi-stage-builds
# https://github.com/Logimethods/docker-eureka
FROM logimethods/eureka:entrypoint as entrypoint
#FROM entrypoint_exp as entrypoint

### MAIN FROM ###

# https://hub.docker.com/_/golang/
FROM golang:1.7-alpine

RUN apk --no-cache add bash jq
COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY --from=entrypoint entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]

# RUN go get github.com/nats-io/nats

RUN mkdir -p /go/src/app
WORKDIR /go/src/app

COPY /go/src/app /go/src/app
RUN apk add --no-cache git \
	&& go-wrapper download

COPY /go/src/app /go/src/app
RUN go-wrapper install

CMD ["go-wrapper", "run"]
