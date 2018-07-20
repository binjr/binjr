#!/usr/bin/env bash
set -ev
#Skip Maven release plugin commits
if git show -s HEAD | grep -F -q "[maven-release-plugin]" ; then
    if [[ "$TRAVIS_OS_NAME" == "osx" && $TRAVIS_COMMIT_MESSAGE == *"prepare release"* ]]; then
        cd binjr
        mvn clean package --settings "./cd/maven_settings.xml" -P buildNativeBundles
    else
        echo "skip maven-release-plugin commit"
    fi
else
    openssl aes-256-cbc -K $encrypted_f20546f8e814_key -iv $encrypted_f20546f8e814_iv -in ./binjr/cd/codesigning.asc.enc -out ./binjr/cd/codesigning.asc -d
    gpg --fast-import ./binjr/cd/codesigning.asc
    if [[ -z "$TRAVIS_TAG" &&  "$TRAVIS_OS_NAME" == "linux" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
        echo "*** RELEASE ***"
         #Explicitly switch to master to avoid detached HEAD
        git checkout master
        cd binjr
        mvn --batch-mode release:prepare release:perform -Dresume=false --settings "./cd/maven_settings.xml" -P binjr-release,buildNativeBundles,sign,build-extras
    else
        echo "*** SNAPSHOT ***"
        cd binjr
        #mvn deploy -P sign,build-extras --settings ./binjr/cd/ossrh_deploy_settings.xml
        mvn clean deploy  --settings "./cd/maven_settings.xml" -P binjr-snapshot,sign,build-extras
    fi
fi