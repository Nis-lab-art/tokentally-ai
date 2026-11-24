#!/bin/sh
# compile the back end and start the menu
cd "$(dirname "$0")"
rm -rf out
javac -encoding UTF-8 -d out $(find src/main -name '*.java') || exit 1
java -cp out tokentally.app.TokenTallyApp "$@"
