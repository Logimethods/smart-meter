# https://hub.docker.com/_/golang/
FROM golang:1.6-alpine

# RUN go get github.com/nats-io/nats

RUN mkdir -p /go/src/app
WORKDIR /go/src/app

COPY /go/src/app /go/src/app
RUN apk add --no-cache git \
	&& go-wrapper download

COPY /go/src/app /go/src/app
RUN go-wrapper install

CMD ["go-wrapper", "run"]
