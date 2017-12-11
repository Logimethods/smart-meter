##!/bin/bash

docker pull ((docker-dz_compose-repository)):((docker-dz_compose-tag))((docker-additional-tag))
docker build -t ((docker-app_compose-repository)):((docker-app_compose-tag))((docker-additional-tag)) .