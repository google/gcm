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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.android.gcm.demo.model.Constants.API_KEYS;
import static com.google.android.gcm.demo.model.Constants.GROUPS;
import static com.google.android.gcm.demo.model.Constants.NAME;
import static com.google.android.gcm.demo.model.Constants.OTHER_TOKENS;
import static com.google.android.gcm.demo.model.Constants.SENDER_ID;
import static com.google.android.gcm.demo.model.Constants.TEST_APP_TOKEN;
import static com.google.android.gcm.demo.model.Constants.TOPICS;

/**
 * This class is an address book entry corresponding to a server using GCM.
 */
public class Sender implements Comparable<Sender> {
    public String name;
    public String senderId;
    public List<String> apiKeys = new ArrayList<>();
    public String testAppToken;
    public ArrayMap<String, String> otherTokens = new ArrayMap<>();
    public ArrayMap<String, Boolean> topics = new ArrayMap<>();
    public ArrayMap<String, DeviceGroup> groups = new ArrayMap<>();


    static public Sender fromJson(JSONObject jsonObject) throws JSONException {
        Sender entry = new Sender();
        // read {"name": "serverName", "serverId": "123456789", "testAppToken": "generatedToken"}
        entry.name = jsonObject.getString(NAME);
        entry.senderId = jsonObject.getString(SENDER_ID);
        entry.testAppToken = jsonObject.optString(TEST_APP_TOKEN, null);
        // read "apiKeys": ["apikey1", "apikey2"]
        JSONArray jsonApiKeys = jsonObject.optJSONArray(API_KEYS);
        if (jsonApiKeys != null) {
            for (int index = 0; index < jsonApiKeys.length(); index++) {
                entry.apiKeys.add(jsonApiKeys.getString(index));
            }
        }
        // read "otherTokens": {"name1" : "token1", "name2" : "token2"}
        JSONObject jsonOtherTokens = jsonObject.optJSONObject(OTHER_TOKENS);
        if (jsonOtherTokens != null) {
            Iterator<String> jsonOtherTokensIterator = jsonOtherTokens.keys();
            while (jsonOtherTokensIterator.hasNext()) {
                String otherTokenName = jsonOtherTokensIterator.next();
                entry.otherTokens.put(otherTokenName, jsonOtherTokens.getString(otherTokenName));
            }
        }
        // read "topics": {"topic1" : true, "topic2" : false}
        JSONObject jsonTopics = jsonObject.optJSONObject(TOPICS);
        if (jsonTopics != null) {
            Iterator<String> jsonTopicsIterator = jsonTopics.keys();
            while (jsonTopicsIterator.hasNext()) {
                String topicName = jsonTopicsIterator.next();
                entry.topics.put(topicName, jsonTopics.getBoolean(topicName));
            }
        }
        // read "groups": [{group1}, {group2}]
        JSONArray jsonGroups = jsonObject.optJSONArray(GROUPS);
        if (jsonGroups != null) {
            for (int index = 0; index < jsonGroups.length(); index++) {
                DeviceGroup deviceGroup = DeviceGroup.fromJson(jsonGroups.getJSONObject(index));
                entry.groups.put(deviceGroup.notificationKeyName, deviceGroup);
            }
        }
        return entry;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(NAME, name);
        jsonObject.put(SENDER_ID, senderId);
        jsonObject.put(API_KEYS, new JSONArray(apiKeys));
        jsonObject.putOpt(TEST_APP_TOKEN, testAppToken);
        jsonObject.put(OTHER_TOKENS, new JSONObject(otherTokens));
        jsonObject.put(TOPICS, new JSONObject(topics));

        JSONArray jsonGroups = new JSONArray();
        for (DeviceGroup deviceGroup : groups.values()) {
            jsonGroups.put(deviceGroup.toJson());
        }
        jsonObject.put(GROUPS, jsonGroups);

        return jsonObject;
    }


    @Override
    public int compareTo(Sender sender) {
        if (sender == null) {
            throw new NullPointerException();
        }
        if (name == null) {
            if (sender.name == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (sender.name == null) {
                return 1;
            } else {
                return this.name.compareTo(sender.name);
            }
        }
    }
}
