#!/usr/bin/env bash
echo "TAG=$TRAVIS_TAG"
echo "MSG=$TRAVIS_COMMIT_MESSAGE"
set -ev
if [[ -z "$TRAVIS_TAG" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
    #Explicitly switch to master to avoid detached HEAD
    git checkout master
    cd binjr
    echo "*** RELEASE ***"
    mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./target/travis/settings.xml" -P binjr-release,buildNativeBundles
    #Trigger AppVeyor build
    curl -H "Authorization: Bearer $APPVEYOR_TOKEN" -H "Content-Type: application/json" -X POST -d '{"accountName":"fthevenet","projectSlug":"binjr","branch":"master"}' https://ci.appveyor.com/api/builds
else
    cd binjr
    echo "*** SNAPSHOT ***"
    mvn deploy --settings "./target/travis/settings.xml" -P binjr-snapshot
fi