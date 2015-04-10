package com.intercom.video.twoway.Interfaces;

import com.intercom.video.twoway.Models.ContactsEntity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by charles on 4/7/15.
 */
public interface UpdateDeviceListInterface {
    public void updateDeviceListFromHashMap(
            ConcurrentHashMap<String, ContactsEntity> deviceList);
}
