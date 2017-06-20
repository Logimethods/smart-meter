#!/bin/bash
if [ $TELEGRAF_DEBUG = "true" ]; then env; fi

if [ "${1:0:1}" = '-' ]; then
    set -- telegraf "$@"
fi
