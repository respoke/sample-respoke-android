package com.digium.respoke;

import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeEndpoint;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 *
 */
public class CallActivity extends Activity implements RespokeCall.Listener {

    private final static String TAG = "CallActivity";
    private boolean audioOnly;
    private RespokeCall call;
    private RespokeEndpoint remoteEndpoint;
    private boolean audioMuted;
    private boolean videoMuted;
    private ImageButton muteAudioButton;
    private ImageButton muteVideoButton;
    private TextView connectingTextView;
    private ProgressBar progressCircle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call);

        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.videoview);
        muteAudioButton = (ImageButton) findViewById(R.id.mute_audio_button);
        muteVideoButton = (ImageButton) findViewById(R.id.mute_video_button);
        connectingTextView = (TextView) findViewById(R.id.connecting_text_view);
        progressCircle = (ProgressBar) findViewById(R.id.progress_circle);

        String remoteEndpointID = null;
        String callID = null;

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            remoteEndpointID = savedInstanceState.getString("endpointID");
            callID = savedInstanceState.getString("endpointID");
            audioOnly = savedInstanceState.getBoolean("audioOnly");

            remoteEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(remoteEndpointID, true);
            call = ContactManager.sharedInstance().sharedClient.callWithID(callID);

            //todo: tell the call about the new video view
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                remoteEndpointID = extras.getString("endpointID");
                callID = extras.getString("callID");
                audioOnly = extras.getBoolean("audioOnly");

                if (null != callID) {
                    call = ContactManager.sharedInstance().sharedClient.callWithID(callID);
                }

                remoteEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(remoteEndpointID, true);

                if (null == call) {
                    if (null != remoteEndpoint) {
                        call = remoteEndpoint.startCall(this, this, videoView, audioOnly);
                    }
                } else {
                    remoteEndpoint = call.endpoint;
                    call.answer(this, this, videoView);
                }
            }
        }

        if (audioOnly) {
            muteVideoButton.setVisibility(View.INVISIBLE);
        }

        if (null == remoteEndpoint) {
            Log.d(TAG, "Couldn't find endpoint record! Aborting call");
            finish();
        } else {
            this.setTitle("Call With " + remoteEndpoint.getEndpointID());
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("endpointID", remoteEndpoint.getEndpointID());
        savedInstanceState.putString("callID", call.getSessionID());
        savedInstanceState.putBoolean("audioOnly", audioOnly);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onPause() {
        super.onPause();

        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.videoview);
        if (null != videoView) {
            videoView.onPause();
        }

        call.pause();
    }

    @Override
    public void onResume() {
        super.onResume();

        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.videoview);
        if (videoView != null) {
            videoView.onResume();
        }

        call.resume();
    }


    @Override
    public void onBackPressed() {
        call.hangup(true);

        super.onBackPressed();
    }


    public void hangup(View view) {
        call.hangup(true);

        finish();
    }


    public void muteAudio(View view) {
        audioMuted = !audioMuted;
        call.muteAudio(audioMuted);

        if (audioMuted) {
            muteAudioButton.setImageResource(R.drawable.unmute_audio);
            muteAudioButton.setSelected(true);
            muteAudioButton.invalidate();
        } else {
            muteAudioButton.setImageResource(R.drawable.mute_audio);
            muteAudioButton.setSelected(false);
            muteAudioButton.invalidate();
        }
    }


    public void muteVideo(View view) {
        videoMuted = !videoMuted;
        call.muteVideo(videoMuted);

        if (videoMuted) {
            muteVideoButton.setImageResource(R.drawable.unmute_video);
            muteVideoButton.setSelected(true);
            muteVideoButton.invalidate();
        } else {
            muteVideoButton.setImageResource(R.drawable.mute_video);
            muteVideoButton.setSelected(false);
            muteVideoButton.invalidate();
        }
    }


    public void onError(final String errorMessage, RespokeCall sender) {
        Log.d(TAG, errorMessage);
        // Update UI on main thread
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getBaseContext());

                // set title
                alertDialogBuilder.setTitle("Call Error");

                // set dialog message
                alertDialogBuilder
                        .setMessage(errorMessage)
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, close
                                // current activity
                                CallActivity.this.finish();
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });*/
    }


    public void onHangup(RespokeCall sender) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }


    public void onConnected(RespokeCall sender) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectingTextView.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
            }
        });
    }


}