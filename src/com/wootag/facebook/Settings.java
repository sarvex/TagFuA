/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.wTagFufacebook.internal.AttributionIdentifiers;
import com.woTagFuacebook.internal.Utility;
import com.wooTagFucebook.internal.Validate;
import com.wootag.facebook.model.GraphObject;

/**
 * Allows some customization of sdk behavior.
 */
public final class Settings {

    private static final String TAG = Settings.class.getCanonicalName();
    private static final Set<LoggingBehavior> loggingBehaviors = new HashSet<LoggingBehavior>(
            Arrays.asList(LoggingBehavior.DEVELOPER_ERRORS));
    private static volatile Executor executor;
    private static volatile boolean shouldAutoPublishInstall;
    private static volatile String appVersion;
    private static volatile String applicationId;
    private static volatile String appClientToken;
    private static volatile boolean defaultsLoaded;
    private static final String FACEBOOK_COM = "facebook.com";
    private static volatile String facebookDomain = FACEBOOK_COM;
    private static AtomicLong onProgressThreshold = new AtomicLong(65536);
    private static volatile boolean platformCompatibilityEnabled;

    private static final int DEFAULT_CORE_POOL_SIZE = 5;
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 128;
    private static final int DEFAULT_KEEP_ALIVE = 1;
    private static final Object LOCK = new Object();

    private static final Uri ATTRIBUTION_ID_CONTENT_URI = Uri
            .parse("content://com.facebook.katana.provider.AttributionIdProvider");
    private static final String ATTRIBUTION_ID_COLUMN_NAME = "aid";

    private static final String ATTRIBUTION_PREFERENCES = "com.facebook.sdk.attributionTracking";
    private static final String PUBLISH_ACTIVITY_PATH = "%s/activities";
    private static final String MOBILE_INSTALL_EVENT = "MOBILE_APP_INSTALL";
    private static final String ANALYTICS_EVENT = "event";
    private static final String AUTO_PUBLISH = "auto_publish";

    private static final String APP_EVENT_PREFERENCES = "com.facebook.sdk.appEventPreferences";

