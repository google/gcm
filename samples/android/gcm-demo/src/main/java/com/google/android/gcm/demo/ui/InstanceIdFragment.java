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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.InstanceIdHelper;
import com.google.android.gcm.demo.logic.QuickTestHelper;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

/**
 * Fragment for registering and un-registering GCM tokens, as well as running quick tests.
 * This is the default fragment shown when the app starts.
 */
public class InstanceIdFragment extends AbstractFragment
        implements View.OnClickListener, MainActivity.RefreshableFragment {

    private QuickTestHelper mQuickTestHelper;
    private InstanceIdHelper mInstanceIdHelper;
    private SenderCollection mSenders;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.fragment_instanceid, container, false);
        view.findViewById(R.id.iid_sender_id).setOnClickListener(this);
        view.findViewById(R.id.iid_get_token).setOnClickListener(this);
        view.findViewById(R.id.iid_delete_token).setOnClickListener(this);
        view.findViewById(R.id.iid_api_key).setOnClickListener(this);
        view.findViewById(R.id.iid_execute_quick_test).setOnClickListener(this);

        view.findViewById(R.id.iid_sender_id).setOnLongClickListener(this);
        view.findViewById(R.id.iid_api_key).setOnLongClickListener(this);

        setHtmlMode(view, R.id.fragment_description);

        loadSavedState(savedState);
        setValueFromFragmentState(view.findViewById(R.id.iid_sender_id), SENDER_ID);
        setValueFromFragmentState(view.findViewById(R.id.iid_api_key), API_KEY);

        mInstanceIdHelper = new InstanceIdHelper(getActivity());
        mQuickTestHelper = new QuickTestHelper(getActivity());
        mSenders = SenderCollection.getInstance(getActivity());

        Spinner quickTests = (Spinner) view.findViewById(R.id.iid_quick_test);
        ArrayAdapter<String> quickTestsAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, mQuickTestHelper.getTests());
        quickTestsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quickTests.setAdapter(quickTestsAdapter);

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
            case R.id.iid_api_key:
                intent = SelectActivity.pickApiKey(getActivity(), R.id.iid_api_key);
                startActivityForResult(intent, 0);
                break;
            case R.id.iid_execute_quick_test:
                mQuickTestHelper.runTest(
                        getValue(R.id.iid_quick_test),
                        getValue(R.id.iid_api_key),
                        getValue(R.id.iid_token));
                break;
        }
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        if (id == R.id.iid_sender_id) {
            mFragmentState.putStringArray(SENDER_ID, new String[]{name, value});
            setValue(R.id.iid_sender_id, name, value);
        } else if (id == R.id.iid_api_key) {
            mFragmentState.putStringArray(API_KEY, new String[]{name, value});
            setValue(R.id.iid_api_key, name, value);
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
                getView().findViewById(R.id.iid_execute_quick_test).setEnabled(true);
                tokenView.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat);
            } else {
                tokenView.setText(getString(R.string.iid_unregistered));
                tokenView.setTextColor(Color.RED);
                tokenView.setOnLongClickListener(null);
                getView().findViewById(R.id.iid_execute_quick_test).setEnabled(false);
            }
            String currentAPIKey = getValue(R.id.iid_api_key);
            if (!server.apiKeys.contains(currentAPIKey)) {
                TextView apiKeyView = ((TextView) getView().findViewById(R.id.iid_api_key));
                apiKeyView.setText(R.string.iid_api_key_hint);
                apiKeyView.setTag(R.id.tag_clipboard_value, "");
            }
        }
    }
}
