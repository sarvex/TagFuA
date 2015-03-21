/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook.internal;

import com.wTagFufacebook.Request;
import com.wootag.facebook.RequestBatch;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of any of the
 * classes in this package is unsupported, and they may be modified or removed without warning at any time.
 */
public class CacheableRequestBatch extends RequestBatch {

    private String cacheKey;
    private boolean forceRoundTrip;

    public CacheableRequestBatch() {

    }

    public CacheableRequestBatch(final Request... requests) {

        super(requests);
    }

    public final String getCacheKeyOverride() {

        return this.cacheKey;
    }

    public final boolean getForceRoundTrip() {

        return this.forceRoundTrip;
    }

    // If this is set, the provided string will override the default key (the URL) for single requests.
    // There is no default for multi-request batches, so no caching will be done unless the override is
    // specified.
    public final void setCacheKeyOverride(final String cacheKey) {

        this.cacheKey = cacheKey;
    }

    public final void setForceRoundTrip(final boolean forceRoundTrip) {

        this.forceRoundTrip = forceRoundTrip;
    }

}