    private static final BlockingQueue<Runnable> DEFAULT_WORK_QUEUE = new LinkedBlockingQueue<Runnable>(10);

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(final Runnable runnable) {

            return new Thread(runnable, "FacebookSdk #" + this.counter.incrementAndGet());
        }
    };

    /**
     * loadDefaultsFromMetadata will attempt to load certain settings (e.g., application ID, client token) from metadata
     * in the app's AndroidManifest.xml. The application ID will be read from this key.
     */
    public static final String APPLICATION_ID_PROPERTY = "com.facebook.sdk.ApplicationId";
    /**
     * loadDefaultsFromMetadata will attempt to load certain settings (e.g., application ID, client token) from metadata
     * in the app's AndroidManifest.xml. The client token will be read from this key.
     */
    public static final String CLIENT_TOKEN_PROPERTY = "com.facebook.sdk.ClientToken";

    /**
     * Certain logging behaviors are available for debugging beyond those that should be enabled in production. Enables
     * a particular extended logging in the sdk.
     *
     * @param behavior The LoggingBehavior to enable
     */
    public static final void addLoggingBehavior(final LoggingBehavior behavior) {

        synchronized (loggingBehaviors) {
            loggingBehaviors.add(behavior);
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be enabled in production. Disables
     * all extended logging behaviors.
     */
    public static final void clearLoggingBehaviors() {

        synchronized (loggingBehaviors) {
            loggingBehaviors.clear();
        }
    }

    /**
     * Gets the Facebook application ID for the current app. This will be null unless explicitly set or unless
     * loadDefaultsFromMetadata has been called.
     *
     * @return the application ID
     */
    public static String getApplicationId() {

        return applicationId;
    }

    /**
     * Gets the application version to the provided string.
     *
     * @return application version set via setAppVersion.
     */
    public static String getAppVersion() {

        return appVersion;
    }

    /**
     * Acquire the current attribution id from the facebook app.
     *
     * @return returns null if the facebook app is not present on the phone.
     */
    public static String getAttributionId(final ContentResolver contentResolver) {

        try {
            final String[] projection = { ATTRIBUTION_ID_COLUMN_NAME };
            final Cursor cursor = contentResolver.query(ATTRIBUTION_ID_CONTENT_URI, projection, null, null, null);
            if ((cursor == null) || !cursor.moveToFirst()) {
                return null;
            }
            final String attributionId = cursor.getString(cursor.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME));
            cursor.close();
            return attributionId;
        } catch (final Exception e) {
            Log.d(TAG, "Caught unexpected exception in getAttributionId(): " + e.toString());
            return null;
        }
    }

    /**
     * Gets the client token for the current app. This will be null unless explicitly set or unless
     * loadDefaultsFromMetadata has been called.
     *
     * @return the client token
     */
    public static String getClientToken() {

        return appClientToken;
    }

    /**
     * Returns the Executor used by the SDK for non-AsyncTask background work. By default this uses AsyncTask Executor
     * via reflection if the API level is high enough. Otherwise this creates a new Executor with defaults similar to
     * those used in AsyncTask.
     *
     * @return an Executor used by the SDK. This will never be null.
     */
    public static Executor getExecutor() {

        synchronized (LOCK) {
            if (Settings.executor == null) {
                Executor executor = getAsyncTaskExecutor();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE,
                            DEFAULT_KEEP_ALIVE, TimeUnit.SECONDS, DEFAULT_WORK_QUEUE, DEFAULT_THREAD_FACTORY);
                }
                Settings.executor = executor;
            }
        }
        return Settings.executor;
    }

    /**
     * Gets the base Facebook domain to use when making Web requests; in production code this will always be
     * "facebook.com".
     *
     * @return the Facebook domain
     */
    public static String getFacebookDomain() {

        return facebookDomain;
    }

    /**
     * Gets whether data such as that generated through AppEventsLogger and sent to Facebook should be restricted from
     * being used for purposes other than analytics and conversions, such as for targeting ads to this user. Defaults to
     * false. This value is stored on the device and persists across app launches.
     *
     * @param context Used to read the value.
     */
    public static boolean getLimitEventAndDataUsage(final Context context) {

        final SharedPreferences preferences = context.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getBoolean("limitEventUsage", false);
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be enabled in production. Returns
     * the types of extended logging that are currently enabled.
     *
     * @return a set containing enabled logging behaviors
     */
    public static final Set<LoggingBehavior> getLoggingBehaviors() {

        synchronized (loggingBehaviors) {
            return Collections.unmodifiableSet(new HashSet<LoggingBehavior>(loggingBehaviors));
        }
    }

    /**
     * Gets the threshold used to report progress on requests.
     */
    public static long getOnProgressThreshold() {

        return onProgressThreshold.get();
    }

    /**
     * Gets whether the SDK is running in Platform Compatibility mode (i.e. making calls to v1.0 endpoints by default)
     * The default is false.
     *
     * @return the value
     */
    public static boolean getPlatformCompatibilityEnabled() {

        return platformCompatibilityEnabled;
    }

    /**
     * Gets the current version of the Facebook SDK for Android as a string.
     *
     * @return the current version of the SDK
     */
    public static String getSdkVersion() {

        return FacebookSdkVersion.BUILD;
    }

    /**
     * Gets whether opening a Session should automatically publish install attribution to the Facebook graph.
     *
     * @return true to automatically publish, false to not This method is deprecated. See
     *         {@link AppEventsLogger#activateApp(Context, String)} for more info.
     */
    @Deprecated
    public static boolean getShouldAutoPublishInstall() {

        return shouldAutoPublishInstall;
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be enabled in production. Checks
     * if a particular extended logging behavior is enabled.
     *
     * @param behavior The LoggingBehavior to check
     * @return whether behavior is enabled
     */
    public static final boolean isLoggingBehaviorEnabled(final LoggingBehavior behavior) {

        // synchronized (loggingBehaviors) {
        // return BuildConfig.DEBUG && loggingBehaviors.contains(behavior);
        // }
        return false;
    }

    /**
     * Loads default values for certain settings from an application's AndroidManifest.xml metadata, if possible. If
     * values have been explicitly set for a particular setting, they will not be overwritten. The following settings
     * are currently loaded from metadata: APPLICATION_ID_PROPERTY, CLIENT_TOKEN_PROPERTY
     *
     * @param context the Context to use for loading metadata
     */
    public static void loadDefaultsFromMetadata(final Context context) {

        defaultsLoaded = true;

        if (context == null) {
            return;
        }

        ApplicationInfo ai = null;
        try {
            ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (final PackageManager.NameNotFoundException e) {
            return;
        }

        if ((ai == null) || (ai.metaData == null)) {
            return;
        }

        if (applicationId == null) {
            applicationId = ai.metaData.getString(APPLICATION_ID_PROPERTY);
        }
        if (appClientToken == null) {
            appClientToken = ai.metaData.getString(CLIENT_TOKEN_PROPERTY);
        }
    }

    /**
     * Manually publish install attribution to the Facebook graph. Internally handles tracking repeat calls to prevent
     * multiple installs being published to the graph.
     *
     * @param context the current Context
     * @param applicationId the fb application being published.
     * @return returns false on error. Applications should retry until true is returned. Safe to call again after true
     *         is returned. This method is deprecated. See {@link AppEventsLogger#activateApp(Context, String)} for more
     *         info.
     */
    @Deprecated
    public static boolean publishInstallAndWait(final Context context, final String applicationId) {

        final Response response = publishInstallAndWaitForResponse(context, applicationId);
        return (response != null) && (response.getError() == null);
    }

    /**
     * Manually publish install attribution to the Facebook graph. Internally handles caching repeat calls to prevent
     * multiple installs being published to the graph.
     *
     * @param context the current Context
     * @param applicationId the fb application being published.
     * @return returns a Response object, carrying the server response, or an error. This method is deprecated. See
     *         {@link AppEventsLogger#activateApp(Context, String)} for more info.
     */
    @Deprecated
    public static Response publishInstallAndWaitForResponse(final Context context, final String applicationId) {

        return publishInstallAndWaitForResponse(context, applicationId, false);
    }

    /**
     * Manually publish install attribution to the Facebook graph. Internally handles tracking repeat calls to prevent
     * multiple installs being published to the graph.
     *
     * @param context the current Context
     * @param applicationId the fb application being published. This method is deprecated. See
     *            {@link AppEventsLogger#activateApp(Context, String)} for more info.
     */
    @Deprecated
    public static void publishInstallAsync(final Context context, final String applicationId) {

        publishInstallAsync(context, applicationId, null);
    }

    /**
     * Manually publish install attribution to the Facebook graph. Internally handles tracking repeat calls to prevent
     * multiple installs being published to the graph.
     *
     * @param context the current Context
     * @param applicationId the fb application being published.
     * @param callback a callback to invoke with a Response object, carrying the server response, or an error. This
     *            method is deprecated. See {@link AppEventsLogger#activateApp(Context, String)} for more info.
     */
    @Deprecated
    public static void publishInstallAsync(final Context context, final String applicationId,
            final Request.Callback callback) {

        // grab the application context ahead of time, since we will return to the caller immediately.
        final Context applicationContext = context.getApplicationContext();
        Settings.getExecutor().execute(new Runnable() {

            @Override
            public void run() {

                final Response response = Settings.publishInstallAndWaitForResponse(applicationContext, applicationId);
                if (callback != null) {
                    // invoke the callback on the main thread.
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            callback.onCompleted(response);
                        }
                    });
                }
            }
        });
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be enabled in production. Disables
     * a particular extended logging behavior in the sdk.
     *
     * @param behavior The LoggingBehavior to disable
     */
    public static final void removeLoggingBehavior(final LoggingBehavior behavior) {

        synchronized (loggingBehaviors) {
            loggingBehaviors.remove(behavior);
        }
    }

    /**
     * Sets the Facebook application ID for the current app.
     *
     * @param applicationId the application ID
     */
    public static void setApplicationId(final String applicationId) {

        Settings.applicationId = applicationId;
    }

    /**
     * Sets the application version to the provided string. AppEventsLogger.logEvent calls logs its event with the
     * current app version, and App Insights allows breakdown of events by app version.
     *
     * @param appVersion The version identifier of the Android app that events are being logged through. Enables
     *            analysis and breakdown of logged events by app version.
     */
    public static void setAppVersion(final String appVersion) {

        Settings.appVersion = appVersion;
    }

    /**
     * Sets the Facebook client token for the current app.
     *
     * @param clientToken the client token
     */
    public static void setClientToken(final String clientToken) {

        appClientToken = clientToken;
    }

    /**
     * Sets the Executor used by the SDK for non-AsyncTask background work.
     *
     * @param executor the Executor to use; must not be null.
     */
    public static void setExecutor(final Executor executor) {

        Validate.notNull(executor, "executor");
        synchronized (LOCK) {
            Settings.executor = executor;
        }
    }

    /**
     * Sets the base Facebook domain to use when making Web requests. This defaults to "facebook.com", but may be
     * overridden to, e.g., "beta.facebook.com" to direct requests at a different domain. This method should never be
     * called from production code.
     *
     * @param facebookDomain the base domain to use instead of "facebook.com"
     */
    public static void setFacebookDomain(final String facebookDomain) {

        // if (!BuildConfig.DEBUG) {
        // Log.w(TAG, "WARNING: Calling setFacebookDomain from non-DEBUG code.");
        // }

        Settings.facebookDomain = facebookDomain;
    }

    /**
     * Sets whether data such as that generated through AppEventsLogger and sent to Facebook should be restricted from
     * being used for purposes other than analytics and conversions, such as for targeting ads to this user. Defaults to
     * false. This value is stored on the device and persists across app launches. Changes to this setting will apply to
     * app events currently queued to be flushed.
     *
     * @param context Used to persist this value across app runs.
     */
    public static void setLimitEventAndDataUsage(final Context context, final boolean limitEventUsage) {

        final SharedPreferences preferences = context.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("limitEventUsage", limitEventUsage);
        editor.commit();
    }

    /**
     * Sets the threshold used to report progress on requests. Note that the value will be read when the request is
     * started and can not be changed during a request (or batch) execution.
     *
     * @param threshold The number of bytes progressed to force a callback.
     */
    public static void setOnProgressThreshold(final long threshold) {

        onProgressThreshold.set(threshold);
    }

    /**
     * Sets whether the SDK is running in Platform Compatibility mode (i.e. making calls to v1.0 endpoints by default)
     * The default is false. This is provided for apps that have strong reason not to take advantage of new capabilities
     * in version 2.0+ of the API.
     *
     * @param platformCompatibilityEnabled whether to set Legacy Graph API mode
     */
    public static void setPlatformCompatibilityEnabled(final boolean platformCompatibilityEnabled) {

        Settings.platformCompatibilityEnabled = platformCompatibilityEnabled;
    }

    /**
     * Sets whether opening a Session should automatically publish install attribution to the Facebook graph.
     *
     * @param shouldAutoPublishInstall true to automatically publish, false to not This method is deprecated. See
     *            {@link AppEventsLogger#activateApp(Context, String)} for more info.
     */
    @Deprecated
    public static void setShouldAutoPublishInstall(final boolean shouldAutoPublishInstall) {

        Settings.shouldAutoPublishInstall = shouldAutoPublishInstall;
    }

    private static Executor getAsyncTaskExecutor() {

        Field executorField = null;
        try {
            executorField = AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
        } catch (final NoSuchFieldException e) {
            return null;
        }

        Object executorObject = null;
        try {
            executorObject = executorField.get(null);
        } catch (final IllegalAccessException e) {
            return null;
        }

        if (executorObject == null) {
            return null;
        }

        if (!(executorObject instanceof Executor)) {
            return null;
        }

        return (Executor) executorObject;
    }

    static void loadDefaultsFromMetadataIfNeeded(final Context context) {

        if (!defaultsLoaded) {
            loadDefaultsFromMetadata(context);
        }
    }

    static Response publishInstallAndWaitForResponse(final Context context, final String applicationId,
            final boolean autoPublish) {

        try {
            if ((context == null) || (applicationId == null)) {
                throw new IllegalArgumentException("Both context and applicationId must be non-null");
            }
            final AttributionIdentifiers identifiers = AttributionIdentifiers.getAttributionIdentifiers(context);
            final SharedPreferences preferences = context.getSharedPreferences(ATTRIBUTION_PREFERENCES,
                    Context.MODE_PRIVATE);
            final String pingKey = applicationId + "ping";
            final String jsonKey = applicationId + "json";
            long lastPing = preferences.getLong(pingKey, 0);
            final String lastResponseJSON = preferences.getString(jsonKey, null);

            // prevent auto publish from occurring if we have an explicit call.
            if (!autoPublish) {
                setShouldAutoPublishInstall(false);
            }

            final GraphObject publishParams = GraphObject.Factory.create();
            publishParams.setProperty(ANALYTICS_EVENT, MOBILE_INSTALL_EVENT);

            Utility.setAppEventAttributionParameters(publishParams, identifiers,
                    Utility.getHashedDeviceAndAppID(context, applicationId), getLimitEventAndDataUsage(context));
            publishParams.setProperty(AUTO_PUBLISH, Boolean.valueOf(autoPublish));
            publishParams.setProperty("application_package_name", context.getPackageName());

            final String publishUrl = String.format(PUBLISH_ACTIVITY_PATH, applicationId);
            final Request publishRequest = Request.newPostRequest(null, publishUrl, publishParams, null);

            if (lastPing != 0) {
                GraphObject graphObject = null;
                try {
                    if (lastResponseJSON != null) {
                        graphObject = GraphObject.Factory.create(new JSONObject(lastResponseJSON));
                    }
                } catch (final JSONException je) {
                    // return the default graph object if there is any problem reading the data.
                }
                if (graphObject == null) {
                    return Response.createResponsesFromString("true", null, new RequestBatch(publishRequest), true)
                            .get(0);
                }
                return new Response(null, null, null, graphObject, true);
            } else if ((identifiers.getAndroidAdvertiserId() == null) && (identifiers.getAttributionId() == null)) {
                throw new FacebookException("No attribution id available to send to server.");
            } else {
                if (!Utility.queryAppSettings(applicationId, false).supportsAttribution()) {
                    throw new FacebookException("Install attribution has been disabled on the server.");
                }

                final Response publishResponse = publishRequest.executeAndWait();

                // denote success since no error threw from the post.
                final SharedPreferences.Editor editor = preferences.edit();
                lastPing = System.currentTimeMillis();
                editor.putLong(pingKey, lastPing);

                // if we got an object response back, cache the string of the JSON.
                if ((publishResponse.getGraphObject() != null)
                        && (publishResponse.getGraphObject().getInnerJSONObject() != null)) {
                    editor.putString(jsonKey, publishResponse.getGraphObject().getInnerJSONObject().toString());
                }
                editor.commit();

                return publishResponse;
            }
        } catch (final Exception e) {
            // if there was an error, fall through to the failure case.
            // Utility.logd("Facebook-publish", e);
            return new Response(null, null, new FacebookRequestError(null, e));
        }
    }
}
