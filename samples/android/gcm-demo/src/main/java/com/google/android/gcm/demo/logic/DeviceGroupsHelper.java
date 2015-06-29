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
package com.google.android.gcm.demo.logic;

import static com.google.android.gcm.demo.logic.HttpRequest.CONTENT_TYPE_JSON;
import static com.google.android.gcm.demo.logic.HttpRequest.HEADER_AUTHORIZATION;
import static com.google.android.gcm.demo.logic.HttpRequest.HEADER_CONTENT_TYPE;
import static com.google.android.gcm.demo.logic.HttpRequest.HEADER_PROJECT_ID;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.DeviceGroup;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;


public class DeviceGroupsHelper {

    private static final String GCM_GROUPS_ENDPOINT =
            "https://gcm-http.googleapis.com/gcm/notification";

    private final Context mContext;
    private final LoggingService.Logger mLogger;
    private final SenderCollection mSenders;


    public DeviceGroupsHelper(Context context) {
        mContext = context.getApplicationContext();
        mLogger = new LoggingService.Logger(mContext);
        mSenders = SenderCollection.getInstance(mContext);
    }

    /**
     * Execute the HTTP call to create the Device Group in background.
     *
     * <code>
     *   Content-Type: application/json
     *   Authorization: key=API_KEY
     *   project_id: SENDER_ID
     *   {
     *     "operation": "create",
     *     "notification_key_name": "appUser-Chris",
     *     "registration_ids": ["4", "8", "15", "16", "23", "42"]
     *   }
     * </code>
     */
    public void asyncCreateGroup(final String senderId, final String apiKey,
                                 final String groupName, Bundle members) {
        final Bundle newMembers = new Bundle(members);
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    HttpRequest httpRequest = new HttpRequest();
                    httpRequest.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
                    httpRequest.setHeader(HEADER_AUTHORIZATION, "key=" + apiKey);
                    httpRequest.setHeader(HEADER_PROJECT_ID, senderId);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("operation", "create");
                    requestBody.put("notification_key_name", groupName);
                    requestBody.put("registration_ids",
                            new JSONArray(bundleValues2Array(newMembers)));

                    httpRequest.doPost(GCM_GROUPS_ENDPOINT, requestBody.toString());

                    JSONObject responseBody = new JSONObject(httpRequest.getResponseBody());

                    if (responseBody.has("error")) {
                        mLogger.log(Log.INFO, "Group creation failed."
                                + "\ngroupName: " + groupName
                                + "\nhttpResponse:" + httpRequest.getResponseBody());
                        showToast(R.string.group_toast_group_creation_failed,
                                responseBody.getString("error"));
                    } else {
                        // Store the group in the local storage.
                        DeviceGroup group = new DeviceGroup();
                        group.notificationKeyName = groupName;
                        group.notificationKey = responseBody.getString("notification_key");
                        for (String name : newMembers.keySet()) {
                            group.tokens.put(name, newMembers.getString(name));
                        }

                        Sender sender = mSenders.getSender(senderId);
                        sender.groups.put(group.notificationKeyName, group);
                        mSenders.updateSender(sender);

                        mLogger.log(Log.INFO, "Group creation succeeded."
                                + "\ngroupName: " + group.notificationKeyName
                                + "\ngroupKey: " + group.notificationKey);
                        showToast(R.string.group_toast_group_creation_succeeded);
                    }
                } catch (JSONException | IOException e) {
                    mLogger.log(Log.INFO, "Exception while creating a new group"
                            + "\nerror: " + e.getMessage()
                            + "\ngroupName: " + groupName);
                    showToast(R.string.group_toast_group_creation_failed, e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    /**
     * Execute the HTTP call to delete a Device Group.
     *
     * This is obtained by removing all the members of the group.
     */
    public void asyncDeleteGroup(final String senderId, final String apiKey,
                                 final String groupName) {
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                Sender sender = mSenders.getSender(senderId);
                if (sender == null) {
                    return null;
                }
                DeviceGroup group = sender.groups.get(groupName);
                if (group == null) {
                    return null;
                }
                Bundle members2Remove = new Bundle();
                for (String name : group.tokens.keySet()) {
                    members2Remove.putString(name, group.tokens.get(name));
                }
                if (members2Remove.size() > 0) {
                    removeMembers(senderId, apiKey, groupName,
                            group.notificationKey, members2Remove);
                }
                sender.groups.remove(group.notificationKeyName);
                mSenders.updateSender(sender);
                return null;
            }
        }.execute();
    }

