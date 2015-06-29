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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
import com.google.android.gcm.demo.logic.PubSubHelper;
import com.google.android.gcm.demo.model.Sender;
import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;
import com.google.android.gcm.demo.ui.addressbook.SelectActivity;

import java.util.Map;

/**
 * This fragment shows a list of subscribed topics, allowing subscribing to new ones or
 * unsubscribing from the ones displayed.
 */
public class TopicsFragment extends AbstractFragment
        implements View.OnClickListener, MainActivity.RefreshableFragment {

    private static final String ACTION_UNSUBSCRIBE = "actionUnsubscribe";

    private PubSubHelper mPubSubHelper;
    private LoggingService.Logger mLogger;
    private SenderCollection mSenders;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mLogger = new LoggingService.Logger(getActivity());
        mPubSubHelper = new PubSubHelper(getActivity());
        mSenders = SenderCollection.getInstance(getActivity());

        View view = inflater.inflate(R.layout.fragment_topics, container, false);
        TextView description = (TextView) view.findViewById(R.id.topics_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(Html.fromHtml(getActivity().getString(R.string.topics_description)));
        view.findViewById(R.id.topics_subscribe).setOnClickListener(this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.topics_subscribe) {
            subscribe();
        } else if (ACTION_UNSUBSCRIBE.equals(v.getTag(R.id.tag_action))) {
            unsubscribe(v);
        }
    }

    @Override
    public void refresh() {
        float density = getActivity().getResources().getDisplayMetrics().density;
        SimpleArrayMap<String, Sender> addressBook =
                mSenders.getSenders();
        LinearLayout sendersList = new LinearLayout(getActivity());
        sendersList.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < addressBook.size(); i++) {
            Sender sender = addressBook.valueAt(i);
            if (sender.testAppToken != null) {
                LinearLayout senderRow = (LinearLayout) getActivity().getLayoutInflater()
                        .inflate(R.layout.widget_icon_text_button_row, sendersList, false);
                ImageView senderIcon = (ImageView) senderRow.findViewById(R.id.widget_itbr_icon);
                TextView senderLabel = (TextView) senderRow.findViewById(R.id.widget_itbr_text);
                senderRow.findViewById(R.id.widget_itbr_button).setVisibility(View.GONE);
                senderIcon.setImageResource(R.drawable.cloud_googblue);
                senderIcon.setPadding(0, 0, (int) (8 * density), 0);
                senderLabel.setText(
                        getString(R.string.topics_sender_id, sender.senderId));
                sendersList.addView(senderRow);
                int subscribedTopics = 0;
                for (Map.Entry<String, Boolean> topic : sender.topics.entrySet()) {
                    if (topic.getValue()) {
                        subscribedTopics++;
                        LinearLayout row = (LinearLayout) getActivity().getLayoutInflater()
                                .inflate(R.layout.widget_icon_text_button_row, sendersList, false);
                        ImageView icon = (ImageView) row.findViewById(R.id.widget_itbr_icon);
                        TextView label = (TextView) row.findViewById(R.id.widget_itbr_text);
                        Button button = (Button) row.findViewById(R.id.widget_itbr_button);
                        icon.setImageResource(R.drawable.bigtop_updates_grey600);
                        label.setText(topic.getKey());
                        button.setText(R.string.topics_unsubscribe);
                        button.setTag(R.id.tag_action, ACTION_UNSUBSCRIBE);
                        button.setTag(R.id.tag_senderid, sender.senderId);
                        button.setTag(R.id.tag_topic, topic.getKey());
                        button.setOnClickListener(this);
                        row.setPadding((int) (16 * density), 0, 0, 0);
                        sendersList.addView(row);
                    }
                }
                if (subscribedTopics == 0) {
                    TextView noTopics = new TextView(getActivity());
                    noTopics.setText(R.string.topics_no_topic_subscribed);
                    noTopics.setTypeface(null, Typeface.ITALIC);
                    noTopics.setPadding((int) (16 * density), 0, 0, 0);
                    sendersList.addView(noTopics);
                }
            }
        }
        if (sendersList.getChildCount() == 0) {
            TextView noTokens = new TextView(getActivity());
            noTokens.setText(getString(R.string.topics_no_sender_registered));
            noTokens.setTypeface(null, Typeface.ITALIC);
            sendersList.addView(noTokens);
            getActivity().findViewById(R.id.topics_subscribe).setEnabled(false);
        } else {
            getActivity().findViewById(R.id.topics_subscribe).setEnabled(true);
        }
        FrameLayout topicsView = (FrameLayout) getActivity().findViewById(R.id.topics_list_wrapper);
        topicsView.removeAllViews();
        topicsView.addView(sendersList);
    }

    private void subscribe() {
        Intent startSelectActivityIntent = SelectActivity.pickTopic(getActivity(),
                R.id.topics_subscribe);
        startActivityForResult(startSelectActivityIntent, 0);
    }

    @Override
    public void handleAddressBookSelection(int id, String senderId, String topic) {
        if (id == R.id.topics_subscribe) {
            Boolean subscribed = mSenders.getSender(senderId).topics.get(topic);
            if (subscribed) {
                Toast.makeText(getActivity(),
                        getString(R.string.topics_already_subscribed),
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            String gcmToken = mSenders.getSender(senderId).testAppToken;
            if (gcmToken == null) {
                Toast.makeText(getActivity(),
                        getString(R.string.topics_sender_not_registered),
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            Toast.makeText(getActivity(),
                    getString(R.string.topics_subscribing, topic),
                    Toast.LENGTH_SHORT)
                    .show();
            mPubSubHelper.subscribeTopic(senderId, gcmToken, topic, null);
        }
    }

    private void unsubscribe(View v) {
        String senderId = (String) v.getTag(R.id.tag_senderid);
        String topic = (String) v.getTag(R.id.tag_topic);
        Sender sender = mSenders.getSender(senderId);
        String gcmToken = (sender != null) ? sender.testAppToken : null;
        if (gcmToken == null) {
            mLogger.log(Log.ERROR, "gcmToken missing while unsubscribing from topic.");
        }
        Toast.makeText(getActivity(),
                getString(R.string.topics_unsubscribing, topic),
                Toast.LENGTH_SHORT)
                .show();
        mPubSubHelper.unsubscribeTopic(senderId, gcmToken, topic);
    }
}
