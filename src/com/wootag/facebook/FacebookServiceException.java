/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook;

/**
 * Represents an error returned from the Facebook service in response to a request.
 */
public class FacebookServiceException extends FacebookException {

    private final FacebookRequestError error;

    private static final long serialVersionUID = 1;

    /**
     * Constructs a new FacebookServiceException.
     *
     * @param error the error from the request
     */
    public FacebookServiceException(final FacebookRequestError error, final String errorMessage) {

        super(errorMessage);
        this.error = error;
    }

    /**
     * Returns an object that encapsulates complete information representing the error returned by Facebook.
     *
     * @return complete information representing the error.
     */
    public final FacebookRequestError getRequestError() {

        return this.error;
    }

    @Override
    public final String toString() {

        return new StringBuilder().append("{FacebookServiceException: ").append("httpResponseCode: ")
                .append(this.error.getRequestStatusCode()).append(", facebookErrorCode: ")
                .append(this.error.getErrorCode()).append(", facebookErrorType: ").append(this.error.getErrorType())
                .append(", message: ").append(this.error.getErrorMessage()).append("}").toString();
    }

}
