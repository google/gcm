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

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.Sender;

/**
 * Adapter for displaying the list of groups of a sender.
 */
public class GroupAdapter extends AbstractElementAdapter {

    public GroupAdapter(SelectActivity activity) {
        super(activity);
    }

    public View getView(final String senderId, ViewGroup parentView) {
        final Sender sender = mSenders.getSender(senderId);
        if (sender == null) {
            mLogger.log(Log.ERROR, "Invalid sender " + senderId);
            return null;
        }
        View view = mActivity.getLayoutInflater().inflate(
                R.layout.widget_sender_element_list, parentView, false);
        TextView title = (TextView) view.findViewById(R.id.sender_element_title);
        title.setText(mActivity.getString(R.string.address_book_group_title));
        Button addButton = (Button) view.findViewById(R.id.sender_element_add);
        addButton.setVisibility(View.GONE);
        final LinearLayout listLayout = (LinearLayout) view.findViewById(R.id.sender_element_list);
        for (String groupName : sender.groups.keySet()) {
            listLayout.addView(getChildView(senderId, groupName, listLayout));
        }
        return view;
    }

    private View getChildView(final String senderId, final String groupName, final LinearLayout
            parent) {
        View view = mActivity.getLayoutInflater().inflate(
                R.layout.widget_icon_text_button_row, parent, false);
        ((ImageView) view.findViewById(R.id.widget_itbr_icon))
                .setImageResource(R.drawable.group_grey600);
        TextView text =
                ((TextView) view.findViewById(R.id.widget_itbr_text));
        text.setText(groupName);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.returnResponse(groupName,
                        mSenders.getSender(senderId).groups.get(groupName).notificationKey);
            }
        });
        Button deleteButton = (Button) view.findViewById(R.id.widget_itbr_button);
        deleteButton.setText(mActivity.getString(R.string.address_book_delete));
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Sender sender = mSenders.getSender(senderId);
                int position = sender.groups.indexOfKey(groupName);
                if (position >= 0) {
                    sender.groups.remove(groupName);
                    removeChild(parent, position);
                }
            }
        });
        return view;
    }
}
