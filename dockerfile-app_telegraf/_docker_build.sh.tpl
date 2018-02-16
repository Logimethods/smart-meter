##!/bin/bash

docker pull ((docker-dz_telegraf-repository)):((docker-dz_telegraf-tag))((docker-additional-tag))
docker build -t ((docker-app_telegraf-repository)):((docker-app_telegraf-tag))((docker-additional-tag)) .