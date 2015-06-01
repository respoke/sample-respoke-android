Respoke Android Demo App
========================

An open source, full featured remote team collaboration app using Respoke's authentication, group messaging, 1:1 messaging, audio calling, video calling, group discovery and presence. 

For more information on how to use the [Respoke Android SDK](https://github.com/respoke/respoke-sdk-android), take a look at the [Getting Started Guide](https://docs.respoke.io/client/android/getting-started.html). We have tried to keep this demo application as simple as possible to make it easy to understand how the SDK works.


Getting Started
===============

This demo application was designed in Android Studio and uses Gradle. To run the application, clone this repository onto your development machine and open Android Studio. Choose "Import Project" and select the root directory of the repository.

When you run the application, you will be given the chance to enter an endpoint ID (similar to a user name) and an optional group. The demo app discovers other endpoints when they join a group that your endpoint is also a member of. To test the real time chat features of Respoke, run this demo app (or the iOS app) on two different devices. Choose a unique endpoint ID on each device, and join the same group. The devices will then discover each other and allow you to chat through text, audio, or video.

Keep in mind that the standard Android emulator does not support multimedia, so audio or video calling will fail if you do not use a real device.


License
=======

The Respoke SDK and demo applications are licensed under the MIT license. Please see the [LICENSE](LICENSE) file for details.