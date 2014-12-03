WEBRTC_REVISION=7780

echo "--- Pulling WebRTC source code for revision $WEBRTC_REVISION"
gclient sync --force -r $WEBRTC_REVISION

echo "--- Finished pulling WebRTC source"
