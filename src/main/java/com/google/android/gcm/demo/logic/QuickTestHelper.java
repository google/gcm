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

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.service.LoggingService;

import java.io.IOException;
import java.util.LinkedHashMap;
/**
 * Quick Tests are pre-configured requests that test specific GCM behaviors.
 *
 * The user can execute the quick tests through the dropdown menu placed in
 * the first screen of the app.
 * This class contains the list of tests {@see #getTests}
 * and the logic to execute them {@see #runTest}.
 **/
public class QuickTestHelper {
    private final Context mContext;
    private final LoggingService.Logger mLogger;
    private final LinkedHashMap<String, Integer> tests;

    public QuickTestHelper(Context context) {
        mContext = context.getApplicationContext();
        mLogger = new LoggingService.Logger(mContext);
        tests = new LinkedHashMap<>();
        addTest(R.string.quicktest_downstream_http_json);
    }

    /**
     * Get the list of test defined
     * @return a {@link String} array containing the tests' names
     */
    public String[] getTests() {
        return tests.keySet().toArray(new String[tests.size()]);
    }

    public void runTest(String test, String apiKey, String token) {
        switch (tests.get(test)) {
            case R.string.quicktest_downstream_http_json:
                doDownstreamHttpJson(apiKey, token);
                break;
        }
    }

    /**
     * Test downstream sending via HTTP with JSON
     * @param apiKey the key used to authorize calls to Google servers
     * @param token the registration token of the app to send to
     */
    private void doDownstreamHttpJson(final String apiKey, final String token) {
        mLogger.log(Log.INFO, mContext.getText(R.string.quicktest_downstream_http_json).toString());

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                final Message.Builder messageBuilder = new Message.Builder();
                GcmServerSideSender sender = new GcmServerSideSender(apiKey, mLogger);
                try {
                    sender.sendHttpJsonDownstreamMessage(token, messageBuilder.build());
                } catch (IOException ex) {
                    mLogger.log(Log.INFO, "Downstream HTTP JSON failed:\nerror: " + ex.getMessage());
                }
                return null;
            }
        }.execute();
    }

    /**
     * Add a new test
     * @param stringId the id of the test's name defined in the strings resource
     */
    private void addTest(int stringId) {
        tests.put(mContext.getText(stringId).toString(), stringId);
    }
}