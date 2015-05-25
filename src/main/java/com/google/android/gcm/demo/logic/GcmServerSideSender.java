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

import static com.google.android.gcm.demo.logic.Util.nonNull;
import static com.google.android.gcm.demo.logic.Util.splitHTTPHeader;
import static com.google.android.gcm.demo.logic.Util.getString;

import android.util.Log;

import com.google.android.gcm.demo.service.LoggingService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * This class is used to send GCM downstream messages in the same way a server would.
 */
public class GcmServerSideSender {

    private static final String TAG = "GCMDemo-Sender";

    /**
     * Constant for UTF-8 encoding.
     */
    private static final String UTF8 = "UTF-8";

    /**
     * Endpoint for sending messages.
     */
    private static final String GCM_SEND_ENDPOINT =
            "https://android.googleapis.com/gcm/send";

    /**
     * HTTP parameter for registration id.
     */
    private static final String PARAM_REGISTRATION_ID = "registration_id";

    /**
     * JSON parameter for registration ids.
     */
    private static final String PARAM_REGISTRATION_IDS = "registration_ids";

    /**
     * JSON/HTTP parameter for destination.
     */
    private static final String PARAM_TO = "to";

    /**
     * HTTP/JSON parameter for collapse key.
     */
    private static final String PARAM_COLLAPSE_KEY = "collapse_key";

    /**
     * HTTP/JSON parameter for delaying the message delivery if the device is idle.
     */
    private static final String PARAM_DELAY_WHILE_IDLE = "delay_while_idle";

    /**
     * HTTP/JSON parameter for telling gcm to validate the message without actually sending it.
     */
    private static final String PARAM_DRY_RUN = "dry_run";

    /**
     * HTTP/JSON parameter for package name that can be used to restrict message delivery by
     * matching
     * against the package name used to generate the registration id.
     */
    private static final String PARAM_RESTRICTED_PACKAGE_NAME = "restricted_package_name";

    /**
     * Prefix to HTTP parameter used to pass key-values in the message payload.
     */
    private static final String PARAM_PAYLOAD_PREFIX = "data.";

    /**
     * JSON parameter used to pass key-values in the message payload.
     */
    private static final String PARAM_PAYLOAD = "data";

    /**
     * JSON parameter used to pass notification parameters.
     */
    private static final String PARAM_NOTIFICATION_PARAMS = "notification";

    /**
     * Prefix to HTTP parameter used to set the message time-to-live.
     */
    private static final String PARAM_TIME_TO_LIVE = "time_to_live";

    /**
     * Token returned by GCM when a message was successfully sent.
     */
    public static final String TOKEN_MESSAGE_ID = "id";

    /**
     * Token returned by GCM when the requested registration id has a canonical
     * value.
     */
    public static final String TOKEN_CANONICAL_REG_ID = "registration_id";

    /**
     * Token returned by GCM when there was an error sending a message.
     */
    public static final String TOKEN_ERROR = "Error";

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
     * Send a downstream message via HTTP cleartext.
     *
     * @param registrationId the registration id of the recipient app.
     * @param message        the message to be sent
     * @throws IOException
     */
    public void sendHttpCleartextDownstreamMessage(String registrationId,
                                                   Message message) throws IOException {
        String requestBody = getCleartextRequestBody(registrationId, message);
        String responseBody = doPostAndGetResponse(requestBody,
                "application/x-www-form-urlencoded;charset=UTF-8");
        String[] lines = responseBody.split("\n");
        if (lines.length == 0 || lines[0].equals("")) {
            throw new IOException("Received empty response from GCM service.");
        }
        String firstLine = lines[0];
        String[] responseParts = splitHTTPHeader(firstLine);
        String token = responseParts[0];
        String value = responseParts[1];
        switch (token) {
            case TOKEN_MESSAGE_ID:
                logger.log(Log.INFO, "Message sent: id = " + value);
                // check for canonical registration id
                if (lines.length > 1) {
                    String secondLine = lines[1];
                    responseParts = splitHTTPHeader(secondLine);
                    token = responseParts[0];
                    value = responseParts[1];
                    if (token.equals(TOKEN_CANONICAL_REG_ID)) {
                        logger.log(Log.INFO, "Message sent: canonical registration id = " + value);
                    } else {
                        logger.log(Log.ERROR, "Invalid response from GCM: " + responseBody);
                    }
                }
                break;
            case TOKEN_ERROR:
                logger.log(Log.ERROR, "Message not sent, server error code " + value);
                break;
            default:
                logger.log(Log.ERROR, "Failed to parse server response:\n" + responseBody);
                break;
        }

    }


