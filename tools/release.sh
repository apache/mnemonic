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
    echo "Usage: $0 command <args>..."
    echo "$0 candidate <release_version> <candidate_id> <skip_test_run[yes|no]>"
    echo "This command is used to create a specified candidate for release"
    echo "e.g. $0 candidate 0.2.0 rc2 no"
    echo "     $0 candidate 0.2.0 rc3 yes"
    echo "$0 bump <release_version> <candidate_id> <new_version>"
    echo "This command is used to bring the candidate into effect and bump into new version"
    echo "e.g. $0 bump 0.2.0 rc2 0.3.0"
    echo "     $0 bump 0.2.0 rc3 0.3.0"
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

purge_candidate_branch() {
    git branch -d ${CANDIDATE_BRANCH_NAME}
    if [ $? -ne 0 ]; then
        echo "Request to forcedly delete existing branch ${CANDIDATE_BRANCH_NAME} in case of not fully merged"
        continueprompt
        git branch -D ${CANDIDATE_BRANCH_NAME}
    fi
    git push upstream --delete ${CANDIDATE_BRANCH_NAME}
    git tag -d ${CANDIDATE_TAG_NAME}
    git push upstream --delete ${CANDIDATE_TAG_NAME}
}

build_candidate_branch() {
    git checkout -b ${CANDIDATE_BRANCH_NAME} || { echo "Create branch failed"; exit 30; }

    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION} &&
    git commit . -m "Prepare for a release v${RELEASE_VERSION}" ||
        { echo "Set release version failed"; exit 33; }

    git tag -s ${CANDIDATE_TAG_NAME} -m "A release candidate ${CANDIDATE_TAG_NAME}" ||
        { echo "Tagging with signing failed"; exit 35; }

    rm -rf target/
    git clean -xdf

    mvn clean prepare-package -DskipTests -Dremoteresources.skip=true &&
    mvn deploy -DskipTests -Dremoteresources.skip=true -P apache-release || { echo "Preparation failed"; exit 40; }

    RELEASEBASENAME=apache-mnemonic-${RELEASE_VERSION}
    RELEASESRCBASENAME=${RELEASEBASENAME}-src
    RELEASESRCPKGFULLNAME=${RELEASESRCBASENAME}.tar.gz

    pushd target || { echo "Generated artifacts not found"; exit 50; }
    md5sum ${RELEASESRCPKGFULLNAME} > ${RELEASESRCPKGFULLNAME}.md5 || { echo "Generate md5 failed"; exit 60; }
    shasum -a 512 ${RELEASESRCPKGFULLNAME} > ${RELEASESRCPKGFULLNAME}.sha512 || { echo "Generate sha failed"; exit 70; }
    popd

    echo "Verifying packaged Source Artifacts"
    rm -rf ${RELEASEBASENAME}/
    tar xzf target/${RELEASESRCPKGFULLNAME} || { echo "Failed to unpack the source artifact"; exit 80; }
    pushd ${RELEASEBASENAME} || { echo "Unpacked source directory does not exist"; exit 90; }
    mvn clean install || { echo "Failed to compile the packaged source artifact"; exit 100; }
    if [ "${SKIP_TEST_RUN}" == "no" ]; then
        python tools/runTestCases.py || { echo "Failed to verify the packaged source artifact"; exit 110; }
    fi
    popd
    rm -rf ${RELEASEBASENAME}/

    gpg --verify target/${RELEASESRCPKGFULLNAME}.asc target/${RELEASESRCPKGFULLNAME} || { echo "The signature of target/${RELEASESRCPKGFULLNAME} is invalid."; exit 120; }

    echo "Prepared Artifacts:"
    echo `ls target/${RELEASESRCPKGFULLNAME}`
    echo `ls target/${RELEASESRCPKGFULLNAME}.asc`
    echo `ls target/${RELEASESRCPKGFULLNAME}.md5`
    echo `ls target/${RELEASESRCPKGFULLNAME}.sha512`
    echo "Please upload those artifacts to your stage repository now."
    continueprompt

    #---------------
    echo "Push release candidate branch & tag to upstream."
    continueprompt

    git push upstream ${CANDIDATE_BRANCH_NAME} &&
    git push upstream ${CANDIDATE_TAG_NAME} ||
        { echo "Push the release candidate branch & tag to upstream failed."; exit 130; }

    echo "The release candidate branch ${CANDIDATE_BRANCH_NAME} and tag ${CANDIDATE_TAG_NAME} has been built and got upstreamed."
}

