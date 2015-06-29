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
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.DeviceGroup;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.logic.DeviceGroupsHelper;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

/**
 * Fragment for managing device groups; this fragment displays a list of groups for each
 * GCM sender of the app. Clicking on any group or adding a new one starts the {@link GroupActivity}
 */
public class GroupsFragment extends AbstractFragment
        implements View.OnClickListener, MainActivity.RefreshableFragment {

    private static final String ACTION_OPEN_GROUP = "actionOpenGroup";
    private static final String ACTION_DELETE_GROUP = "actionDeleteGroup";
    private static final String STATE_GROUP_TO_BE_DELETED_NAME = "STATE_GROUP_TO_BE_DELETED_NAME";
    private static final String STATE_GROUP_TO_BE_DELETED_SENDER_ID =
            "STATE_GROUP_TO_BE_DELETED_SENDER_ID";

    private static int selectableBackgroundResource;

    private String mGroupToBeDeletedSenderId;
    private String mGroupToBeDeletedName;
    private DeviceGroupsHelper mDeviceGroupsHelper;


    private SenderCollection mSenders;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mSenders = SenderCollection.getInstance(getActivity());

        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        TextView description = (TextView) view.findViewById(R.id.groups_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(Html.fromHtml(getActivity().getString(R.string.groups_description)));
        view.findViewById(R.id.groups_create_new).setOnClickListener(this);

        if (savedState != null) {
            mGroupToBeDeletedSenderId = savedState.getString(STATE_GROUP_TO_BE_DELETED_SENDER_ID);
            mGroupToBeDeletedName = savedState.getString(STATE_GROUP_TO_BE_DELETED_NAME);
        }

        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = getActivity().obtainStyledAttributes(attrs);
        selectableBackgroundResource = typedArray.getResourceId(0, 0);

        mDeviceGroupsHelper = new DeviceGroupsHelper(getActivity());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        savedState.putString(STATE_GROUP_TO_BE_DELETED_SENDER_ID, mGroupToBeDeletedSenderId);
        savedState.putString(STATE_GROUP_TO_BE_DELETED_NAME, mGroupToBeDeletedName);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.groups_create_new) {
            getActivity().startActivity(new Intent(getActivity(), GroupActivity.class));
        } else if(ACTION_OPEN_GROUP.equals(v.getTag(R.id.tag_action))) {
            Intent intent = new Intent(getActivity(), GroupActivity.class);
            intent.putExtra(GroupActivity.EXTRA_SENDER_ID, (String) v.getTag(R.id.tag_senderid));
            intent.putExtra(GroupActivity.EXTRA_GROUP_NAME, (String) v.getTag(R.id.tag_group));
            getActivity().startActivity(intent);
        } else if (ACTION_DELETE_GROUP.equals(v.getTag(R.id.tag_action))) {
            mGroupToBeDeletedSenderId = (String) v.getTag(R.id.tag_senderid);
            mGroupToBeDeletedName = (String) v.getTag(R.id.tag_group);
            Intent intent = SelectActivity.pickApiKey(getActivity(), R.id.widget_itbr_button);
            getActivity().startActivityForResult(intent, 0);
        }
    }

    @Override
    public void handleAddressBookSelection(int id, String name, String value) {
        if (id == R.id.widget_itbr_button) {
            mDeviceGroupsHelper.asyncDeleteGroup(mGroupToBeDeletedSenderId,
                    value, mGroupToBeDeletedName);
        }
    }


    @Override
    public void refresh() {
        float density = getActivity().getResources().getDisplayMetrics().density;
        SimpleArrayMap<String, Sender> senders = mSenders.getSenders();
        LinearLayout sendersList = new LinearLayout(getActivity());
        sendersList.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < senders.size(); i++) {
            Sender sender = senders.valueAt(i);
            if (sender.groups.size() > 0) {
                LinearLayout senderRow = (LinearLayout) getActivity().getLayoutInflater()
                        .inflate(R.layout.widget_icon_text_button_row, null);
                ImageView senderIcon = (ImageView) senderRow.findViewById(R.id.widget_itbr_icon);
                TextView senderText = (TextView) senderRow.findViewById(R.id.widget_itbr_text);
                senderRow.findViewById(R.id.widget_itbr_button).setVisibility(View.GONE);
                senderIcon.setImageResource(R.drawable.cloud_googblue);
                senderIcon.setPadding(0, 0, (int) (8 * density), 0);
                senderText.setText(getString(R.string.groups_sender_id, sender.senderId));
                sendersList.addView(senderRow);
                for (DeviceGroup deviceGroup : sender.groups.values()) {
                    LinearLayout row = (LinearLayout) getActivity().getLayoutInflater()
                            .inflate(R.layout.widget_icon_text_button_row, null);
                    ImageView icon = (ImageView) row.findViewById(R.id.widget_itbr_icon);
                    TextView label = (TextView) row.findViewById(R.id.widget_itbr_text);
                    Button button = (Button) row.findViewById(R.id.widget_itbr_button);
                    icon.setImageResource(R.drawable.group_grey600);
                    label.setText(deviceGroup.notificationKeyName);
                    label.setBackgroundResource(selectableBackgroundResource);
                    label.setTag(R.id.tag_action, ACTION_OPEN_GROUP);
                    label.setTag(R.id.tag_senderid, sender.senderId);
                    label.setTag(R.id.tag_group, deviceGroup.notificationKeyName);
                    label.setOnClickListener(this);

                    button.setText(R.string.groups_delete);
                    button.setTag(R.id.tag_action, ACTION_DELETE_GROUP);
                    button.setTag(R.id.tag_senderid, sender.senderId);
                    button.setTag(R.id.tag_group, deviceGroup.notificationKeyName);
                    button.setOnClickListener(this);
                    row.setPadding((int) (16 * density), 0, 0, 0);
                    sendersList.addView(row);
                }
            }
        }
        if (sendersList.getChildCount() == 0) {
            TextView noTokens = new TextView(getActivity());
            noTokens.setText(getString(R.string.groups_no_groups_available));
            noTokens.setTypeface(null, Typeface.ITALIC);
            sendersList.addView(noTokens);
        }
        FrameLayout topicsView = (FrameLayout) getActivity().findViewById(R.id.groups_list_wrapper);
        topicsView.removeAllViews();
        topicsView.addView(sendersList);
    }
}
