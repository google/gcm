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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilities for device registration.
 * <p>
 * <strong>Note:</strong> this class uses a private {@link SharedPreferences}
 * object to keep track of the registration token.
 */
public final class GCMRegistrar {

    /**
     * Default lifespan (7 days) of the {@link #isRegisteredOnServer(Context)}
     * flag until it is considered expired.
     */
    // NOTE: cannot use TimeUnit.DAYS because it's not available on API Level 8
    public static final long DEFAULT_ON_SERVER_LIFESPAN_MS =
            1000 * 3600 * 24 * 7;

    private static final String TAG = "GCMRegistrar";
    private static final String BACKOFF_MS = "backoff_ms";
    private static final String GSF_PACKAGE = "com.google.android.gsf";
    private static final String PREFERENCES = "com.google.android.gcm";
    private static final int DEFAULT_BACKOFF_MS = 3000;
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER = "onServer";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME =
            "onServerExpirationTime";
    private static final String PROPERTY_ON_SERVER_LIFESPAN =
            "onServerLifeSpan";

    /**
     * {@link GCMBroadcastReceiver} instance used to handle the retry intent.
     *
     * <p>
     * This instance cannot be the same as the one defined in the manifest
     * because it needs a different permission.
     */
    // guarded by GCMRegistrar.class
    private static GCMBroadcastReceiver sRetryReceiver;

    // guarded by GCMRegistrar.class
    private static Context sRetryReceiverContext;
    // guarded by GCMRegistrar.class
    private static String sRetryReceiverClassName;

    // guarded by GCMRegistrar.class
    private static PendingIntent sAppPendingIntent;

    /**
     * Checks if the device has the proper dependencies installed.
     * <p>
     * This method should be called when the application starts to verify that
     * the device supports GCM.
     *
     * @param context application context.
     * @throws UnsupportedOperationException if the device does not support GCM.
     */
    public static void checkDevice(Context context) {
        int version = Build.VERSION.SDK_INT;
        if (version < 8) {
            throw new UnsupportedOperationException("Device must be at least " +
                    "API Level 8 (instead of " + version + ")");
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(GSF_PACKAGE, 0);
        } catch (NameNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Device does not have package " + GSF_PACKAGE);
        }
    }

    /**
     * Checks that the application manifest is properly configured.
     * <p>
     * A proper configuration means:
     * <ol>
     *    <li>It creates a custom permission called
     *      {@code PACKAGE_NAME.permission.C2D_MESSAGE}.
     *    <li>It defines at least one {@link BroadcastReceiver} with category
     *      {@code PACKAGE_NAME}.
     *    <li>The {@link BroadcastReceiver}(s) uses the
     *      {@value com.google.android.gcm.GCMConstants#PERMISSION_GCM_INTENTS}
     *      permission.
     *    <li>The {@link BroadcastReceiver}(s) handles the 2 GCM intents
     *      ({@value com.google.android.gcm.GCMConstants#INTENT_FROM_GCM_MESSAGE}
     *      and
     *      {@value com.google.android.gcm.GCMConstants#INTENT_FROM_GCM_REGISTRATION_CALLBACK}).
     * </ol>
     * ...where {@code PACKAGE_NAME} is the application package.
     * <p>
     * This method should be used during development time to verify that the
     * manifest is properly set up, but it doesn't need to be called once the
     * application is deployed to the users' devices.
     *
     * @param context application context.
     * @throws IllegalStateException if any of the conditions above is not met.
     */
    public static void checkManifest(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        String permissionName = packageName + ".permission.C2D_MESSAGE";
        // check permission
        try {
            packageManager.getPermissionInfo(permissionName,
                    PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException(
                    "Application does not define permission " + permissionName);
        }
        // check receivers
        PackageInfo receiversInfo;
        try {
            receiversInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_RECEIVERS);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException(
                    "Could not get receivers for package " + packageName);
        }
        ActivityInfo[] receivers = receiversInfo.receivers;
        if (receivers == null || receivers.length == 0) {
            throw new IllegalStateException("No receiver for package " +
                    packageName);
        }
        log(context, Log.VERBOSE, "number of receivers for %s: %d",
                packageName, receivers.length);
        Set<String> allowedReceivers = new HashSet<String>();
        for (ActivityInfo receiver : receivers) {
            if (GCMConstants.PERMISSION_GCM_INTENTS.equals(
                    receiver.permission)) {
                allowedReceivers.add(receiver.name);
            }
        }
        if (allowedReceivers.isEmpty()) {
            throw new IllegalStateException("No receiver allowed to receive " +
                    GCMConstants.PERMISSION_GCM_INTENTS);
        }
        checkReceiver(context, allowedReceivers,
                GCMConstants.INTENT_FROM_GCM_REGISTRATION_CALLBACK);
        checkReceiver(context, allowedReceivers,
                GCMConstants.INTENT_FROM_GCM_MESSAGE);
    }

