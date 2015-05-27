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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
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
public class DownstreamFragment extends AbstractFragment
        implements View.OnClickListener {

    protected LoggingService.Logger mLogger;
    private String mCurrentToken;
    private String mCurrentApiKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mLogger = new LoggingService.Logger(getActivity());
        View view = inflater.inflate(getLayoutId(), container, false);
        view.findViewById(R.id.downstream_send_button).setOnClickListener(this);
        view.findViewById(R.id.downstream_api_key).setOnLongClickListener(this);
        view.findViewById(R.id.downstream_select_api_key).setOnClickListener(this);
        view.findViewById(R.id.downstream_token).setOnLongClickListener(this);
        view.findViewById(R.id.downstream_select_token).setOnClickListener(this);

        // Set default values
        TextView tokenView = (TextView) view.findViewById(R.id.downstream_token);
        tokenView.setText(getString(R.string.downstream_select_reg_id));
        tokenView.setTag(R.id.tag_clipboard_value, "");

        TextView apiKeyView = (TextView) view.findViewById(R.id.downstream_api_key);
        apiKeyView.setText(getString(R.string.downstream_select_api_key));
        apiKeyView.setTag(R.id.tag_clipboard_value, "");

        // Set saved value if they exist
        if (savedState != null) {
            mCurrentToken = savedState.getString(TOKEN);
            mCurrentApiKey = savedState.getString(API_KEY);
        }
        if (mCurrentToken != null) {
            tokenView.setText(truncateToLongString(mCurrentToken));
            tokenView.setTag(R.id.tag_clipboard_value, mCurrentToken);
        }
        if (mCurrentApiKey != null) {
            apiKeyView.setText(truncateToLongString(mCurrentApiKey));
            apiKeyView.setTag(R.id.tag_clipboard_value, mCurrentApiKey);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putString(TOKEN, mCurrentToken);
        savedState.putString(API_KEY, mCurrentApiKey);
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        super.handleAddressBookSelection(id, name, value);
        if (id == R.id.downstream_token) {
            mCurrentToken = value;
        } else if (id == R.id.downstream_api_key) {
            mCurrentApiKey = value;
        }
    }

    @Override
    public void onClick(View v) {
        Intent startSelectActivityIntent;
        switch (v.getId()) {
            case R.id.downstream_send_button:
                doGcmSend();
                break;
            case R.id.downstream_select_api_key:
                startSelectActivityIntent = SelectActivity.pickApiKey(getActivity(),
                        R.id.downstream_api_key);
                startActivityForResult(startSelectActivityIntent, 0);
                break;
            case R.id.downstream_select_token:
                startSelectActivityIntent = SelectActivity.pickDestination(getActivity(),
                        R.id.downstream_token);
                startActivityForResult(startSelectActivityIntent, 0);
                break;
        }

    }

    protected int getLayoutId() {
        return R.layout.fragment_downstream;
    }

    protected void doGcmSend() {
        final Activity activity = this.getActivity();
        final Message.Builder messageBuilder = new Message.Builder();
        String collapseKey = ((EditText) activity
                .findViewById(R.id.downstream_collapse_key)).getText().toString();
        if (isNotEmpty((collapseKey))) {
            messageBuilder.collapseKey(collapseKey.trim());
        }
        String restrictedPackageName = ((EditText) activity
                .findViewById(R.id.downstream_restricted_package_name)).getText().toString();
        if (isNotEmpty((restrictedPackageName))) {
            messageBuilder.restrictedPackageName(restrictedPackageName.trim());
        }
        String ttlString = ((EditText) activity.findViewById(R.id.downstream_ttl))
                .getText().toString();
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