if [ -z "${MNEMONIC_HOME}" ]; then
    source "$(dirname "$0")/find-mnemonic-home.sh" || { echo "Not found find-mnemonic-home.sh script."; exit 10; }
fi
pushd "$MNEMONIC_HOME" || { echo "the environment variable \$MNEMONIC_HOME contains invalid home directory of Mnemonic project."; exit 11; }

if git remote get-url --push upstream | grep -q "apache/mnemonic"; then
    echo "Upstream push URL: $(git remote get-url --push upstream)"
else
    echo "Upstream push URL is not set correctly, please find one in https://github.com/apache/mnemonic"
    exit 25;
fi

[[ -n "$(git status --porcelain)" ]] &&
    echo "please commit all changes first." && exit 20

[[ $# -lt 3 ]]  && usage

echo "NOTE: Please ensure to backup all uncommitted or untracked files, and a remote 'upstream' has got set up."

RELEASE_COMMAND="$1"
RELEASE_VERSION="$2"
CANDIDATE_ID="$3"
if [ -z ${JAVA_HOME} ]; then
    JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"
    export JAVA_HOME
fi

CANDIDATE_BRANCH_NAME="branch-${RELEASE_VERSION}-${CANDIDATE_ID}"
CANDIDATE_TAG_NAME="v${RELEASE_VERSION}-${CANDIDATE_ID}"
RELEASE_TAG_NAME="v${RELEASE_VERSION}"

echo "You have specified:"
echo "JAVA_HOME = ${JAVA_HOME}"
echo "RELEASE_COMMAND = ${RELEASE_COMMAND}"
echo "RELEASE_VERSION = ${RELEASE_VERSION}"
echo "CANDIDATE_ID = ${CANDIDATE_ID}"

if [ ${RELEASE_COMMAND} == "candidate" ]; then
    SKIP_TEST_RUN="${4:-no}"
    echo "SKIP_TEST_RUN = ${SKIP_TEST_RUN}"
    if [ "${SKIP_TEST_RUN}" == "no" ]; then
        echo "It will take long time to run code tests. You can skip it if appropriate, please refer to usage."
    else
        echo "The test run will be skipped as specified."
    fi
    continueprompt

    git checkout master

    if git rev-list -n 1 ${CANDIDATE_BRANCH_NAME} -- 2> /dev/null; then
        echo "Found the candidate branch ${CANDIDATE_BRANCH_NAME} that needs to be purged from local and remote."
        continueprompt
        purge_candidate_branch
    fi

    echo "Preparing to create a candidate branch ${CANDIDATE_BRANCH_NAME} for release"
    continueprompt

    build_candidate_branch

    exit 0;
fi

if [ ${RELEASE_COMMAND} == "bump" ]; then
    NEW_VERSION="$4"
    echo "NEW_VERSION = ${NEW_VERSION}"
    continueprompt

    git checkout master

    echo "Merge release candidate branch ${CANDIDATE_BRANCH_NAME} to master."
    continueprompt
    git merge --ff ${CANDIDATE_BRANCH_NAME} || { echo "The release candidate branch doesn't exist or not the latest candidate."; exit 210; }

    BUMP_VERSION="${NEW_VERSION}-SNAPSHOT"
    mvn versions:set -DgenerateBackupPoms=false -DnewVersion="${BUMP_VERSION}" &&
    git commit . -m "Bump version to ${BUMP_VERSION}" ||
         { echo "Set bump version failed"; exit 220; }

    if git rev-list -n 1 tags/${CANDIDATE_TAG_NAME} -- 2> /dev/null; then
        REVISION_ID=$(git rev-list -n 1 tags/${CANDIDATE_TAG_NAME} --)
    else
        echo "Cannot find the revision of tag ${CANDIDATE_TAG_NAME}."; exit 230;
    fi
    git tag -s ${RELEASE_TAG_NAME} -m "A release ${RELEASE_TAG_NAME}" ${REVISION_ID} ||
        { echo "Tagging with signing failed"; exit 240; }

    echo "Push the effective release and bump version to upstream."
    continueprompt

    git push upstream master &&
    git push upstream ${RELEASE_TAG_NAME} ||
        { echo "Push new release to upstream failed."; exit 250; }

    exit 0;
fi

usage

popd
