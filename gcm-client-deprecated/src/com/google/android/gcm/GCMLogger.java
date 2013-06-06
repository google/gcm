/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gcm;

import android.util.Log;

/**
 * Custom logger.
 */
class GCMLogger {

    private final String mTag;
    // can't use class name on TAG since size is limited to 23 chars
    private final String mLogPrefix;

    GCMLogger(String tag, String logPrefix) {
        mTag = tag;
        mLogPrefix = logPrefix;
    }

    /**
     * Logs a message on logcat.
     *
     * @param priority logging priority
     * @param template message's template
     * @param args list of arguments
     */
    protected void log(int priority, String template, Object... args) {
        if (Log.isLoggable(mTag, priority)) {
            String message = String.format(template, args);
            Log.println(priority, mTag, mLogPrefix + message);
        }
    }
}
