#!/bin/sh
# build the api (it compiles ../core too) and start it on port 8080
cd "$(dirname "$0")"
mvn -q -DskipTests package || exit 1
java -jar target/tokentally-api-1.0.jar
