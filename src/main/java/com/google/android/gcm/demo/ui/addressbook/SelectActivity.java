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
package com.google.android.gcm.demo.ui.addressbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gcm.demo.R;

/**
 * This activity allows selecting elements from the app's storage, as well as adding new elements or
 * deleting existing ones
 */
public class SelectActivity extends Activity {

    // Intent categories, used to specify what this activity should select
    public static final String INTENT_CATEGORY_SENDERID =
            "com.google.android.gcm.demo.intent.category.SENDER_ID";
    public static final String INTENT_CATEGORY_API_KEY =
            "com.google.android.gcm.demo.intent.category.API_KEY";
    public static final String INTENT_CATEGORY_TOKEN = "com.google.android.gcm.demo.intent" +
            ".category.TOKEN";
    public static final String INTENT_CATEGORY_TOPIC = "com.google.android.gcm.demo.intent" +
            ".category.TOPIC";
    public static final String INTENT_CATEGORY_GROUP = "com.google.android.gcm.demo.intent" +
            ".category.GROUP";

    // Intent extra fields used to pass back results
    public static final String INTENT_EXTRA_NAME = "com.google.android.gcm.demo.INTENT_EXTRA_NAME";
    public static final String INTENT_EXTRA_VALUE = "com.google.android.gcm.demo" +
            ".INTENT_EXTRA_VALUE";
    public static final String INTENT_EXTRA_ID = "com.google.android.gcm.demo.INTENT_EXTRA_ID";
    public static final String INTENT_EXTRA_TITLE = "com.google.android.gcm.demo" +
            ".INTENT_EXTRA_TITLE";

    private RecyclerView mRecyclerView;
    private SenderAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getCategories() == null) {
            setResult(RESULT_CANCELED);
            finish();
        }

        setContentView(R.layout.dialog_select_element);

        TextView title = (TextView) findViewById(R.id.select_dialog_title);
        title.setText(getIntent().getStringExtra(INTENT_EXTRA_TITLE));

        mRecyclerView = (RecyclerView) findViewById(R.id.select_dialog_list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new SenderAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        Button addSenderButton = (Button) findViewById(R.id.select_dialog_add);
        addSenderButton.setOnClickListener(mAdapter);
    }

    public LinearLayout getAddDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        return layout;
    }

    public void returnResponse(String name, String value) {
        Intent result = new Intent();
        result.putExtra(INTENT_EXTRA_NAME, name);
        result.putExtra(INTENT_EXTRA_VALUE, value);
        result.putExtra(INTENT_EXTRA_ID, getIntent().getIntExtra(INTENT_EXTRA_ID, 0));
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    public static Intent pickSenderId(Context context, int extraId) {
        Intent intent = new Intent(context, SelectActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_SENDERID);
        intent.putExtra(SelectActivity.INTENT_EXTRA_ID, extraId);
        intent.putExtra(SelectActivity.INTENT_EXTRA_TITLE,
                context.getString(R.string.address_book_select_sender_id));
        return intent;
    }

    public static Intent pickApiKey(Context context, int extraId) {
        Intent intent = new Intent(context, SelectActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_API_KEY);
        intent.putExtra(SelectActivity.INTENT_EXTRA_ID, extraId);
        intent.putExtra(SelectActivity.INTENT_EXTRA_TITLE,
                context.getString(R.string.address_book_select_api_key));
        return intent;
    }

    public static Intent pickToken(Context context, int extraId) {
        Intent intent = new Intent(context, SelectActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_TOKEN);
        intent.putExtra(SelectActivity.INTENT_EXTRA_ID, extraId);
        intent.putExtra(SelectActivity.INTENT_EXTRA_TITLE,
                context.getString(R.string.address_book_select_token));
        return intent;
    }

    public static Intent pickDestination(Context context, int extraId) {
        Intent intent = new Intent(context, SelectActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_TOKEN);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_GROUP);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_TOPIC);
        intent.putExtra(SelectActivity.INTENT_EXTRA_ID, extraId);
        intent.putExtra(SelectActivity.INTENT_EXTRA_TITLE,
                context.getString(R.string.address_book_select_destination));
        return intent;
    }

    public static Intent pickTopic(Context context, int extraId) {
        Intent intent = new Intent(context, SelectActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_TOPIC);
        intent.putExtra(SelectActivity.INTENT_EXTRA_ID, extraId);
        intent.putExtra(SelectActivity.INTENT_EXTRA_TITLE,
                context.getString(R.string.address_book_select_topic));
        return intent;
    }

    public static Intent pickGroup(Context context, int extraId) {
        Intent intent = new Intent(context, SelectActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        intent.addCategory(SelectActivity.INTENT_CATEGORY_GROUP);
        intent.putExtra(SelectActivity.INTENT_EXTRA_ID, extraId);
        intent.putExtra(SelectActivity.INTENT_EXTRA_TITLE,
                context.getString(R.string.address_book_select_group));
        return intent;
    }

}
