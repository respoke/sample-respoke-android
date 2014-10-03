package com.digium.respoke;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeGroup;


public class GroupChatActivity extends Activity {

    private final static String TAG = "GroupChatActivity";
    private RespokeGroup group;
    public Conversation conversation;
    private ListDataAdapter listAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        Button buttonSend = (Button) findViewById(R.id.buttonSend);

        EditText chatText = (EditText) findViewById(R.id.chatText);
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendChatMessage();
                    return true;
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendChatMessage();
            }
        });

        String groupID = null;

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            groupID = savedInstanceState.getString("groupID");
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                groupID = extras.getString("groupID");
            }
        }

        this.setTitle(groupID);

        for (RespokeGroup eachGroup : ContactManager.sharedInstance().groups) {
            if (eachGroup.getGroupID().equals(groupID)) {
                group = eachGroup;
                break;
            }
        }

        conversation = ContactManager.sharedInstance().groupConversations.get(groupID);

        listAdapter = new ListDataAdapter();

        ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
        lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("groupID", group.getGroupID());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.group_chat, menu);
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

        IntentFilter iff = new IntentFilter(ContactManager.GROUP_MESSAGE_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataInvalidatedReceiver, iff);

        conversation.unreadCount = 0;
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
                String groupID = extras.getString("groupID");

                if (groupID.equals(group.getGroupID())) {
                    // Tell the ListView to reconfigure itself based on the new data
                    listAdapter.notifyDataSetChanged();
                    listAdapter.notifyDataSetInvalidated();
                    conversation.unreadCount = 0;
                }
            }
        }
    };


    private void sendChatMessage() {
        EditText chatText = (EditText) findViewById(R.id.chatText);
        String message = chatText.getText().toString();

        if (message.length() > 0) {
            chatText.setText("");
            conversation.addMessage(message, ContactManager.sharedInstance().username);

            // Tell the ListView to reconfigure itself based on the new data
            listAdapter.notifyDataSetChanged();
            listAdapter.notifyDataSetInvalidated();

            group.sendMessage(message, new Respoke.TaskCompletionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "message sent");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.d(TAG, "Error sending message!");
                }
            });
        }
    }


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
