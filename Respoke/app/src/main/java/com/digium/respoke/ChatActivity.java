package com.digium.respoke;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
import com.digium.respokesdk.RespokeEndpoint;

import java.lang.ref.WeakReference;


public class ChatActivity extends FragmentActivity {

    private final static String TAG = "ChatActivity";
    public Conversation conversation;
    private ListDataAdapter listAdapter;
    private RespokeEndpoint remoteEndpoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Button buttonSend = (Button) findViewById(R.id.buttonSend);

        EditText chatText = (EditText) findViewById(R.id.chatText);
        chatText = (EditText) findViewById(R.id.chatText);
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

        String remoteEndpointID = null;

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            remoteEndpointID = savedInstanceState.getString("endpointID");
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                remoteEndpointID = extras.getString("endpointID");
            }
        }

        conversation = ContactManager.sharedInstance().conversations.get(remoteEndpointID);
        remoteEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(remoteEndpointID, true);
        setTitle(remoteEndpoint.getEndpointID());

        listAdapter = new ListDataAdapter();

        ListView lv = (ListView) findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
        lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("endpointID", remoteEndpoint.getEndpointID());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_call:
                ChatTypeDialog dialog = new ChatTypeDialog();
                dialog.show(getFragmentManager(), "chat_type");
                return true;

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter iff = new IntentFilter(ContactManager.ENDPOINT_MESSAGE_RECEIVED);
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
                String endpointID = extras.getString("endpointID");

                if (endpointID.equals(remoteEndpoint.getEndpointID())) {
                    // Tell the ListView to reconfigure itself based on the new data
                    listAdapter.notifyDataSetChanged();
                    listAdapter.notifyDataSetInvalidated();
                    conversation.unreadCount = 0;

                    final ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
                    lv.post(new Runnable() {
                        @Override
                        public void run() {
                            // Select the last row so it will scroll into view...
                            lv.setSelection(listAdapter.getCount() - 1);
                        }
                    });
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

            final ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
            lv.post(new Runnable() {
                @Override
                public void run() {
                    // Select the last row so it will scroll into view...
                    lv.setSelection(listAdapter.getCount() - 1);
                }
            });

            remoteEndpoint.sendMessage(message, new Respoke.TaskCompletionListener() {
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


    public static class ChatTypeDialog extends DialogFragment {
        private WeakReference<ChatActivity> mActivityReference;


        @Override
        public void onAttach(Activity activity)
        {
            if (activity instanceof ChatActivity)
            {
                mActivityReference = new WeakReference<ChatActivity>((ChatActivity) activity);
            }

            super.onAttach(activity);
        }


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_call)
                    .setItems(R.array.call_types, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ChatActivity mActivity = mActivityReference.get();

                            if (null != mActivity) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                if (which == 0) {
                                    dismiss();
                                    Intent i = new Intent(mActivity, CallActivity.class);
                                    i.putExtra("endpointID", mActivity.remoteEndpoint.getEndpointID());
                                    i.putExtra("audioOnly", false);
                                    startActivity(i);
                                } else if (which == 1) {
                                    dismiss();
                                    Intent i = new Intent(mActivity, CallActivity.class);
                                    i.putExtra("endpointID", mActivity.remoteEndpoint.getEndpointID());
                                    i.putExtra("audioOnly", true);
                                    startActivity(i);
                                } else {
                                    dismiss();
                                }
                            } else {
                                dismiss();
                            }
                        }
                    });
            return builder.create();
        }
    }
}
