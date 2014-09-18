package com.digium.respoke;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;


public class ChatActivity extends Activity {

    public Conversation conversation;
    private ListDataAdapter listAdapter;
    private String remoteEndpointID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            remoteEndpointID = extras.getString("endpointID");
            conversation = ContactManager.sharedInstance().conversations.get(remoteEndpointID);
            setTitle(remoteEndpointID);

            listAdapter = new ListDataAdapter();

            ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
            lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
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
    protected void onResume() {
        super.onResume();

        IntentFilter iff = new IntentFilter(ContactManager.ENDPOINT_MESSAGE_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataInvalidatedReceiver, iff);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        LocalBroadcastManager.getInstance(this).unregisterReceiver(contactDataInvalidatedReceiver);
    }


    private BroadcastReceiver contactDataInvalidatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String endpointID = extras.getString("endpointID");

                if (endpointID.equals(remoteEndpointID)) {
                    // Tell the ListView to reconfigure itself based on the new data
                    listAdapter.notifyDataSetChanged();
                    listAdapter.notifyDataSetInvalidated();
                }
            }
        }
    };


    private class ListDataAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return conversation.messages.size();
        }


        @Override
        public Object getItem(int position) {
            return conversation.messages.get(position);
        }


        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public boolean isEnabled(int position) {
            return false;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ConversationMessage message = (ConversationMessage) getItem(position);

            if (message.senderEndpoint.equals(ContactManager.sharedInstance().username)) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_local_message, parent, false);

                TextView tvText = (TextView) v.findViewById(R.id.textView1);
                tvText.setText(message.message);

                return v;
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_remote_message, parent, false);

                TextView tvText = (TextView) v.findViewById(R.id.textView1);
                tvText.setText(message.message);

                return v;
            }
        }
    }
}
