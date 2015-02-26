package com.digium.respoke;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeDirectConnection;
import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;


public class ConnectActivity extends Activity implements RespokeClient.Listener, View.OnKeyListener, TextWatcher {

    private static final String TAG = "ConnectActivity";
    public static final String RESPOKE_SETTINGS = "RESPOKE_SETTINGS";
    private static final String LAST_USER_KEY = "LAST_USER_KEY";
    private static final String LAST_GROUP_KEY = "LAST_GROUP_KEY";
    private static final String LAST_APP_ID_KEY = "LAST_APP_ID_KEYs";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private boolean isConnecting = false;
    private boolean brokeredAuthOn = false;
    private static boolean registered = false;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "540194358645";

    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String regid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        EditText endpointTextBox = (EditText)findViewById(R.id.endpoint_id_text_box);
        EditText groupTextBox = (EditText)findViewById(R.id.group_id_text_box);
        TextView errorMessageView = (TextView)findViewById(R.id.error_message);

        // Give the Contact manager initial context
        ContactManager.sharedInstance().context = this.getApplicationContext();

        SharedPreferences settings = this.getApplicationContext().getSharedPreferences(RESPOKE_SETTINGS, 0);
        String lastUserID = settings.getString(LAST_USER_KEY, null);
        String lastGroupID = settings.getString(LAST_GROUP_KEY, null);

        if (null != lastUserID) {
            endpointTextBox.setText(lastUserID);
        }

        if (null != lastGroupID) {
            groupTextBox.setText(lastGroupID);
        }

        errorMessageView.setText("");

        endpointTextBox.setOnKeyListener(this);
        endpointTextBox.addTextChangedListener(this);
        groupTextBox.setOnKeyListener(this);
        groupTextBox.addTextChangedListener(this);

        context = getApplicationContext();

        // Only register for push notifications once
        if (!registered) {
            // Check device for Play Services APK. If check succeeds, proceed with
            //  GCM registration.
            if (checkPlayServices()) {
                gcm = GoogleCloudMessaging.getInstance(this);
                regid = getRegistrationId(context);

                if (regid.isEmpty()) {
                    registerInBackground();
                } else {
                    // We already have a token, so just send it to the Respoke push server
                    Respoke.sharedInstance().registerPushToken(regid);
                    registered = true;
                }
            } else {
                Log.i(TAG, "No valid Google Play Services APK found.");
            }
        }
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connect, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        EditText myEditText = (EditText) view;

