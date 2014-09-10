WEBRTC_REVISION=6604

echo "--- Pulling WebRTC source code for revision $WEBRTC_REVISION"
gclient sync --force -r $WEBRTC_REVISION

echo "--- Finished pulling WebRTC source"