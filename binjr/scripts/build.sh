#!/usr/bin/env bash

if [[ $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
    git checkout master
    cd binjr
    echo "Start Maven release"
    mvn --batch-mode release:prepare release:perform -Dresume=false --settings target/travis/settings.xml
else
    cd binjr
    echo "Start Maven test"
    mvn clean test
fi