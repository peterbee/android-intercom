package com.intercom.video.twoway.Models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

/**
 * Created by charles on 3/9/15.
 */
public class ContactsEntity implements Serializable {

    private String deviceName;
    private String picture;
    private String ip;

    /**
     * Constructor
     *
     * @param deviceName
     * @param picture
     * @param ip
     */
    public ContactsEntity(String deviceName, Bitmap picture, String ip) {
        this.deviceName = deviceName;
        this.picture = ContactsEntity.encodePictureToBase64(picture);
        this.ip = stripIp(ip);
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }


    public void setPicture(Bitmap picture) {
        this.picture = ContactsEntity.encodePictureToBase64(picture);
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return this.ip;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public Bitmap getPicture() {
        if (picture != null) {
            return ContactsEntity.decodePictureFromBase64(picture);
        } else {
            return null;
        }

    }

    /**
     * Overwrote Equals to ensure that all objects within are equal.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContactsEntity that = (ContactsEntity) o;

        if (deviceName != null ? !deviceName.equals(that.deviceName) : that.deviceName != null)
            return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (picture != null ? !picture.equals(that.picture) : that.picture != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceName != null ? deviceName.hashCode() : 0;
        result = 31 * result + (picture != null ? picture.hashCode() : 0);
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        return result;
    }

    /**
     * Strips out the port identifier from any IPs we have discovered
     *
     * @param ipToStrip
     * @return
     */
    private String stripIp(String ipToStrip) {
        if (ipToStrip == null) {
            return "";
        }
        if (!ipToStrip.contains(":")) {
            return ipToStrip;
        }
        String[] splitIp = ipToStrip.split(":");
        return splitIp[0];
    }

    /**
     * Takes in a Bitmap, adds JPEG compression, converts to base64 in a byte array, and then
     * stores the result in a String.
     *
     * @param picture
     * @return
     */
    public static String encodePictureToBase64(Bitmap picture) {
        if (picture == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        picture.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }

    /**
     * Takes in a String which has been encoded using encodePictureToBase64 and decodes it back
     * into a Bitmap
     *
     * @param picToDecode
     * @return
     */
    public static Bitmap decodePictureFromBase64(String picToDecode) {
        byte[] b = Base64.decode(picToDecode, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
        return bitmap;
    }

}