        if (keyCode == EditorInfo.IME_ACTION_SEARCH ||
                keyCode == EditorInfo.IME_ACTION_DONE ||
                event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

            if (!event.isShiftPressed()) {
                switch (view.getId()) {
                    case R.id.endpoint_id_text_box:
                        EditText groupTextBox = (EditText)findViewById(R.id.group_id_text_box);
                        groupTextBox.requestFocus();
                        break;
                    case R.id.group_id_text_box:
                        connect(null);
                        break;
                }
                return true;
            }

        }
        return false; // pass on to other listeners.
    }


    // TextWatcher delegate methods


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        TextView errorMessageView = (TextView)findViewById(R.id.error_message);
        errorMessageView.setText("");
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }


    // Misc methods

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }


    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Object,String,Object>() {
            @Override
            protected Object doInBackground(Object[] objects) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    Respoke.sharedInstance().registerPushToken(regid);
                    registered = true;

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            protected void onPostExecute(String msg) {
                //mDisplay.append(msg + "\n");
                Log.d(TAG, msg);
            }
        }.execute(null, null, null);
    }


    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }


    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(ConnectActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }


    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }


    public void connect(View view) {
        Button connectButton = (Button)findViewById(R.id.connect_button);
        ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
        EditText endpointTextBox = (EditText)findViewById(R.id.endpoint_id_text_box);
        EditText groupTextBox = (EditText)findViewById(R.id.group_id_text_box);
        TextView errorMessageView = (TextView)findViewById(R.id.error_message);

        String endpointID = endpointTextBox.getText().toString();
        String groupID = groupTextBox.getText().toString();

        if (!isConnecting) {
            if (endpointID.length() > 0) {
                errorMessageView.setText("");
                connectButton.setText("");
                progressCircle.setVisibility(View.VISIBLE);

                SharedPreferences settings = this.getApplicationContext().getSharedPreferences(RESPOKE_SETTINGS, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(LAST_USER_KEY, endpointID).apply();
                editor.putString(LAST_GROUP_KEY, groupID).apply();

                isConnecting = true;

                ContactManager.sharedInstance().sharedClient = Respoke.sharedInstance().createClient(this);
                ContactManager.sharedInstance().sharedClient.setListener(this);

                if (brokeredAuthOn) {
                    // The text in the endpointTextBox is actually the Token ID in this case
                    ContactManager.sharedInstance().sharedClient.connect(endpointID, null, this, new RespokeClient.ConnectCompletionListener() {
                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                } else {
                    String appID = "7c15ec35-71a9-457f-8b73-97caf4eb43ca";

                    ContactManager.sharedInstance().sharedClient.connect(endpointID, appID, true, null, this.getApplicationContext(), new RespokeClient.ConnectCompletionListener() {
                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                }
            } else {
                endpointTextBox.requestFocus();
                errorMessageView.setText(R.string.endpoint_id_blank);
            }
        }
    }


    public void showError(final String message) {
        isConnecting = false;

        Button connectButton = (Button)findViewById(R.id.connect_button);
        ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
        TextView errorMessageView = (TextView)findViewById(R.id.error_message);

        progressCircle.setVisibility(View.INVISIBLE);
        errorMessageView.setText(message);
        connectButton.setText("Connect");
    }


    public void onBrokeredAuthClicked(View view) {
        // Is the toggle on?
        brokeredAuthOn = ((ToggleButton) view).isChecked();
        EditText endpointTextBox = (EditText)findViewById(R.id.endpoint_id_text_box);

        if (brokeredAuthOn) {
            // Enable Brokered Auth
            endpointTextBox.setHint("Token ID");
        } else {
            // Disable Brokered Auth
            endpointTextBox.setHint("Endpoint ID");
        }
    }


    // RespokeClientListener methods


    public void onConnect(RespokeClient sender) {
        Log.d(TAG, "Connected to Respoke! Joining group...");

        EditText groupTextBox = (EditText)findViewById(R.id.group_id_text_box);
        String defaultGroupID = "RespokeTeam";
        String groupID = groupTextBox.getText().toString();

        if ((groupID != null) && (groupID.length() <= 0)) {
            groupID = defaultGroupID;
        }

        ContactManager.sharedInstance().username = sender.getEndpointID();

        ContactManager.sharedInstance().joinGroup(groupID, new Respoke.TaskCompletionListener() {
            @Override
            public void onSuccess() {
                isConnecting = false;

                Intent i = new Intent(ConnectActivity.this, GroupListActivity.class);
                startActivity(i);

                Button connectButton = (Button)findViewById(R.id.connect_button);
                ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
                TextView errorMessageView = (TextView)findViewById(R.id.error_message);

                progressCircle.setVisibility(View.INVISIBLE);
                errorMessageView.setText("");
                connectButton.setText("Connect");
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }


    public void onDisconnect(RespokeClient sender, boolean reconnecting) {
        showError("Socket unexpectedly disconnected");
    }


    public void onError(RespokeClient sender, String errorMessage) {
        showError(errorMessage);
    }


    public void onCall(RespokeClient sender, RespokeCall call) {

    }


    public void onIncomingDirectConnection(RespokeDirectConnection directConnection, RespokeEndpoint endpoint) {

    }


    public void onMessage(String message, RespokeEndpoint sender, RespokeGroup group, Date timestamp) {

    }


}
