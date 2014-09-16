package com.digium.respoke;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeClientDelegate;
import com.digium.respokesdk.RespokeGroup;
import com.digium.respokesdk.RespokeJoinGroupCompletionDelegate;
import com.digium.respokesdk.RespokeTaskCompletionDelegate;
import com.digium.respokesdk.RestAPI.*;


public class ConnectActivity extends Activity implements RespokeClientDelegate {

    private static final String RESPOKE_SETTINGS = "RESPOKE_SETTINGS";
    private static final String LAST_USER_KEY = "LAST_USER_KEY";
    private static final String LAST_GROUP_KEY = "LAST_GROUP_KEY";
    private static final String LAST_APP_ID_KEY = "LAST_APP_ID_KEYs";

    private EditText endpointTextBox = null;
    private EditText groupTextBox = null;
    private APIGetToken apiGetToken = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        endpointTextBox = (EditText)findViewById(R.id.editText1);
        groupTextBox = (EditText)findViewById(R.id.editText2);

        SharedPreferences settings = this.getApplicationContext().getSharedPreferences(RESPOKE_SETTINGS, 0);
        String lastUserID = settings.getString(LAST_USER_KEY, null);
        String lastGroupID = settings.getString(LAST_GROUP_KEY, null);

        if (null != lastUserID) {
            endpointTextBox.setText(lastUserID);
        }

        if (null != lastGroupID) {
            groupTextBox.setText(lastGroupID);
        }
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


    public void connect(View view) {
        String endpointID = endpointTextBox.getText().toString();
        String groupID = groupTextBox.getText().toString();
        String appID = "2b446810-6d92-4fa4-826a-2eabced82d60";

        SharedPreferences settings = this.getApplicationContext().getSharedPreferences(RESPOKE_SETTINGS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(LAST_USER_KEY, endpointID).apply();
        editor.putString(LAST_GROUP_KEY, groupID).apply();

        ContactManager.sharedInstance().sharedClient = Respoke.sharedInstance().createClient();
        ContactManager.sharedInstance().sharedClient.delegate = this;
        ContactManager.sharedInstance().sharedClient.connect(endpointID, appID, true, null, this.getApplicationContext(), new RespokeTaskCompletionDelegate() {
            @Override
            public void onSuccess() {
                // Do nothing. The onConnect delegate method will be called if successful
            }

            @Override
            public void onError(String errorMessage) {

            }
        });
    }


    // RespokeClientDelegate methods


    public void onConnect(RespokeClient sender) {
        String defaultGroupID = "RespokeTeam";
        String groupID = groupTextBox.getText().toString();

        if ((groupID == null) && (groupID.length() <= 0)) {
            groupID = defaultGroupID;
        }

        ContactManager.sharedInstance().joinGroup(groupID, new RespokeTaskCompletionDelegate() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(String errorMessage) {

            }
        });
    }


    public void onDisconnect(RespokeClient sender, boolean reconnecting) {

    }


    public void onError(RespokeClient sender, String errorMessage) {

    }


    public void onCall(RespokeClient sender, RespokeCall call) {

    }
}
