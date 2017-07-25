
COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY --from=entrypoint entrypoint.sh /entrypoint.sh

COPY entrypoint_insert.sh /entrypoint_insert.sh

RUN apk --no-cache add jq bash netcat-openbsd>1.130

ENTRYPOINT ["/entrypoint.sh", "gatling.sh"]
