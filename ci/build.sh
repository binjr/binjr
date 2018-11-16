#!/usr/bin/env bash
set -ev

java -version

gradlew clean packageDistribution
