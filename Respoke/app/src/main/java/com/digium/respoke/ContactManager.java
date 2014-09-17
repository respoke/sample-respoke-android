package com.digium.respoke;

import android.util.Log;

import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeConnection;
import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeEndpointDelegate;
import com.digium.respokesdk.RespokeGetGroupMembersCompletionDelegate;
import com.digium.respokesdk.RespokeGroup;
import com.digium.respokesdk.RespokeGroupDelegate;
import com.digium.respokesdk.RespokeJoinGroupCompletionDelegate;
import com.digium.respokesdk.RespokeTaskCompletionDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jasonadams on 9/14/14.
 */
public class ContactManager implements RespokeGroupDelegate, RespokeEndpointDelegate {

    private final static String TAG = "ContactManager";
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


    public void joinGroup(final String groupName, final RespokeTaskCompletionDelegate completionDelegate) {
        if (null != sharedClient) {
            sharedClient.joinGroup(groupName, new RespokeJoinGroupCompletionDelegate() {
                @Override
                public void onSuccess(RespokeGroup group) {
                    Log.d(TAG, "Group joined, fetching member list");

                    group.delegate = ContactManager.this;
                    groups.add(group);

                    group.getMembers(new RespokeGetGroupMembersCompletionDelegate() {
                        @Override
                        public void onSuccess(ArrayList<RespokeConnection> memberArray) {
                            // Establish the connection and endpoint tracking arrays for this group
                            groupConnectionArrays.put(groupName, new ArrayList<RespokeConnection>(memberArray));

                            ArrayList<RespokeEndpoint> groupEndpoints = new ArrayList<RespokeEndpoint>();
                            groupEndpointArrays.put(groupName, groupEndpoints);

                            // Start tracking the conversation with this group
                            Conversation groupConversation = new Conversation(groupName);
                            groupConversations.put(groupName, groupConversation);

                            // Evaluate each connection in the new group
                            for (RespokeConnection each : memberArray) {
                                // Find the endpoint to which the connection belongs
                                RespokeEndpoint parentEndpoint = each.getEndpoint();

                                // If this endpoint is not known in any group, remember it
                                if (-1 == allKnownEndpoints.indexOf(parentEndpoint)) {
                                    allKnownEndpoints.add(parentEndpoint);
                                    parentEndpoint.delegate = ContactManager.this;

                                    // Start tracking the conversation with this endpoint
                                    Conversation conversation = new Conversation(parentEndpoint.endpointID);
                                    conversations.put(parentEndpoint.endpointID, conversation);
                                }

                                // If this endpoint is not known in this specific group, remember it
                                if (-1 == groupEndpoints.indexOf(parentEndpoint)) {
                                    groupEndpoints.add(parentEndpoint);
                                }
                            }

                            //TODO Notify UI

                            //TODO register presence

                            completionDelegate.onSuccess();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            completionDelegate.onError(errorMessage);
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    completionDelegate.onError(errorMessage);
                }
            });
        }
    }


    public void leaveGroup(final RespokeGroup group, final RespokeTaskCompletionDelegate completionDelegate) {
        group.leave(new RespokeTaskCompletionDelegate() {
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
                //TODO Notify UI

                completionDelegate.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                completionDelegate.onError(errorMessage);
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


    // RespokeGroupDelegate methods


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
                Log.d(TAG, "Joined: " + parentEndpoint.endpointID);
                allKnownEndpoints.add(parentEndpoint);
                parentEndpoint.delegate = this;

                // Start tracking the conversation with this endpoint
                Conversation conversation = new Conversation(parentEndpoint.endpointID);
                conversations.put(parentEndpoint.endpointID, conversation);

                // Notify any UI listeners that a new endpoint has been discovered
                //TODO notify UI

                //TODO Register presence
            }


            // If this endpoint is not known in this specific group, remember it
            if (-1 == groupEndpoints.indexOf(parentEndpoint)) {
                groupEndpoints.add(parentEndpoint);

                // Notify any UI listeners that a new endpoint has joined this group
                //TODO notify UI
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
                        Log.d(TAG, "Left: " + parentEndpoint.endpointID);
                        int endpointIndex = allKnownEndpoints.indexOf(parentEndpoint);

                        if (-1 != endpointIndex) {
                            allKnownEndpoints.remove(endpointIndex);
                            conversations.remove(parentEndpoint.endpointID);

                            // Notify any UI listeners that an endpoint has left
                            //TODO Notify UI
                        }
                    }

                    if (groupConnectionCount == 0) {
                        int groupIndex = groupEndpoints.indexOf(parentEndpoint);

                        if (-1 != groupIndex) {
                            groupEndpoints.remove(groupIndex);

                            // Notify any UI listeners that an endpoint has left this group
                            //TODO Notify UI
                        }
                    }
                }
            }
        }
    }


    public void onGroupMessage(String message, RespokeEndpoint endpoint, RespokeGroup sender) {
        Conversation conversation = groupConversations.get(sender.getGroupID());
        conversation.addMessage(message, endpoint.endpointID);
        conversation.unreadCount++;

        // Notify any UI listeners that a message has been received from a remote endpoint
        //TODO Notify UI
    }


    // RespokeEndpointDelegate methods


    public void onMessage(String message, RespokeEndpoint sender) {
        Conversation conversation = conversations.get(sender.endpointID);
        conversation.addMessage(message, sender.endpointID);
        conversation.unreadCount++;

        // Notify any UI listeners that a message has been received from a remote endpoint
        //TODO Notify UI
    }


    public void onPresence(Object presence, RespokeEndpoint sender) {
        // Notify any UI listeners that presence for this endpoint has been updated
        //TODO Notify UI
    }


}
