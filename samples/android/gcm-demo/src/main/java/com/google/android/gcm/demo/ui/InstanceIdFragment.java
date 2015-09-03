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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.InstanceIdHelper;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.model.Token;

import java.text.DateFormat;
import java.util.Date;


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
        mInstanceIdHelper = new InstanceIdHelper(getActivity());
        mSenders = SenderCollection.getInstance(getActivity());

        View view = inflater.inflate(R.layout.fragment_instanceid, container, false);

        setHtmlMode(view, R.id.fragment_description);
        view.findViewById(R.id.iid_delete_instance_id).setOnClickListener(this);
        view.findViewById(R.id.iid_get_token).setOnClickListener(this);
        view.findViewById(R.id.iid_instance_id).setOnLongClickListener(this);
        view.findViewById(R.id.iid_creation_time).setOnLongClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedState) {
        refresh();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iid_get_token:
                getActivity().startActivity(new Intent(getActivity(), TokenActivity.class));
                break;
            case R.id.widget_itbr_button:
                String authorizedEntity = (String) v.getTag(R.id.tag_senderid);
                String scope = (String) v.getTag(R.id.tag_scope);
                mInstanceIdHelper.deleteTokenInBackground(authorizedEntity, scope);
                break;
            case R.id.iid_delete_instance_id:
                confirmAndDeleteInstanceId();
                break;
        }
    }

    private void confirmAndDeleteInstanceId() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.iid_delete_instance_id)
                .setMessage(R.string.iid_delete_instance_id_message)
                .setPositiveButton(R.string.iid_delete_instance_id,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    setValue(activity.findViewById(R.id.iid_instance_id),
                                            getString(R.string.iid_instance_id_loading));
                                    setValue(activity.findViewById(R.id.iid_creation_time),
                                            getString(R.string.iid_instance_id_loading));
                                }
                                mInstanceIdHelper.deleteInstanceIdInBackground();
                            }
                        });
        builder.create().show();
    }

    @Override
    public void refresh() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final String instanceId = mInstanceIdHelper.getInstanceId();
                final String creationTime = DateFormat.getDateTimeInstance()
                        .format(new Date(mInstanceIdHelper.getCreationTime()));
                final Activity activity = getActivity();
                if (activity != null) {
                    Handler handler = new Handler(activity.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            setValue(activity.findViewById(R.id.iid_instance_id), instanceId);
                            setValue(activity.findViewById(R.id.iid_creation_time), creationTime);
                        }
                    });
                }
                return null;
            }
        }.execute();
        float density = getActivity().getResources().getDisplayMetrics().density;
        SimpleArrayMap<String, Sender> addressBook = mSenders.getSenders();
        LinearLayout sendersList = new LinearLayout(getActivity());
        sendersList.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < addressBook.size(); i++) {
            Sender sender = addressBook.valueAt(i);
            if (sender.appTokens.size() > 0) {
                LinearLayout senderRow = (LinearLayout) getActivity().getLayoutInflater()
                        .inflate(R.layout.widget_icon_text_button_row, sendersList, false);
                ImageView senderIcon = (ImageView) senderRow.findViewById(R.id.widget_itbr_icon);
                TextView senderLabel = (TextView) senderRow.findViewById(R.id.widget_itbr_text);
                senderRow.findViewById(R.id.widget_itbr_button).setVisibility(View.GONE);
                senderIcon.setImageResource(R.drawable.cloud_googblue);
                senderIcon.setPadding(0, 0, (int) (8 * density), 0);
                senderLabel.setText(getString(R.string.topics_sender_id, sender.senderId));
                sendersList.addView(senderRow);
                for (Token token : sender.appTokens.values()) {
                    LinearLayout row = (LinearLayout) getActivity().getLayoutInflater()
                            .inflate(R.layout.widget_icon_text_button_row, sendersList, false);
                    ImageView icon = (ImageView) row.findViewById(R.id.widget_itbr_icon);
                    TextView label = (TextView) row.findViewById(R.id.widget_itbr_text);
                    Button button = (Button) row.findViewById(R.id.widget_itbr_button);
                    icon.setImageResource(R.drawable.smartphone_grey600);
                    label.setText(token.scope + " - "
                            + AbstractFragment.truncateToMediumString(token.token));
                    button.setText(R.string.iid_delete_token);
                    button.setTag(R.id.tag_senderid, sender.senderId);
                    button.setTag(R.id.tag_scope, token.scope);
                    button.setOnClickListener(this);
                    row.setPadding((int) (16 * density), 0, 0, 0);
                    sendersList.addView(row);
                }
            }
        }
        if (sendersList.getChildCount() == 0) {
            TextView noTokens = new TextView(getActivity());
            noTokens.setText(getString(R.string.iid_no_tokens));
            noTokens.setTypeface(null, Typeface.ITALIC);
            sendersList.addView(noTokens);
        }
        FrameLayout tokensView = (FrameLayout) getActivity().findViewById(R.id.iid_tokens_wrapper);
        tokensView.removeAllViews();
        tokensView.addView(sendersList);
    }
}
