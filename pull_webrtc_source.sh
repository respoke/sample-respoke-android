# !/bin/bash

# Copyright 2015, Digium, Inc.
# All rights reserved.
#
# This source code is licensed under The MIT License found in the
# LICENSE file in the root directory of this source tree.
#
# For all details and documentation:  https://www.respoke.io

WEBRTC_REVISION=7780

echo "--- Pulling WebRTC source code for revision $WEBRTC_REVISION"
gclient sync --force -r $WEBRTC_REVISION

echo "--- Finished pulling WebRTC source"
