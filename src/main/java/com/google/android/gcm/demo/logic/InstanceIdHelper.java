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
import android.util.Log;

import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

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
     * Register for GCM
     *
     * @param senderId the project id used by the app's server
     */
    public void getGcmTokenInBackground(final String senderId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    String token =
                            InstanceID.getInstance(mContext).getToken(senderId,
                                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    mLogger.log(Log.INFO, "registration succeeded." +
                            "\nsenderId: " + senderId + "\ntoken: " + token);
                    // Save the token in the address book
                    Sender entry = mSenders.getSender(senderId);
                    if (entry == null) {
                        mLogger.log(Log.ERROR, "Could not save token, missing sender id");
                        return null;
                    }
                    entry.testAppToken = token;
                    mSenders.updateSender(entry);
                } catch (final IOException e) {
                    mLogger.log(Log.INFO, "registration failed." +
                            "\nsenderId: " + senderId + "\nerror: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    /**
     * Unregister by deleting the token
     *
     * @param senderId the project id used by the app's server
     */
    public void deleteGcmTokeInBackground(final String senderId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InstanceID.getInstance(mContext).deleteToken(senderId,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    mLogger.log(Log.INFO, "delete token succeeded." +
                            "\nsenderId: " + senderId);
                    Sender entry = mSenders.getSender(senderId);
                    if (entry == null) {
                        mLogger.log(Log.ERROR, "Could not remove token, missing sender id");
                        return null;
                    }
                    entry.testAppToken = null;
                    mSenders.updateSender(entry);
                } catch (final IOException e) {
                    mLogger.log(Log.INFO, "remove token failed." +
                            "\nsenderId: " + senderId + "\nerror: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }
}
