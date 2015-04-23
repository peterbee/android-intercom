package com.intercom.video.twoway.Fragments;

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

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.Models.ContactsEntity;
import com.intercom.video.twoway.R;
import com.intercom.video.twoway.Utilities.SharedPreferenceAccessor;


public class SettingsFragment extends Fragment {
    private EditText deviceNickname;
    private CheckBox useCameraView;
    private ImageView profilePicture;
    private SharedPreferenceAccessor sharedPreferenceAccessor;
    private Uri mCurrentPhotoPath;
    private int UPDATE_PROFILE_PICTURE = 446;
    private ProfileController profileController;
    private ProfileControllerTransferInterface mListener;
    private ContactsEntity profile;

    /**
     * Lifecycle method that is called when the fragment view is created.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_menu, container, false);

        initializeComponents(view);
        loadProfilePicture();
        activateSettingsMenuListeners();
        doRememberDeviceNic();
        doRememberCameraViewFlag();

        // Inflate the menu; this adds items to the action bar if it is present.
        //parentActivity.getMenuInflater().inflate(R.menu.menu_main, menu);
        return view;
    }

    /**
     * Lifecycle method that is called before onCreate to initialize components.
     * @param view
     */
    private void initializeComponents(View view) {
        this.deviceNickname = (EditText) view.findViewById(R.id.settings_menu_editText_deviceNic);
        this.useCameraView = (CheckBox) view.findViewById(R.id.settings_menu_checkBox_usecamaraview);
        this.profilePicture = (ImageView) view.findViewById(R.id.imageView_device_avatar);
        this.profileController = mListener.retrieveProfileController();
        this.profile = profileController.getProfile();
        this.sharedPreferenceAccessor = new SharedPreferenceAccessor(this.getActivity());
    }

    /**
     * auto-triggered when device nic is changed
     */
    public void activateSettingsMenuListeners() {

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

    /**
     * Action Handler for the Use Camera View checkbox
     * @param isChecked
     */
    public void setUseCameraViewFlag(boolean isChecked) {
        this.sharedPreferenceAccessor.writeBooleanToSharedPrefs(
                SharedPreferenceAccessor.USE_CAMERA_VIEW,
                isChecked, SharedPreferenceAccessor.SETTINGS_MENU);
    }

    /**
     * Updates the Device nickname
     * @param newDeviceNickname
     */
    public void setDeviceNic(String newDeviceNickname) {
        this.profileController.updateProfileName(newDeviceNickname);
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

    /**
     * Method called from settings fragment to take a profile picture for you device
     * Likely needs to be moved
     */
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

    /**
     * Called when going to settings layout, or when a device wants the contacts entity
     */
    private void setProfilePicture() {
        // Returns the Uri for a photo stored on disk given the fileName
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(getRealPathFromURI(mCurrentPhotoPath));
            if(bitmap == null)
            {
                return;
            }
            this.profilePicture.setImageBitmap(bitmap);
            saveProfilePictures(bitmap);
        } catch (Exception e) {
            Log.d("No Image", "Profile Image does not exist");
        }
    }

    private void loadProfilePicture() {
        try {
            this.profilePicture.setImageBitmap(profile.getPicture());
        } catch (Exception e) {
                Log.d("No Image", "Profile Image does not exist");
        }
    }

    /**
     * Returns the actual path that can be used to grab the picture.
     * @param contentUri
     * @return
     */
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
        this.profile = this.profileController.updateProfilePicture(picture);
    }

    /**
     * Action Handler for handling when the picture is updated.
     * @param requestCode
     * @param resultCode
     * @param imageReturnedIntent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == UPDATE_PROFILE_PICTURE) {
                setProfilePicture();
            }
        }
    }


    public interface ProfileControllerTransferInterface {
        public ProfileController retrieveProfileController();
    }

    /**
     * Lifecycle method called when the fragment is attached to the activity
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ProfileControllerTransferInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "CUSTOM ERROR: must implement " +
                    "ProfileControllerTransferInterface");
        }
    }

}
