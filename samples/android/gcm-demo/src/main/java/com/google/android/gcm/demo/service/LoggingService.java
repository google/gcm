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
package com.google.android.gcm.demo.service;

import com.google.android.gcm.demo.BuildConfig;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Service used to receive logs, persist them to a file and forward them to the system logger.
 * The logs from the file are displayed in a view inside the main activity.
 */
public class LoggingService extends IntentService {
    public static final String LOG_TAG = "GcmDemo";
    public static final String ACTION_LOG = BuildConfig.APPLICATION_ID + ".LOG";
    public static final String ACTION_CLEAR_LOGS = BuildConfig.APPLICATION_ID + ".CLEAR_LOGS";
    public static final String EXTRA_LOG_PRIORITY = "log_priority";
    public static final String EXTRA_LOG_MESSAGE = "log_message";

    private static final String LOG_FILE = "gcm-demo.log";
    private static final String LOG_SEPARATOR = "\n\n";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    public LoggingService() {
        super("LoggingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_LOG:
                doLog(intent);
                break;
            case ACTION_CLEAR_LOGS:
                doClear();
                break;
            default:
                Log.e(LOG_TAG, "LoggingService received unknown action: " + intent.getAction());
        }
    }

    private void doLog(Intent intent) {
        int priority = intent.getIntExtra(EXTRA_LOG_PRIORITY, Log.INFO);
        String msg = intent.getStringExtra(EXTRA_LOG_MESSAGE);

        // Make the log available through adb logcat
        Log.println(priority, LOG_TAG, msg);

        // Add the timestamp to the message
        String timestamp = dateFormat.format(new Date());
        msg = timestamp + " " + msg;

        // Forward the log to LocalBroadcast subscribers (i.e. UI)
        Intent localIntent = new Intent(ACTION_LOG);
        localIntent.putExtra(EXTRA_LOG_MESSAGE, msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        // Write log to file
        try {
            FileOutputStream outputFile = openFileOutput(LOG_FILE, MODE_APPEND);
            outputFile.write(msg.getBytes());
            outputFile.write(LOG_SEPARATOR.getBytes());
            outputFile.close();
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error while writing in the log file", ex);
        }
    }

    private void doClear() {
        // Delete the log files from the device
        Log.i(LOG_TAG, "Deleting " + LOG_FILE);
        deleteFile(LOG_FILE);

        // Forward the command to LocalBroadcast subscribers (i.e. UI)
        Intent localIntent = new Intent(ACTION_CLEAR_LOGS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    public static class Logger {
        private Context mContext;

        public Logger(Context context) {
            mContext = context;
        }

        public void log(int level, String msg) {
            log(level, msg, null);
        }

        public void log(int priority, String msg, Throwable tr) {
            // Forward the log to LoggingService
            Intent intent = new Intent(ACTION_LOG);
            intent.setClass(mContext, LoggingService.class);
            intent.putExtra(EXTRA_LOG_PRIORITY, priority);
            if (tr != null) {
                msg = msg + "\nexception: " + Log.getStackTraceString(tr);
            }
            intent.putExtra(EXTRA_LOG_MESSAGE, msg);
            mContext.startService(intent);
        }

        public void clearLogs() {
            // Forward the command to LoggingService
            Intent intent = new Intent(ACTION_CLEAR_LOGS);
            intent.setClass(mContext, LoggingService.class);
            mContext.startService(intent);
        }

        public List<String> getLogsFromFile() {
            try {
                FileInputStream fileInput = mContext.openFileInput(LOG_FILE);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInput);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                LinkedList<String> logs = new LinkedList<>();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    StringBuilder log = new StringBuilder();
                    do {
                        log.append(line).append("\n");
                    } while ((line = bufferedReader.readLine()) != null && !("".equals(line)));
                    logs.addFirst(log.toString());
                }
                return logs;
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Error while reading the log file: " + ex);
                return Collections.emptyList();
            }
        }

        public void registerCallback(BroadcastReceiver broadcastReceiver) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_CLEAR_LOGS);
            filter.addAction(ACTION_LOG);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver, filter);
        }

        public void unregisterCallback(BroadcastReceiver broadcastReceiver) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
        }
    }
}
