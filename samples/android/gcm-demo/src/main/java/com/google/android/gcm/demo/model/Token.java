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

import android.support.v4.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import static com.google.android.gcm.demo.model.Constants.TOKENS;

/**
 * Persist a {@link com.google.android.gms.iid.InstanceID}'s Token
 */
public class Token {

    private static final String JSON_KEY_SCOPE = "scope";
    private static final String JSON_KEY_TOKEN = "token";
    private static final String JSON_KEY_EXTRAS = "extras";
    private static final String JSON_KEY_CREATED_AT = "created_at";

    public String token;
    public String scope;
    public ArrayMap<String, String> extras = new ArrayMap<>();
    public long createdAt;

    static public Token fromJson(JSONObject json) throws JSONException {
        Token token = new Token();

        // read {token: "aaa", "scope": "GCM", "created_at": 123456}
        token.token = json.getString(JSON_KEY_TOKEN);
        token.scope = json.optString(JSON_KEY_SCOPE);
        token.createdAt = json.optLong(JSON_KEY_CREATED_AT);

        // read "extras": {"key1" : "value1", "key2" : "value2"}
        JSONObject jsonExtras = json.optJSONObject(TOKENS);
        if (jsonExtras != null) {
            Iterator<String> jsonExtrasIterator = jsonExtras.keys();
            while (jsonExtrasIterator.hasNext()) {
                String key = jsonExtrasIterator.next();
                token.extras.put(key, jsonExtras.getString(key));
            }
        }
        return token;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_KEY_TOKEN, token);
        json.putOpt(JSON_KEY_SCOPE, scope);
        json.putOpt(JSON_KEY_EXTRAS, new JSONObject(extras));
        json.put(JSON_KEY_CREATED_AT, createdAt);
        return json;
    }
}
