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
package com.google.android.gcm.demo.logic.quicktest;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.GcmServerSideSender;
import com.google.android.gcm.demo.logic.Message;
import com.google.android.gcm.demo.service.LoggingService.Logger;
import com.google.android.gcm.demo.ui.MainActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class DownstreamHttpJsonQuickTest implements QuickTest {

    @Override
    public int getName() {
        return R.string.quicktest_downstream_http_json;
    }

    @Override
    public List<Integer> getRequiredParameters() {
        return Arrays.asList(R.id.home_api_key, R.id.home_destination);
    }

    @Override
    public void execute(final Logger logger, final Context context,
                        SimpleArrayMap<Integer, String> params) {
        final String apiKey = params.get(R.id.home_api_key);
        final String destination = params.get(R.id.home_destination);
        logger.log(Log.INFO, context.getText(R.string.quicktest_downstream_http_json).toString());
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                final Message.Builder messageBuilder = new Message.Builder();
                GcmServerSideSender sender = new GcmServerSideSender(apiKey, logger);
                try {
                    String response = sender
                            .sendHttpJsonDownstreamMessage(destination, messageBuilder.build());
                    MainActivity.showToast(context,
                            R.string.downstream_toast_success, response);
                } catch (IOException ex) {
                    logger.log(Log.INFO, "Downstream HTTP JSON failed:\nerror: " + ex.getMessage());
                    MainActivity.showToast(context,
                            R.string.downstream_toast_failure, ex.getMessage());
                }
                return null;
            }
        }.execute();
    }
}
