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

usage() {
    echo "Usage: $0 Release_Version Next_Release_Version Candidate_Id skipTestRun[yes|no]"
    echo "e.g. $0 0.2.0 0.2.0 rc2 no"
    echo "     $0 0.2.0 0.2.1 rc3 yes"
    exit 1
}

continueprompt() {
    while true; do
        read -p "Do you wish to continue [y/n] ? " yn
        case $yn in
            [Yy]* ) break;;
            [Nn]* ) exit 2;;
            * ) echo "Please answer yes or no.";;
            esac
        done
}

if [ -z "${MNEMONIC_HOME}" ]; then
  source "$(dirname "$0")/find-mnemonic-home.sh" || { echo "Not found find-mnemonic-home.sh script."; exit 10; }
fi
pushd "$MNEMONIC_HOME" || { echo "the environment variable \$MNEMONIC_HOME contains invalid home directory of Mnemonic project."; exit 11; }

[[ -n "$(git status --porcelain)" ]] &&
    echo "please commit all changes first." && exit 20

[[ $# -lt 3 ]]  && usage

RELEASE_VERSION="$1"
NEXT_RELEASE_VERSION="$2"
RELEASE_CANDIDATE_ID="$3"
SKIP_TEST_RUN="${4:-no}"
IS_SAME_VERSION=false

echo "You have specified:"
echo "RELEASE_VERSION = ${RELEASE_VERSION}"
echo "NEXT_RELEASE_VERSION = ${NEXT_RELEASE_VERSION}"
echo "RELEASE_CANDIDATE_ID = ${RELEASE_CANDIDATE_ID}"
echo "SKIP_TEST_RUN = ${SKIP_TEST_RUN}"
if [ "${SKIP_TEST_RUN}" == "no" ]; then
    echo "It will take long time to run code tests. You can skip it if appropriate, please refer to usage."
else
    echo "The test run will be skipped as specified."
fi
echo "NOTE: Please ensure there are no uncommitted or untracked files in your local workplace/repo. before continue"
continueprompt

git checkout master

if [ "${RELEASE_VERSION}" == "${NEXT_RELEASE_VERSION}" ]; then
    IS_SAME_VERSION=true
    echo "You are trying to prepare a same version candidate so going to clean up existing branch <branch-${RELEASE_VERSION}> and tag <v${RELEASE_VERSION}> if exists"
    continueprompt
    git branch -d branch-${RELEASE_VERSION}
    if [ $? -ne 0 ]; then
        echo "Request to forcedly delete existing branch <branch-${RELEASE_VERSION}> in case of not fully merged"
        continueprompt
        git branch -D branch-${RELEASE_VERSION}
    fi
    git push upstream --delete branch-${RELEASE_VERSION}
    git tag -d v${RELEASE_VERSION}
    git push upstream --delete v${RELEASE_VERSION}
fi

echo "Preparing to create a branch branch-${RELEASE_VERSION} for release"
continueprompt

git checkout -b branch-${RELEASE_VERSION} || { echo "Create branch failed"; exit 30; }

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}
git commit . -m "Prepare for releasing ${RELEASE_VERSION} ${RELEASE_CANDIDATE_ID}"

git tag -s v${RELEASE_VERSION} -m "Release ${RELEASE_VERSION} ${RELEASE_CANDIDATE_ID}" ||
    { echo "Tagging with signing failed"; exit 35; }

rm -rf target/
git clean -xdf

mvn clean prepare-package -DskipTests -Dremoteresources.skip=true &&
mvn prepare-package -DskipTests -Dremoteresources.skip=true &&
mvn deploy -DskipTests -Dremoteresources.skip=true -P apache-release || { echo "Preparation failed"; exit 40; }

RELEASEBASENAME=apache-mnemonic-${RELEASE_VERSION}
RELEASESRCBASENAME=${RELEASEBASENAME}-src
RELEASESRCPKGFULLNAME=${RELEASESRCBASENAME}.tar.gz

pushd target || { echo "Artifacts not found"; exit 50; }
md5sum ${RELEASESRCPKGFULLNAME} > ${RELEASESRCPKGFULLNAME}.md5 || { echo "Generate md5 failed"; exit 60; }
shasum -a 512 ${RELEASESRCPKGFULLNAME} > ${RELEASESRCPKGFULLNAME}.sha512 || { echo "Generate sha failed"; exit 70; }
popd

echo "Verifying packaged Source Artifacts"
rm -rf ${RELEASEBASENAME}/
tar xzf target/${RELEASESRCPKGFULLNAME} || { echo "Failed to unpack the source artifact"; exit 80; }
pushd ${RELEASEBASENAME} || { echo "Unpacked source directory does not exist"; exit 90; }
mvn clean install || { echo "Failed to compile the packaged source artifact"; exit 100; }
if [ "${SKIP_TEST_RUN}" == "no" ]; then
    python bin/runTestCases.py || { echo "Failed to verify the packaged source artifact"; exit 110; }
fi
popd
rm -rf ${RELEASEBASENAME}/

echo "Prepared Artifacts:"
echo `ls target/${RELEASESRCPKGFULLNAME}`
echo `ls target/${RELEASESRCPKGFULLNAME}.asc`
echo `ls target/${RELEASESRCPKGFULLNAME}.md5`
echo `ls target/${RELEASESRCPKGFULLNAME}.sha512`
echo "Please upload those artifacts to your stage repository now."
continueprompt

#---------------
echo "Push release branch & label to upstream branch <branch-${RELEASE_VERSION}>."
continueprompt

git push upstream branch-${RELEASE_VERSION}
git push upstream v${RELEASE_VERSION}

echo "Merge release branch <branch-${RELEASE_VERSION}> to master & Commit next version <${NEXT_RELEASE_VERSION}-SNAPSHOT>."
continueprompt

git checkout master
git merge --no-ff branch-${RELEASE_VERSION}

if [ "$IS_SAME_VERSION" = true ]; then
    NEXT_RELEASE_VERSION_POM="${RELEASE_VERSION}-SNAPSHOT"
    NEXT_RELEASE_VERSION_COMMIT="Version ${RELEASE_VERSION} ${RELEASE_CANDIDATE_ID}"
else
    NEXT_RELEASE_VERSION_POM="${NEXT_RELEASE_VERSION}-SNAPSHOT"
    NEXT_RELEASE_VERSION_COMMIT="Bump version to ${NEXT_RELEASE_VERSION}-SNAPSHOT"
fi
mvn versions:set -DgenerateBackupPoms=false -DnewVersion="${NEXT_RELEASE_VERSION_POM}"
git commit . -m "${NEXT_RELEASE_VERSION_COMMIT}"

echo "Push release merge and new version to upstream."
continueprompt

git push upstream master

popd
