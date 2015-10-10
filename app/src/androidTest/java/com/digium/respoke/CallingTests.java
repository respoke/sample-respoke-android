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

import android.support.test.espresso.action.ViewActions;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeEndpoint;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class CallingTests extends RespokeUITestCase<ConnectActivity> {
    static final String TEST_BOT_CALL_ME_VIDEO_MESSAGE = "Testbot! Call me using video!";
    static final String TEST_BOT_HANGUP_MESSAGE = "Hang up dude. I'm done talking.";

    private RespokeEndpoint testbotEndpoint;
    private boolean callbackDidSucceed;
    private CountDownLatch asyncTaskSignal;


    public CallingTests() {
        super(ConnectActivity.class);
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }


    public void sendMessageToTestbot(final String message) throws Throwable {
        asyncTaskSignal = new CountDownLatch(1); // Reset the countdown signal
        callbackDidSucceed = false;

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                testbotEndpoint.sendMessage(message, false, true, new Respoke.TaskCompletionListener() {
                    @Override
                    public void onSuccess() {
                        callbackDidSucceed = true;
                        asyncTaskSignal.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        assertTrue("Should successfully send a message. Error: " + errorMessage, false);
                        asyncTaskSignal.countDown();
                    }
                });
            }
        });

        assertTrue("Test timed out", asyncTaskSignal.await(RespokeUITestCase.TEST_TIMEOUT, TimeUnit.SECONDS));
        assertTrue("sendMessage should call onSuccess", callbackDidSucceed);
    }


    public void testVideoAnswering() throws Throwable {
        String testEndpointID = generateTestEndpointID();
        String testGroupID = generateTestGroupID();

        // Attempt to connect to Respoke with the specified endpoint and group IDs
        loginEndpoint(testEndpointID, testGroupID);

        String testbotID = RespokeUITestCase.getTestBotEndpointId(getActivity());
        testbotEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(testbotID, false);
        ContactManager.sharedInstance().trackEndpoint(testbotEndpoint);

        // Test 4 sequential calls
        for (int ii = 0; ii < 4; ii++) {
            // Send a message to the web test bot to initiate an incoming video call to this android client
            sendMessageToTestbot(TEST_BOT_CALL_ME_VIDEO_MESSAGE);

            // Wait for the incoming call screen to appear
            waitForActivity(TEST_TIMEOUT, CallActivity.class);

            // Press the answer button
            onView(withId(R.id.answer_call_button)).check(matches(isDisplayed()));
            onView(withId(R.id.answer_call_button)).perform(ViewActions.click());

            onView(withId(R.id.hangup_button)).check(matches(isDisplayed()));

            // Let the call run for a few seconds
            CountDownLatch activeCallLatch = new CountDownLatch(1);
            activeCallLatch.await(5, TimeUnit.SECONDS);

            if (ii % 2 == 0) {
                // Hang up the call locally on even-numbered attempts
                onView(withId(R.id.hangup_button)).check(matches(isDisplayed()));
                onView(withId(R.id.hangup_button)).perform(ViewActions.click());
            } else {
                // Send a message to the web test but to hang up the call from the remote side
                sendMessageToTestbot(TEST_BOT_HANGUP_MESSAGE);
            }

            // The UI should return to the group list activity
            waitForActivity(TEST_TIMEOUT, GroupListActivity.class);
        }

        logout();
    }


    public void testMissedCall() throws Throwable {
        String testEndpointID = generateTestEndpointID();
        String testGroupID = generateTestGroupID();

        // Attempt to connect to Respoke with the specified endpoint and group IDs
        loginEndpoint(testEndpointID, testGroupID);

        String testbotID = RespokeUITestCase.getTestBotEndpointId(getActivity());
        testbotEndpoint = ContactManager.sharedInstance().sharedClient.getEndpoint(testbotID, false);
        ContactManager.sharedInstance().trackEndpoint(testbotEndpoint);

        // Send a message to the web test bot to initiate an incoming video call to this android client
        sendMessageToTestbot(TEST_BOT_CALL_ME_VIDEO_MESSAGE);

        // Wait for the incoming call screen to appear
        waitForActivity(TEST_TIMEOUT, CallActivity.class);
        onView(withId(R.id.answer_call_button)).check(matches(isDisplayed()));

        // Send a message to the web test but to hang up the call from the remote side
        sendMessageToTestbot(TEST_BOT_HANGUP_MESSAGE);

        // The UI should return to the group list activity
        waitForActivity(TEST_TIMEOUT, GroupListActivity.class);

        logout();
    }

}
