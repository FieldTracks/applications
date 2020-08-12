#!/bin/sh
docker run \
  --name fieldtracks \
  --hostname local-dev.fieldtracks.org \
  --rm \
  -it \
  -p 8485:80 \
  --mount type=bind,source="$(pwd)"/data,target=/data \
  fieldtracks:latest
