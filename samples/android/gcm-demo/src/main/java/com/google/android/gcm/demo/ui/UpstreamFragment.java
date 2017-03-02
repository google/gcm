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

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * Fragment for sending upstream messages from the app to a server.
 * Upstream messages can be received by a server connecting to GCM using the XMPP protocol.
 */
public class UpstreamFragment extends AbstractFragment implements View.OnClickListener {

    private LoggingService.Logger mLogger;
    private SenderCollection mSenders;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.fragment_upstream, container, false);
        view.findViewById(R.id.upstream_send_button).setOnClickListener(this);
        view.findViewById(R.id.upstream_sender_id).setOnClickListener(this);
        view.findViewById(R.id.upstream_sender_id).setOnLongClickListener(this);
        setHtmlMode(view, R.id.upstream_description);

        mLogger = new LoggingService.Logger(getActivity());
        mSenders = SenderCollection.getInstance(getActivity());

        loadSavedState(savedState);
        setValueFromFragmentState(view.findViewById(R.id.upstream_sender_id), SENDER_ID);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upstream_send_button:
                doGcmSendUpstreamMessage();
                break;
            case R.id.upstream_sender_id:
                Intent intent = SelectActivity.pickSenderId(getActivity(), R.id.upstream_sender_id);
                startActivityForResult(intent, 0);
                break;
        }
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        if (id == R.id.upstream_sender_id) {
            Sender sender = mSenders.getSender(value);
            if (sender != null && sender.getGcmDemoAppToken() != null) {
                setValue(R.id.upstream_sender_id, name, value);
                mFragmentState.putStringArray(SENDER_ID, new String[]{name, value});
            } else {
                Toast.makeText(getActivity(),
                        R.string.upstream_sender_id_not_registered, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doGcmSendUpstreamMessage() {
        final Activity activity = getActivity();
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity);
        final String senderId = getValue(R.id.upstream_sender_id);
        final String msgId = getValue(R.id.upstream_message_id);
        final String ttl = getValue(R.id.upstream_ttl);
        final Bundle data = new Bundle();
        EditableMapView dataView = (EditableMapView) activity.findViewById(R.id.upstream_data);
        for (EditableMapView.MapEntry entry : dataView.getMapEntries()) {
            data.putString(entry.key, entry.value);
        }

        if (senderId.equals(getString(R.string.upstream_sender_id_hint))) {
            Toast.makeText(activity, R.string.upstream_sender_id_not_select, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (msgId.equals("")) {
            Toast.makeText(activity, R.string.upstream_message_id_not_provided, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    if (isNotEmpty(ttl)) {
                        try {
                            gcm.send(senderId + "@gcm.googleapis.com", msgId,
                                    Long.parseLong(ttl), data);
                        } catch (NumberFormatException ex) {
                            mLogger.log(Log.ERROR,
                                    "Error sending upstream message: could not parse ttl", ex);
                            return "Error sending upstream message: could not parse ttl";
                        }
                    } else {
                        gcm.send(senderId + "@gcm.googleapis.com", msgId, data);
                    }
                    mLogger.log(Log.INFO, "Successfully sent upstream message");
                    return null;
                } catch (IOException ex) {
                    mLogger.log(Log.ERROR, "Error sending upstream message", ex);
                    return "Error sending upstream message:" + ex.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    Toast.makeText(activity,
                            "send message failed: " + result,
                            Toast.LENGTH_LONG).show();
                }
            }
        }.execute(null, null, null);
    }
}
