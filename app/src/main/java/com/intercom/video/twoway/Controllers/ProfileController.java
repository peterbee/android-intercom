package com.intercom.video.twoway.Controllers;

import android.util.Log;

import com.intercom.video.twoway.Models.ContactsEntity;
import com.intercom.video.twoway.Network.NetworkConstants;
import com.intercom.video.twoway.Network.Tcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
/**
 * Must be in Listener Service - Proof of concept
 * Assumes there
 */
public class ProfileController {
    private static int PORT = 6644;
    private static int PREPARE_FOR_ENTRY = 1;
    private static int PREPARED_FOR_ENTRY = 2;

    private ArrayList<ContactsEntity> contacts;
    private ContactsEntity currentDevice;
    private InputStream tcpIn;
    private OutputStream tcpOut;
    private Socket tcpSocket;
    private ServerSocket serverSocket;
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    private Tcp tcpEngine;

    /*Passes wifi manager and bitmap pic for now for testing purposes, need to already have device
        Profile set up
    */
    public void ProfileController()
    {
        tcpEngine = new Tcp();
        //Hard Coding Values for testing Purposes
        contacts = new ArrayList<ContactsEntity>();
        initializeDeviceProfile();
    }

    //Add Contact To Master List
    public void addContact(ContactsEntity contactToAdd)
    {
        if(!this.contacts.contains(contactToAdd))
        {
            this.contacts.add(contactToAdd);
        }
        else
        {
            Log.i("Duplicate Contact", "Profile Controller attempted to add a duplicate contact");
        }
    }

    //Some Collection of profiles
    //Dunno if it's stored on the device
    public void sendDeviceInfoByIp(String ip)
    {
        Thread deviceProfileTransfer = new Thread() {
            public void run(){
                try
                {
                    int response;
                    boolean entry = false;
                    serverSocket = new ServerSocket(PORT);
                    tcpSocket = serverSocket.accept();
                    objectOut = new ObjectOutputStream(tcpSocket.getOutputStream());
                    objectIn = new ObjectInputStream(tcpSocket.getInputStream());



                    tcpOut.write(ProfileController.PREPARE_FOR_ENTRY);
                    tcpOut.flush();

                    response = tcpIn.read();

                    if(response == ProfileController.PREPARE_FOR_ENTRY)
                    {
                        entry = true;
                    }

                    if(entry) {
                        objectOut.writeObject(currentDevice);
                        objectOut.flush();
                    }
                }
                catch(Exception e)
                {
                    Log.i("Bad Profile Transfer", "There was an error transferring profiles");
                }
            }
        };
    }

    public void getDeviceInformation(String ip)
    {
        tcpEngine.connectToDevice(ip, NetworkConstants.PROFILE);
    }

    public void receiveDeviceInfoByIP(String ip)
    {
        Thread deviceProfileTransfer = new Thread() {
            public void run(){
                try
                {
                    int response;
                    boolean entry = false;
                    serverSocket = new ServerSocket(PORT);
                    tcpSocket = serverSocket.accept();
                    objectOut = new ObjectOutputStream(tcpSocket.getOutputStream());
                    objectIn = new ObjectInputStream(tcpSocket.getInputStream());

                    response = tcpIn.read();

                    if(response == ProfileController.PREPARE_FOR_ENTRY)
                    {
                        tcpOut.write(ProfileController.PREPARE_FOR_ENTRY);
                        tcpOut.flush();
                        entry = true;
                    }

                    if(entry) {
                        addContact((ContactsEntity) objectIn.readObject());
                    }
                }
                catch(Exception e)
                {
                    Log.i("Bad Profile Transfer", "There was an error transferring profiles");
                }
            }
        };
    }

    private void initializeDeviceProfile()
    {
        //TODO: Need to initialize our profile

    }

    //These were just in the main method in my old repository... should have been a little bit smarter
    //With how i handled that, but oh well

    //Method called from settings fragment to take a profile picture for you device
//    public void takeProfilePicture(){
//        ContentValues values = new ContentValues();
//        values.put(MediaStore.Images.Media.TITLE, "ProfilePicture");
//        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
//        mCurrentPhotoPath = getContentResolver().insert(
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);
//        startActivityForResult(intent, UPDATE_PROFILE_PICTURE);
//    }
//
//    private File createImageFile() throws IOException {
//        // Create an image file name
//        String imageFileName = "ProfilePicture";
//        File storageDir = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
//
//        // Save a file: path for use with ACTION_VIEW intents
////        mCurrentPhotoPath = new Uri.Builder("file:" + image.getAbsolutePath());
//        return image;
//    }
//
//    private String loadFromSharedPreferences(String prefsName, String settingsTitle)
//    {
//        // Log.i(TAG,"getDeviceNic Called ");
//        String preferencesToReturn;
//        SharedPreferences settings = getApplicationContext().getSharedPreferences(prefsName, 0);
//        // Log.i(TAG,"DeviceNic recovered: "+settings.getString("device_nic","0"));
//        preferencesToReturn = settings.getString(settingsTitle, "0");
//        return preferencesToReturn;
//
//    }
//
//    //Called when going to settings layout, or when a device wants the contacts entity
//    private void setProfilePictures() {
//        // Returns the Uri for a photo stored on disk given the fileName
//        ImageView profilePic = (ImageView) findViewById(R.id.imageView_device_avatar);
//        try {
////            File f = new File(mCurrentPhotoPath);
////            Uri contentUri = Uri.fromFile(f);
//            Bitmap bitmap = BitmapFactory.decodeFile(getRealPathFromURI(mCurrentPhotoPath));
//            //Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentUri);
//            profilePic.setImageBitmap(bitmap);
//            saveProfilePictures(bitmap);
//        }
//        catch(Exception e)
//        {
//            Log.d("No Image", "Profile Image does not exist");
//        }
//    }
//
//    private void loadProfilePictureFromPreferences()
//    {
//        String picture = loadFromSharedPreferences("SETTINGS MENU", "Profile Picture");
//
//        if(picture.equals("0"))
//        {
//            Log.d("No Profile Picture", "There is no saved Profile picture");
//        }
//        else
//        {
//            // Returns the Uri for a photo stored on disk given the fileName
//            ImageView profilePic = (ImageView) findViewById(R.id.imageView_device_avatar);
//            try {
//                Bitmap bitmap = ContactsEntity.decodePictureFromBase64(picture);
//                profilePic.setImageBitmap(bitmap);
//            }
//            catch(Exception e)
//            {
//                Log.d("No Image", "Profile Image does not exist");
//            }
//        }
//    }
//
//    private void saveProfilePictures(Bitmap picture)
//    {
//        writeToSharedPrefs("Profile Picture", ContactsEntity.encodePictureToBase64(picture), "SETTINGS MENU");
//    }


}