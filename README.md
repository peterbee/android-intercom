This readme file covers building and installing the Two-Way android app by 7Bit.

1. Requirements
2. Building Two-Way App From Source
3. Installing Two-Way App

1. Requirements
    1.1. Gradle
        Gradle is used to build the Two-Way app.  If you're not sure whether
        you have gradle installed, open a command prompt or a bash screen and
        run the following command:
            gradle -v
        If gradle is installed, you will see a description of the current
        gradle version installed.  If you do not have gradle installed, go to
        https://gradle.org/downloads to download it.  Follow the instructions
        provided to set up gradle.
    1.2. Android Device
        As this is an android app, an android device is required to run it.

2. Building the Two-Way App From Source
    2.1. Check out the Code
        The source can either be downloaded as a zip archive or by using git.
        2.1.1. Downloading Archive
            Go to https://github.com/robertzas/7bit and click the
            "Download Zip" button.  Extract the 7bit-master folder from the
            archive.
        2.1.2. Checkout Using Git
            Open a command prompt or bash and navigate to the location you want
            the source saved to.  Run the following command:
                git clone https://github.com/robertzas/7bit
            Enter your GitHub login and password if prompted.  The code will be
            checked out to a folder called 7bit in the current directory.
    2.2. Build the Two-Way App
        Open a command prompt or bash screen and navigate to the 7bit folder.
        Run the following command:
            gradlew clean packageDebug
        The .apk file will be created in the 7bit\app\build\outputs\apk folder.

3. Installing the Two-Way App
    3.1. Copy the .apk file to your android device.  Open a file explorer and
        navigate to the location you copied the .apk file to.
    3.2. Touch the .apk file and then touch the "Install" button to install it.
        3.2.1. If an "Install blocked" message is displayed, continue with this
            section.  Otherwise, skip to section 3.3.
        3.2.2. From the "Install blocked" message, touch the "Settings" button
            to go to android security settings.
        3.2.3. Touch the box next to Unknown Sources, and then touch OK.  This
            will allow you to install the Two-Way app on your android device.
    3.3. Touch the "Install" button to accept permissions and finish installing
        the Two-Way app.
