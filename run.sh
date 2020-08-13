#!/bin/sh
docker run \
  --name fieldtracks \
  --hostname local-dev.fieldtracks.org \
  --rm \
  -it \
  -p 8485:80 \
  -p 8443:443 \
  -p 2225:22 \
  -p 2883:1883 \
  -p 8883:8883 \
  --mount type=bind,source="$(pwd)"/data,target=/data \
  fieldtracks:latest
