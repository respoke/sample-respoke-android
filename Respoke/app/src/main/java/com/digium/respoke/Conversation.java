package com.digium.respoke;

import java.util.ArrayList;

/**
 * Created by jasonadams on 9/14/14.
 */
public class Conversation {

    public ArrayList<ConversationMessage> messages;
    public String name;
    public int unreadCount;


    public Conversation(String newName) {
        name = newName;
        messages = new ArrayList<ConversationMessage>();
    }


    public void addMessage(String message, String sender) {
        ConversationMessage newMessage = new ConversationMessage();
        newMessage.message = message;
        newMessage.senderEndpoint = sender;
        messages.add(newMessage);
    }


}
