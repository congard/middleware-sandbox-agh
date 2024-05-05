#!/bin/bash

generate() {
  python -m grpc_tools.protoc \
    -I../grpc-smarthome-server-kt/src/main/proto \
    --python_out=gen \
    --pyi_out=gen \
    --grpc_python_out=gen \
    "../grpc-smarthome-server-kt/src/main/proto/$1"
}

generate "dht.proto"
generate "bulb.proto"