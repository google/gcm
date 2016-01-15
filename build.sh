#!/bin/bash
tagCount=$(git tag -l --points-at HEAD | wc -l)
if [ $tagCount -gt 0 ]; then
  echo "tagged so deploy"
  ant -f ./client-libraries/java/rest-client/build.xml -lib ./client-libraries/java/rest-client/lib deploy
else
  echo "not tagged so stage"
  ant -f ./client-libraries/java/rest-client/build.xml -lib ./client-libraries/java/rest-client/lib stage
fi
