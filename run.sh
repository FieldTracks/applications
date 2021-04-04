#!/bin/sh
docker run \
  --name fieldtracks \
  --hostname local-dev.fieldtracks.org \
  --rm \
  -it \
  -p 8485:80 \
  -p 8883:8883 \
  --mount type=bind,source="$(pwd)"/data,target=/data \
  fieldtracks/fieldtracks:latest
