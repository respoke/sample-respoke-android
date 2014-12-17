package com.digium.respoke;

import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeDirectConnection;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call);

        ImageButton muteVideoButton = (ImageButton) findViewById(R.id.mute_video_button);
        View answerView = findViewById(R.id.answer_view);
        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.videoview);

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
                    answerView.setVisibility(View.INVISIBLE);
                    if (null != remoteEndpoint) {
                        call = remoteEndpoint.startCall(this, this, videoView, audioOnly);
                    }
                } else {
                    answerView.setVisibility(View.VISIBLE);
                    remoteEndpoint = call.endpoint;
                    TextView callerNameView = (TextView) findViewById(R.id.caller_name_text);

                    if (null != remoteEndpoint) {
                        callerNameView.setText(remoteEndpoint.getEndpointID());
                    } else {
                        callerNameView.setText("Unknown Caller");
                    }

                    call.attachVideoRenderer(videoView);
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

        if (null != call) {
            call.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.videoview);
        if (videoView != null) {
            videoView.onResume();
        }

        if (null != call) {
            call.resume();
        }
    }


    @Override
    public void onBackPressed() {
        if (null != call) {
            call.hangup(true);
        }

        super.onBackPressed();
    }


    public void answerCall(View view) {
        View answerView = findViewById(R.id.answer_view);

        answerView.setVisibility(View.INVISIBLE);
        call.answer(this, this);
    }


    public void ignoreCall(View view) {
        hangup(view);
    }


    public void hangup(View view) {
        if (null != call) {
            call.hangup(true);
        }

        finish();
    }


    public void muteAudio(View view) {
        ImageButton muteAudioButton = (ImageButton) findViewById(R.id.mute_audio_button);

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
        ImageButton muteVideoButton = (ImageButton) findViewById(R.id.mute_video_button);

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
                TextView connectingTextView = (TextView) findViewById(R.id.connecting_text_view);
                ProgressBar progressCircle = (ProgressBar) findViewById(R.id.progress_circle);

                connectingTextView.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
            }
        });
    }


    public void directConnectionAvailable(RespokeDirectConnection directConnection, RespokeEndpoint endpoint) {

    }
}