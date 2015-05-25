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
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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
import com.google.android.gcm.demo.model.SenderCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying the list of senders.
 */
public class SenderAdapter
        extends RecyclerView.Adapter<SenderAdapter.AddressBookViewHolder>
        implements View.OnClickListener {

    private SelectActivity mActivity;
    private SenderCollection mSenders;
    private List<AbstractElementAdapter> mAdapters;
    private String mExpandedSender = null;

    public SenderAdapter(SelectActivity activity) {
        mActivity = activity;
        mSenders = SenderCollection.getInstance(activity);
        mAdapters = new ArrayList<>();
        if (mActivity.getIntent().getCategories().contains(SelectActivity
                .INTENT_CATEGORY_API_KEY)) {
            mAdapters.add(new ApiKeyAdapter(mActivity));
        }
        if (mActivity.getIntent().getCategories().contains(SelectActivity.INTENT_CATEGORY_TOKEN)) {
            mAdapters.add(new TokenAdapter(mActivity));
        }
        if (mActivity.getIntent().getCategories().contains(SelectActivity.INTENT_CATEGORY_TOPIC)) {
            mAdapters.add(new TopicAdapter(mActivity));
        }
        if (mActivity.getIntent().getCategories().contains(SelectActivity.INTENT_CATEGORY_GROUP)) {
            mAdapters.add(new GroupAdapter(mActivity));
        }

    }

    @Override
    public AddressBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView senderView = (CardView) mActivity.getLayoutInflater().inflate(
                R.layout.widget_sender_container, parent, false);
        return new AddressBookViewHolder(senderView);
    }

    @Override
    public int getItemCount() {
        return mSenders.getSenders().size();
    }

    @Override
    public void onBindViewHolder(AddressBookViewHolder viewHolder, int position) {
        final Sender sender = mSenders.getSenders().valueAt(position);
        final View senderView = viewHolder.getView();
        ImageView icon = (ImageView) senderView.findViewById(R.id.widget_itbr_icon);
        icon.setImageResource(R.drawable.cloud_googblue);
        Button deleteButton = (Button) senderView.findViewById(R.id.widget_itbr_button);
        if (sender.senderId.equals(SenderCollection.DEFAULT_SENDER_ID)) {
            deleteButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }
        deleteButton.setText(mActivity.getString(R.string.address_book_delete));
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = mSenders.getSenders().indexOfKey(sender.senderId);
                mSenders.deleteSender(sender.senderId);
                notifyItemRemoved(position);
            }
        });
        TextView textView = (TextView) senderView.findViewById(R.id.widget_itbr_text);
        textView.setText(sender.name + "(" + sender.senderId + ")");
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity.getIntent().getCategories() != null &&
                        mActivity.getIntent().getCategories().contains(SelectActivity
                                .INTENT_CATEGORY_SENDERID)) {
                    // a click selects the sender: return the result
                    mActivity.returnResponse(sender.name, sender.senderId);
                } else {
                    // expand
                    if (mExpandedSender == null) {
                        mExpandedSender = sender.senderId;
                    } else if (mExpandedSender.equals(sender.senderId)) {
                        mExpandedSender = null;
                    } else {
                        String oldExpandedSender = mExpandedSender;
                        mExpandedSender = sender.senderId;
                        notifyItemChanged(mSenders.getSenders().indexOfKey(oldExpandedSender));
                    }
                    notifyItemChanged(mSenders.getSenders().indexOfKey(sender.senderId));
                }
            }
        });
        LinearLayout container =
                (LinearLayout) senderView.findViewById(R.id.sender_element_container);
        if (sender.senderId.equals(mExpandedSender)) {
            container.removeAllViews();
            for (AbstractElementAdapter adapter : mAdapters) {
                View view = adapter.getView(sender.senderId, container);
                if (view != null) {
                    container.addView(view);
                }
            }
            container.setVisibility(View.VISIBLE);
        } else {
            container.removeAllViews();
            container.invalidate();
            container.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select_dialog_add:
                // Create the dialog
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mActivity);
                alertBuilder.setTitle(R.string.address_book_add_sender_title);
                alertBuilder.setMessage(R.string.address_book_add_sender_message);
                // Create the input fields
                final EditText nameView = new EditText(mActivity);
                final EditText idView = new EditText(mActivity);
                nameView.setHint(R.string.address_book_add_sender_nameHint);
                idView.setHint(R.string.address_book_add_sender_idhint);
                LinearLayout layout = mActivity.getAddDialogLayout();
                layout.addView(nameView);
                layout.addView(idView);
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
                                String id = idView.getText().toString().trim();
                                if (!mSenders.getSenders().containsKey(id)) {
                                    Sender entry = new Sender();
                                    entry.senderId = id;
                                    entry.name = name;
                                    mSenders.updateSender(entry);
                                    notifyItemInserted(mSenders.getSenders().indexOfKey(id));
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(mActivity,
                                            R.string.address_book_add_sender_invalid_sender_id,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                break;
        }
    }

    class AddressBookViewHolder extends RecyclerView.ViewHolder {

        private ViewGroup mView;

        public AddressBookViewHolder(ViewGroup itemView) {
            super(itemView);
            this.mView = itemView;
        }

        public View getView() {
            return mView;
        }
    }
}
