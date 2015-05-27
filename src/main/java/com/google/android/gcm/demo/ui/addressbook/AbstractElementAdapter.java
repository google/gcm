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

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gcm.demo.model.SenderCollection;
import com.google.android.gcm.demo.service.LoggingService;

/**
 * This is the base class for the different adapters used to show views of Sender elements like
 * tokens, api keys, topics or groups. Subclasses must implement
 * {@link #getView(String senderId, ViewGroup parent)}.
 */
public abstract class AbstractElementAdapter {

    protected SelectActivity mActivity;
    protected SenderCollection mSenders;
    protected LoggingService.Logger mLogger;

    public AbstractElementAdapter(SelectActivity activity) {
        mActivity = activity;
        mSenders = SenderCollection.getInstance(mActivity);
        mLogger = new LoggingService.Logger(mActivity);
    }

    public abstract View getView(final String senderId, ViewGroup parentView);

    protected void removeChild(LinearLayout layout, int position) {
        layout.removeViewAt(position);
    }

    protected void addChild(LinearLayout layout, View element, int position) {
        layout.addView(element, position);
    }
}
