/**
 * Copyright 2015, Digium, Inc.
 * All rights reserved.
 *
 * This source code is licensed under The MIT License found in the
 * LICENSE file in the root directory of this source tree.
 *
 * For all details and documentation:  https://www.respoke.io
 */

package com.digium.respoke;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;

import java.util.ArrayList;


public class GroupActivity extends Activity implements AdapterView.OnItemClickListener {

    private final static String TAG = "GroupActivity";
    private final static String GROUP_ID_KEY = "groupID";
    private RespokeGroup group;
    private ListDataAdapter listAdapter;
    private boolean leaving;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        String groupID = null;

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            groupID = savedInstanceState.getString(GROUP_ID_KEY);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                groupID = extras.getString(GROUP_ID_KEY);
            } else {
                // The activity must have been destroyed while it was hidden to save memory. Use the most recent persistent data.
                SharedPreferences prefs = getSharedPreferences(ConnectActivity.RESPOKE_SETTINGS, 0);
                groupID = prefs.getString(GROUP_ID_KEY, "");
            }
        }

        this.setTitle(groupID);

        for (RespokeGroup eachGroup : ContactManager.sharedInstance().groups) {
            if (eachGroup.getGroupID().equals(groupID)) {
                group = eachGroup;
                break;
            }
        }

        listAdapter = new ListDataAdapter();

        ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
        lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView

        lv.setOnItemClickListener(this);
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(GROUP_ID_KEY, group.getGroupID());
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
        iff = new IntentFilter(ContactManager.ENDPOINT_JOINED_GROUP);
        LocalBroadcastManager.getInstance(this).registerReceiver(contactDataInvalidatedReceiver, iff);
        iff = new IntentFilter(ContactManager.ENDPOINT_LEFT_GROUP);
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

        // Save key information in case this activity is killed while it is not visible
        SharedPreferences prefs = getSharedPreferences(ConnectActivity.RESPOKE_SETTINGS, 0);
        SharedPreferences.Editor editor = prefs.edit();
        if (null != group) {
            editor.putString(GROUP_ID_KEY, group.getGroupID()).apply();
        }
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
        getMenuInflater().inflate(R.menu.group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_leave) {
            ContactManager.sharedInstance().leaveGroup(group, new Respoke.TaskCompletionListener() {
                @Override
                public void onSuccess() {
                    leaving = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.d(TAG, errorMessage);
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Object item = listAdapter.getItem(position);

        if (item instanceof RespokeEndpoint) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("endpointID", ((RespokeEndpoint) item).getEndpointID());
            startActivity(i);
        } else if (item instanceof RespokeGroup) {
            Intent i = new Intent(this, GroupChatActivity.class);
            i.putExtra("groupID", ((RespokeGroup) item).getGroupID());
            startActivity(i);
        }
    }


    public class ListDataAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            if (leaving) {
                return 0;
            } else {
                Integer memberCount = 3;
                if (null != group) {
                    ArrayList<RespokeEndpoint> members = ContactManager.sharedInstance().groupEndpointArrays.get(group.getGroupID());
                    if (null != members) {
                        memberCount += members.size();
                    }
                }

                return memberCount;
            }
        }


        @Override
        public Object getItem( int position ) {
            if (position == 0) {
                return "";
            } else if (position == 1) {
                return group;
            } else if (position == 2) {
                return "";
            } else {
                ArrayList<RespokeEndpoint> endpoints = ContactManager.sharedInstance().groupEndpointArrays.get(group.getGroupID());
                return endpoints.get(position - 3);
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
                Conversation conversation = ContactManager.sharedInstance().groupConversations.get(group.getGroupID());

                View v = LayoutInflater.from(parent.getContext()).inflate( R.layout.list_row_group, parent, false );
                TextView itemText = (TextView)v.findViewById( R.id.textView1 );
                TextView unreadCountText = (TextView)v.findViewById( R.id.messageCount );

                itemText.setText(group.getGroupID() + " group messages");

                if (conversation.unreadCount == 0) {
                    unreadCountText.setVisibility(View.INVISIBLE);
                } else {
                    unreadCountText.setText(Integer.toString(conversation.unreadCount));
                    unreadCountText.setVisibility(View.VISIBLE);
                }

                return v;
            } else {
                RespokeEndpoint endpoint = (RespokeEndpoint) item;
                Conversation conversation = null;
                if (null != endpoint) {
                    conversation = ContactManager.sharedInstance().conversations.get(endpoint.getEndpointID());
                }

                View v = LayoutInflater.from(parent.getContext()).inflate( R.layout.list_row_endpoint, parent, false );
                TextView endpointText = (TextView)v.findViewById( R.id.textView1 );
                TextView presenceText = (TextView)v.findViewById( R.id.textView2 );
                TextView unreadCountText = (TextView)v.findViewById( R.id.messageCount );

                if (null!= endpoint) {
                    endpointText.setText(endpoint.getEndpointID());

                    if (endpoint.presence instanceof String) {
                        presenceText.setText((String) endpoint.presence);
                        presenceText.setVisibility(View.VISIBLE);
                    } else {
                        presenceText.setVisibility(View.INVISIBLE);
                    }
                } else {
                    presenceText.setVisibility(View.INVISIBLE);
                }


                if ((null == conversation) || (conversation.unreadCount == 0)) {
                    unreadCountText.setVisibility(View.INVISIBLE);
                } else {
                    unreadCountText.setText(Integer.toString(conversation.unreadCount));
                    unreadCountText.setVisibility(View.VISIBLE);
                }

                return v;
            }
        }
    }
}
