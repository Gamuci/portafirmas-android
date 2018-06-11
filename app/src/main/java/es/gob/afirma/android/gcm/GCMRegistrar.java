package es.gob.afirma.android.gcm;

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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import es.gob.afirma.android.signfolder.AppPreferences;

/**
 * Utilities for device registration.
 * <p>
 * object to keep track of the registration token.
 */
public final class GCMRegistrar {

    private static final String TAG = "GCMRegistrar";
    private static final String BACKOFF_MS = "backoff_ms";
    private static final String GSF_PACKAGE = "com.google.android.gsf";
    private static final int DEFAULT_BACKOFF_MS = 3000;
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER = "onServer";

    static void internalRegister(Context context, String... senderIds) {
        if (senderIds == null || senderIds.length == 0 ) {
            throw new IllegalArgumentException("No senderIds");
        }
        StringBuilder builder = new StringBuilder(senderIds[0]);
        for (int i = 1; i < senderIds.length; i++) {
            builder.append(',').append(senderIds[i]);
        }
        String senders = builder.toString();
        Log.v(TAG, "Registering app "  + context.getPackageName() +
                " of senders " + senders);
        Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_REGISTRATION);
        intent.setPackage(GSF_PACKAGE);
        intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
                PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        intent.putExtra(GCMConstants.EXTRA_SENDER, senders);
        context.startService(intent);
    }

    static void internalUnregister(Context context) {
        Log.v(TAG, "Unregistering app "  + context.getPackageName() );
        Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_UNREGISTRATION);
        intent.setPackage(GSF_PACKAGE);
        intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
                PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(intent);
    }


    /**
     * Gets the current registration id for application on GCM service.
     * <p>
     * If result is empty, the registration has failed.
     *
     * @return registration id, or empty string if the registration is not
     *         complete.
     */
    private static String getRegistrationId(Context context) {
        String registrationId = AppPreferences.getInstance().getPreference(PROPERTY_REG_ID, "");
        // check if app was updated; if so, it must clear registration id to
        // avoid a race condition if GCM sends a message
        int oldVersion = AppPreferences.getInstance().getPreferenceInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int newVersion = getAppVersion(context);
        if (oldVersion != Integer.MIN_VALUE && oldVersion != newVersion) {
            Log.v(TAG, "App version changed from " + oldVersion + " to " +
                    newVersion + "; resetting registration id");
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
     * @param context application's context.
     * @return old registration id.
     */
    static String clearRegistrationId(Context context) {
        return setRegistrationId(context, "");
    }

    /**
     * Sets the registration id in the persistence store.
     *
     * @param context application's context.
     * @param regId registration id
     */
    static String setRegistrationId(Context context, String regId) {
        String oldRegistrationId = AppPreferences.getInstance().getPreference(PROPERTY_REG_ID, "");
        int appVersion = getAppVersion(context);
        Log.v(TAG, "Saving regId on app version " + appVersion);
        AppPreferences.getInstance().setPreference(PROPERTY_REG_ID, regId);
        AppPreferences.getInstance().setPreferenceInt(PROPERTY_APP_VERSION, appVersion);
        return oldRegistrationId;
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     */
    public static void setRegisteredOnServer(boolean flag) {
        Log.v(TAG, "Setting registered on server status as: " + flag);
        AppPreferences.getInstance().setPreferenceBool(PROPERTY_ON_SERVER, flag);
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     */
    public static boolean isRegisteredOnServer() {
        boolean isRegistered = AppPreferences.getInstance().getPreferenceBool(PROPERTY_ON_SERVER, false);
        Log.v(TAG, "Is registered on server: " + isRegistered);
        return isRegistered;
    }

    /**
     * Gets the application version.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(),0);
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
        Log.d(TAG, "resetting backoff for " + context.getPackageName());
        setBackoff(DEFAULT_BACKOFF_MS);
    }

    /**
     * Gets the current backoff counter.
     *
     * @return current backoff counter, in milliseconds.
     */
    static int getBackoff() {
        return AppPreferences.getInstance().getPreferenceInt(BACKOFF_MS, DEFAULT_BACKOFF_MS);
    }

    /**
     * Sets the backoff counter.
     * <p>
     * This method should be called after a GCM call fails, passing an
     * exponential value.
     *
     * @param backoff new backoff counter, in milliseconds.
     */
    static void setBackoff(int backoff) {
        AppPreferences.getInstance().setPreferenceInt(BACKOFF_MS, backoff);
    }

    private GCMRegistrar() {
        throw new UnsupportedOperationException();
    }
}