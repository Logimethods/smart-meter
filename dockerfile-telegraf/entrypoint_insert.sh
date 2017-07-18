#!/bin/bash
if [[ -n "$TELEGRAF_DEBUG" && $TELEGRAF_DEBUG = "true" ]]; then env; fi

if [ "${1:0:1}" = '-' ]; then
    set -- telegraf "$@"
fi
