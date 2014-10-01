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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class GroupListActivity extends FragmentActivity implements AdapterView.OnItemClickListener, RespokeClient.Listener {

    private final static String TAG = "GroupListActivity";
    private ListDataAdapter listAdapter;
    private ArrayList<String> groupsToJoin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        setTitle(ContactManager.sharedInstance().username);

        listAdapter = new ListDataAdapter();

        ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
        lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView

        lv.setOnItemClickListener(this);

        ContactManager.sharedInstance().sharedClient.setListener(this);

        // set the initial status for this client
        setStatus("available");
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter iff = new IntentFilter(ContactManager.ENDPOINT_MESSAGE_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataChangedReceiver, iff);
        iff = new IntentFilter(ContactManager.ENDPOINT_PRESENCE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataChangedReceiver, iff);
        iff = new IntentFilter(ContactManager.GROUP_MEMBERSHIP_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataInvalidatedReceiver, iff);
        iff = new IntentFilter(ContactManager.ENDPOINT_DISCOVERED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataInvalidatedReceiver, iff);
        iff = new IntentFilter(ContactManager.ENDPOINT_DISAPPEARED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataInvalidatedReceiver, iff);
        iff = new IntentFilter(ContactManager.GROUP_MESSAGE_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataChangedReceiver, iff);

        listAdapter.notifyDataSetChanged();
        listAdapter.notifyDataSetInvalidated();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(contactDataChangedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(contactDataInvalidatedReceiver);
    }


    private BroadcastReceiver contactDataChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Tell the ListView to reconfigure itself based on the new data
            listAdapter.notifyDataSetChanged();
        }
    };


    private BroadcastReceiver contactDataInvalidatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Tell the ListView to reconfigure itself based on the new data
            listAdapter.notifyDataSetChanged();
            listAdapter.notifyDataSetInvalidated();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.group_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        /*if (id == R.id.action_join) {
            Log.d(TAG, "Hi!");
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed()
    {
        boolean notConnected = true;

        if (null != ContactManager.sharedInstance().sharedClient)
        {
            notConnected = !ContactManager.sharedInstance().sharedClient.isConnected();

            // send a disconnect either way to let the client clean itself up
            ContactManager.sharedInstance().sharedClient.disconnect();
        }

        if (notConnected) {
            // Switch views immediately, since there will be no callback function
            returnToConnectActivity();
        }
    }


    public void returnToConnectActivity() {
        // send a disconnect either way to let the client clean itself up
        ContactManager.sharedInstance().disconnected();
        this.finish();
    }


    public void rejoinGroups() {
        String nextGroupID = groupsToJoin.get(0);
        groupsToJoin.remove(0);

        ContactManager.sharedInstance().joinGroup(nextGroupID, new Respoke.TaskCompletionListener() {
            @Override
            public void onSuccess() {
                if (groupsToJoin.size() > 0) {
                    rejoinGroups();
                } else {
                    groupsToJoin = null;
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.d(TAG, "Error rejoining group: " + errorMessage);
            }
        });
    }


    public void onPresence(View view) {
        PresenceDialog dialog = new PresenceDialog();
        dialog.show(getFragmentManager(), "status_type");
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Object item = listAdapter.getItem(position);

        if (item instanceof RespokeEndpoint) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("endpointID", ((RespokeEndpoint) item).getEndpointID());
            startActivity(i);
        }
    }


    public class ListDataAdapter extends BaseAdapter {

        private final static String groupHeaderText = "Groups";
        private final static String endpointHeaderText = "All Known Endpoints";


        @Override
        public int getCount() {
            return ContactManager.sharedInstance().groups.size() + ContactManager.sharedInstance().allKnownEndpoints.size() + 2;
        }


        @Override
        public Object getItem( int position ) {
            if (position == 0) {
                return groupHeaderText;
            } else if (position <= ContactManager.sharedInstance().groups.size()) {
                return ContactManager.sharedInstance().groups.get(position - 1);
            } else if (position == ContactManager.sharedInstance().groups.size() + 1) {
                return endpointHeaderText;
            } else {
                return ContactManager.sharedInstance().allKnownEndpoints.get(position - ContactManager.sharedInstance().groups.size() - 2);
            }
        }


        @Override
        public long getItemId( int position ) {
            return position;
        }


        @Override
        public boolean isEnabled(int position) {
            Object item = getItem(position);

            if (item instanceof String) {
                return false;
            } else {
                return true;
            }
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);

            if (item instanceof String) {
                View v = LayoutInflater.from(parent.getContext()).inflate( R.layout.list_row_header, parent, false );

                TextView tvText = (TextView)v.findViewById( R.id.textView1 );
                tvText.setText((String) item);

                return v;
            } else if (item instanceof RespokeGroup) {
                RespokeGroup group = (RespokeGroup) item;
                Conversation conversation = ContactManager.sharedInstance().groupConversations.get(group.getGroupID());

                View v = LayoutInflater.from(parent.getContext()).inflate( R.layout.list_row_group, parent, false );
                TextView itemText = (TextView)v.findViewById( R.id.textView1 );
                TextView unreadCountText = (TextView)v.findViewById( R.id.messageCount );

                itemText.setText(group.getGroupID());

                if (conversation.unreadCount == 0) {
                    unreadCountText.setVisibility(View.INVISIBLE);
                } else {
                    unreadCountText.setText(Integer.toString(conversation.unreadCount));
                    unreadCountText.setVisibility(View.VISIBLE);
                }

                return v;
            } else {
                RespokeEndpoint endpoint = (RespokeEndpoint) item;
                Conversation conversation = ContactManager.sharedInstance().conversations.get(endpoint.getEndpointID());

                View v = LayoutInflater.from(parent.getContext()).inflate( R.layout.list_row_endpoint, parent, false );
                TextView endpointText = (TextView)v.findViewById( R.id.textView1 );
                TextView presenceText = (TextView)v.findViewById( R.id.textView2 );
                TextView unreadCountText = (TextView)v.findViewById( R.id.messageCount );

                endpointText.setText(endpoint.getEndpointID());

                if (endpoint.presence instanceof String) {
                    presenceText.setText((String) endpoint.presence);
                    presenceText.setVisibility(View.VISIBLE);
                } else {
                    presenceText.setVisibility(View.INVISIBLE);
                }

                if (conversation.unreadCount == 0) {
                    unreadCountText.setVisibility(View.INVISIBLE);
                } else {
                    unreadCountText.setText(Integer.toString(conversation.unreadCount));
                    unreadCountText.setVisibility(View.VISIBLE);
                }

                return v;
            }
        }
    }


    public static class PresenceDialog extends DialogFragment {
        private WeakReference<GroupListActivity> mActivityReference;


        @Override
        public void onAttach(Activity activity)
        {
            if (activity instanceof GroupListActivity)
            {
                mActivityReference = new WeakReference<GroupListActivity>((GroupListActivity) activity);
            }

            super.onAttach(activity);
        }


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_presence)
                    .setItems(R.array.presence_types, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            GroupListActivity mActivity = mActivityReference.get();

                            if (null != mActivity) {
                                dismiss();
                                Resources res = getResources();
                                String[] presenceTypes = res.getStringArray(R.array.presence_types);

                                if (which < presenceTypes.length - 1) {
                                    String newPresence = presenceTypes[which];
                                    mActivity.setStatus(newPresence);
                                }
                            } else {
                                dismiss();
                            }
                        }
                    });
            return builder.create();
        }
    }


    public void setStatus(final String newPresence) {
        if ((null != ContactManager.sharedInstance().sharedClient) && (ContactManager.sharedInstance().sharedClient.isConnected())) {
            Button presenceButton = (Button) findViewById(R.id.button1);
            presenceButton.setVisibility(View.INVISIBLE);
            ProgressBar progressCircle = (ProgressBar) findViewById(R.id.progress_circle);
            progressCircle.setVisibility(View.VISIBLE);

            ContactManager.sharedInstance().sharedClient.setPresence(newPresence, new Respoke.TaskCompletionListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button presenceButton = (Button) findViewById(R.id.button1);
                            presenceButton.setText("Your Status: " + newPresence);
                            presenceButton.setVisibility(View.VISIBLE);
                            ProgressBar progressCircle = (ProgressBar) findViewById(R.id.progress_circle);
                            progressCircle.setVisibility(View.INVISIBLE);
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.d(TAG, errorMessage);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button presenceButton = (Button) findViewById(R.id.button1);
                            presenceButton.setVisibility(View.VISIBLE);
                            ProgressBar progressCircle = (ProgressBar) findViewById(R.id.progress_circle);
                            progressCircle.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });
        }
    }


    // RespokeClientListener methods


    public void onConnect(RespokeClient sender) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button presenceButton = (Button) findViewById(R.id.button1);
                presenceButton.setVisibility(View.VISIBLE);
                ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
                progressCircle.setVisibility(View.INVISIBLE);

                if (null != groupsToJoin) {
                    rejoinGroups();
                }
            }
        });
    }


    public void onDisconnect(RespokeClient sender, boolean reconnecting) {
        if (reconnecting) {
            if (ContactManager.sharedInstance().groups.size() > 0) {
                groupsToJoin = new ArrayList<String>();

                for (RespokeGroup eachGroup : ContactManager.sharedInstance().groups) {
                    groupsToJoin.add(eachGroup.getGroupID());
                }
            }

            ContactManager.sharedInstance().disconnected();

            // Update UI on main thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button presenceButton = (Button)findViewById(R.id.button1);
                    presenceButton.setVisibility(View.INVISIBLE);
                    ProgressBar progressCircle = (ProgressBar)findViewById(R.id.progress_circle);
                    progressCircle.setVisibility(View.VISIBLE);

                    // Tell the ListView to reconfigure itself based on the new data
                    listAdapter.notifyDataSetChanged();
                    listAdapter.notifyDataSetInvalidated();
                }
            });

            //todo pop to this activity?
        } else {
            // Update UI on main thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    returnToConnectActivity();
                }
            });
        }
    }


    public void onError(RespokeClient sender, String errorMessage) {
        Log.d(TAG, "RespokeSDK Error: " + errorMessage);
    }


    public void onCall(RespokeClient sender, RespokeCall call) {
        Intent i = new Intent(this, CallActivity.class);
        i.putExtra("callID", call.getSessionID());
        i.putExtra("audioOnly", call.audioOnly);
        startActivity(i);
    }


}
