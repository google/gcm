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
package com.google.android.gcm.demo.ui;

import static com.google.android.gcm.demo.service.LoggingService.LOG_TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.GcmServerSideSender;
import com.google.android.gcm.demo.logic.Message;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

import java.io.IOException;


/**
 * Fragment for sending downstream GCM messages
 */
public class DownstreamFragment extends AbstractFragment implements View.OnClickListener {

    protected LoggingService.Logger mLogger;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mLogger = new LoggingService.Logger(getActivity());
        View view = inflater.inflate(getLayoutId(), container, false);

        view.findViewById(R.id.downstream_api_key).setOnClickListener(this);
        view.findViewById(R.id.downstream_token).setOnClickListener(this);
        view.findViewById(R.id.downstream_send_button).setOnClickListener(this);

        view.findViewById(R.id.downstream_api_key).setOnLongClickListener(this);
        view.findViewById(R.id.downstream_token).setOnLongClickListener(this);

        setHtmlMode(view, R.id.fragment_description);

        loadSavedState(savedState);
        setValueFromFragmentState(view.findViewById(R.id.downstream_api_key), API_KEY);
        setValueFromFragmentState(view.findViewById(R.id.downstream_token), TOKEN);

        return view;
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        if (id == R.id.downstream_api_key) {
            setValue(R.id.downstream_api_key, name, value);
            mFragmentState.putStringArray(API_KEY, new String[]{name, value});
        } else if (id == R.id.downstream_token) {
            setValue(R.id.downstream_token, name, value);
            mFragmentState.putStringArray(TOKEN, new String[]{name, value});
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.downstream_send_button:
                doGcmSend();
                break;
            case R.id.downstream_api_key:
                intent = SelectActivity.pickApiKey(getActivity(), R.id.downstream_api_key);
                startActivityForResult(intent, 0);
                break;
            case R.id.downstream_token:
                intent = SelectActivity.pickDestination(getActivity(), R.id.downstream_token);
                startActivityForResult(intent, 0);
                break;
        }
    }

    protected int getLayoutId() {
        return R.layout.fragment_downstream;
    }

    protected void doGcmSend() {
        final Activity activity = this.getActivity();
        final Message.Builder messageBuilder = new Message.Builder();
        String collapseKey = getValue(R.id.downstream_collapse_key);
        if (isNotEmpty((collapseKey))) {
            messageBuilder.collapseKey(collapseKey.trim());
        }
        String restrictedPackageName = getValue(R.id.downstream_restricted_package_name);
        if (isNotEmpty((restrictedPackageName))) {
            messageBuilder.restrictedPackageName(restrictedPackageName.trim());
        }
        String ttlString = getValue(R.id.downstream_ttl);
        try {
            int ttl = Integer.parseInt(ttlString);
            messageBuilder.timeToLive(ttl);
        } catch (NumberFormatException e) {
            // ttl not set properly, ignoring
            Log.d(LOG_TAG, "Failed to parse TTL, ignoring: " + ttlString);
        }
        messageBuilder.delayWhileIdle(
                ((CheckBox) activity.findViewById(R.id.downstream_delay_while_idle))
                        .isChecked());
        messageBuilder.dryRun(
                ((CheckBox) activity.findViewById(R.id.downstream_dry_run))
                        .isChecked());
        EditableMapView data = (EditableMapView) activity.findViewById(R.id.downstream_data);
        for (EditableMapView.MapEntry mapEntry : data.getMapEntries()) {
            if (isNotEmpty(mapEntry.key) && isNotEmpty(mapEntry.value)) {
                messageBuilder.addData(mapEntry.key, mapEntry.value);
            }
        }

        final boolean json = ((RadioButton) activity.findViewById(R.id.downstream_radio_json))
                .isChecked();
        final String apiKey = getValue(R.id.downstream_api_key);
        final String registrationId = getValue(R.id.downstream_token);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                GcmServerSideSender sender = new GcmServerSideSender(apiKey, mLogger);
                try {
                    if (json) {
                        sender.sendHttpJsonDownstreamMessage(registrationId,
                                messageBuilder.build());
                    } else {
                        sender.sendHttpPlaintextDownstreamMessage(registrationId,
                                messageBuilder.build());
                    }
                } catch (final IOException e) {
                    return e.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    Toast.makeText(activity,
                            "send message failed: " + result,
                            Toast.LENGTH_LONG).show();
                }
            }
        }.execute();

    }
}
