package com.intercom.video.twoway;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import com.intercom.video.twoway.Models.ContactsEntity;


public class SettingsFragment extends Fragment {
    private EditText deviceNickname;
    private CheckBox useCameraView;
    private ImageView profilePicture;
    private SharedPreferenceAccessor sharedPreferenceAccessor;
    private Uri mCurrentPhotoPath;
    private int UPDATE_PROFILE_PICTURE = 446;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_menu, container, false);

        initializeComponents(view);
        loadProfilePictureFromPreferences();
        activateSettingsMenuListeners();
        doRememberDeviceNic();
        doRememberCameraViewFlag();

        // Inflate the menu; this adds items to the action bar if it is present.
        //parentActivity.getMenuInflater().inflate(R.menu.menu_main, menu);
        return view;
    }

    private void initializeComponents(View view) {
        this.deviceNickname = (EditText) view.findViewById(R.id.settings_menu_editText_deviceNic);
        this.useCameraView = (CheckBox) view.findViewById(R.id.settings_menu_checkBox_usecamaraview);
        this.profilePicture = (ImageView) view.findViewById(R.id.imageView_device_avatar);
        this.sharedPreferenceAccessor = new SharedPreferenceAccessor(this.getActivity());
    }

    public void activateSettingsMenuListeners() {
        /*
        auto-triggered when device nic is changed
         */

        // auto-save on text change for deviceNIC in settings menu
        this.deviceNickname.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setDeviceNic(s.toString());
            }
        });


        // auto-save on checkbox flag change
        this.useCameraView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setUseCameraViewFlag(isChecked);
            }
        });

        this.profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeProfilePicture();
            }
        });

    }

    public void setUseCameraViewFlag(boolean isChecked) {
        this.sharedPreferenceAccessor.writeBooleanToSharedPrefs(
                SharedPreferenceAccessor.SETTINGS_MENU,
                isChecked, SharedPreferenceAccessor.USE_CAMERA_VIEW);
    }

    public void setDeviceNic(String newDeviceNickname) {
        this.sharedPreferenceAccessor.writeStringToSharedPrefs(
                SharedPreferenceAccessor.SETTINGS_MENU,
                SharedPreferenceAccessor.DEVICE_NICKNAME, newDeviceNickname);
    }

    public boolean getUseCameraViewFlag() {
        return this.sharedPreferenceAccessor.loadBooleanFromSharedPreferences(
                SharedPreferenceAccessor.SETTINGS_MENU,
                SharedPreferenceAccessor.USE_CAMERA_VIEW);
    }

    public String getDeviceNic() {
        return this.sharedPreferenceAccessor.loadStringFromSharedPreferences(
                SharedPreferenceAccessor.SETTINGS_MENU,
                SharedPreferenceAccessor.DEVICE_NICKNAME);
    }

    public void doRememberCameraViewFlag() {
        boolean mCameraFlag = getUseCameraViewFlag();
        this.useCameraView.setChecked(mCameraFlag);
    }

    public void doRememberDeviceNic() {
        String mDeviceNic = getDeviceNic();
        this.deviceNickname.setText(mDeviceNic);
    }

    //These were just in the main method in my old repository... should have been a little bit smarter
    //With how i handled that, but oh well

    //Method called from settings fragment to take a profile picture for you device
    public void takeProfilePicture() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "ProfilePicture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        mCurrentPhotoPath = this.getActivity().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);
        startActivityForResult(intent, UPDATE_PROFILE_PICTURE);
    }

    //Called when going to settings layout, or when a device wants the contacts entity
    private void setProfilePicture() {
        // Returns the Uri for a photo stored on disk given the fileName
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(getRealPathFromURI(mCurrentPhotoPath));
            this.profilePicture.setImageBitmap(bitmap);
            saveProfilePictures(bitmap);
        } catch (Exception e) {
            Log.d("No Image", "Profile Image does not exist");
        }
    }

    private void loadProfilePictureFromPreferences() {
        String picture = this.sharedPreferenceAccessor.loadStringFromSharedPreferences(
                SharedPreferenceAccessor.SETTINGS_MENU, SharedPreferenceAccessor.PROFILE_PICTURE);
        if (picture.equals(SharedPreferenceAccessor.NO_SUCH_SAVED_PREFERENCE)) {
            Log.d("No Profile Picture", "There is no saved Profile picture");
        } else {
            // Returns the Uri for a photo stored on disk given the fileName
            try {
                Bitmap bitmap = ContactsEntity.decodePictureFromBase64(picture);
                this.profilePicture.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.d("No Image", "Profile Image does not exist");
            }
        }
    }

    //Returns the actual path that can be used to grab the picture.
    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getActivity().getContentResolver().query(contentUri, proj,
                null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private void saveProfilePictures(Bitmap picture) {
        this.sharedPreferenceAccessor.writeStringToSharedPrefs(
                SharedPreferenceAccessor.PROFILE_PICTURE,
                ContactsEntity.encodePictureToBase64(picture), SharedPreferenceAccessor.SETTINGS_MENU);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == UPDATE_PROFILE_PICTURE) {
                setProfilePicture();
            }
        }
    }

}
