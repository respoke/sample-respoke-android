respoke-android
===============

This repository uses submodules! Make sure to clone this repo using the --recursive flag in order to automatically clone the SDK submodule as well.


```
git clone --recursive https://<USER_NAME>@stash.digium.com/stash/scm/sa/respoke-android.git
```

Prerequisites:

Android Studio (I used v1.0.1)
Android NDK (I used r10b, Mac 64-bit)

https://developer.android.com/tools/sdk/ndk/index.html




Building the WebRTC libaries
============================

Unlike iOS, the WebRTC libraries for Android can ONLY be built on Linux. It WILL NOT work on Mac OS X. If, like me, you are only working with a Mac then one easy-ish approach is to create an Ubuntu virtual machine and perform the build there. Once the library has been built, it may be used inside of the Android Studio project on Mac again without issues.

The following steps are what was required for me to build the libraries using a trial version of VMWare and Ubuntu 14.04 LTS.

1) Download and install VMWare Fusion (I used v7.0)

https://www.vmware.com/products/fusion/features.html


2) Download the Ubuntu 14.04 LTS install image. Get the "64-bit Mac (AMD64)" image

http://www.ubuntu.com/download/desktop


3) Create a new virtual machine using the Ubuntu image. Give it at least 30GB of space to work with. I choose not to let it share home directories with my Mac user so it was easier to clean up afterwards.


4) Login to the new Ubuntu virtual machine desktop and open a terminal (Ctrl-Alt-T)


5) Install git

sudo apt-get install git


6) Configure Git for your Github credentials, and then clone the Respoke repository into your home directory

username@ubuntu:~$  git clone https://github.com/Ninjanetic/respoke-android.git


7) Install dependenices part 1

A series of scripts have been provided in the Respoke repository to make it easier to set up and run the build. It requires several steps and takes several hours to finish, so be prepared.

cd respoke-android
./build_webrtc_libs.sh install_dependencies
./build_webrtc_libs.sh install_jdk1_6
./build_webrtc_libs.sh pull_depot_tools


8) Close your terminal and reopen it so that you have the new JDK environment variables, or run:

source ~/.bashrc


9) Pull WebRTC source part 1

Due to a chicken-and-egg problem, we will attempt to pull the massive WebRTC source code and at some point it will fail. By that time, it will have grabbed another script with the commands necessary to install the dependencies that are missing.

./pull_webrtc_source.sh

This took me ~4 hours to finish. Eventually it will fail, complaining about missing Ubuntu packages.


10) Install dependencies part 2

A script now exists that will fortunately install the remaining dependencies for us, so run it:

src/build/install-build-deps-android.sh


11) Pull WebRTC source part 2. It should actually finish successfully this time!

./pull_webrtc_source.sh


12) Install last dependencies & build

The following command will install some last dependency packages and start the actual build in release mode. 

./build_webrtc_libs.sh build_apprtc


License
=======

The Respoke SDK and demo applications are licensed under the MIT license. Please see the [LICENSE](LICENSE) file for details.