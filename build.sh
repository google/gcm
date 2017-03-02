#!/bin/bash
# This script chooses the appropriate build target.
# Pull requests - use test target.
# Commits on master - use stage target, stage SNAPSHOT artifacts.
# Tagged commits - use deploy target, stage release artifacts.
#
# Get length of tag (len will be 0 if commit has no tag)
tagLen=${#TRAVIS_TAG}
# If commit is not a pull request use deploy or stage based on the commit tag,
# if it is a pull request use tests.
if [ "${TRAVIS_PULL_REQUEST}" == "false" ]; then
  # If tag length is grater than 0 then decrypt and import gpg key then use
  # deploy target to build sign and deploy artifacts. If tag length is 0 use
  # stage target to build and stage artifacts.
  if [ $tagLen -gt 0 ]; then
    echo "tagged so deploy"
    openssl aes-256-cbc -K $encrypted_8864fe5d711d_key -iv $encrypted_8864fe5d711d_iv -in sign.key.enc -out sign.key -d
    gpg --import sign.key
    ant -f ./client-libraries/java/rest-client/build.xml -lib ./client-libraries/java/rest-client/lib deploy
  else
    echo "not tagged so stage"
    ant -f ./client-libraries/java/rest-client/build.xml -lib ./client-libraries/java/rest-client/lib stage
  fi
else
  echo "PR so test"
  ant -f ./client-libraries/java/rest-client/build.xml -lib ./client-libraries/java/rest-client/lib tests
fi
