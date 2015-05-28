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

import android.content.Context;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.android.gcm.demo.service.LoggingService;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Singleton class for managing GCM sender, tokens, API Keys and groups.
 * All data is stored in a file in the Internal Storage.
 */
public class SenderCollection {

    private static final String GCM_SENDERS_FILE = "gcm-addressbook.json";

    public static final String DEFAULT_SENDER_ID = "220154448488";

    private static volatile SenderCollection mInstance;

    private final Context mContext;
    private final LoggingService.Logger mLogger;
    private SimpleArrayMap<String, Sender> mSenders;

    private SenderCollection(Context context) {
        mContext = context;
        mLogger = new LoggingService.Logger(context);
        loadSenders();
    }

    public static SenderCollection getInstance(Context context) {
        if (mInstance == null) {
            synchronized (SenderCollection.class) {
                if (mInstance == null) {
                    mInstance = new SenderCollection(context);
                }
            }
        }
        return mInstance;
    }

    private void loadSenders() {
        mSenders = new SimpleArrayMap<>();
        File file = new File(mContext.getFilesDir(), GCM_SENDERS_FILE);
        if (!file.exists() || file.length() == 0) {
            Sender entry = new Sender();
            entry.senderId = DEFAULT_SENDER_ID;
            entry.name = "Default";
            mSenders.put(DEFAULT_SENDER_ID, entry);
            saveSenders();
            return;
        }
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        FileInputStream in = null;
        // try-with-resources supported only starting with API 19 :-(
        try {
            in = new FileInputStream(file);
            int bytesRead = in.read(bytes);
            if (bytesRead == -1) {
                mLogger.log(Log.ERROR, "Failed to read senders file.");
            } else {
                JSONArray sendersJson = new JSONArray(new String(bytes));
                for (int i = 0; i < sendersJson.length(); i++) {
                    Sender sender = Sender.fromJson(sendersJson.getJSONObject(i));
                    mSenders.put(sender.senderId, sender);
                }
            }
        } catch (IOException e) {
            mLogger.log(Log.ERROR, "Failed to read senders file.", e);
        } catch (JSONException e) {
            mLogger.log(Log.ERROR, "Failed to deserialize senders.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // silently ignore
                }
            }
        }
    }

    private void saveSenders() {
        File file = new File(mContext.getFilesDir(), GCM_SENDERS_FILE);
        JSONArray jsonArray = new JSONArray();
        FileOutputStream out = null;
        try {
            for (int i = 0; i < mSenders.size(); i++) {
                Sender sender = mSenders.get(mSenders.keyAt(i));
                jsonArray.put(sender.toJson());
            }
            out = new FileOutputStream(file);
            out.write(jsonArray.toString().getBytes());
        } catch (IOException e) {
            mLogger.log(Log.ERROR, "Failed to write senders file.", e);
        } catch (JSONException e) {
            mLogger.log(Log.ERROR, "Failed to serialize senders.", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // silently ignore
                }
            }
        }
    }

    public void clearSenders() {
        mSenders.clear();
        saveSenders();
    }

    public void updateSender(Sender sender) {
        mSenders.put(sender.senderId, sender);
        saveSenders();
    }

    public void deleteSender(String senderId) {
        Sender sender = mSenders.remove(senderId);
        if (sender != null) {
            saveSenders();
        }
    }

    public Sender getSender(String senderId) {
        return mSenders.get(senderId);
    }

    public SimpleArrayMap<String, Sender> getSenders() {
        return mSenders;
    }
}
