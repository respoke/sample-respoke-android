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

import java.util.ArrayList;

public class Conversation {

    public ArrayList<ConversationMessage> messages;
    public String name;
    public int unreadCount;


    public Conversation(String newName) {
        name = newName;
        messages = new ArrayList<ConversationMessage>();
    }


    public void addMessage(String message, String sender, boolean directMessage) {
        ConversationMessage newMessage = new ConversationMessage();
        newMessage.message = message;
        newMessage.senderEndpoint = sender;
        newMessage.direct = directMessage;
        messages.add(newMessage);
    }


}
