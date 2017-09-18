#!/usr/bin/env bash
set -ev
#Skip Maven release plugin commits
if git show -s HEAD | grep -F -q "[maven-release-plugin]" ; then
    if [[ "$TRAVIS_OS_NAME" == "osx" && $TRAVIS_COMMIT_MESSAGE == *"prepare release"* ]]; then
        cd binjr
        mvn clean deploy --settings "./target/travis/settings.xml" -P buildNativeBundles
    else
        echo "skip maven-release-plugin commit"
        exit 0
    fi
fi

if [[ -z "$TRAVIS_TAG" &&  "$TRAVIS_OS_NAME" == "linux" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
    echo "*** RELEASE ***"
     #Explicitly switch to master to avoid detached HEAD
    git checkout master
    cd binjr
    mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./target/travis/settings.xml" -P binjr-release,buildNativeBundles
else
    echo "*** SNAPSHOT ***"
    cd binjr
    mvn deploy --settings "./target/travis/settings.xml" -P binjr-snapshot
fi