# Building and Installing the Two-Way App

* [Requirements](https://github.com/robertzas/7bit/wiki/Building-and-Installing#requirements)
* [Building Two-Way App From Source](https://github.com/robertzas/7bit/wiki/Building-and-Installing#building-two-way-app-from-source)
* [Installing Two-Way App](https://github.com/robertzas/7bit/wiki/Building-and-Installing#installing-two-way-app)

---

## Requirements
* Gradle
 * Gradle is used to build the Two-Way app.  If you're not sure whether you have gradle installed, open a command prompt or a bash screen and run the following command:<br />        `gradle -v`<br />If gradle is installed, you will see a description of the current gradle version installed.  If you do not have gradle installed, go to https://gradle.org/downloads to download it.  Follow the instructions provided to set up gradle.
* Android Device
 * As this is an android app, an android device is required to run it.

---

## Building the Two-Way App From Source
1. Check out the Code<br />
    The source can either be downloaded as a zip archive or by using git.
 1. Downloading Archive<br />
        Go to https://github.com/robertzas/7bit and click the <b>Download Zip</b> button.  Extract the 7bit-master folder from the archive.
 1. Checkout Using Git<br />
        Open a command prompt or bash and navigate to the location you want the source saved to.  Run the following command:<br />`git clone https://github.com/robertzas/7bit`<br /> Enter your GitHub login and password if prompted.  The code will be checked out to a folder called 7bit in the current directory.
1. Build the Two-Way App<br/>
        Open a command prompt or bash screen and navigate to the 7bit folder.  Run the following command:<br />`gradlew clean packageDebug`<br />The .apk file will be created in the 7bit\app\build\outputs\apk folder.

---

## Installing the Two-Way App
1. Copy the .apk file to your android device.  Open a file explorer and navigate to the location you copied the .apk file to.
1. Touch the .apk file and then touch the <b>Install</b> button to install it.
 1. If an <b>Install blocked</b> message is displayed, continue with this section.  Otherwise, skip to section 3.
 1. From the <b>Install blocked</b> message, touch the <b>Settings</b> button to go to android security settings.
 1. Touch the box next to <b>Unknown Sources</b>, and then touch <b>OK</b>.  This will allow you to install the Two-Way app on your android device.
1. Touch the <b>Install</b> button to accept permissions and finish installing the Two-Way app.