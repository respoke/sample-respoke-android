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


License
=======

The Respoke SDK and demo applications are licensed under the MIT license. Please see the [LICENSE](LICENSE) file for details.