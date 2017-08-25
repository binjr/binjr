#!/usr/bin/env bash
set -ev
#Skip Maven release plugin commits
if git show -s HEAD | grep -F -q "[maven-release-plugin]" ; then echo "skip maven-release-plugin commit" && exit 0 ; fi

if [[ -z "$TRAVIS_TAG" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
    #Explicitly switch to master to avoid detached HEAD
    git checkout master
    cd binjr
    echo "*** RELEASE ***"
    mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./target/travis/settings.xml" -P binjr-release,buildNativeBundles
else
    cd binjr
    echo "*** SNAPSHOT ***"
    mvn deploy --settings "./target/travis/settings.xml" -P binjr-snapshot
fi