    /**
     * Execute in background the HTTP calls to add and remove members.
     */
    public void asyncUpdateGroup(final String senderId, final String apiKey,
                                 final String groupName, final String groupKey,
                                 Bundle newMembers, Bundle removedMembers) {
        final Bundle members2Add = new Bundle(newMembers);
        final Bundle members2Remove = new Bundle(removedMembers);
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                if (members2Add.size() > 0) {
                    addMembers(senderId, apiKey, groupName, groupKey, members2Add);
                }
                if (members2Remove.size() > 0) {
                    removeMembers(senderId, apiKey, groupName, groupKey, members2Remove);
                }
                return null;
            }
        }.execute();
    }

    /**
     * Execute the HTTP call to remove registration_ids from a the Device Group.
     *
     * <code>
     *   Content-Type: application/json
     *   Authorization: key=API_KEY
     *   project_id: SENDER_ID
     *   {
     *     "operation": "add",
     *     "notification_key_name": "appUser-Chris",
     *     "notification_key": "aUniqueKey",
     *     "registration_ids": ["4", "8", "15", "16", "23", "42"]
     *   }
     * </code>
     */
    public void addMembers(String senderId, String apiKey, String groupName,
                           String groupKey, Bundle members) {
        try {
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
            httpRequest.setHeader(HEADER_AUTHORIZATION, "key=" + apiKey);
            httpRequest.setHeader(HEADER_PROJECT_ID, senderId);

            JSONObject requestBody = new JSONObject();
            requestBody.put("operation", "add");
            requestBody.put("notification_key_name", groupName);
            requestBody.put("notification_key", groupKey);
            requestBody.put("registration_ids", new JSONArray(bundleValues2Array(members)));

            httpRequest.doPost(GCM_GROUPS_ENDPOINT, requestBody.toString());

            JSONObject responseBody = new JSONObject(httpRequest.getResponseBody());

            if (responseBody.has("error")) {
                mLogger.log(Log.INFO, "Error while adding new group members."
                        + "\ngroupName: " + groupName
                        + "\ngroupKey: " + groupKey
                        + "\nhttpResponse: " + httpRequest.getResponseBody());
                showToast(R.string.group_toast_add_members_failed,
                        responseBody.getString("error"));
            } else {
                // Store the group in the local storage.
                Sender sender = mSenders.getSender(senderId);
                DeviceGroup newGroup = sender.groups.get(groupName);
                for(String name : members.keySet()) {
                    newGroup.tokens.put(name, members.getString(name));
                }
                mSenders.updateSender(sender);

                mLogger.log(Log.INFO, "Group members added successfully."
                        + "\ngroupName: " + groupName
                        + "\ngroupKey: " + groupKey);
                showToast(R.string.group_toast_add_members_succeeded);
            }
        } catch (JSONException | IOException e) {
            mLogger.log(Log.INFO, "Exception while adding new group members."
                    + "\nerror: " + e.getMessage()
                    + "\ngroupName: " + groupName
                    + "\ngroupKey: " + groupKey);
            showToast(R.string.group_toast_add_members_failed, e.getMessage());
        }
    }

    /**
     * Execute the HTTP call to remove registration_ids from a the Device Group.
     *
     * <code>
     *   Content-Type: application/json
     *   Authorization: key=API_KEY
     *   project_id: SENDER_ID
     *   {
     *     "operation": "remove",
     *     "notification_key_name": "appUser-Chris",
     *     "notification_key": "aUniqueKey",
     *     "registration_ids": ["4", "8", "15", "16", "23", "42"]
     *   }
     * </code>
     */
    public void removeMembers(String senderId, String apiKey, String groupName,
                              String groupKey, Bundle members) {
        try {
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
            httpRequest.setHeader(HEADER_AUTHORIZATION, "key=" + apiKey);
            httpRequest.setHeader(HEADER_PROJECT_ID, senderId);

            JSONObject requestBody = new JSONObject();
            requestBody.put("operation", "remove");
            requestBody.put("notification_key_name", groupName);
            requestBody.put("notification_key", groupKey);
            requestBody.put("registration_ids", new JSONArray(bundleValues2Array(members)));

            httpRequest.doPost(GCM_GROUPS_ENDPOINT, requestBody.toString());

            JSONObject responseBody = new JSONObject(httpRequest.getResponseBody());

            if (responseBody.has("error")) {
                mLogger.log(Log.INFO, "Error while removing group members."
                    + "\ngroupName: " + groupName
                    + "\ngroupKey: " + groupKey
                    + "\nhttpResponse: " + httpRequest.getResponseBody());
                showToast(R.string.group_toast_remove_members_failed,
                        responseBody.getString("error"));
            } else {
                // Store the group in the local storage.
                SenderCollection senders = SenderCollection.getInstance(mContext);
                Sender sender = senders.getSender(senderId);
                DeviceGroup newGroup = sender.groups.get(groupName);
                for(String name : members.keySet()) {
                    newGroup.tokens.remove(name);
                }
                senders.updateSender(sender);

                mLogger.log(Log.INFO, "Group members removed successfully."
                        + "\ngroupName: " + groupName
                        + "\ngroupKey: " + groupKey);
                showToast(R.string.group_toast_remove_members_succeeded);
            }
        } catch (JSONException | IOException e) {
            mLogger.log(Log.INFO, "Exception while removing group members."
                    + "\nerror: " + e.getMessage()
                    + "\ngroupName: " + groupName
                    + "\ngroupKey: " + groupKey);
            showToast(R.string.group_toast_remove_members_failed, e.getMessage());
        }
    }

    private List<String> bundleValues2Array(Bundle bundle) {
        ArrayList<String> values = new ArrayList<>();
        for (String key : bundle.keySet()) {
            values.add(bundle.getString(key));
        }
        return values;
    }

    private void showToast(final int msgId, final Object... args) {
        Handler handler = new Handler(mContext.getMainLooper());
        handler.post( new Runnable(){
            public void run(){
                Toast.makeText(mContext, mContext.getString(msgId, args), Toast.LENGTH_LONG).show();
            }
        });
    }
}
