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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeConnection;
import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class ContactManager implements RespokeGroup.Listener, RespokeEndpoint.Listener {
    
    public static final String ENDPOINT_MESSAGE_RECEIVED = "ENDPOINT_MESSAGE_RECEIVED";
    public static final String GROUP_MEMBERSHIP_CHANGED = "GROUP_MEMBERSHIP_CHANGED";
    public static final String ENDPOINT_DISCOVERED = "ENDPOINT_DISCOVERED";
    public static final String ENDPOINT_DISAPPEARED = "ENDPOINT_DISAPPEARED";
    public static final String ENDPOINT_JOINED_GROUP = "ENDPOINT_JOINED_GROUP";
    public static final String ENDPOINT_LEFT_GROUP = "ENDPOINT_LEFT_GROUP";
    public static final String GROUP_MESSAGE_RECEIVED = "GROUP_MESSAGE_RECEIVED";
    public static final String ENDPOINT_PRESENCE_CHANGED = "ENDPOINT_PRESENCE_CHANGED";

    private final static String TAG = "ContactManager";
    public Context context;
    public RespokeClient sharedClient;
    public String username;
    public ArrayList<RespokeGroup> groups;
    public Map<String, ArrayList<RespokeConnection>> groupConnectionArrays;
    public Map<String, ArrayList<RespokeEndpoint>> groupEndpointArrays;
    public Map<String, Conversation> conversations;
    public Map<String, Conversation> groupConversations;
    public ArrayList<RespokeEndpoint> allKnownEndpoints;

    private static ContactManager _instance;


    public ContactManager() {
        groups = new ArrayList<RespokeGroup>();
        groupConnectionArrays = new HashMap<String, ArrayList<RespokeConnection>>();
        groupEndpointArrays = new HashMap<String, ArrayList<RespokeEndpoint>>();
        conversations = new HashMap<String, Conversation>();
        groupConversations = new HashMap<String, Conversation>();
        allKnownEndpoints = new ArrayList<RespokeEndpoint>();
    }


    public static ContactManager sharedInstance()
    {
        if (_instance == null)
        {
            _instance = new ContactManager();
        }

        return _instance;
    }


    public void joinGroup(final String groupName, final Respoke.TaskCompletionListener completionListener) {
        if (null != sharedClient) {
            ArrayList<String> groupsToJoin = new ArrayList<String>();
            groupsToJoin.add(groupName);

            sharedClient.joinGroups(groupsToJoin, new RespokeClient.JoinGroupCompletionListener() {
                @Override
                public void onSuccess(final ArrayList<RespokeGroup> groupList) {
                    Log.d(TAG, "Group joined, fetching member list");

                    // This demo app will only ever join one group at a time, so just grab the first entry
                    final RespokeGroup group = groupList.get(0);
                    group.setListener(ContactManager.this);
                    groups.add(group);

                    // Establish the connection and endpoint tracking arrays for this group
                    groupConnectionArrays.put(groupName, new ArrayList<RespokeConnection>());

                    final ArrayList<RespokeEndpoint> groupEndpoints = new ArrayList<RespokeEndpoint>();
                    groupEndpointArrays.put(groupName, groupEndpoints);

                    // Start tracking the conversation with this group
                    Conversation groupConversation = new Conversation(groupName);
                    groupConversations.put(groupName, groupConversation);

                    group.getMembers(new RespokeGroup.GetGroupMembersCompletionListener() {
                        @Override
                        public void onSuccess(ArrayList<RespokeConnection> memberArray) {
                            groupConnectionArrays.put(groupName, new ArrayList<RespokeConnection>(memberArray));

                            // Evaluate each connection in the new group
                            for (RespokeConnection each : memberArray) {
                                // Find the endpoint to which the connection belongs
                                RespokeEndpoint parentEndpoint = each.getEndpoint();

                                trackEndpoint(parentEndpoint);

                                // If this endpoint is not known in this specific group, remember it
                                if (-1 == groupEndpoints.indexOf(parentEndpoint)) {
                                    groupEndpoints.add(parentEndpoint);
                                }
                            }

                            // Notify any UI listeners that group membership has changed
                            Intent intent = new Intent(GROUP_MEMBERSHIP_CHANGED);
                            intent.putExtra("groupID", group.getGroupID());
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                            for (RespokeEndpoint eachEndpoint : groupEndpoints) {
                                eachEndpoint.registerPresence(new Respoke.TaskCompletionListener() {
                                    @Override
                                    public void onSuccess() {
                                        // do nothing
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.d(TAG, "Error registering presence: " + errorMessage);
                                    }
                                });
                            }

                            completionListener.onSuccess();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            completionListener.onError(errorMessage);
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    completionListener.onError(errorMessage);
                }
            });
        }
    }


    public void leaveGroup(final RespokeGroup group, final Respoke.TaskCompletionListener completionListener) {
        group.leave(new Respoke.TaskCompletionListener() {
            @Override
            public void onSuccess() {
                String groupName = group.getGroupID();

                ArrayList<RespokeEndpoint> endpoints = groupEndpointArrays.get(groupName);

                // Purge all of the group data
                groups.remove(group);
                groupEndpointArrays.remove(groupName);
                groupConnectionArrays.remove(groupName);
                groupConversations.remove(groupName);

                // Purge any endpoints that were only a member of this group from the combined endpoint list
                for (RespokeEndpoint eachEndpoint : endpoints) {
                    int membershipCount = 0;

                    for (Map.Entry<String, ArrayList<RespokeConnection>> entry : groupConnectionArrays.entrySet()) {
                        for (RespokeConnection eachConnection : entry.getValue()) {
                            // Find the endpoint to which the connection belongs
                            RespokeEndpoint parentEndpoint = eachConnection.getEndpoint();

                            if (eachEndpoint == parentEndpoint) {
                                membershipCount++;
                            }
                        }
                    }

                    if (membershipCount == 0) {
                        // This endpoint is not a member of any of the other groups, so remove it from the list
                        allKnownEndpoints.remove(eachEndpoint);
                    }
                }

                // Notify any UI listeners that group membership has changed
                Intent intent = new Intent(GROUP_MEMBERSHIP_CHANGED);
                intent.putExtra("groupID", group.getGroupID());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                completionListener.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                completionListener.onError(errorMessage);
            }
        });
    }


    public void disconnected() {
        groups.clear();
        groupConnectionArrays.clear();
        groupEndpointArrays.clear();
        allKnownEndpoints.clear();
        conversations.clear();
        groupConversations.clear();
    }


    public void trackEndpoint(RespokeEndpoint newEndpoint) {
        // If this endpoint is not known in any group, remember it
        if (-1 == allKnownEndpoints.indexOf(newEndpoint)) {
            allKnownEndpoints.add(newEndpoint);
            newEndpoint.setListener(ContactManager.this);

            // Start tracking the conversation with this endpoint
            Conversation conversation = new Conversation(newEndpoint.getEndpointID());
            conversations.put(newEndpoint.getEndpointID(), conversation);
        }
    }


    // RespokeGroupListener methods


    public void onJoin(RespokeConnection connection, RespokeGroup sender) {
        if (-1 != groups.indexOf(sender)) {
            String groupName = sender.getGroupID();

            // Get the list of known connections for this group
            ArrayList<RespokeConnection> groupConnections = groupConnectionArrays.get(groupName);
            groupConnections.add(connection);

            // Get the list of known endpoints for this group
            ArrayList<RespokeEndpoint> groupEndpoints = groupEndpointArrays.get(groupName);

            // Get the endpoint that owns this new connection
            RespokeEndpoint parentEndpoint = connection.getEndpoint();

            // If this endpoint is not known anywhere, remember it
            if (-1 == allKnownEndpoints.indexOf(parentEndpoint)) {
                Log.d(TAG, "Joined: " + parentEndpoint.getEndpointID());

                trackEndpoint(parentEndpoint);

                // Notify any UI listeners that a new endpoint has been discovered
                Intent intent = new Intent(ENDPOINT_DISCOVERED);
                intent.putExtra("endpointID", parentEndpoint.getEndpointID());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                parentEndpoint.registerPresence(new Respoke.TaskCompletionListener() {
                    @Override
                    public void onSuccess() {
                        // Do nothing
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.d(TAG, "Error registering presence: " + errorMessage);
                    }
                });
            }

            // If this endpoint is not known in this specific group, remember it
            if (-1 == groupEndpoints.indexOf(parentEndpoint)) {
                groupEndpoints.add(parentEndpoint);

                // Notify any UI listeners that a new endpoint has joined this group

                // Notify any UI listeners that group membership has changed
                Intent intent = new Intent(ENDPOINT_JOINED_GROUP);
                intent.putExtra("endpointID", parentEndpoint.getEndpointID());
                intent.putExtra("groupID", sender.getGroupID());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }
    }


    public void onLeave(RespokeConnection connection, RespokeGroup sender) {
        if (-1 != groups.indexOf(sender)) {
            String groupName = sender.getGroupID();

            // Get the list of known connections for this group
            ArrayList<RespokeConnection> groupMembers = groupConnectionArrays.get(groupName);
            int index = groupMembers.indexOf(connection);

            // Get the list of known endpoints for this group
            ArrayList<RespokeEndpoint> groupEndpoints = groupEndpointArrays.get(groupName);

            if (-1 != index) {
                groupMembers.remove(index);
                RespokeEndpoint parentEndpoint = connection.getEndpoint();

                if (null != parentEndpoint) {
                    // Make sure that this is the last connection for this endpoint before removing it from the list
                    int connectionCount = 0;
                    int groupConnectionCount = 0;

                    for (Map.Entry<String, ArrayList<RespokeConnection>> entry : groupConnectionArrays.entrySet()) {
                        for (RespokeConnection eachConnection : entry.getValue()) {
                            if (eachConnection.getEndpoint() == parentEndpoint) {
                                connectionCount++;

                                if (entry.getValue() == groupMembers) {
                                    groupConnectionCount++;
                                }
                            }
                        }
                    }

                    if (connectionCount == 0) {
                        Log.d(TAG, "Left: " + parentEndpoint.getEndpointID());
                        int endpointIndex = allKnownEndpoints.indexOf(parentEndpoint);

                        if (-1 != endpointIndex) {
                            allKnownEndpoints.remove(endpointIndex);
                            conversations.remove(parentEndpoint.getEndpointID());

                            // Notify any UI listeners that an endpoint has left
                            Intent intent = new Intent(ENDPOINT_DISAPPEARED);
                            intent.putExtra("endpointID", parentEndpoint.getEndpointID());
                            intent.putExtra("index", endpointIndex);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                    }

                    if (groupConnectionCount == 0) {
                        int groupIndex = groupEndpoints.indexOf(parentEndpoint);

                        if (-1 != groupIndex) {
                            groupEndpoints.remove(groupIndex);

                            // Notify any UI listeners that an endpoint has left this group
                            Intent intent = new Intent(ENDPOINT_LEFT_GROUP);
                            intent.putExtra("endpointID", parentEndpoint.getEndpointID());
                            intent.putExtra("groupID", sender.getGroupID());
                            intent.putExtra("index", groupIndex);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                    }
                }
            }
        }
    }


    public void onGroupMessage(String message, RespokeEndpoint endpoint, RespokeGroup sender, Date timestamp) {
        Conversation conversation = groupConversations.get(sender.getGroupID());
        conversation.addMessage(message, endpoint.getEndpointID(), false);
        conversation.unreadCount++;

        // Notify any UI listeners that a message has been received from a remote endpoint
        Intent intent = new Intent(GROUP_MESSAGE_RECEIVED);
        intent.putExtra("groupID", sender.getGroupID());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    // RespokeEndpointListener methods


    public void onMessage(String message, Date timestamp, RespokeEndpoint sender) {
        Conversation conversation = conversations.get(sender.getEndpointID());
        conversation.addMessage(message, sender.getEndpointID(), false);
        conversation.unreadCount++;

        // Notify any UI listeners that a message has been received from a remote endpoint
        Intent intent = new Intent(ENDPOINT_MESSAGE_RECEIVED);
        intent.putExtra("endpointID", sender.getEndpointID());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    public void onPresence(Object presence, RespokeEndpoint sender) {
        // Notify any UI listeners that presence for this endpoint has been updated
        Intent intent = new Intent(ENDPOINT_PRESENCE_CHANGED);
        intent.putExtra("endpointID", sender.getEndpointID());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


}
