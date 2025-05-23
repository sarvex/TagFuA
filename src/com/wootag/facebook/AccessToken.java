/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.wTagFufacebook.internal.NativeProtocol;
import com.woTagFuacebook.internal.Utility;
import com.wootag.facebook.internal.Validate;

/**
 * This class represents an access token returned by the Facebook Login service, along with associated metadata such as
 * its expiration date and permissions. In general, the {@link Session} class will abstract away the need to worry about
 * the details of an access token, but there are situations (such as handling native links, importing
 * previously-obtained access tokens, etc.) where it is useful to deal with access tokens directly. Factory methods are
 * provided to construct access tokens.
 * <p/>
 * For more information on access tokens, see <a
 * href="https://developers.facebook.com/docs/facebook-login/access-tokens/">Access Tokens</a>.
 */
public final class AccessToken implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String EXPIRES_IN_KEY = "expires_in";
    private static final Date MIN_DATE = new Date(Long.MIN_VALUE);
    private static final Date MAX_DATE = new Date(Long.MAX_VALUE);
    private static final Date DEFAULT_EXPIRATION_TIME = MAX_DATE;
    private static final Date DEFAULT_LAST_REFRESH_TIME = new Date();
    private static final AccessTokenSource DEFAULT_ACCESS_TOKEN_SOURCE = AccessTokenSource.FACEBOOK_APPLICATION_WEB;
    private static final Date ALREADY_EXPIRED_EXPIRATION_TIME = MIN_DATE;

    private final Date expires;
    private final List<String> permissions;
    private final List<String> declinedPermissions;
    private final String token;
    private final AccessTokenSource source;
    private final Date lastRefresh;

    AccessToken(final String token, final Date expires, List<String> permissions, List<String> declinedPermissions,
            final AccessTokenSource source, final Date lastRefresh) {

        if (permissions == null) {
            permissions = Collections.emptyList();
        }
        if (declinedPermissions == null) {
            declinedPermissions = Collections.emptyList();
        }

        this.expires = expires;
        this.permissions = Collections.unmodifiableList(permissions);
        this.declinedPermissions = Collections.unmodifiableList(declinedPermissions);
        this.token = token;
        this.source = source;
        this.lastRefresh = lastRefresh;
    }

    /**
     * Creates a new AccessToken using the supplied information from a previously-obtained access token (for instance,
     * from an already-cached access token obtained prior to integration with the Facebook SDK).
     *
     * @param accessToken the access token string obtained from Facebook
     * @param expirationTime the expiration date associated with the token; if null, an infinite expiration time is
     *            assumed (but will become correct when the token is refreshed)
     * @param lastRefreshTime the last time the token was refreshed (or when it was first obtained); if null, the
     *            current time is used.
     * @param accessTokenSource an enum indicating how the token was originally obtained (in most cases, this will be
     *            either AccessTokenSource.FACEBOOK_APPLICATION or AccessTokenSource.WEB_VIEW); if null,
     *            FACEBOOK_APPLICATION is assumed.
     * @param permissions the permissions that were requested when the token was obtained (or when it was last
     *            reauthorized); may be null if permission set is unknown
     * @return a new AccessToken
     */
    public static AccessToken createFromExistingAccessToken(final String accessToken, Date expirationTime,
            Date lastRefreshTime, AccessTokenSource accessTokenSource, final List<String> permissions) {

        if (expirationTime == null) {
            expirationTime = DEFAULT_EXPIRATION_TIME;
        }
        if (lastRefreshTime == null) {
            lastRefreshTime = DEFAULT_LAST_REFRESH_TIME;
        }
        if (accessTokenSource == null) {
            accessTokenSource = DEFAULT_ACCESS_TOKEN_SOURCE;
        }

        return new AccessToken(accessToken, expirationTime, permissions, null, accessTokenSource, lastRefreshTime);
    }

    /**
     * Creates a new AccessToken using the information contained in an Intent populated by the Facebook application in
     * order to launch a native link. For more information on native linking, please see
     * https://developers.facebook.com/docs/mobile/android/deep_linking/.
     *
     * @param intent the Intent that was used to start an Activity; must not be null
     * @return a new AccessToken, or null if the Intent did not contain enough data to create one
     */
    public static AccessToken createFromNativeLinkingIntent(final Intent intent) {

        Validate.notNull(intent, "intent");

        if (intent.getExtras() == null) {
            return null;
        }

        return createFromBundle(null, intent.getExtras(), AccessTokenSource.FACEBOOK_APPLICATION_WEB, new Date());
    }

    private static AccessToken createFromBundle(final List<String> requestedPermissions, final Bundle bundle,
            final AccessTokenSource source, final Date expirationBase) {

        final String token = bundle.getString(ACCESS_TOKEN_KEY);
        final Date expires = getBundleLongAsDate(bundle, EXPIRES_IN_KEY, expirationBase);

        if (Utility.isNullOrEmpty(token) || (expires == null)) {
            return null;
        }

        return new AccessToken(token, expires, requestedPermissions, null, source, new Date());
    }

    private static AccessToken createNew(final List<String> grantedPermissions, final List<String> declinedPermissions,
            final String accessToken, final Date expires, final AccessTokenSource source) {

        if (Utility.isNullOrEmpty(accessToken) || (expires == null)) {
            return createEmptyToken();
        }
        return new AccessToken(accessToken, expires, grantedPermissions, declinedPermissions, source, new Date());
    }

    private static Date getBundleLongAsDate(final Bundle bundle, final String key, final Date dateBase) {

        if (bundle == null) {
            return null;
        }

        long secondsFromBase = Long.MIN_VALUE;

        final Object secondsObject = bundle.get(key);
        if (secondsObject instanceof Long) {
            secondsFromBase = (Long) secondsObject;
        } else if (secondsObject instanceof String) {
            try {
                secondsFromBase = Long.parseLong((String) secondsObject);
            } catch (final NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        if (secondsFromBase == 0) {
            return new Date(Long.MAX_VALUE);
        }
        return new Date(dateBase.getTime() + (secondsFromBase * 1000L));
    }

    static AccessToken createEmptyToken() {

        return new AccessToken("", ALREADY_EXPIRED_EXPIRATION_TIME, null, null, AccessTokenSource.NONE,
                DEFAULT_LAST_REFRESH_TIME);
    }

    static AccessToken createFromCache(final Bundle bundle) {

        final List<String> permissions = getPermissionsFromBundle(bundle, TokenCachingStrategy.PERMISSIONS_KEY);
        final List<String> declinedPermissions = getPermissionsFromBundle(bundle,
                TokenCachingStrategy.DECLINED_PERMISSIONS_KEY);

        return new AccessToken(bundle.getString(TokenCachingStrategy.TOKEN_KEY), TokenCachingStrategy.getDate(bundle,
                TokenCachingStrategy.EXPIRATION_DATE_KEY), permissions, declinedPermissions,
                TokenCachingStrategy.getSource(bundle), TokenCachingStrategy.getDate(bundle,
                        TokenCachingStrategy.LAST_REFRESH_DATE_KEY));
    }

    static AccessToken createFromNativeLogin(final Bundle bundle, final AccessTokenSource source) {

        final Date expires = getBundleLongAsDate(bundle, NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, new Date(0));
        final ArrayList<String> permissions = bundle.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS);
        final String token = bundle.getString(NativeProtocol.EXTRA_ACCESS_TOKEN);

        return createNew(permissions, null, token, expires, source);
    }

    @SuppressLint("FieldGetter")
    static AccessToken createFromRefresh(final AccessToken current, final Bundle bundle) {

        // Only tokens obtained via SSO support refresh. Token refresh returns the expiration date in
        // seconds from the epoch rather than seconds from now.
        assert ((current.source == AccessTokenSource.FACEBOOK_APPLICATION_WEB)
                || (current.source == AccessTokenSource.FACEBOOK_APPLICATION_NATIVE) || (current.source == AccessTokenSource.FACEBOOK_APPLICATION_SERVICE));

        final Date expires = getBundleLongAsDate(bundle, EXPIRES_IN_KEY, new Date(0));
        final String token = bundle.getString(ACCESS_TOKEN_KEY);

        return createNew(current.getPermissions(), current.getDeclinedPermissions(), token, expires, current.source);
    }

    static AccessToken createFromString(final String token, final List<String> permissions,
            final AccessTokenSource source) {

        return new AccessToken(token, DEFAULT_EXPIRATION_TIME, permissions, null, source, DEFAULT_LAST_REFRESH_TIME);
    }

    static AccessToken createFromTokenWithRefreshedPermissions(final AccessToken token,
            final List<String> grantedPermissions, final List<String> declinedPermissions) {

        return new AccessToken(token.token, token.expires, grantedPermissions, declinedPermissions, token.source,
                token.lastRefresh);
    }

    static AccessToken createFromWebBundle(List<String> requestedPermissions, final Bundle bundle,
            final AccessTokenSource source) {

        final Date expires = getBundleLongAsDate(bundle, EXPIRES_IN_KEY, new Date());
        final String token = bundle.getString(ACCESS_TOKEN_KEY);

        // With Login v4, we now get back the actual permissions granted, so update the permissions to be the real thing
        final String grantedPermissions = bundle.getString("granted_scopes");
        if (!Utility.isNullOrEmpty(grantedPermissions)) {
            requestedPermissions = new ArrayList<String>(Arrays.asList(grantedPermissions.split(",")));
        }
        final String deniedPermissions = bundle.getString("denied_scopes");
        List<String> declinedPermissions = null;
        if (!Utility.isNullOrEmpty(deniedPermissions)) {
            declinedPermissions = new ArrayList<String>(Arrays.asList(deniedPermissions.split(",")));
        }

        return createNew(requestedPermissions, declinedPermissions, token, expires, source);
    }

    static List<String> getPermissionsFromBundle(final Bundle bundle, final String key) {

        // Copy the list so we can guarantee immutable
        final List<String> originalPermissions = bundle.getStringArrayList(key);
        List<String> permissions;
        if (originalPermissions == null) {
            permissions = Collections.emptyList();
        } else {
            permissions = Collections.unmodifiableList(new ArrayList<String>(originalPermissions));
        }
        return permissions;
    }

    /**
     * Gets the list of permissions declined by the user with this access token. It represents the entire set of
     * permissions that have been requested and declined. Note that the most up-to-date list of permissions is
     * maintained by the Facebook service, so this list may be outdated if permissions have been granted or declined
     * since the last time an AccessToken object was created.
     *
     * @return a read-only list of strings representing the permissions declined by the user
     */
    public List<String> getDeclinedPermissions() {

        return this.declinedPermissions;
    }

    /**
     * Gets the date at which the access token expires.
     *
     * @return the expiration date of the token
     */
    public Date getExpires() {

        return this.expires;
    }

    /**
     * Gets the date at which the token was last refreshed. Since tokens expire, the Facebook SDK will attempt to renew
     * them periodically.
     *
     * @return the date at which this token was last refreshed
     */
    public Date getLastRefresh() {

        return this.lastRefresh;
    }

    /**
     * Gets the list of permissions associated with this access token. Note that the most up-to-date list of permissions
     * is maintained by the Facebook service, so this list may be outdated if permissions have been added or removed
     * since the time the AccessToken object was created. For more information on permissions, see
     * https://developers.facebook.com/docs/reference/login/#permissions.
     *
     * @return a read-only list of strings representing the permissions granted via this access token
     */
    public List<String> getPermissions() {

        return this.permissions;
    }

    /**
     * Gets the {@link AccessTokenSource} indicating how this access token was obtained.
     *
     * @return the enum indicating how the access token was obtained
     */
    public AccessTokenSource getSource() {

        return this.source;
    }

    /**
     * Gets the string representing the access token.
     *
     * @return the string representing the access token
     */
    public String getToken() {

        return this.token;
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();

        builder.append("{AccessToken");
        builder.append(" token:").append(this.tokenToString());
        this.appendPermissions(builder);
        builder.append("}");

        return builder.toString();
    }

    private void appendPermissions(final StringBuilder builder) {

        builder.append(" permissions:");
        if (this.permissions == null) {
            builder.append("null");
        } else {
            builder.append("[");
            builder.append(TextUtils.join(", ", this.permissions));
            builder.append("]");
        }
    }

    // have a readObject that throws to prevent spoofing
    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {

        throw new InvalidObjectException("Cannot readObject, serialization proxy required");
    }

    private String tokenToString() {

        if (this.token == null) {
            return "null";
            // } else if (Settings.isLoggingBehaviorEnabled(LoggingBehavior.INCLUDE_ACCESS_TOKENS)) {
            // return this.token;
        }
        return "ACCESS_TOKEN_REMOVED";
    }

    private Object writeReplace() {

        return new SerializationProxyV2(this.token, this.expires, this.permissions, this.declinedPermissions,
                this.source, this.lastRefresh);
    }

    boolean isInvalid() {

        return Utility.isNullOrEmpty(this.token) || new Date().after(this.expires);
    }

    Bundle toCacheBundle() {

        final Bundle bundle = new Bundle();

        bundle.putString(TokenCachingStrategy.TOKEN_KEY, this.token);
        TokenCachingStrategy.putDate(bundle, TokenCachingStrategy.EXPIRATION_DATE_KEY, this.expires);
        bundle.putStringArrayList(TokenCachingStrategy.PERMISSIONS_KEY, new ArrayList<String>(this.permissions));
        bundle.putStringArrayList(TokenCachingStrategy.DECLINED_PERMISSIONS_KEY, new ArrayList<String>(
                this.declinedPermissions));
        bundle.putSerializable(TokenCachingStrategy.TOKEN_SOURCE_KEY, this.source);
        TokenCachingStrategy.putDate(bundle, TokenCachingStrategy.LAST_REFRESH_DATE_KEY, this.lastRefresh);

        return bundle;
    }

    private static class SerializationProxyV1 implements Serializable {

        private static final long serialVersionUID = -2488473066578201069L;
        private final Date expires;
        private final List<String> permissions;
        private final String token;
        private final AccessTokenSource source;
        private final Date lastRefresh;

        private SerializationProxyV1(final String token, final Date expires, final List<String> permissions,
                final AccessTokenSource source, final Date lastRefresh) {

            this.expires = expires;
            this.permissions = permissions;
            this.token = token;
            this.source = source;
            this.lastRefresh = lastRefresh;
        }

        private Object readResolve() {

            return new AccessToken(this.token, this.expires, this.permissions, null, this.source, this.lastRefresh);
        }
    }

    private static class SerializationProxyV2 implements Serializable {

        private static final long serialVersionUID = -2488473066578201068L;
        private final Date expires;
        private final List<String> permissions;
        private final List<String> declinedPermissions;
        private final String token;
        private final AccessTokenSource source;
        private final Date lastRefresh;

        SerializationProxyV2(final String token, final Date expires, final List<String> permissions,
                final List<String> declinedPermissions, final AccessTokenSource source, final Date lastRefresh) {

            this.expires = expires;
            this.permissions = permissions;
            this.declinedPermissions = declinedPermissions;
            this.token = token;
            this.source = source;
            this.lastRefresh = lastRefresh;
        }

        private Object readResolve() {

            return new AccessToken(this.token, this.expires, this.permissions, this.declinedPermissions, this.source,
                    this.lastRefresh);
        }
    }
}
