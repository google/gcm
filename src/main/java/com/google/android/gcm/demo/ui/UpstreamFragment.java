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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
public class UpstreamFragment extends AbstractFragment
        implements View.OnClickListener, MainActivity.RefreshableFragment {

    private LoggingService.Logger mLogger;
    private SenderCollection mSenders;
    private String mCurrentSenderId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mLogger = new LoggingService.Logger(getActivity());
        View view = inflater.inflate(R.layout.fragment_upstream, container, false);
        view.findViewById(R.id.upstream_send_button).setOnClickListener(this);
        view.findViewById(R.id.upstream_sender_id).setOnLongClickListener(this);
        view.findViewById(R.id.upstream_select_sender_id).setOnClickListener(this);

        if (savedState != null) {
            mCurrentSenderId = savedState.getString(SENDER_ID);
        }
        if (mCurrentSenderId != null) {
            TextView senderIdView = (TextView) view.findViewById(R.id.upstream_sender_id);
            senderIdView.setText(truncateToLongString(mCurrentSenderId));
            senderIdView.setTag(R.id.tag_clipboard_value, mCurrentSenderId);
        }

        mSenders = SenderCollection.getInstance(getActivity());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedState) {
        refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putString(SENDER_ID, mCurrentSenderId);
    }

    @Override
    public void refresh() {
        Sender server = mSenders.getSender(getValue(R.id.upstream_sender_id));
        if (server == null) {
            server = mSenders.getSender(SenderCollection.DEFAULT_SENDER_ID);
        }
        if (getView() != null) {
            TextView senderIdView = (TextView) getView().findViewById(R.id.upstream_sender_id);
            TextView tokenView = (TextView) getView().findViewById(R.id.upstream_token);
            senderIdView.setText(truncateToLongString(server.senderId));
            senderIdView.setTag(R.id.tag_clipboard_value, server.senderId);
            if (server.testAppToken != null) {
                tokenView.setText(truncateToLongString(server.testAppToken));
                tokenView.setTag(R.id.tag_clipboard_value, server.testAppToken);
                tokenView.setTextColor(Color.BLUE);
                tokenView.setOnLongClickListener(this);
                getView().findViewById(R.id.upstream_send_button).setEnabled(true);
            } else {
                tokenView.setText(getString(R.string.upstream_unregistered));
                tokenView.setTextColor(Color.RED);
                tokenView.setOnLongClickListener(null);
                getView().findViewById(R.id.upstream_send_button).setEnabled(false);
            }
        } else {
            // nothing, the fragment has been already destroyed
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upstream_send_button:
                doGcmSendUpstreamMessage();
                break;
            case R.id.upstream_select_sender_id:
                Intent startSelectActivityIntent = SelectActivity.pickSenderId(getActivity(),
                        R.id.upstream_sender_id);
                startActivityForResult(startSelectActivityIntent, 0);
                break;
        }

    }

    private void doGcmSendUpstreamMessage() {
        final Activity activity = this.getActivity();
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity);
        final String senderId = getValue(R.id.upstream_sender_id);
        final String msgId = getValue(R.id.upstream_message_id);
        final Bundle data = new Bundle();
        EditableMapView dataView = (EditableMapView) activity.findViewById(R.id.upstream_data);
        for (EditableMapView.MapEntry entry : dataView.getMapEntries()) {
            data.putString(entry.key, entry.value);
        }
        final String ttl = getValue(R.id.upstream_ttl);
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

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        super.handleAddressBookSelection(id, name, value);
        if (id == R.id.upstream_sender_id) {
            mCurrentSenderId = value;
        }
        refresh();
    }

}

