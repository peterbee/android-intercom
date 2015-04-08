# Building and Installing the Two-Way App

For minimum device requirements, view the list [here](wiki/Minimum-Specifications).

* [Building and Installing Using the Command Line](#building-and-installing-using-the-command-line)
 * [Requirements](#requirements)
 * [Building](#building)
 * [Installing](#installing)
* [Building and Installing With Android Studio](#building-and-installing-with-android-studio)
 * [Requirements](#requirements-1)
 * [Building and Installing](#building-and-installing)

---

## Building and Installing Using the Command Line


### Requirements
1. Gradle
 1. Gradle is used to build the Two-Way app.  If you're not sure whether you have gradle installed, open a command prompt or a bash screen and run the following command:<br>        `gradle -v`<br>If gradle is installed, you will see a description of the current gradle version installed.  If you do not have gradle installed, go to https://gradle.org/downloads and follow the instructions provided to download and set up gradle.
1. Android Software Development Kit (SDK) 
 1. An Android SDK is required to install the Two-Way app via command line properly.  Go to http://developer.android.com/sdk/index.html and follow the instructions provided to download and set up an Android SDK.
1. Android Device
 1. As this is an Android app, an Android device is required to run it, as well as a USB cable to connect the computer to the Android device.
 1. Your Android device must also have USB debugging enabled.
<br>Taken from http://developer.android.com/tools/device.html:
<i>On most devices running Android 3.2 or older, you can find the option under Settings > Applications > Development.
On Android 4.0 and newer, it's in Settings > Developer options.
Note: On Android 4.2 and newer, Developer options is hidden by default. To make it available, go to Settings > About phone and tap Build number seven times. Return to the previous screen to find Developer options.</i>

---

### Building
1. Check out the Code<br>
    The source can either be downloaded as a zip archive or by using git.
 1. Downloading Archive
<br><br><br>![](Images/Build Instructions/Clone/01.png)<br>
        Go to https://github.com/robertzas/7bit and click the <b>Download Zip</b> button.  Extract the 7bit-master folder from the archive.
 1. Checkout Using Git<br>
        Open a command prompt or bash and navigate to the location you want the source saved to.  Run the following command:<br>`git clone https://github.com/robertzas/7bit`<br> Enter your GitHub login and password if prompted.  The code will be checked out to a folder called 7bit in the current directory.
1. Build the Two-Way App<br/>
        Open a command prompt or bash screen and navigate to the 7bit folder.  Run the following command:<br>`gradlew clean packageDebug`<br>The .apk file will be created in the 7bit\app\build\outputs\apk folder.

---

### Installing
1. Open a command prompt or bash screen and navigate to the 7bit\app\build\outputs\apk folder.
1. Connect your Android device to your computer with a USB cable.
1. Run the following command:<br>`adb install app-debug-unaligned.apk`<br>
1. This will install the Two-Way app on your Android device.

---

## Building and Installing With Android Studio


### Requirements
1. Gradle
 1. Gradle is used to build the Two-Way app.  If you're not sure whether you have gradle installed, open a command prompt or a bash screen and run the following command:<br>        `gradle -v`<br>If gradle is installed, you will see a description of the current gradle version installed.  If you do not have gradle installed, go to https://gradle.org/downloads and follow the instructions provided to download and set up gradle.
1. Android Studio
 1. The Two-Way App was built using Android Studio (The official development environment for Android).  Go to http://developer.android.com/sdk/index.html and follow the instructions provided to download and set up an Android Studio.
1. Android Software Development Kit (SDK)
 1. The latest version of the Android SDK will be installed as part of the process of setting up Android Studio.
1. Android Device
 1. As this is an Android app, an Android device is required to run it, as well as a USB cable to connect the computer to the Android device.
 1. Your Android device must also have USB debugging enabled.
<br>Taken from http://developer.android.com/tools/device.html:
<i>On most devices running Android 3.2 or older, you can find the option under Settings > Applications > Development.
On Android 4.0 and newer, it's in Settings > Developer options.
Note: On Android 4.2 and newer, Developer options is hidden by default. To make it available, go to Settings > About phone and tap Build number seven times. Return to the previous screen to find Developer options.</i>
 
---

### Building and Installing
1. Open Android Studio.
<br><br><br>![](Images/Build Instructions/Android Studio/01.png)<br>
1. From the <b>Quick Start</b> menu, select <b>Check out project from Version Control</b> and select <b>GitHub</b>
<br><br><br>![](Images/Build Instructions/Android Studio/02.png)<br>
1. Enter <i>http<b></b>s://github.com/robertzas/7bit.git</i> in the <b>Vcs Repository URL</b> field.
1. Select a folder to checkout the source code in under the <b>Parent Directory</b> field.
1. Leave the <b>Directory Name</b> as <i>7bit</i>.
1. Click the <b>Clone</b> button.
<br><br><br>![](Images/Build Instructions/Android Studio/03.png)<br>
1. Click <b>Yes</b>.
1. Connect your Android device to your computer with a USB cable.
<br><br><br>![](Images/Build Instructions/Android Studio/04.png)<br>
1. Click <b>Run</b> from the menu and then <b>Run 'app'</b>.
<br><br><br>![](Images/Build Instructions/Android Studio/05.png)<br>
1. You will see a <b>Choose Device</b> window, select the device to install the app to and click <b>OK</b>.
1. This will build the app in Android studio and install and run the app on your Android device.
