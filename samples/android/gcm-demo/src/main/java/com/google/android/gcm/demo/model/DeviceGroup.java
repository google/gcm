/*
Copyright 2015 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.google.android.gcm.demo.model;

import android.support.v4.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import static com.google.android.gcm.demo.model.Constants.NOTIFICATION_KEY;
import static com.google.android.gcm.demo.model.Constants.NOTIFICATION_KEY_NAME;
import static com.google.android.gcm.demo.model.Constants.TOKENS;

/**
 * This class is used to persist a {@link Sender}'s groups
 */
public class DeviceGroup {

    public String notificationKeyName;
    public String notificationKey;
    public ArrayMap<String, String> tokens = new ArrayMap<>();

    static public DeviceGroup fromJson(JSONObject json) throws JSONException {
        DeviceGroup deviceGroup = new DeviceGroup();

        // read {"notificationKeyName": "name", "notificationKey": "123456789"}
        deviceGroup.notificationKeyName = json.getString(NOTIFICATION_KEY_NAME);
        deviceGroup.notificationKey = json.optString(NOTIFICATION_KEY);

        // read "tokens": {"token1" : "1234", "token2" : "3434"}
        JSONObject jsonTokens = json.optJSONObject(TOKENS);
        if (jsonTokens != null) {
            Iterator<String> jsonTokensIterator = jsonTokens.keys();
            while (jsonTokensIterator.hasNext()) {
                String tokenName = jsonTokensIterator.next();
                deviceGroup.tokens.put(tokenName, jsonTokens.getString(tokenName));
            }
        }
        return deviceGroup;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(NOTIFICATION_KEY_NAME, notificationKeyName);
        json.put(NOTIFICATION_KEY, notificationKey);
        json.put(TOKENS, new JSONObject(tokens));
        return json;
    }
}
