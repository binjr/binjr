#!/usr/bin/env bash
cd binjr

if [[ $TRAVIS_COMMIT_MESSAGE == *"=>Release:v"* ]]; then
  echo "Start Maven release"
  mvn clean deploy --settings target/travis/settings.xml
else
  echo "Start Maven test"
    mvn clean test
fi