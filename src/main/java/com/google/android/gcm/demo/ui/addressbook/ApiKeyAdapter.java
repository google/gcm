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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.Sender;

/**
 * Adapter for displaying the list of API keys of a sender.
 */
public class ApiKeyAdapter extends AbstractElementAdapter {

    public ApiKeyAdapter(SelectActivity activity) {
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
        title.setText(mActivity.getString(R.string.address_book_api_key_title));
        Button addButton = (Button) view.findViewById(R.id.sender_element_add);
        addButton.setText(mActivity.getString(R.string.address_book_api_key_add));
        final LinearLayout listLayout = (LinearLayout) view.findViewById(R.id.sender_element_list);
        for (String apiKey : sender.apiKeys) {
            listLayout.addView(getChildView(senderId, apiKey, listLayout));
        }
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddApiKeyDialog(senderId, listLayout);

            }
        });
        return view;
    }

    private void showAddApiKeyDialog(final String senderId, final LinearLayout listLayout) {
        // Create the dialog
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mActivity);
        alertBuilder.setTitle(R.string.address_book_add_api_key_title);
        alertBuilder.setMessage(R.string.address_book_add_api_key_message);
        // Create the input fields
        final EditText nameView = new EditText(mActivity);
        nameView.setHint(R.string.address_book_add_api_key_hint);
        LinearLayout layout = mActivity.getAddDialogLayout();
        layout.addView(nameView);
        alertBuilder.setView(layout);
        // Buttons
        alertBuilder.setPositiveButton(R.string.address_book_dialog_add, null);
        alertBuilder.setNegativeButton(R.string.address_book_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        // Show the dialog
        final AlertDialog dialog = alertBuilder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        // Add a new address book entry
                        String name = nameView.getText().toString().trim();
                        if (!mSenders.getSender(senderId).apiKeys.contains(name)) {
                            Sender entry = mSenders.getSender(senderId);
                            entry.apiKeys.add(name);
                            mSenders.updateSender(entry);
                            addChild(listLayout, getChildView(senderId, name, listLayout),
                                    entry.apiKeys.indexOf(name));
                            dialog.dismiss();
                        } else {
                            Toast.makeText(mActivity,
                                    R.string.address_book_add_sender_invalid_api_key,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private View getChildView(final String senderId, final String apiKey, final LinearLayout
            parent) {
        View view = mActivity.getLayoutInflater().inflate(
                R.layout.widget_icon_text_button_row, parent, false);
        ((ImageView) view.findViewById(R.id.widget_itbr_icon))
                .setImageResource(R.drawable.key_grey600);
        TextView text =
                ((TextView) view.findViewById(R.id.widget_itbr_text));
        text.setText(apiKey);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.returnResponse(null, apiKey);
            }
        });
        Button deleteButton = (Button) view.findViewById(R.id.widget_itbr_button);
        deleteButton.setText(mActivity.getString(R.string.address_book_delete));

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Sender sender = mSenders.getSender(senderId);
                int position = sender.apiKeys.indexOf(apiKey);
                if (position >= 0) {
                    sender.apiKeys.remove(position);
                    removeChild(parent, position);
                }
            }
        });

        return view;
    }

}
