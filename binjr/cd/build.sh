#!/usr/bin/env bash
set -ev
cd binjr
if [ "$TRAVIS_OS_NAME" == "linux" ]; then
    BUNDLE_OS_PROFILE="bundle-linux"
else
    if [ "$TRAVIS_OS_NAME" == "osx" ]; then
        BUNDLE_OS_PROFILE="bundle-macos"
    elsechmo
         BUNDLE_OS_PROFILE=""
    fi
fi

mvn clean deploy  --settings "./cd/maven_settings.xml" -P binjr-snapshot,build-native-bundle,$BUNDLE_OS_PROFILE


##Skip Maven release plugin commits
#if git show -s HEAD | grep -F -q "[maven-release-plugin]" ; then
#    if [[ "$TRAVIS_OS_NAME" == "osx" && $TRAVIS_COMMIT_MESSAGE == *"prepare release"* ]]; then
#        cd binjr
#        mvn clean package --settings "./cd/maven_settings.xml" -P buildNativeBundles
#    else
#        echo "skip maven-release-plugin commit"
#    fi
#else
#    gpg --fast-import ./binjr/cd/codesigning.asc
#    if [[ -z "$TRAVIS_TAG" &&  "$TRAVIS_OS_NAME" == "linux" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
#        echo "*** RELEASE ***"
#         #Explicitly switch to master to avoid detached HEAD
#        git checkout master
#        cd binjr
#        mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./cd/maven_settings.xml" -P binjr-release,buildNativeBundles,sign,build-extras
#    else
#        echo "*** SNAPSHOT ***"
#        cd binjr
#        mvn clean deploy  --settings "./cd/maven_settings.xml" -P binjr-snapshot,sign,build-extras
#    fi
#fi