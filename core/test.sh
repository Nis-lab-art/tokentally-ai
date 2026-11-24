#!/bin/sh
# compile the back end with the tests and run them on the saved data
cd "$(dirname "$0")"
rm -rf out
javac -d out $(find src -name '*.java') || exit 1
java -cp out tokentally.TestRunner data
