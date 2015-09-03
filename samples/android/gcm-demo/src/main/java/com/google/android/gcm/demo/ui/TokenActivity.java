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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.InstanceIdHelper;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;


/**
 * Activity for creating an InstanceID Authorization Token
 */
public class TokenActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String STATE_AUDIENCE = "STATE_AUDIENCE";

    private TextView audienceIdTexView;
    private TextView scopeTexView;

    private BroadcastReceiver mLoggerCallback;
    private LoggingService.Logger mLogger;
    private InstanceIdHelper mInstanceIdHelper;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_token);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.token_activity_title);
        }

        mLogger = new LoggingService.Logger(this);
        mInstanceIdHelper = new InstanceIdHelper(this);

        audienceIdTexView = (TextView) findViewById(R.id.token_audience);
        scopeTexView = (TextView) findViewById(R.id.token_scope);
        audienceIdTexView.setOnClickListener(this);
        findViewById(R.id.token_generate).setOnClickListener(this);
        ((TextView) findViewById(R.id.editable_map_title)).setText(R.string.token_extra);

        if (savedState != null) {
            if (savedState.containsKey(STATE_AUDIENCE)) {
                String senderId = savedState.getString(STATE_AUDIENCE);
                Sender sender = SenderCollection.getInstance(this).getSender(senderId);
                if (sender != null) {
                    AbstractFragment.setValue(audienceIdTexView, sender.name, sender.senderId);
                }
            }
        }

        // Piggyback on the "NEW_LOG" event to refresh the activity UI.
        mLoggerCallback = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Quick hack until we introduce a proper manager for async events.
                String log = intent.getStringExtra(LoggingService.EXTRA_LOG_MESSAGE);
                if (log != null && log.contains("succeeded")) {
                    NavUtils.navigateUpFromSameTask(TokenActivity.this);
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.token_audience:
                intent = SelectActivity.pickSenderId(this, R.id.group_choose_sender_id);
                startActivityForResult(intent, 0);
                break;
            case R.id.token_generate:
                doGenerateToken();
                break;
        }
    }

    private void doGenerateToken() {
        EditableMapView data = (EditableMapView) findViewById(R.id.token_extra);
        Bundle extra = new Bundle();
        for (EditableMapView.MapEntry mapEntry : data.getMapEntries()) {
            extra.putString(mapEntry.key, mapEntry.value);
        }
        String audience = AbstractFragment.getValue(audienceIdTexView);
        String scope = AbstractFragment.getValue(scopeTexView);

        mInstanceIdHelper.getTokenInBackground(audience, scope, extra);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            int id = data.getIntExtra(SelectActivity.INTENT_EXTRA_ID, 0);
            String name = data.getStringExtra(SelectActivity.INTENT_EXTRA_NAME);
            String value = data.getStringExtra(SelectActivity.INTENT_EXTRA_VALUE);
            switch (id) {
                case R.id.group_choose_sender_id:
                    AbstractFragment.setValue(audienceIdTexView, name, value);
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLogger.registerCallback(mLoggerCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLogger.unregisterCallback(mLoggerCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putString(STATE_AUDIENCE, AbstractFragment.getValue(audienceIdTexView));
    }
}
