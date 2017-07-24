#!/usr/bin/env bash
set -ev
echo $TRAVIS_TAG
if [[ -z "$TRAVIS_TAG" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
   #"Explicitly swith to master to avoid disjointed HEAD"
    git checkout master
    cd binjr
    echo "Start Maven release"
    mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./target/travis/settings.xml"
else
    cd binjr
    echo "Start Maven package"
    mvn package
fi