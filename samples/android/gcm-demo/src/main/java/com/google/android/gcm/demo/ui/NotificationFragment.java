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

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.GcmServerSideSender;
import com.google.android.gcm.demo.logic.Message;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Fragment used for sending GCM notifications. Notifications are a special type of downstream
 * messages used for showing app specific notifications. The icon, color and text of the
 * notification are controlled by the sender.
 */
public class NotificationFragment extends DownstreamFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_notification;
    }

    @Override
    protected void doGcmSend() {
        final Activity activity = this.getActivity();
        final Message.Builder messageBuilder = new Message.Builder();
        String icon = getValue(R.id.notification_icon);
        if (!TextUtils.isEmpty(icon)) {
            messageBuilder.notificationIcon(icon);
        } else {
            mLogger.log(Log.ERROR,
                    getResources().getString(R.string.notification_send_fail_icon_required));
            return;
        }
        String title = getValue(R.id.notification_title);
        if (!TextUtils.isEmpty(title)) {
            messageBuilder.notificationTitle(title);
        } else {
            mLogger.log(Log.ERROR,
                    getResources().getString(R.string.notification_send_fail_title_required));
            return;
        }
        String body = getValue(R.id.notification_body);
        if (!TextUtils.isEmpty(body)) {
            messageBuilder.notificationBody(body);
        }
        String clickAction = getValue(R.id.notification_click_action);
        if (!TextUtils.isEmpty(clickAction)) {
            messageBuilder.notificationClickAction(clickAction);
        }
        String sound = getValue(R.id.notification_sound);
        if (!TextUtils.isEmpty(sound)) {
            messageBuilder.notificationSound(sound);
        }
        String tag = getValue(R.id.notification_tag);
        if (!TextUtils.isEmpty(tag)) {
            messageBuilder.notificationTag(tag);
        }
        EditableMapView data = (EditableMapView) activity.findViewById(R.id.downstream_data);
        for (EditableMapView.MapEntry mapEntry : data.getMapEntries()) {
            if (!TextUtils.isEmpty(mapEntry.key) && !TextUtils.isEmpty(mapEntry.value)) {
                messageBuilder.addData(mapEntry.key, mapEntry.value);
            }
        }

        final String apiKey = getValue(R.id.downstream_api_key);
        final String registrationId = getValue(R.id.downstream_token);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                GcmServerSideSender sender = new GcmServerSideSender(apiKey, mLogger);
                try {
                    sender.sendHttpJsonDownstreamMessage(registrationId, messageBuilder.build());
                } catch (final IOException e) {
                    return e.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    String format = getResources().getString(R.string.notification_send_failed);
                    String toast = String.format(format, result);
                    Toast.makeText(activity, toast, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();

    }
}
