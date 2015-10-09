Respoke Android Demo App
========================

An open source, full featured remote team collaboration app using Respoke's authentication, group messaging, 1:1 messaging, audio calling, video calling, group discovery and presence. 

For more information on how to use the [Respoke Android SDK](https://github.com/respoke/respoke-sdk-android), take a look at the [Getting Started Guide](https://docs.respoke.io/client/android/getting-started.html). We have tried to keep this demo application as simple as possible to make it easy to understand how the SDK works.


Getting Started
===============

This demo application was designed in Android Studio and uses Gradle. To run the application, do the following:

1) Create a Respoke developer account and define a Respoke application in the [developer console](https://portal.respoke.io/#/signup). Make a note of the **application ID** for the Respoke Application you created.

2) _(Optional)_ If you would like to receive push notifications in the Android demo app, follow the [Obtaining Push Credentials](https://docs.respoke.io/client/android/android-push-notification-credentials.html) guide in the Respoke documentation site to configure your Google Developer console for Respoke. Make note of the **GCMS Sender ID** that gets assigned to you.

3) Clone this repository onto your development machine and open Android Studio. Choose "Import Project" and select the root directory of the repository.

4) Open ConnectActivity.java and replace the value of the static string `RESPOKE_APP_ID` with the application ID you received in step 1.

5) _(Optional)_ If you configured your app for push notifications in step 2, replace the value of the static string `SENDER_ID` in ConnectActivity.java with the GCMS Sender ID that you were assigned.

When you run the application, you will be given the chance to enter an endpoint ID (similar to a user name) and an optional group. The demo app discovers other endpoints when they join a group that your endpoint is also a member of. To test the real time chat features of Respoke, run this demo app (or the iOS app) on two different devices. Choose a unique endpoint ID on each device, and join the same group. The devices will then discover each other and allow you to chat through text, audio, or video.

Keep in mind that the standard Android emulator does not support multimedia, so audio or video calling will fail if you do not use a real device.


Running the test cases
==========================

The sample application supplies a few UI test cases that you may find useful when building your own application. To run the test cases, do the following:

1) Complete the steps described in the section "Getting started" above.

2) Start the web TestBot in either Chrome or Firefox as described in the section "Starting the Web TestBot" below, passing your Respoke application ID as a parameter on the URL.

6) In Android Studio, open the 'Run' menu and select "Edit Configurations". In the upper left corner of the dialog that appears, press the '+' button to create a new run configuration and select 'Android Tests'.

7) Name the new test configuration "All Tests", select the "app" module from the drop-down box, and choose the settings in the "Target Device" box that make the most sense for your test set up. save the new configuration. You only have to do this once, in the future you can just select the existing configuration to run the tests.

8) Open the "Run" menu again, and choose "debug…". Select the "All Tests" configuration to run the test cases, displaying the results inside of Android Studio. You will also see debug messages and video displayed in the web browser running the TestBot.

Starting the Web TestBot
========================

The functional test cases that use RespokeCall require a specific Web application based on Respoke.js that is set up to automatically respond to certain actions that the test cases perform. Because the web application will use audio and video, it requires special user permissions from browsers that support WebRTC and typically requires user interaction. Therefore it must run from either the context of a web server, or by loading the html file from the file system with specific command line parameters for Chrome. 

Additionally, the Android Studio test project has been set up to expect that the web application will connect to Respoke with a specific endpoint ID in the following format:

testbot-username

This username is the user that you are logged into your development computer with when you run the tests. This is done to avoid conflicts that can occur when multiple developers are running multiple instances of the test web application simultaneously. 

To set up your system to perform these tests, do one of the following:

#### A) Load the html from a file with Chrome.


1) You can use command line parameters to load the test bot with Chrome tell it to use a fake audio and video source during testing. On Mac OS, the command would look like this:

    $ "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
    --use-fake-ui-for-media-stream \
    --use-fake-device-for-media-stream \
    --allow-file-access-from-files \
    ./WebTestBot/index.html &

2) Once the file has loaded, append your local username and Respoke application ID to the URL to match what Android Studio will search for as the tests run:

    file:///sample-respoke-android/WebTestBot/index.html#?un=mymacusername&app_id=my-respoke-app-id

3) Run the test cases



#### B) Run with a local web server.


1) Install http-server

    $ sudo npm i -g http-server

2) Start http-server from the testbot directory:

    $ cd WebTestBot/
    $ http-server

3) Start Chrome using command line parameters to use fake audio/video and auto accept media permissions so that no human interaction is required:

    $ /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --use-fake-ui-for-media-stream --use-fake-device-for-media-stream

This can alternately be done with Firefox by navigating to "about:config" and then setting the "media.navigator.permission.disabled" option to TRUE

4) Open the TestBot in a Chrome tab by loading http://localhost:8080/#?un=mymacusername&app_id=my-respoke-app-id

5) Run the test cases

License
=======

The Respoke SDK and demo applications are licensed under the MIT license. Please see the [LICENSE](LICENSE) file for details.