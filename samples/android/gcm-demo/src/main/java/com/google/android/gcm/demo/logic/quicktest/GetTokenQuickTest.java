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
package com.google.android.gcm.demo.logic.quicktest;

import android.content.Context;
import android.support.v4.util.SimpleArrayMap;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.logic.InstanceIdHelper;
import com.google.android.gcm.demo.service.LoggingService.Logger;

import java.util.Arrays;
import java.util.List;

public class GetTokenQuickTest implements QuickTest {

    @Override
    public int getName() {
        return R.string.quicktest_get_registration_token;
    }

    @Override
    public List<Integer> getRequiredParameters() {
        return Arrays.asList(R.id.home_sender_id);
    }

    @Override
    public void execute(Logger logger, Context context, SimpleArrayMap<Integer, String> params) {
        final String senderId = params.get(R.id.home_sender_id);

        InstanceIdHelper instanceIdHelper = new InstanceIdHelper(context);
        instanceIdHelper.getTokenInBackground(senderId, "GCM", null);
    }
}
