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

import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.service.LoggingService;

import java.io.IOException;

/**
 * This class used to subscribe and unsubscribe to topics.
 */
public class PubSubHelper {

    private final Context mContext;
    private final LoggingService.Logger mLogger;
    private final SenderCollection mSenders;

    public PubSubHelper(Context context) {
        mContext = context;
        mLogger = new LoggingService.Logger(mContext);
        mSenders = SenderCollection.getInstance(mContext);
    }

    /**
     *
     * @param senderId the project id used by the app's server
     * @param gcmToken the registration token obtained by registering
     * @param topic the topic to subscribe to
     * @param extras bundle with extra parameters
     */
    public void subscribeTopic(final String senderId, final String gcmToken,
                               final String topic, final Bundle extras) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    GcmPubSub.getInstance(mContext).subscribe(gcmToken, topic, extras);
                    mLogger.log(Log.INFO, "topic subscription succeeded."
                            + "\ngcmToken: " + gcmToken
                            + "\ntopic: " + topic
                            + "\nextras: " + extras);
                    // Save the token in the address book
                    Sender entry = mSenders.getSender(senderId);
                    if (entry == null) {
                        mLogger.log(Log.ERROR, "Could not subscribe to topic, missing sender id");
                        return null;
                    }
                    entry.topics.put(topic, true);
                    mSenders.updateSender(entry);
                } catch (IOException | IllegalArgumentException e) {
                    mLogger.log(Log.INFO, "topic subscription failed."
                            + "\nerror: " + e.getMessage()
                            + "\ngcmToken: " + gcmToken
                            + "\ntopic: " + topic
                            + "\nextras: " + extras);
                }
                return null;
            }
        }.execute();
    }

    /**
     *
     * @param senderId the project id used by the app's server
     * @param gcmToken the registration token obtained by registering
     * @param topic the topic to unsubscribe from
     */
    public void unsubscribeTopic(final String senderId, final String gcmToken, final String topic) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    GcmPubSub.getInstance(mContext).unsubscribe(gcmToken, topic);
                    mLogger.log(Log.INFO, "topic unsubscription succeeded."
                            + "\ngcmToken: " + gcmToken
                            + "\ntopic: " + topic);
                    // Save the token in the address book
                    Sender entry = mSenders.getSender(senderId);
                    if (entry == null) {
                        mLogger.log(Log.ERROR, "Could not save token, missing sender id");
                        return null;
                    }
                    entry.topics.put(topic, false);
                    mSenders.updateSender(entry);
                } catch (IOException | IllegalArgumentException e) {
                    mLogger.log(Log.INFO, "topic unsubscription failed."
                            + "\nerror: " + e.getMessage()
                            + "\ngcmToken: " + gcmToken
                            + "\ntopic: " + topic);
                }
                return null;
            }
        }.execute();
    }

}
