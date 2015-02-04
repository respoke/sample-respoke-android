package com.digium.respoke;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeDirectConnection;
import com.digium.respokesdk.RespokeEndpoint;

import java.lang.ref.WeakReference;


public class ChatActivity extends FragmentActivity implements RespokeDirectConnection.Listener, RespokeCall.Listener {

    private final static String TAG = "ChatActivity";
    private final static String ENDPOINT_ID_KEY = "endpointID";
    private final static String DIRECT_CONNECTION_KEY = "directConnection";
    public Conversation conversation;
    private ListDataAdapter listAdapter;
    private RespokeEndpoint remoteEndpoint;
    private RespokeDirectConnection directConnection;


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
        boolean shouldStartDirectConnection = false;

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            remoteEndpointID = savedInstanceState.getString(ENDPOINT_ID_KEY);
            shouldStartDirectConnection = savedInstanceState.getBoolean(DIRECT_CONNECTION_KEY, false);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                remoteEndpointID = extras.getString(ENDPOINT_ID_KEY);
                shouldStartDirectConnection = extras.getBoolean(DIRECT_CONNECTION_KEY, false);
            } else {
                // The activity must have been destroyed while it was hidden to save memory. Use the most recent persistent data.
                SharedPreferences prefs = getSharedPreferences(ConnectActivity.RESPOKE_SETTINGS, 0);
                remoteEndpointID = prefs.getString(ENDPOINT_ID_KEY, "");
                shouldStartDirectConnection = prefs.getBoolean(DIRECT_CONNECTION_KEY, false);
            }
        }

        conversation = ContactManager.sharedInstance().conversations.get(remoteEndpointID);
        remoteEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(remoteEndpointID, true);
        setTitle(remoteEndpoint.getEndpointID());

        listAdapter = new ListDataAdapter();

        ListView lv = (ListView) findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
        lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView

        if (shouldStartDirectConnection && (null == remoteEndpoint.directConnection())) {
            // If the direct connection has not been started yet, start it now
            remoteEndpoint.startDirectConnection();
        }

        directConnection = remoteEndpoint.directConnection();
        if (null != directConnection) {
            directConnection.setListener(this);
            RespokeCall call = directConnection.getCall();
            boolean caller = false;

            if (null != call) {
                call.setListener(this);
                caller = call.isCaller();
            }

            View answerView = findViewById(R.id.answer_view);
            View connectingView = findViewById(R.id.connecting_view);
            TextView callerNameView = (TextView) findViewById(R.id.caller_name_text);

            if (caller) {
                answerView.setVisibility(View.INVISIBLE);
                connectingView.setVisibility(View.VISIBLE);
            } else {
                answerView.setVisibility(View.VISIBLE);
                connectingView.setVisibility(View.INVISIBLE);
            }

            if (null != remoteEndpoint) {
                callerNameView.setText(remoteEndpoint.getEndpointID());
            } else {
                callerNameView.setText("Unknown Caller");
            }

            ActionBar actionBar = getActionBar();
            actionBar.setBackgroundDrawable(new ColorDrawable(R.color.incoming_connection_bg));
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        if (null != remoteEndpoint) {
            savedInstanceState.putString(ENDPOINT_ID_KEY, remoteEndpoint.getEndpointID());
        }

        if (null != directConnection) {
            savedInstanceState.putBoolean(DIRECT_CONNECTION_KEY, true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (null == directConnection) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.chat, menu);
        }
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
                if (null != directConnection) {
                    RespokeCall call = directConnection.getCall();

                    if (null != call) {
                        call.hangup(true);
                    }
                }

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
        listAdapter.notifyDataSetChanged();
        listAdapter.notifyDataSetInvalidated();
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(contactDataInvalidatedReceiver);

        // Save key information in case this activity is killed while it is not visible
        SharedPreferences prefs = getSharedPreferences(ConnectActivity.RESPOKE_SETTINGS, 0);
        SharedPreferences.Editor editor = prefs.edit();
        if (null != remoteEndpoint) {
            editor.putString(ENDPOINT_ID_KEY, remoteEndpoint.getEndpointID()).apply();
        }

        if (null != directConnection) {
            editor.putBoolean(DIRECT_CONNECTION_KEY, true);
        }
    }


    @Override
    public void onBackPressed() {
        if (null != directConnection) {
            RespokeCall call = directConnection.getCall();

            if (null != call) {
                call.hangup(true);
            }
        }

        super.onBackPressed();
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
            conversation.addMessage(message, ContactManager.sharedInstance().username, directConnection != null);

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

            if (null != directConnection) {
                directConnection.sendMessage(message, new Respoke.TaskCompletionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "direct message sent");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.d(TAG, "Error sending direct message! " + errorMessage);
                    }
                });
            } else {
                remoteEndpoint.sendMessage(message, new Respoke.TaskCompletionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "message sent");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.d(TAG, "Error sending message! " + errorMessage);
                    }
                });
            }
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

                ImageView lockView = (ImageView) v.findViewById(R.id.lockImage);
                lockView.setVisibility(message.direct ? View.VISIBLE : View.INVISIBLE);

                return v;
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_remote_message, parent, false);

                TextView tvText = (TextView) v.findViewById(R.id.textView1);
                tvText.setText(message.message);

                ImageView lockView = (ImageView) v.findViewById(R.id.lockImage);
                lockView.setVisibility(message.direct ? View.VISIBLE : View.INVISIBLE);

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
                                } else if (which == 2) {
                                    dismiss();
                                    Intent i = new Intent(mActivity, ChatActivity.class);
                                    i.putExtra("endpointID", mActivity.remoteEndpoint.getEndpointID());
                                    i.putExtra("directConnection", true);
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


    public void acceptConnection(View view) {
        directConnection.accept();
        View answerView = findViewById(R.id.answer_view);
        View connectingView = findViewById(R.id.connecting_view);
        answerView.setVisibility(View.INVISIBLE);
        connectingView.setVisibility(View.VISIBLE);
    }


    public void ignoreConnection(View view) {
        finish();
    }


    // RespokeCall.Listener methods


    public void onError(String errorMessage, RespokeCall sender) {
        Log.d(TAG, "Call error: " + errorMessage);
    }


    public void onHangup(RespokeCall sender) {
        finish();
    }


    public void onConnected(RespokeCall sender) {
        View connectingView = findViewById(R.id.connecting_view);
        connectingView.setVisibility(View.INVISIBLE);
    }


    public void directConnectionAvailable(RespokeDirectConnection directConnection, RespokeEndpoint endpoint) {

    }


    // RespokeDirectConnection.Listener methods


    public void onStart(RespokeDirectConnection sender) {

    }


    public void onOpen(RespokeDirectConnection sender) {

    }


    public void onClose(RespokeDirectConnection sender) {

    }


    public void onMessage(String message, RespokeDirectConnection sender) {
        conversation.addMessage(message, remoteEndpoint.getEndpointID(), true);

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
    }


}
