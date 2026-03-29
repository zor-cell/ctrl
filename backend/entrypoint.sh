#!/bin/sh
export DB_USERNAME=$(cat /run/secrets/db_username)
export DB_PASSWORD=$(cat /run/secrets/db_password)
exec java -jar backend.jar
