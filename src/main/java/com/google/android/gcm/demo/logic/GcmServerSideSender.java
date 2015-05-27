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

import static com.google.android.gcm.demo.logic.HttpRequest.CONTENT_TYPE_FORM_ENCODED;
import static com.google.android.gcm.demo.logic.HttpRequest.CONTENT_TYPE_JSON;
import static com.google.android.gcm.demo.logic.HttpRequest.HEADER_CONTENT_TYPE;
import static com.google.android.gcm.demo.logic.HttpRequest.HEADER_AUTHORIZATION;

import android.util.Log;

import com.google.android.gcm.demo.service.LoggingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Map;

/**
 * This class is used to send GCM downstream messages in the same way a server would.
 */
public class GcmServerSideSender {

    private static final String GCM_SEND_ENDPOINT = "https://gcm-http.googleapis.com/gcm/send";

    private static final String UTF8 = "UTF-8";

    private static final String PARAM_TO = "to";
    private static final String PARAM_COLLAPSE_KEY = "collapse_key";
    private static final String PARAM_DELAY_WHILE_IDLE = "delay_while_idle";
    private static final String PARAM_TIME_TO_LIVE = "time_to_live";
    private static final String PARAM_DRY_RUN = "dry_run";
    private static final String PARAM_RESTRICTED_PACKAGE_NAME = "restricted_package_name";

    private static final String PARAM_PLAINTEXT_PAYLOAD_PREFIX = "data.";

    private static final String PARAM_JSON_PAYLOAD = "data";
    private static final String PARAM_JSON_NOTIFICATION_PARAMS = "notification";

    public static final String RESPONSE_PLAINTEXT_MESSAGE_ID = "id";
    public static final String RESPONSE_PLAINTEXT_CANONICAL_REG_ID = "registration_id";
    public static final String RESPONSE_PLAINTEXT_ERROR = "Error";

    private final String key;
    private final LoggingService.Logger logger;


    /**
     * @param key    The API key used to authorize calls to Google
     * @param logger the GCM Demo logger
     */
    public GcmServerSideSender(String key, LoggingService.Logger logger) {
        this.key = key;
        this.logger = logger;
    }

