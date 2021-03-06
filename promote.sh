#!/bin/bash

if [[ $# != 2 ]]; then
  echo "Usage: $0 releaseAsVersion incrementToVersion"
  echo "    This will update the version in gradle.proprties and the complete docker-compose.yml to <releaseAsVersion>, if possible. Then it will tag to trigger promotion strategy, and finally update and push the gradle.properties and docker-compose.yml with <incrementToVersion>"
  exit 1
fi

releaseAsVersion=$1
incrementToVersion=$2

# do not continue if there are staged changes which might accidentally be included in the build
if ! git diff --cached --quiet; then
  echo "You cannot trigger this action with staged changes. Please commit or revert them."
  exit 1
fi

# do not continue if there are unmodified changes to the files we need to update that might accidentally be included
if [[ $(( $(git diff-index --name-only HEAD | grep gradle.properties | wc -l) + $(git diff-index --name-only HEAD | grep docker-compose.yml | wc -l) )) > 0 ]]; then
  echo "You cannot trigger this action with uncommitted changes to gradle.properties or docker-compose.yml"
  exit 1
fi

# update the two files
updateVersions() {
  sed -i -- "s/version=.*/version=$1/g" gradle.properties
  sed -i -- "s/\(.* image: cedardevs.*:\).*/\1$1/g" docker-compose.yml
}

# commit and push
updateAndCommit() {
  updateVersions $1
  git add gradle.properties docker-compose.yml
  git commit -m "Updating version to $1"
  git push
}

# update versions and tag
tag="v$releaseAsVersion"
updateAndCommit $releaseAsVersion
git tag "$tag"; git push origin "$tag"
updateAndCommit $incrementToVersion
