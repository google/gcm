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
package com.google.android.gcm.demo.model;

/**
 * Constants used for persisting the app's objects
 */
public class Constants {
    // Sednder constants
    public static final String NAME = "name";
    public static final String SENDER_ID = "senderId";
    public static final String API_KEYS = "apiKeys";
    public static final String TEST_APP_TOKEN = "testAppToken";
    public static final String OTHER_TOKENS = "otherTokens";
    public static final String TOPICS = "topics";
    public static final String GROUPS = "groups";
    // DeviceGroup constants
    public static final String NOTIFICATION_KEY_NAME = "notificationKeyName";
    public static final String NOTIFICATION_KEY = "notificationKey";
    public static final String TOKENS = "tokens";
    // TaskTracker constants
    public static final String TAG = "TAG";
    public static final String WINDOW_START_ELAPSED_SECS = "WINDOW_START_ELAPSED_SECS";
    public static final String WINDOW_STOP_ELAPSED_SECS = "WINDOW_STOP_ELAPSED_SECS";
    public static final String PERIOD = "PERIOD";
    public static final String FLEX = "FLEX";
    public static final String CREATED_AT_ELAPSED_SECS = "CREATED_AT_ELAPSED_SECS";
    public static final String CANCELLED = "CANCELLED";
    public static final String EXECUTED = "EXECUTED";

    private Constants() {

    }

}
