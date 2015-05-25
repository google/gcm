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

import static com.google.android.gcm.demo.logic.Util.getString;

import android.support.v4.util.SimpleArrayMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class representing an HTTP request
 */
public class HttpRequest {
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_PROJECT_ID = "project_id";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String CONTENT_TYPE_FORM_ENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_JSON = "application/json";

    private SimpleArrayMap<String, String> mHeaders = new SimpleArrayMap<>();
    private int responseCode;
    private String responseBody;

    /**
     * Add a request header
     * @param name the header's name
     * @param value the header's value
     */
    public void setHeader(String name, String value) {
        this.mHeaders.put(name, value);
    }

    /**
     * @return this request's response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     *
     * @return this request's response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Post the request
     * @param url where to post to
     * @param requestBody the body of the request
     * @throws IOException
     */
    public void doPost(String url, String requestBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(requestBody.getBytes().length);
        conn.setRequestMethod("POST");
        for (int i = 0; i < mHeaders.size(); i++) {
            conn.setRequestProperty(mHeaders.keyAt(i), mHeaders.valueAt(i));
        }
        try (OutputStream out = conn.getOutputStream()) {
            out.write(requestBody.getBytes());
        }

        responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            try (InputStream errorStream = conn.getErrorStream()) {
                responseBody = getString(errorStream);
            }
        } else {
            try (InputStream inputStream = conn.getInputStream()) {
                responseBody = getString(inputStream);
            }
        }
        conn.disconnect();
    }

}
