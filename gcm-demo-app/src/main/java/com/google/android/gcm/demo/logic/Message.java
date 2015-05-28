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
package com.google.android.gcm.demo.logic;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GCM message copied from GCM HTTP helper library.
 * https://github.com/google/gcm/tree/master/gcm-server/src/com/google/android/gcm/server
 *
 * <p>
 * Instances of this class are immutable and should be created using a
 * {@link Builder}. Examples:
 *
 * <strong>Simplest message:</strong>
 * <pre><code>
 * Message message = new Message.Builder().build();
 * </pre></code>
 *
 * <strong>Message with optional attributes:</strong>
 * <pre><code>
 * Message message = new Message.Builder()
 *    .collapseKey(collapseKey)
 *    .timeToLive(3)
 *    .delayWhileIdle(true)
 *    .dryRun(true)
 *    .restrictedPackageName(restrictedPackageName)
 *    .build();
 * </pre></code>
 *
 * <strong>Message with optional attributes and payload data:</strong>
 * <pre><code>
 * Message message = new Message.Builder()
 *    .collapseKey(collapseKey)
 *    .timeToLive(3)
 *    .delayWhileIdle(true)
 *    .dryRun(true)
 *    .restrictedPackageName(restrictedPackageName)
 *    .addData("key1", "value1")
 *    .addData("key2", "value2")
 *    .build();
 * </pre></code>
 */
public final class Message implements Serializable {

    private final String collapseKey;
    private final Boolean delayWhileIdle;
    private final Integer timeToLive;
    private final Map<String, String> data;
    private final Map<String, String> notificationParams;
    private final Boolean dryRun;
    private final String restrictedPackageName;

    public static final class Builder {

        private final Map<String, String> data;
        private final Map<String, String> notificationParams;

        // optional parameters
        private String collapseKey;
        private Boolean delayWhileIdle;
        private Integer timeToLive;
        private Boolean dryRun;
        private String restrictedPackageName;

        public Builder() {
            this.data = new LinkedHashMap<>();
            this.notificationParams = new LinkedHashMap<>();
        }

        /**
         * Sets the collapseKey property.
         */
        public Builder collapseKey(String value) {
            collapseKey = value;
            return this;
        }

        /**
         * Sets the delayWhileIdle property (default value is {@literal false}).
         */
        public Builder delayWhileIdle(boolean value) {
            delayWhileIdle = value;
            return this;
        }

        /**
         * Sets the time to live, in seconds.
         */
        public Builder timeToLive(int value) {
            timeToLive = value;
            return this;
        }

        /**
         * Adds a key/value pair to the payload data.
         */
        public Builder addData(String key, String value) {
            data.put(key, value);
            return this;
        }

        /**
         * Sets the dryRun property (default value is {@literal false}).
         */
        public Builder dryRun(boolean value) {
            dryRun = value;
            return this;
        }

        /**
         * Sets the restrictedPackageName property.
         */
        public Builder restrictedPackageName(String value) {
            restrictedPackageName = value;
            return this;
        }

        /**
         * Sets the notification icon.
         */
        public Builder notificationIcon(String value) {
            notificationParams.put("icon", value);
            return this;
        }

        /**
         * Sets the notification title text.
         */
        public Builder notificationTitle(String value) {
            notificationParams.put("title", value);
            return this;
        }

        /**
         * Sets the notification body text.
         */
        public Builder notificationBody(String value) {
            notificationParams.put("body", value);
            return this;
        }

        /**
         * Sets the notification click action.
         */
        public Builder notificationClickAction(String value) {
            notificationParams.put("click_action", value);
            return this;
        }

        /**
         * Sets the notification sound.
         */
        public Builder notificationSound(String value) {
            notificationParams.put("sound", value);
            return this;
        }

        /**
         * Sets the notification tag.
         */
        public Builder notificationTag(String value) {
            notificationParams.put("tag", value);
            return this;
        }

        /**
         * Sets the notification color.
         */
        public Builder notificationColor(String value) {
            notificationParams.put("color", value);
            return this;
        }

        public Message build() {
            return new Message(this);
        }

    }

    private Message(Builder builder) {
        collapseKey = builder.collapseKey;
        delayWhileIdle = builder.delayWhileIdle;
        data = Collections.unmodifiableMap(builder.data);
        notificationParams = Collections.unmodifiableMap(builder.notificationParams);
        timeToLive = builder.timeToLive;
        dryRun = builder.dryRun;
        restrictedPackageName = builder.restrictedPackageName;
    }

    /**
     * Gets the collapse key.
     */
    public String getCollapseKey() {
        return collapseKey;
    }

    /**
     * Gets the delayWhileIdle flag.
     */
    public Boolean isDelayWhileIdle() {
        return delayWhileIdle;
    }

    /**
     * Gets the time to live (in seconds).
     */
    public Integer getTimeToLive() {
        return timeToLive;
    }

    /**
     * Gets the dryRun flag.
     */
    public Boolean isDryRun() {
        return dryRun;
    }

    /**
     * Gets the restricted package name.
     */
    public String getRestrictedPackageName() {
        return restrictedPackageName;
    }

    /**
     * Gets the payload data, which is immutable.
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * Gets the notification params, which are immutable.
     */
    public Map<String, String> getNotificationParams() {
        return notificationParams;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Message(");
        if (collapseKey != null) {
            builder.append("collapseKey=").append(collapseKey).append(", ");
        }
        if (timeToLive != null) {
            builder.append("timeToLive=").append(timeToLive).append(", ");
        }
        if (delayWhileIdle != null) {
            builder.append("delayWhileIdle=").append(delayWhileIdle).append(", ");
        }
        if (dryRun != null) {
            builder.append("dryRun=").append(dryRun).append(", ");
        }
        if (restrictedPackageName != null) {
            builder.append("restrictedPackageName=").append(restrictedPackageName).append(", ");
        }
        appendMap(builder, "data", data);
        appendMap(builder, "notificationParams", notificationParams);
        // Remove trailing ", "
        if (builder.charAt(builder.length() - 1) == ' ') {
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append(")");
        return builder.toString();
    }

    private void appendMap(StringBuilder builder, String name, Map<String, String> map) {
        if (!map.isEmpty()) {
            builder.append(name).append(": {");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                builder.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
            }
            // Remove trailing ","
            builder.delete(builder.length() - 1, builder.length());
            builder.append("}, ");
        }
    }

}