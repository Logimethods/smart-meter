#FROM logimethods/eureka:entrypoint
FROM entrypoint_exp

COPY entrypoint_insert.sh /entrypoint_insert.sh

COPY /main /

ENTRYPOINT ["/entrypoint.sh"]
CMD ["/main"]
