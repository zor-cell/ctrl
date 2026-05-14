#!/bin/bash

export MSYS_NO_PATHCONV=1

cd "$(dirname "$0")/../.." || exit 1

echo "Loading environment variables from .env..."
source .env

echo "Fetching Node ID..."
NODE_ID=$(docker compose exec -T garage /garage node id 2>/dev/null | cut -d'@' -f1 | tr -d '\r')

if [ -z "$NODE_ID" ]; then
    echo "Error: Could not get Node ID. Is the garage container running?"
    exit 1
fi

echo "Initializing layout for node: $NODE_ID"
docker compose exec -T garage /garage layout assign -z dc1 -c 10G "$NODE_ID" || true
docker compose exec -T garage /garage layout apply --version 1 || true

GARAGE_PRIVATE_BUCKET_NAME=${GARAGE_PRIVATE_BUCKET_NAME:-${GARAGE_BUCKET_NAME:-ctrl-dev-media-private}}
GARAGE_PUBLIC_BUCKET_NAME=${GARAGE_PUBLIC_BUCKET_NAME:-ctrl-dev-media-public}

echo "Setting up private bucket '$GARAGE_PRIVATE_BUCKET_NAME'..."
docker compose exec -T garage /garage bucket create "$GARAGE_PRIVATE_BUCKET_NAME" || true
docker compose exec -T garage /garage bucket website --deny "$GARAGE_PRIVATE_BUCKET_NAME" || true

echo "Setting up public bucket '$GARAGE_PUBLIC_BUCKET_NAME'..."
docker compose exec -T garage /garage bucket create "$GARAGE_PUBLIC_BUCKET_NAME" || true
docker compose exec -T garage /garage bucket website --allow "$GARAGE_PUBLIC_BUCKET_NAME" || true

echo "Importing access keys..."
docker compose exec -T garage /garage key import --yes "$GARAGE_ACCESS_KEY" "$GARAGE_SECRET_KEY" -n backend-key || true
docker compose exec -T garage /garage bucket allow --read --write --owner "$GARAGE_PRIVATE_BUCKET_NAME" --key "$GARAGE_ACCESS_KEY" || true
docker compose exec -T garage /garage bucket allow --read --write --owner "$GARAGE_PUBLIC_BUCKET_NAME" --key "$GARAGE_ACCESS_KEY" || true
