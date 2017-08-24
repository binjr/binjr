#!/usr/bin/env bash
set -ev
if [[ -z "$TRAVIS_TAG" && $TRAVIS_COMMIT_MESSAGE == *"[ci release]"* ]]; then
    currentCommitId="$(git rev-parse HEAD)"
    echo $currentCommitId
    echo "*** Trigger AppVeyor build on release ***"
    curl -H "Authorization: Bearer $APPVEYOR_TOKEN" -H "Content-Type: application/json" -X POST -d '{"accountName":"'$SCM_USERNAME'","projectSlug":"binjr","branch":"master","commitId": "'$currentCommitId'"}' https://ci.appveyor.com/api/builds
fi
