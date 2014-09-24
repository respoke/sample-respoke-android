package com.digium.respoke;

import com.digium.respoke.util.SystemUiHider;
import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeCallDelegate;
import com.digium.respokesdk.RespokeEndpoint;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import org.webrtc.PeerConnectionFactory;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class CallActivity extends Activity implements RespokeCallDelegate {

    private final static String TAG = "CallActivity";
    private boolean audioOnly;
    private RespokeCall call;
    private RespokeEndpoint remoteEndpoint;
    private CallVideoView videoView;
    private boolean audioMuted;
    private boolean videoMuted;
    private ImageButton muteAudioButton;
    private ImageButton muteVideoButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call);

        videoView = (CallVideoView) findViewById(R.id.videoview);
        muteAudioButton = (ImageButton) findViewById(R.id.mute_audio_button);
        muteVideoButton = (ImageButton) findViewById(R.id.mute_video_button);

        /*Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(displaySize);

        videoView.updateDisplaySize(displaySize);*/

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String remoteEndpointID = extras.getString("endpointID");
            String callID = extras.getString("callID");
            audioOnly = extras.getBoolean("audioOnly");

            if (null != callID) {
                call = ContactManager.sharedInstance().sharedClient.callWithID(callID);
            }

            remoteEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(remoteEndpointID, true);

            if (null == call) {
                call = remoteEndpoint.startCall(this, this, videoView, audioOnly);
            } else {
                remoteEndpoint = call.endpoint;
                call.answer(this, this, videoView);
            }
        }

        this.setTitle("Call With " + remoteEndpoint.getEndpointID());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

    @Override
    public void onPause() {
        super.onPause();
        videoView.onPause();
        call.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoView.onResume();
        call.resume();
    }


    @Override
    public void onBackPressed()
    {
        call.hangup(true);
        super.onBackPressed();
    }


    /*@Override
    public void onConfigurationChanged (Configuration newConfig) {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        videoView.updateDisplaySize(displaySize);
        Log.d(TAG, "configurationChanged: " + displaySize.x + " " + displaySize.y);
        super.onConfigurationChanged(newConfig);
    }*/


    public void hangup(View view) {
        videoView.onPause();
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
        runOnUiThread(new Runnable() {
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
        });
    }


    public void onHangup(RespokeCall sender) {
        finish();
    }


    public void onConnected(RespokeCall sender) {

    }
}
