package com.digium.respoke;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RestAPI.*;


public class ConnectActivity extends Activity {

    private EditText endpointTextBox = null;
    private EditText groupTextBox = null;
    private APIGetToken apiGetToken = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        endpointTextBox = (EditText)findViewById(R.id.editText1);
        groupTextBox = (EditText)findViewById(R.id.editText2);
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

        RespokeClient client = Respoke.sharedInstance().createClient();
        client.connect(endpointID, appID, true, null, this.getApplicationContext());
    }
}