    /**
     * Send a downstream message via HTTP plain text.
     *
     * @param destination the registration id of the recipient app.
     * @param message     the message to be sent
     * @throws IOException
     */
    public void sendHttpPlaintextDownstreamMessage(String destination, Message message)
            throws IOException {

        StringBuilder request = new StringBuilder();
        request.append(PARAM_TO).append('=').append(destination);
        addOptParameter(request, PARAM_DELAY_WHILE_IDLE, message.isDelayWhileIdle());
        addOptParameter(request, PARAM_DRY_RUN, message.isDryRun());
        addOptParameter(request, PARAM_COLLAPSE_KEY, message.getCollapseKey());
        addOptParameter(request, PARAM_RESTRICTED_PACKAGE_NAME, message.getRestrictedPackageName());
        addOptParameter(request, PARAM_TIME_TO_LIVE, message.getTimeToLive());
        for (Map.Entry<String, String> entry : message.getData().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                String prefixedKey = PARAM_PLAINTEXT_PAYLOAD_PREFIX + entry.getKey();
                addOptParameter(request, prefixedKey, URLEncoder.encode(entry.getValue(), UTF8));
            }
        }

        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM_ENCODED);
        httpRequest.setHeader(HEADER_AUTHORIZATION, "key=" + key);
        httpRequest.doPost(GCM_SEND_ENDPOINT, request.toString());

        if (httpRequest.getResponseCode() != 200) {
            throw new IOException("Invalid request."
                    + "\nStatus: " + httpRequest.getResponseCode()
                    + "\nResponse: " + httpRequest.getResponseBody());
        }

        String[] lines = httpRequest.getResponseBody().split("\n");
        if (lines.length == 0 || lines[0].equals("")) {
            throw new IOException("Received empty response from GCM service.");
        }

        String[] firstLineValues = lines[0].split("=");
        if (firstLineValues.length != 2) {
            throw new IOException("Invalid response from GCM: " + httpRequest.getResponseBody());
        }

        switch (firstLineValues[0]) {
            case RESPONSE_PLAINTEXT_MESSAGE_ID:
                logger.log(Log.INFO, "Message sent.\nid: " + firstLineValues[1]);
                // check for canonical registration id
                if (lines.length > 1) {
                    // If the response includes a 2nd line we expect it to be the CANONICAL REG ID
                    String[] secondLineValues = lines[1].split("=");
                    if (secondLineValues.length == 2
                            && secondLineValues[0].equals(RESPONSE_PLAINTEXT_CANONICAL_REG_ID)) {
                        logger.log(Log.INFO, "Message sent: canonical registration id = "
                                + secondLineValues[1]);
                    } else {
                        logger.log(Log.ERROR, "Invalid response from GCM."
                                + "\nResponse: " + httpRequest.getResponseBody());
                    }
                }
                break;
            case RESPONSE_PLAINTEXT_ERROR:
                logger.log(Log.ERROR, "Message failed.\nError: " + firstLineValues[1]);
                break;
            default:
                logger.log(Log.ERROR, "Invalid response from GCM."
                        + "\nResponse: " + httpRequest.getResponseBody());
                break;
        }
    }

    /**
     * Send a downstream message via HTTP JSON.
     *
     * @param destination the registration id of the recipient app.
     * @param message        the message to be sent
     * @throws IOException
     */
    public void sendHttpJsonDownstreamMessage(String destination,
                                              Message message) throws IOException {

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put(PARAM_TO, destination);
            jsonBody.putOpt(PARAM_COLLAPSE_KEY, message.getCollapseKey());
            jsonBody.putOpt(PARAM_RESTRICTED_PACKAGE_NAME, message.getRestrictedPackageName());
            jsonBody.putOpt(PARAM_TIME_TO_LIVE, message.getTimeToLive());
            jsonBody.putOpt(PARAM_DELAY_WHILE_IDLE, message.isDelayWhileIdle());
            jsonBody.putOpt(PARAM_DRY_RUN, message.isDryRun());
            if (message.getData().size() > 0) {
                JSONObject jsonPayload = new JSONObject(message.getData());
                jsonBody.put(PARAM_JSON_PAYLOAD, jsonPayload);
            }
            if (message.getNotificationParams().size() > 0) {
                JSONObject jsonNotificationParams = new JSONObject(message.getNotificationParams());
                jsonBody.put(PARAM_JSON_NOTIFICATION_PARAMS, jsonNotificationParams);
            }
        } catch (JSONException e) {
            logger.log(Log.ERROR, "Failed to build JSON body");
            return;
        }

        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        httpRequest.setHeader(HEADER_AUTHORIZATION, "key=" + key);
        httpRequest.doPost(GCM_SEND_ENDPOINT, jsonBody.toString());

        if (httpRequest.getResponseCode() != 200) {
            throw new IOException("Invalid request."
                    + " status: " + httpRequest.getResponseCode()
                    + " response: " + httpRequest.getResponseBody());
        }

        JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(httpRequest.getResponseBody());
            logger.log(Log.INFO, "Send message:\n" + jsonResponse.toString(2));
        } catch (JSONException e) {
            logger.log(Log.ERROR, "Failed to parse server response:\n"
                    + httpRequest.getResponseBody());
        }
    }

    /**
     * Adds a new parameter to the HTTP POST body without doing any encoding.
     *
     * @param body  HTTP POST body.
     * @param name  parameter's name.
     * @param value parameter's value.
     */
    private static void addOptParameter(StringBuilder body, String name, Object value) {
        if (value != null) {
            String encodedValue = value.toString();
            if (value instanceof Boolean) {
                encodedValue = ((Boolean) value) ? "1" : "0";
            }
            body.append('&').append(name).append('=').append(encodedValue);
        }
    }
}