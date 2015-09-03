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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.quicktest.QuickTest;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Fragment for managing device groups; this fragment displays a list of groups for each
 * GCM sender of the app. Clicking on any group or adding a new one starts the {@link GroupActivity}
 */
public class HomeFragment extends AbstractFragment
        implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private LinkedHashMap<String, QuickTest> mQuickTests;
    private HintAdapter mQuickTestsAdapter;
    private LoggingService.Logger mLogger;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mLogger = new LoggingService.Logger(getActivity());

        mQuickTests = MainMenu.getTests(getActivity());
        mQuickTestsAdapter = new HintAdapter(getActivity(), android.R.layout.simple_spinner_item,
                new ArrayList<>(mQuickTests.keySet()), getString(R.string.home_quick_test_hint));
        mQuickTestsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Spinner quickTests = (Spinner) view.findViewById(R.id.home_quick_test);
        quickTests.setPrompt(getString(R.string.home_quick_test_hint));
        quickTests.setAdapter(mQuickTestsAdapter);
        quickTests.setSelection(mQuickTestsAdapter.getCount());
        quickTests.setOnItemSelectedListener(this);

        view.findViewById(R.id.home_sender_id).setOnClickListener(this);
        view.findViewById(R.id.home_api_key).setOnClickListener(this);
        view.findViewById(R.id.home_destination).setOnClickListener(this);
        view.findViewById(R.id.home_submit).setOnClickListener(this);

        loadSavedState(savedState);
        setValueFromFragmentState(view.findViewById(R.id.home_sender_id), SENDER_ID);
        setValueFromFragmentState(view.findViewById(R.id.home_api_key), API_KEY);
        setValueFromFragmentState(view.findViewById(R.id.home_destination), TOKEN);

        return view;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position < mQuickTestsAdapter.getCount()) {
            // User selected one of the API to test.
            getActivity().findViewById(R.id.home_placeholder).setVisibility(View.GONE);
            getActivity().findViewById(R.id.home_api_layout).setVisibility(View.VISIBLE);
            // Reset the parameters visibility to hidden.
            getActivity().findViewById(R.id.home_sender_id_layout).setVisibility(View.GONE);
            getActivity().findViewById(R.id.home_api_key_layout).setVisibility(View.GONE);
            getActivity().findViewById(R.id.home_destination_layout).setVisibility(View.GONE);
            // Enable the parameters needed by the current test.
            List<Integer> requiredParameters =
                    mQuickTests.get(getValue(R.id.home_quick_test)).getRequiredParameters();
            for (Integer parameter : requiredParameters) {
                ((View) getActivity().findViewById(parameter).getParent())
                        .setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.home_sender_id:
                intent = SelectActivity.pickSenderId(getActivity(), R.id.home_sender_id);
                startActivityForResult(intent, 0);
                break;
            case R.id.home_api_key:
                intent = SelectActivity.pickApiKey(getActivity(), R.id.home_api_key);
                startActivityForResult(intent, 0);
                break;
            case R.id.home_destination:
                intent = SelectActivity.pickDestination(getActivity(), R.id.home_destination);
                startActivityForResult(intent, 0);
                break;
            case R.id.home_submit:
                doExecuteSelectedTest();
                break;
        }
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        switch (id) {
            case R.id.home_sender_id:
                mFragmentState.putStringArray(SENDER_ID, new String[]{name, value});
                setValue(R.id.home_sender_id, name, value);
                break;
            case R.id.home_api_key:
                mFragmentState.putStringArray(API_KEY, new String[]{name, value});
                setValue(R.id.home_api_key, name, value);
                break;
            case R.id.home_destination:
                mFragmentState.putStringArray(TOKEN, new String[]{name, value});
                setValue(R.id.home_destination, name, value);
                break;
        }
    }

    private void doExecuteSelectedTest() {
        QuickTest test = mQuickTests.get(getValue(R.id.home_quick_test));
        SimpleArrayMap<Integer, String> params = new SimpleArrayMap<>();
        for (Integer paramsId : test.getRequiredParameters()) {
            params.put(paramsId, getValue(paramsId));
        }
        test.execute(mLogger, getActivity(), params);
    }


    /**
     * HintAdapter allows to create spinner with a hint text as default value.
     */
    private class HintAdapter extends ArrayAdapter<String> {

        public HintAdapter(Context c, int layoutResourceId, ArrayList<String> values, String hint) {
            super(c, layoutResourceId, values);
            this.add(hint);
        }

        @Override
        public int getCount() {
            // don't display last item. It is used as hint.
            int count = super.getCount();
            return count > 0 ? count - 1 : count;
        }
    }

}