    /**
     * Send a downstream message via HTTP JSON.
     *
     * @param registrationId the registration id of the recipient app.
     * @param message        the message to be sent
     * @throws IOException
     */
    public void sendHttpJsonDownstreamMessage(String registrationId,
                                              Message message) throws IOException {

        String requestBody;
        try {
            requestBody = getJsonRequestBody(registrationId, message);
        } catch (JSONException e) {
            logger.log(Log.ERROR, "Failed to build JSON body");
            return;
        }
        String responseBody = doPostAndGetResponse(requestBody, "application/json");
        JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(responseBody);
            logger.log(Log.INFO, "Send message:\n" + jsonResponse.toString(2));
        } catch (JSONException e) {
            logger.log(Log.ERROR, "Failed to parse server response:\n" + responseBody);
        }
    }

    private String doPostAndGetResponse(String requestBody, String contentTye) throws IOException {
        int status;
        HttpURLConnection conn = post(GCM_SEND_ENDPOINT, contentTye, requestBody);
        status = conn.getResponseCode();

        String responseBody;
        if (status != 200) {
            try (InputStream errorStream = conn.getErrorStream()) {
                responseBody = getString(errorStream);
            } catch (IOException e) {
                // ignore the exception since it will thrown an IOException anyway
                responseBody = "N/A";
            }
            throw new IOException("Invalid request: status = " + status + "\n" + responseBody);
        } else {
            try (InputStream inputStream = conn.getInputStream()) {
                responseBody = getString(inputStream);
            }
        }
        conn.disconnect();

        return responseBody;
    }

    /**
     * Adds a new parameter to the HTTP POST body without doing any encoding.
     *
     * @param body  HTTP POST body.
     * @param name  parameter's name.
     * @param value parameter's value.
     */
    private static void addParameter(StringBuilder body, String name,
                                     String value) {
        nonNull(body).append('&')
                .append(nonNull(name)).append('=').append(nonNull(value));
    }


    /**
     * Creates a new HTTP request body.
     *
     * @param registrationId the registration id of the recipient app.
     * @param message        the message to be sent
     * @return a String with the content of the body.
     * @throws UnsupportedEncodingException
     */
    private String getCleartextRequestBody(String registrationId,
                                           Message message)
            throws UnsupportedEncodingException {
        StringBuilder body = new StringBuilder(
                nonNull(PARAM_TO)).append('=').append(nonNull(registrationId));
        if (message.isDelayWhileIdle() != null) {
            addParameter(body, PARAM_DELAY_WHILE_IDLE, message.isDelayWhileIdle() ? "1" : "0");
        }
        if (message.isDryRun() != null) {
            addParameter(body, PARAM_DRY_RUN, message.isDryRun() ? "1" : "0");
        }
        if (message.getCollapseKey() != null) {
            addParameter(body, PARAM_COLLAPSE_KEY, message.getCollapseKey());
        }
        if (message.getRestrictedPackageName() != null) {
            addParameter(body, PARAM_RESTRICTED_PACKAGE_NAME, message.getRestrictedPackageName());
        }
        if (message.getTimeToLive() != null) {
            addParameter(body, PARAM_TIME_TO_LIVE, Integer.toString(message.getTimeToLive()));
        }
        if (message.getData() != null) {
            for (Map.Entry<String, String> entry : message.getData().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null || value == null) {
                    // skipping null key/value
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Skipping empty key/value: " + key + " " + value);
                    }
                } else {
                    key = PARAM_PAYLOAD_PREFIX + key;
                    addParameter(body, key, URLEncoder.encode(value, UTF8));
                }
            }
        }
        if (message.getNotificationParams() != null && message.getNotificationParams().size() > 0) {
            // This should never be invoked via the UI
            throw new IllegalArgumentException(
                    "Notification payload is not supported in clear text requests");
        }
        return body.toString();
    }

    /**
     * Creates a new HTTP request body.
     *
     * @param registrationId the registration id of the recipient app.
     * @param message        the message to be sent
     * @return a String with the content of the body.
     * @throws JSONException
     */
    private String getJsonRequestBody(String registrationId,
                                      Message message) throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put(PARAM_TO, registrationId);
        if (message.getCollapseKey() != null) {
            jsonBody.put(PARAM_COLLAPSE_KEY, message.getCollapseKey());
        }
        if (message.getRestrictedPackageName() != null) {
            jsonBody.put(PARAM_RESTRICTED_PACKAGE_NAME, message.getRestrictedPackageName());
        }
        if (message.getTimeToLive() != null) {
            jsonBody.put(PARAM_TIME_TO_LIVE, message.getTimeToLive());
        }
        if (message.isDelayWhileIdle() != null) {
            jsonBody.put(PARAM_DELAY_WHILE_IDLE, message.isDelayWhileIdle());
        }
        if (message.isDelayWhileIdle() != null) {
            jsonBody.put(PARAM_DRY_RUN, message.isDryRun());
        }
        if (message.getData() != null) {
            JSONObject jsonPayload = new JSONObject(message.getData());
            jsonBody.put(PARAM_PAYLOAD, jsonPayload);
        }
        if (message.getNotificationParams() != null) {
            JSONObject jsonNotificationParams = new JSONObject(message.getNotificationParams());
            jsonBody.put(PARAM_NOTIFICATION_PARAMS, jsonNotificationParams);
        }
        return jsonBody.toString();
    }


    /**
     * Makes an HTTP POST request to a given endpoint.
     * <p/>
     * <p/>
     * <strong>Note: </strong> the returned connected should not be disconnected,
     * otherwise it would kill persistent connections made using Keep-Alive.
     *
     * @param url         endpoint to post the request.
     * @param contentType type of request.
     * @param body        body of the request.
     * @return the underlying connection.
     * @throws IOException propagated from underlying methods.
     */
    private HttpURLConnection post(String url, String contentType, String body)
            throws IOException {
        if (url == null || body == null) {
            throw new IllegalArgumentException("arguments cannot be null");
        }
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", "key=" + key);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
        }
        return conn;
    }

}
