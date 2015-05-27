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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.DeviceGroupsHelper;
import com.google.android.gcm.demo.model.DeviceGroup;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

/**
 * Activity for adding or editing a device group
 */
public class GroupActivity extends AppCompatActivity
        implements View.OnClickListener {

    public static final String EXTRA_SENDER_ID = "INTENT_EXTRA_SENDER_ID";
    public static final String EXTRA_GROUP_NAME = "INTENT_EXTRA_GROUP_NAME";

    private static final String STATE_SELECTED_SENDER_ID = "STATE_SELECTED_SENDER_ID";
    private static final String STATE_SELECTED_API_KEY = "STATE_SELECTED_API_KEY";
    private static final String STATE_SELECTED_GROUP_NAME = "STATE_SELECTED_GROUP_NAME";
    private static final String STATE_NEW_MEMBERS = "STATE_NEW_MEMBERS";
    private static final String STATE_REMOVED_MEMBERS = "STATE_REMOVED_MEMBERS";
    private static final String ACTION_REMOVE_MEMBER = "remove-member";
    private static final String ACTION_UNDO_ADD_MEMBER = "undo-add-member";
    private static final String ACTION_UNDO_REMOVE_MEMBER = "undo-remove-member";

    private Bundle newMembers = new Bundle();
    private Bundle removedMembers = new Bundle();

    private boolean editMode;
    private Sender sender = new Sender();
    private String senderApiKey;
    private DeviceGroup group = new DeviceGroup();

    private int disabledTextColor;
    private int enabledTextColor;
    private View newMembersView;
    private View noNewMembersView;
    private View removedMembersView;
    private View currentMembersView;
    private LinearLayout newMembersList;
    private LinearLayout removedMembersList;
    private LinearLayout currentMembersList;
    private TextView senderIdTexView;
    private TextView apiKeyTextView;
    private EditText newNameEditText;

    private SenderCollection mSenders;
    private DeviceGroupsHelper mDeviceGroupsHelper;
    private BroadcastReceiver mLoggerCallback;
    private LoggingService.Logger mLogger;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_group);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mLogger = new LoggingService.Logger(this);
        mDeviceGroupsHelper = new DeviceGroupsHelper(this);
        mSenders = SenderCollection.getInstance(this);

        if (savedState != null) {
            if (savedState.containsKey(STATE_NEW_MEMBERS)) {
                newMembers = savedState.getBundle(STATE_NEW_MEMBERS);
            }
            if (savedState.containsKey(STATE_REMOVED_MEMBERS)) {
                removedMembers = savedState.getBundle(STATE_REMOVED_MEMBERS);
            }
            sender.senderId = savedState.getString(STATE_SELECTED_SENDER_ID);
            senderApiKey = savedState.getString(STATE_SELECTED_SENDER_ID);
            group.notificationKeyName = savedState.getString(STATE_SELECTED_SENDER_ID);
        }

        enabledTextColor = getResources().getColor(R.color.black);
        disabledTextColor = getResources().getColor(R.color.grey_500);

        newMembersView = findViewById(R.id.group_new_members);
        noNewMembersView = findViewById(R.id.group_no_new_members);
        removedMembersView = findViewById(R.id.group_removed_members);
        currentMembersView = findViewById(R.id.group_current_members);

        senderIdTexView = (TextView) findViewById(R.id.group_sender_id);
        apiKeyTextView = (TextView) findViewById(R.id.group_api_key);
        newNameEditText = (EditText) findViewById(R.id.group_new_name);

        newMembersList = (LinearLayout) findViewById(R.id.group_new_members_list);
        removedMembersList = (LinearLayout) findViewById(R.id.group_removed_members_list);
        currentMembersList = (LinearLayout) findViewById(R.id.group_current_members_list);

        Button submit = (Button) findViewById(R.id.group_submit);

        if (getIntent().hasExtra(EXTRA_GROUP_NAME)) {
            // Caller provided a group name, entering EDIT mode.
            editMode = true;
            getSupportActionBar().setTitle(R.string.group_activity_title_edit_group);
            String senderId = getIntent().getStringExtra(EXTRA_SENDER_ID);
            String groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME);
            sender = mSenders.getSender(senderId);
            group = sender.groups.get(groupName);

            senderIdTexView.setText(sender.senderId);
            senderIdTexView.setTextColor(disabledTextColor);

            TextView nameTextView = (TextView) findViewById(R.id.group_name);
            TextView keyTextView = (TextView) findViewById(R.id.group_key);
            nameTextView.setText(group.notificationKeyName);
            nameTextView.setTextColor(disabledTextColor);
            keyTextView.setText(AbstractFragment.truncateToMediumString(group.notificationKey));
            keyTextView.setTextColor(disabledTextColor);

            submit.setText(R.string.group_submit_edit_group);

            newNameEditText.setVisibility(View.GONE);
        } else {
            // Caller did not provide a group name, entering NEW-GROUP mode.
            editMode = false;
            getSupportActionBar().setTitle(R.string.group_activity_title_new_group);
            findViewById(R.id.group_key_view).setVisibility(View.GONE);
            senderIdTexView.setOnClickListener(this);
            submit.setText(R.string.group_submit_new_group);
        }

        submit.setOnClickListener(this);
        findViewById(R.id.group_new_member).setOnClickListener(this);

        refresh();

        // Piggyback on the "NEW_LOG" event to refresh the activity UI.
        mLoggerCallback = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refresh();
            }
        };
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
        savedState.putBundle(STATE_NEW_MEMBERS, newMembers);
        savedState.putBundle(STATE_REMOVED_MEMBERS, removedMembers);
        savedState.putString(STATE_SELECTED_SENDER_ID, sender.senderId);
        savedState.putString(STATE_SELECTED_API_KEY, senderApiKey);
        savedState.putString(STATE_SELECTED_GROUP_NAME, group.notificationKeyName);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            int id = data.getIntExtra(SelectActivity.INTENT_EXTRA_ID, 0);
            String name = data.getStringExtra(SelectActivity.INTENT_EXTRA_NAME);
            String value = data.getStringExtra(SelectActivity.INTENT_EXTRA_VALUE);
            switch (id) {
                case R.id.group_sender_id:
                    sender = mSenders.getSender(value);
                    senderIdTexView.setText(value);
                    refresh();
                    break;
                case R.id.group_api_key:
                    senderApiKey = value;
                    apiKeyTextView.setText(AbstractFragment.truncateToMediumString(value));
                    break;
                case R.id.group_new_member:
                    newMembers.putString(name, value);
                    refresh();
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.group_sender_id:
                intent = SelectActivity.pickSenderId(this, R.id.group_sender_id);
                startActivityForResult(intent, 0);
                break;
            case R.id.group_api_key:
                intent = SelectActivity.pickApiKey(this, R.id.group_api_key);
                startActivityForResult(intent, 0);
                break;
            case R.id.group_new_member:
                intent = SelectActivity.pickToken(this, R.id.group_new_member);
                startActivityForResult(intent, 0);
                break;
            case R.id.widget_itbr_button:
                memberAction(view);
                break;
            case R.id.group_submit:
                if (editMode) {
                    submitApplyChanges();
                } else {
                    submitCreateGroup();
                }
                break;
        }
    }

    private void memberAction(View view) {
        String tokenName = (String) view.getTag(R.id.tag_token);
        switch ((String) view.getTag(R.id.tag_action)) {
            case ACTION_REMOVE_MEMBER:
                String tokenKey = group.tokens.get(tokenName);
                removedMembers.putString(tokenName, tokenKey);
                break;
            case ACTION_UNDO_ADD_MEMBER:
                newMembers.remove(tokenName);
                break;
            case ACTION_UNDO_REMOVE_MEMBER:
                removedMembers.remove(tokenName);
                break;
        }
        refresh();
    }

    private void submitApplyChanges() {
        if (newMembers.size() == 0 && removedMembers.size() == group.tokens.size()) {
            // If the user removes all the members, the group is going to be deleted by the server
            askConfirmationAndDeleteGroup();
            return;
        }
        mDeviceGroupsHelper.asyncUpdateGroup(sender.senderId, senderApiKey,
                group.notificationKeyName, group.notificationKey, newMembers, removedMembers);
        newMembers.clear();
        removedMembers.clear();
    }

    private void askConfirmationAndDeleteGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.group_alert_group_will_be_deleted)
                .setPositiveButton(R.string.group_alert_delete_group,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mDeviceGroupsHelper.asyncDeleteGroup(sender.senderId, senderApiKey,
                                        group.notificationKeyName);
                            }
                        })
                .setNegativeButton(R.string.group_alert_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
        builder.create().show();
    }

    private void submitCreateGroup() {
        group.notificationKeyName = newNameEditText.getText().toString().trim();
        if (sender.senderId == null) {
            Toast.makeText(this, R.string.group_toast_sender_id_not_selected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (senderApiKey == null) {
            Toast.makeText(this, R.string.group_toast_api_key_not_selected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if ("".equals(group.notificationKeyName)) {
            Toast.makeText(this, R.string.group_toast_name_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        mDeviceGroupsHelper.asyncCreateGroup(sender.senderId, senderApiKey,
                group.notificationKeyName, newMembers);
    }

    private void refresh() {
        // Reset UI
        currentMembersView.setVisibility(View.GONE);
        newMembersView.setVisibility(View.GONE);
        noNewMembersView.setVisibility(View.GONE);
        removedMembersView.setVisibility(View.GONE);
        currentMembersList.removeAllViews();
        newMembersList.removeAllViews();
        removedMembersList.removeAllViews();

        if (editMode) {
            // Reload group if in edit mode.
            // If the group doesn't exist anymore it means it has been deleted.
            sender = mSenders.getSender(sender.senderId);
            if (sender != null && group != null) {
                group = sender.groups.get(group.notificationKeyName);
            }
            if (sender == null || group == null) {
                NavUtils.navigateUpFromSameTask(this);
                return;
            }
        } else if (sender.senderId != null && group.notificationKeyName != null) {
            // If in new-group mode, and we found the group in the local storage,
            // it means the user successfully created it. Thus let's leave the activity.
            Sender tmpSender =
                    mSenders.getSender(sender.senderId);
            if (tmpSender != null && tmpSender.groups.get(group.notificationKeyName) != null) {
                NavUtils.navigateUpFromSameTask(this);
            }
        }

        if (sender.senderId == null) {
            apiKeyTextView.setText(R.string.group_sender_api_key_disable);
            apiKeyTextView.setTextColor(disabledTextColor);
        } else {
            apiKeyTextView.setOnClickListener(this);
            apiKeyTextView.setTextColor(enabledTextColor);
            if (senderApiKey == null) {
                if (sender.apiKeys.size() > 0) {
                    senderApiKey = sender.apiKeys.get(0);
                    apiKeyTextView.setText(AbstractFragment.truncateToMediumString(senderApiKey));
                } else {
                    apiKeyTextView.setText(R.string.group_sender_api_key_hint);
                }
            }
        }

        // Load new members UI
        for (String tokenName : newMembers.keySet()) {
            newMembersView.setVisibility(View.VISIBLE);
            newMembersList.addView(createNewMemberRow(tokenName, newMembers.getString(tokenName)));
        }

        // Load removed members UI
        for (String tokenName : removedMembers.keySet()) {
            removedMembersView.setVisibility(View.VISIBLE);
            removedMembersList.addView(
                    createRemovedMemberRow(tokenName, removedMembers.getString(tokenName)));
        }

        // Load current members UI
        for (String name : group.tokens.keySet()) {
            if (removedMembers.containsKey(name)) {
                continue;
            }
            currentMembersView.setVisibility(View.VISIBLE);
            currentMembersList.addView(createCurrentMemberRow(name, group.tokens.get(name)));
        }

        // Customizations for create vs edit group
        if (!editMode) {
            newMembersView.setVisibility(View.VISIBLE);
            if (newMembers.size() == 0) {
                noNewMembersView.setVisibility(View.VISIBLE);
            }
        }
    }

    private View createNewMemberRow(String name, String value) {
        LinearLayout row = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.widget_icon_text_button_row, null);
        ImageView icon = (ImageView) row.findViewById(R.id.widget_itbr_icon);
        TextView label = (TextView) row.findViewById(R.id.widget_itbr_text);
        Button button = (Button) row.findViewById(R.id.widget_itbr_button);

        icon.setImageResource(R.drawable.smartphone_grey600);
        label.setText(name + ": " + AbstractFragment.truncateToShortString(value));
        button.setText(R.string.group_undo_add_member);
        button.setTag(R.id.tag_action, ACTION_UNDO_ADD_MEMBER);
        button.setTag(R.id.tag_token, name);
        button.setOnClickListener(this);
        return row;
    }

    private View createRemovedMemberRow(String name, String value) {
        LinearLayout row = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.widget_icon_text_button_row, null);
        ImageView icon = (ImageView) row.findViewById(R.id.widget_itbr_icon);
        TextView label = (TextView) row.findViewById(R.id.widget_itbr_text);
        Button button = (Button) row.findViewById(R.id.widget_itbr_button);

        icon.setImageResource(R.drawable.smartphone_grey600);
        label.setText(name + ": " + AbstractFragment.truncateToShortString(value));
        button.setText(R.string.group_undo_remove_member);
        button.setTag(R.id.tag_action, ACTION_UNDO_REMOVE_MEMBER);
        button.setTag(R.id.tag_token, name);
        button.setOnClickListener(this);
        return row;
    }

    private View createCurrentMemberRow(String name, String value) {
        LinearLayout row = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.widget_icon_text_button_row, null);
        ImageView icon = (ImageView) row.findViewById(R.id.widget_itbr_icon);
        TextView label = (TextView) row.findViewById(R.id.widget_itbr_text);
        Button button = (Button) row.findViewById(R.id.widget_itbr_button);

        icon.setImageResource(R.drawable.smartphone_grey600);
        label.setText(name + ": " + AbstractFragment.truncateToShortString(value));
        button.setText(R.string.group_remove_member);
        button.setTag(R.id.tag_action, ACTION_REMOVE_MEMBER);
        button.setTag(R.id.tag_token, name);
        button.setOnClickListener(this);
        return row;
    }
}
