package com.digium.respoke;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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


public class ConnectActivity extends Activity implements RespokeClient.Listener, View.OnKeyListener, TextWatcher {

    private static final String TAG = "ConnectActivity";
    private static final String RESPOKE_SETTINGS = "RESPOKE_SETTINGS";
    private static final String LAST_USER_KEY = "LAST_USER_KEY";
    private static final String LAST_GROUP_KEY = "LAST_GROUP_KEY";
    private static final String LAST_APP_ID_KEY = "LAST_APP_ID_KEYs";

    private boolean isConnecting = false;
    private boolean brokeredAuthOn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        EditText endpointTextBox = (EditText)findViewById(R.id.editText1);
        EditText groupTextBox = (EditText)findViewById(R.id.editText2);
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
                    case R.id.editText1:
                        EditText groupTextBox = (EditText)findViewById(R.id.editText2);
                        groupTextBox.requestFocus();
                        break;
                    case R.id.editText2:
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


    public void connect(View view) {
        Button connectButton = (Button)findViewById(R.id.button1);
        ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
        EditText endpointTextBox = (EditText)findViewById(R.id.editText1);
        EditText groupTextBox = (EditText)findViewById(R.id.editText2);
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
                    ContactManager.sharedInstance().sharedClient.connect(endpointID, null, this, new Respoke.TaskCompletionListener() {
                        @Override
                        public void onSuccess() {
                            // Do nothing. The onConnect delegate method will be called if successful
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                } else {
                    String appID = "2b446810-6d92-4fa4-826a-2eabced82d60";

                    ContactManager.sharedInstance().sharedClient.connect(endpointID, appID, true, null, this.getApplicationContext(), new Respoke.TaskCompletionListener() {
                        @Override
                        public void onSuccess() {
                            // Do nothing. The onConnect delegate method will be called if successful
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                }
            } else {
                endpointTextBox.requestFocus();
                errorMessageView.setText("Username may not be blank");
            }
        }
    }


    public void showError(final String message) {
        isConnecting = false;

        Button connectButton = (Button)findViewById(R.id.button1);
        ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
        TextView errorMessageView = (TextView)findViewById(R.id.error_message);

        progressCircle.setVisibility(View.INVISIBLE);
        errorMessageView.setText(message);
        connectButton.setText("Connect");
    }


    public void onBrokeredAuthClicked(View view) {
        // Is the toggle on?
        brokeredAuthOn = ((ToggleButton) view).isChecked();
        EditText endpointTextBox = (EditText)findViewById(R.id.editText1);

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

        EditText groupTextBox = (EditText)findViewById(R.id.editText2);
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

                Button connectButton = (Button)findViewById(R.id.button1);
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


}
