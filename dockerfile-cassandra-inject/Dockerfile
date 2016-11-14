# https://hub.docker.com/_/golang/
FROM golang:1.6-alpine

RUN mkdir -p /go/src/app
WORKDIR /go/src/app

COPY /go/src/app /go/src/app
RUN go-wrapper download
RUN go-wrapper install

CMD ["go-wrapper", "run"]
