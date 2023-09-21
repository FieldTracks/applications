#!/bin/bash

DIR=./src/main/resources


if [[ ! -d "$DIR" ]]; then
  # try to add .. - if the script is run from contrib
  DIR=../src/main/resources
  if [[ ! -d "$DIR" ]]; then
        echo "src/main/resources" not accessible - exiting
        exit 1
  fi
fi

openssl genrsa -out $DIR/rsaPrivateKey.pem 2048
openssl rsa -pubout -in $DIR/rsaPrivateKey.pem -out $DIR/publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in  $DIR/rsaPrivateKey.pem -outform pem -out $DIR/privateKey.pem
