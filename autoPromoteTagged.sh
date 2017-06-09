#!/bin/bash

# This checks if the tag name starts with 'v', allowing other tags to be used without accidentally triggering an artifact promotion.
if [[ $(git tag -l --points-at HEAD) == v* ]]; then
  ./gradelw promote
fi
