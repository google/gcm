package com.google.android.gcm.server;

import static com.google.android.gcm.server.Constants.FCM_SEND_ENDPOINT;

/**
 * Created 25.11.2016
 * @author Žana Hrastovšek
 */
public class FirebaseSender extends Sender {

    /**
     * Default constructor.
     *

     * @param key API key obtained through the Google API Console.
     */
    public FirebaseSender(String key) {
        super(key);
    }

    @Override
    public String getEndpoint() {
        return FCM_SEND_ENDPOINT;
    }
}
