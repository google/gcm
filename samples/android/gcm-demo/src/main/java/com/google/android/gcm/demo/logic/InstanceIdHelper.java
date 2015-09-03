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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.model.Token;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.AbstractFragment;
import com.google.android.gcm.demo.ui.MainActivity;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class used to register and unregister the app for GCM.
 * Registration involves getting the app's instance id and using it to request a token with
 * the scope {@link GoogleCloudMessaging.INSTANCE_ID_SCOPE} and the audience set to the project's
 * id.
 */
public class InstanceIdHelper {

    private final Context mContext;
    private final LoggingService.Logger mLogger;
    private SenderCollection mSenders;

    public InstanceIdHelper(Context context) {
        mContext = context;
        mLogger = new LoggingService.Logger(mContext);
        mSenders = SenderCollection.getInstance(context);
    }

    /**
     * Get a Instance ID authorization Token
     */
    public void getTokenInBackground(final String authorizedEntity, final String scope,
                                        final Bundle extras) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    String token = InstanceID.getInstance(mContext)
                            .getToken(authorizedEntity, scope, extras);
                    mLogger.log(Log.INFO, "getToken succeeded." +
                            "\nsenderId: " + authorizedEntity + "\ntoken: " + token);
                    MainActivity.showToast(mContext, R.string.iid_get_token_toast_success,
                            AbstractFragment.truncateToLongString(token));

                    // Save the token in the address book
                    Sender entry = mSenders.getSender(authorizedEntity);
                    if (entry == null) {
                        mLogger.log(Log.ERROR, "Could not save token, missing sender id");
                        return null;
                    }
                    Token tokenModel = new Token();
                    tokenModel.token = token;
                    tokenModel.scope = scope;
                    if (extras != null) {
                        for (String key : extras.keySet()) {
                            tokenModel.extras.put(key, extras.getString(key));
                        }
                    }
                    tokenModel.createdAt = System.currentTimeMillis();
                    entry.appTokens.put(token, tokenModel);
                    mSenders.updateSender(entry);

                } catch (final IOException e) {
                    mLogger.log(Log.INFO, "getToken failed." +
                            "\nsenderId: " + authorizedEntity + "\nerror: " + e.getMessage());
                    MainActivity.showToast(mContext, R.string.iid_toast_error, e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    /**
     * Unregister by deleting the token
     */
    public void deleteTokenInBackground(final String authorizedEntity, final String scope) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InstanceID.getInstance(mContext).deleteToken(authorizedEntity, scope);
                    mLogger.log(Log.INFO, "delete token succeeded." +
                            "\nsenderId: " + authorizedEntity);
                    MainActivity.showToast(mContext, R.string.iid_delete_token_toast_success);

                    Sender entry = mSenders.getSender(authorizedEntity);
                    if (entry == null) {
                        mLogger.log(Log.ERROR, "Could not remove token, missing sender id");
                        return null;
                    }
                    // In rare cases multiple token with same authorizedEntity:scope could exists
                    // example: during a rotation period, the app could have for the same
                    // authorizedEntity:scope an old token and a newer one.
                    List<String> toBeRemoved = new LinkedList<>();
                    for (Token token : entry.appTokens.values()) {
                        if (token.scope.equals(scope)) {
                            toBeRemoved.add(token.token);
                        }
                    }
                    for (String token : toBeRemoved) {
                        entry.appTokens.remove(token);
                    }
                    mSenders.updateSender(entry);
                } catch (final IOException e) {
                    mLogger.log(Log.INFO, "remove token failed." +
                            "\nsenderId: " + authorizedEntity + "\nerror: " + e.getMessage());
                    MainActivity.showToast(mContext, R.string.iid_toast_error, e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    public String getInstanceId() {
        return InstanceID.getInstance(mContext).getId();
    }

    public long getCreationTime() {
        return InstanceID.getInstance(mContext).getCreationTime();
    }

    public void deleteInstanceIdInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InstanceID.getInstance(mContext).deleteInstanceID();
                    mLogger.log(Log.INFO, "delete instanceId succeeded.");
                    MainActivity.showToast(mContext, R.string.iid_delete_token_toast_success);

                    // Remove all appTokens and topics subscriptions tied to any Sender.
                    for (int i = 0;  i < mSenders.getSenders().size(); i++) {
                        Sender sender = mSenders.getSenders().valueAt(i);
                        boolean senderIsDirty = false;
                        if (sender.appTokens.size() > 0) {
                            sender.appTokens.clear();
                            senderIsDirty = true;
                        }
                        for (String topic : sender.topics.keySet()) {
                            if (sender.topics.get(topic)) {
                                sender.topics.put(topic, false);
                                senderIsDirty = true;
                            }
                        }
                        if (senderIsDirty) {
                            mSenders.updateSender(sender);
                        }
                    }
                } catch (final IOException e) {
                    mLogger.log(Log.INFO, "delete instanceId failed.\nerror: " + e.getMessage());
                    MainActivity.showToast(mContext, R.string.iid_toast_error, e.getMessage());
                }
                return null;
            }
        }.execute();
    }
}
