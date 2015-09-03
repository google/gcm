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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.InstanceIdHelper;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

/**
 * Fragment for registering and un-registering GCM tokens, as well as running quick tests.
 * This is the default fragment shown when the app starts.
 */
public class InstanceIdFragment extends AbstractFragment
        implements View.OnClickListener, MainActivity.RefreshableFragment {

    private InstanceIdHelper mInstanceIdHelper;
    private SenderCollection mSenders;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.fragment_instanceid, container, false);
        view.findViewById(R.id.iid_sender_id).setOnClickListener(this);
        view.findViewById(R.id.iid_get_token).setOnClickListener(this);
        view.findViewById(R.id.iid_delete_token).setOnClickListener(this);

        view.findViewById(R.id.iid_sender_id).setOnLongClickListener(this);

        setHtmlMode(view, R.id.fragment_description);

        loadSavedState(savedState);
        setValueFromFragmentState(view.findViewById(R.id.iid_sender_id), SENDER_ID);

        mInstanceIdHelper = new InstanceIdHelper(getActivity());
        mSenders = SenderCollection.getInstance(getActivity());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedState) {
        refresh();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.iid_sender_id:
                intent = SelectActivity.pickSenderId(getActivity(), R.id.iid_sender_id);
                startActivityForResult(intent, 0);
                break;
            case R.id.iid_get_token:
                mInstanceIdHelper.getGcmTokenInBackground(getValue(R.id.iid_sender_id));
                break;
            case R.id.iid_delete_token:
                mInstanceIdHelper.deleteGcmTokeInBackground(getValue(R.id.iid_sender_id));
                break;
        }
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        if (id == R.id.iid_sender_id) {
            mFragmentState.putStringArray(SENDER_ID, new String[]{name, value});
            setValue(R.id.iid_sender_id, name, value);
        }
        refresh();
    }

    @Override
    public void refresh() {
        Sender server = mSenders.getSender(getValue(R.id.iid_sender_id));
        if (server == null) {
            server = mSenders.getSender(SenderCollection.DEFAULT_SENDER_ID);
        }
        // Check if the view exists, if so refresh it.
        if (getView() != null) {
            setValue(R.id.iid_sender_id, server.name, server.senderId);
            TextView tokenView = (TextView) getView().findViewById(R.id.iid_token);
            if (server.testAppToken != null) {
                setValue(R.id.iid_token, null, server.testAppToken);
                tokenView.setText(truncateToLongString(server.testAppToken));
                tokenView.setOnLongClickListener(this);
                tokenView.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat);
            } else {
                tokenView.setText(getString(R.string.iid_unregistered));
                tokenView.setTextColor(Color.RED);
                tokenView.setOnLongClickListener(null);
            }
        }
    }
}
