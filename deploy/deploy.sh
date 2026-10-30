#!/bin/sh
set -eu

ENVIRONMENT="$1"
TAG="$2"
export TAG

docker compose -p "psoft-$ENVIRONMENT" -f deploy/docker-compose.yml pull
docker compose -p "psoft-$ENVIRONMENT" -f deploy/docker-compose.yml up -d
