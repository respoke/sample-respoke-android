package com.digium.respoke;

import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jasonadams on 9/14/14.
 */
public class ContactManager {

    public RespokeClient sharedClient;
    public String username;
    public ArrayList groups;
    public Map<String, ArrayList> groupConnectionArrays;
    public Map<String, ArrayList> groupEndpointArrays;
    public Map<String, Conversation> conversations;
    public Map<String, Conversation> groupConversations;
    public ArrayList allKnownEndpoints;

    private static ContactManager _instance;


    public ContactManager() {
        groups = new ArrayList();
        groupConnectionArrays = new HashMap<String, ArrayList>();
        groupEndpointArrays = new HashMap<String, ArrayList>();
        conversations = new HashMap<String, Conversation>();
        groupConversations = new HashMap<String, Conversation>();
        allKnownEndpoints = new ArrayList();
    }


    public static ContactManager sharedInstance()
    {
        if (_instance == null)
        {
            _instance = new ContactManager();
        }

        return _instance;
    }


    public void joinGroup(String groupName) {
        if (null != sharedClient) {
            sharedClient.joinGroup(groupName);
        }
    }


    public void leaveGroup(RespokeGroup group) {

    }


    public void disconnected() {

    }


}
