#!/usr/bin/env bash
set -ev

mvn clean deploy  --settings "./cd/maven_settings.xml" -P binjr-snapshot,$BUNDLE_OS_PROFILE
