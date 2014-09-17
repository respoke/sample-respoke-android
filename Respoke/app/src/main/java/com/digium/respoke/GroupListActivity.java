package com.digium.respoke;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;


public class GroupListActivity extends Activity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        dataAdapter listAdapter = new dataAdapter();

        ListView lv = (ListView)findViewById(R.id.list); //retrieve the instance of the ListView from your main layout
        lv.setAdapter(listAdapter); //assign the Adapter to be used by the ListView

        lv.setOnItemClickListener(this);
    }


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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Log.d("WAT", "click!");
    }


    public class dataAdapter extends BaseAdapter {

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
                    unreadCountText.setText(conversation.unreadCount);
                    unreadCountText.setVisibility(View.INVISIBLE);
                }

                return v;
            } else {
                RespokeEndpoint endpoint = (RespokeEndpoint) item;
                Conversation conversation = ContactManager.sharedInstance().conversations.get(endpoint.getEndpointID());

                View v = LayoutInflater.from(parent.getContext()).inflate( R.layout.list_row_group, parent, false );
                TextView tvText = (TextView)v.findViewById( R.id.textView1 );
                TextView unreadCountText = (TextView)v.findViewById( R.id.messageCount );

                tvText.setText(endpoint.getEndpointID());

                if (conversation.unreadCount == 0) {
                    unreadCountText.setVisibility(View.INVISIBLE);
                } else {
                    unreadCountText.setText(conversation.unreadCount);
                    unreadCountText.setVisibility(View.INVISIBLE);
                }

                return v;
            }
        }
    }
}
