#!/bin/bash

SCRIPT="${0}"
SCRIPT_DIR=$(cd $(dirname ${SCRIPT}) && pwd)

WL_BASE_DIR=$(cd ${SCRIPT_DIR}/../.. && pwd)

PACK_NAME="wordland-client-pack"
BUILD_DIR="${WL_BASE_DIR}/wordland-server/target/${PACK_NAME}"
PACK_ZIP="$(dirname ${BUILD_DIR})/${PACK_NAME}.zip"

# start fresh
rm -rf ${BUILD_DIR} ${PACK_ZIP}
mkdir -p ${BUILD_DIR}

# copy from cli dir to build dir
cd ${BUILD_DIR}
cp -R ${WL_BASE_DIR}/cli/* ./

# remove temp files
find . -type f -name "*~" | xargs rm

# build the zip file
cd ${BUILD_DIR}/..
zip -r ${PACK_ZIP} ${PACK_NAME}