    private static void checkReceiver(Context context,
            Set<String> allowedReceivers, String action) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        Intent intent = new Intent(action);
        intent.setPackage(packageName);
        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent,
                PackageManager.GET_INTENT_FILTERS);
        if (receivers.isEmpty()) {
            throw new IllegalStateException("No receivers for action " +
                    action);
        }
        log(context, Log.VERBOSE, "Found %d receivers for action %s",
                receivers.size(), action);
        // make sure receivers match
        for (ResolveInfo receiver : receivers) {
            String name = receiver.activityInfo.name;
            if (!allowedReceivers.contains(name)) {
                throw new IllegalStateException("Receiver " + name +
                        " is not set with permission " +
                        GCMConstants.PERMISSION_GCM_INTENTS);
            }
        }
    }

    /**
     * Initiate messaging registration for the current application.
     * <p>
     * The result will be returned as an
     * {@link GCMConstants#INTENT_FROM_GCM_REGISTRATION_CALLBACK} intent with
     * either a {@link GCMConstants#EXTRA_REGISTRATION_ID} or
     * {@link GCMConstants#EXTRA_ERROR}.
     *
     * @param context application context.
     * @param senderIds Google Project ID of the accounts authorized to send
     *    messages to this application.
     * @throws IllegalStateException if device does not have all GCM
     *             dependencies installed.
     */
    public static void register(Context context, String... senderIds) {
        GCMRegistrar.resetBackoff(context);
        internalRegister(context, senderIds);
    }

    static void internalRegister(Context context, String... senderIds) {
        String flatSenderIds = getFlatSenderIds(senderIds);
        log(context, Log.VERBOSE, "Registering app for senders %s",
                flatSenderIds);
        Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_REGISTRATION);
        intent.setPackage(GSF_PACKAGE);
        setPackageNameExtra(context, intent);
        intent.putExtra(GCMConstants.EXTRA_SENDER, flatSenderIds);
        context.startService(intent);
    }

    /**
     * Unregister the application.
     * <p>
     * The result will be returned as an
     * {@link GCMConstants#INTENT_FROM_GCM_REGISTRATION_CALLBACK} intent with an
     * {@link GCMConstants#EXTRA_UNREGISTERED} extra.
     */
    public static void unregister(Context context) {
        GCMRegistrar.resetBackoff(context);
        internalUnregister(context);
    }

    static void internalUnregister(Context context) {
        log(context, Log.VERBOSE, "Unregistering app");
        Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_UNREGISTRATION);
        intent.setPackage(GSF_PACKAGE);
        setPackageNameExtra(context, intent);
        context.startService(intent);
    }

    static String getFlatSenderIds(String... senderIds) {
        if (senderIds == null || senderIds.length == 0) {
            throw new IllegalArgumentException("No senderIds");
        }
        StringBuilder builder = new StringBuilder(senderIds[0]);
        for (int i = 1; i < senderIds.length; i++) {
            builder.append(',').append(senderIds[i]);
        }
        return builder.toString();
    }

    /**
     * Clear internal resources.
     *
     * <p>
     * This method should be called by the main activity's {@code onDestroy()}
     * method.
     */
    public static synchronized void onDestroy(Context context) {
        if (sRetryReceiver != null) {
            log(context, Log.VERBOSE, "Unregistering retry receiver");
            sRetryReceiverContext.unregisterReceiver(sRetryReceiver);
            sRetryReceiver = null;
            sRetryReceiverContext = null;
        }
    }

    static synchronized void cancelAppPendingIntent() {
        if (sAppPendingIntent != null) {
            sAppPendingIntent.cancel();
            sAppPendingIntent = null;
        }
    }

    private synchronized static void setPackageNameExtra(Context context,
            Intent intent) {
        if (sAppPendingIntent == null) {
            log(context, Log.VERBOSE,
                    "Creating pending intent to get package name");
            sAppPendingIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(), 0);
        }
        intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
                sAppPendingIntent);
    }

    /**
     * Lazy initializes the {@link GCMBroadcastReceiver} instance.
     */
    static synchronized void setRetryBroadcastReceiver(Context context) {
        if (sRetryReceiver == null) {
            if (sRetryReceiverClassName == null) {
                // should never happen
                log(context, Log.ERROR,
                        "internal error: retry receiver class not set yet");
                sRetryReceiver = new GCMBroadcastReceiver();
            } else {
                Class<?> clazz;
                try {
                    clazz = Class.forName(sRetryReceiverClassName);
                    sRetryReceiver = (GCMBroadcastReceiver) clazz.newInstance();
                } catch (Exception e) {
                    log(context, Log.ERROR, "Could not create instance of %s. "
                            + "Using %s directly.", sRetryReceiverClassName,
                            GCMBroadcastReceiver.class.getName());
                    sRetryReceiver = new GCMBroadcastReceiver();
                }
            }
            String category = context.getPackageName();
            IntentFilter filter = new IntentFilter(
                    GCMConstants.INTENT_FROM_GCM_LIBRARY_RETRY);
            filter.addCategory(category);
            log(context, Log.VERBOSE, "Registering retry receiver");
            sRetryReceiverContext = context;
            sRetryReceiverContext.registerReceiver(sRetryReceiver, filter);
        }
    }

    /**
     * Sets the name of the retry receiver class.
     */
    static synchronized void setRetryReceiverClassName(Context context,
            String className) {
        log(context, Log.VERBOSE,
                "Setting the name of retry receiver class to %s", className);
        sRetryReceiverClassName = className;
    }

    /**
     * Gets the current registration id for application on GCM service.
     * <p>
     * If result is empty, the registration has failed.
     *
     * @return registration id, or empty string if the registration is not
     *         complete.
     */
    public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        // check if app was updated; if so, it must clear registration id to
        // avoid a race condition if GCM sends a message
        int oldVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int newVersion = getAppVersion(context);
        if (oldVersion != Integer.MIN_VALUE && oldVersion != newVersion) {
            log(context, Log.VERBOSE, "App version changed from %d to %d;"
                    + "resetting registration id", oldVersion, newVersion);
            clearRegistrationId(context);
            registrationId = "";
        }
        return registrationId;
    }

    /**
     * Checks whether the application was successfully registered on GCM
     * service.
     */
    public static boolean isRegistered(Context context) {
        return getRegistrationId(context).length() > 0;
    }

    /**
     * Clears the registration id in the persistence store.
     *
     * <p>As a side-effect, it also expires the registeredOnServer property.
     *
     * @param context application's context.
     * @return old registration id.
     */
    static String clearRegistrationId(Context context) {
        setRegisteredOnServer(context, null, 0);
        return setRegistrationId(context, "");
    }

    /**
     * Sets the registration id in the persistence store.
     *
     * @param context application's context.
     * @param regId registration id
     */
    static String setRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String oldRegistrationId = prefs.getString(PROPERTY_REG_ID, "");
        int appVersion = getAppVersion(context);
        log(context, Log.VERBOSE, "Saving regId on app version %d", appVersion);
        Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
        return oldRegistrationId;
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     */
    public static void setRegisteredOnServer(Context context, boolean flag) {
        // set the flag's expiration date
        long lifespan = getRegisterOnServerLifespan(context);
        long expirationTime = System.currentTimeMillis() + lifespan;
        setRegisteredOnServer(context, flag, expirationTime);
    }

    private static void setRegisteredOnServer(Context context, Boolean flag,
            long expirationTime) {
        final SharedPreferences prefs = getGCMPreferences(context);
        Editor editor = prefs.edit();
        if (flag != null) {
            editor.putBoolean(PROPERTY_ON_SERVER, flag);
            log(context, Log.VERBOSE,
                    "Setting registeredOnServer flag as %b until %s",
                    flag, new Timestamp(expirationTime));
        } else {
            log(context, Log.VERBOSE,
                    "Setting registeredOnServer expiration to %s",
                    new Timestamp(expirationTime));
        }
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.commit();
    }

    /**
     * Checks whether the device was successfully registered in the server side,
     * as set by {@link #setRegisteredOnServer(Context, boolean)}.
     *
     * <p>To avoid the scenario where the device sends the registration to the
     * server but the server loses it, this flag has an expiration date, which
     * is {@link #DEFAULT_ON_SERVER_LIFESPAN_MS} by default (but can be changed
     * by {@link #setRegisterOnServerLifespan(Context, long)}).
     */
    public static boolean isRegisteredOnServer(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        boolean isRegistered = prefs.getBoolean(PROPERTY_ON_SERVER, false);
        log(context, Log.VERBOSE, "Is registered on server: %b", isRegistered);
        if (isRegistered) {
            // checks if the information is not stale
            long expirationTime =
                    prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
            if (System.currentTimeMillis() > expirationTime) {
                log(context, Log.VERBOSE, "flag expired on: %s",
                        new Timestamp(expirationTime));
                return false;
            }
        }
        return isRegistered;
    }

    /**
     * Gets how long (in milliseconds) the {@link #isRegistered(Context)}
     * property is valid.
     *
     * @return value set by {@link #setRegisteredOnServer(Context, boolean)} or
     *      {@link #DEFAULT_ON_SERVER_LIFESPAN_MS} if not set.
     */
    public static long getRegisterOnServerLifespan(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        long lifespan = prefs.getLong(PROPERTY_ON_SERVER_LIFESPAN,
                DEFAULT_ON_SERVER_LIFESPAN_MS);
        return lifespan;
    }

    /**
     * Sets how long (in milliseconds) the {@link #isRegistered(Context)}
     * flag is valid.
     */
    public static void setRegisterOnServerLifespan(Context context,
            long lifespan) {
        final SharedPreferences prefs = getGCMPreferences(context);
        Editor editor = prefs.edit();
        editor.putLong(PROPERTY_ON_SERVER_LIFESPAN, lifespan);
        editor.commit();
    }

    /**
     * Gets the application version.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Coult not get package name: " + e);
        }
    }

    /**
     * Resets the backoff counter.
     * <p>
     * This method should be called after a GCM call succeeds.
     *
     * @param context application's context.
     */
    static void resetBackoff(Context context) {
        log(context, Log.VERBOSE, "Resetting backoff");
        setBackoff(context, DEFAULT_BACKOFF_MS);
    }

    /**
     * Gets the current backoff counter.
     *
     * @param context application's context.
     * @return current backoff counter, in milliseconds.
     */
    static int getBackoff(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        return prefs.getInt(BACKOFF_MS, DEFAULT_BACKOFF_MS);
    }

    /**
     * Sets the backoff counter.
     * <p>
     * This method should be called after a GCM call fails, passing an
     * exponential value.
     *
     * @param context application's context.
     * @param backoff new backoff counter, in milliseconds.
     */
    static void setBackoff(Context context, int backoff) {
        final SharedPreferences prefs = getGCMPreferences(context);
        Editor editor = prefs.edit();
        editor.putInt(BACKOFF_MS, backoff);
        editor.commit();
    }

    private static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    /**
     * Logs a message on logcat.
     *
     * @param context application's context.
     * @param priority logging priority
     * @param template message's template
     * @param args list of arguments
     */
    private static void log(Context context, int priority, String template,
            Object... args) {
        if (Log.isLoggable(TAG, priority)) {
            String message = String.format(template, args);
            Log.println(priority, TAG, "[" + context.getPackageName() + "]: "
                    + message);
        }
    }

    private GCMRegistrar() {
        throw new UnsupportedOperationException();
    }
}
