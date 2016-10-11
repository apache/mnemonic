#!/bin/bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

usage(){
    echo "Usage: $0 Release_Version Next_Release_Version Candidate_Id"
    echo "e.g. $0 0.2.0 0.2.0 rc2"
    echo "     $0 0.2.0 0.2.1 rc3"
    exit 1
}

continueprompt() {
    while true; do
	read -p "Do you wish to continue [y/n] ? " yn
	case $yn in
	    [Yy]* ) break;;
	    [Nn]* ) exit;;
	    * ) echo "Please answer yes or no.";;
	    esac
	done
}

[[ -n "$(git status --porcelain)" ]] &&
    echo "please commit all changes first." && exit

[[ $# -ne 3 ]]  && usage

RELEASE_VERSION="$1"
NEXT_RELEASE_VERSION="$2"
RELEASE_CANDIDATE_ID="$3"
NEXT_VER_COMMIT_PREFIX="Bump version"

echo "You have specified:"
echo "RELEASE_VERSION = ${RELEASE_VERSION}"
echo "NEXT_RELEASE_VERSION = ${NEXT_RELEASE_VERSION}"
echo "RELEASE_CANDIDATE_ID = ${RELEASE_CANDIDATE_ID}"
echo "NOTE: Please ensure there are no uncommitted or untracked files in your local workplace/repo. before continue"
continueprompt

git checkout master

if [ "${RELEASE_VERSION}" == "${NEXT_RELEASE_VERSION}" ]; then
    echo "You are trying to prepare a same version candidate so going to clean up existing branch <branch-${RELEASE_VERSION}> and tag <v${RELEASE_VERSION}-incubating> if any"
    continueprompt
    git branch -d branch-${RELEASE_VERSION}
    if [ $? -ne 0 ]; then
      echo "Request to forcedly delete existing branch <branch-${RELEASE_VERSION}> in case of not fully merged"
      continueprompt
      git branch -D branch-${RELEASE_VERSION}
    fi
    git push upstream --delete branch-${RELEASE_VERSION}
    git tag -d v${RELEASE_VERSION}-incubating
    git push upstream --delete v${RELEASE_VERSION}-incubating
    NEXT_VER_COMMIT_PREFIX="Stay version"
fi

echo "Preparing to create a branch branch-${RELEASE_VERSION} for release"
continueprompt

git checkout -b branch-${RELEASE_VERSION} || { echo "Create branch failed"; exit; }

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}-incubating
git commit . -m "Prepare for releasing ${RELEASE_VERSION}-incubating ${RELEASE_CANDIDATE_ID}"

git tag -s v${RELEASE_VERSION}-incubating -m "Releasing ${RELEASE_VERSION}-incubating ${RELEASE_CANDIDATE_ID}"

rm -rf target/
git clean -xdf

mvn clean prepare-package -DskipTests -Dremoteresources.skip=true &&
mvn prepare-package -DskipTests -Dremoteresources.skip=true &&
mvn deploy -DskipTests -Dremoteresources.skip=true -P apache-release || { echo "Preparation failed"; exit; }

RELEASEBASENAME=apache-mnemonic-${RELEASE_VERSION}-incubating-src
RELEASEFULLNAME=${RELEASEBASENAME}.tar.gz
pushd target || { echo "Artifacts not found"; exit; }
md5sum ${RELEASEFULLNAME} > ${RELEASEFULLNAME}.md5 || { echo "Generate md5 failed"; exit; }
shasum -a 512 ${RELEASEFULLNAME} > ${RELEASEFULLNAME}.sha512 || { echo "Generate sha failed"; exit; }
popd

echo "Prepared Artifacts:"
echo `ls target/${RELEASEFULLNAME}`
echo `ls target/${RELEASEFULLNAME}.asc`
echo `ls target/${RELEASEFULLNAME}.md5`
echo `ls target/${RELEASEFULLNAME}.sha512`
echo "Please upload those artifacts to your stage repository now."
continueprompt

#---------------
echo "Push release branch & label to upstream branch <branch-${RELEASE_VERSION}>."
continueprompt

git push upstream branch-${RELEASE_VERSION}
git push upstream v${RELEASE_VERSION}-incubating

echo "Merge release branch <branch-${RELEASE_VERSION}> to master & Commit next version <${NEXT_RELEASE_VERSION}-incubating-SNAPSHOT>."
continueprompt

git checkout master
git merge --no-ff branch-${RELEASE_VERSION}
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${NEXT_RELEASE_VERSION}-incubating-SNAPSHOT
git commit . -m "${NEXT_VER_COMMIT_PREFIX} to ${NEXT_RELEASE_VERSION}-incubating-SNAPSHOT"

echo "Push release merge and new version to upstream."
continueprompt

git push upstream master

