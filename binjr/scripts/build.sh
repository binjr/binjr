#!/usr/bin/env bash
set -ev
#Skip Maven release plugin commits
if git show -s HEAD | grep -F -q "[maven-release-plugin]" ; then echo "skip maven-release-plugin commit" && exit 0 ; fi

if [[ -z "$TRAVIS_TAG" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
    echo "*** RELEASE ***"
     #Explicitly switch to master to avoid detached HEAD
    git checkout master
    cd binjr
    if [[ "$TRAVIS_OS_NAME" == "osx" ]]; then
        mvn clean deploy --settings "./target/travis/settings.xml" -P buildNativeBundles
    else
        mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./target/travis/settings.xml" -P binjr-release,buildNativeBundles
    fi
else
 cd binjr
    echo "*** SNAPSHOT ***"
    cd binjr
    if [[ "$TRAVIS_OS_NAME" == "osx" ]]; then
        mvn verify --settings "./target/travis/settings.xml"
    else
        mvn deploy --settings "./target/travis/settings.xml" -P binjr-snapshot
    fi
fi