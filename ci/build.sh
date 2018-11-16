#!/usr/bin/env bash
set -ev
java -version
pwd
./gradlew clean packageDistribution
