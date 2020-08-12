#!/bin/sh
docker run \
  -it \
  --mount type=bind,source="$(pwd)"/data,target=/data \
  fieldtracks:latest
