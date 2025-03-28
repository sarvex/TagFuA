/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes. Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 *******************************************************************************/
package com.TagFu.pulltorefresh;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.TagFu.R;

public class PullToRefreshWebView extends PullToRefreshBase<WebView> {

    private static final OnRefreshListener<WebView> defaultOnRefreshListener = new OnRefreshListener<WebView>() {

        @Override
        public void onRefresh(final PullToRefreshBase<WebView> refreshView) {

            refreshView.getRefreshableView().reload();
        }

    };

    private final WebChromeClient defaultWebChromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(final WebView view, final int newProgress) {

            if (newProgress == 100) {
                PullToRefreshWebView.this.onRefreshComplete();
            }
        }

    };

    public PullToRefreshWebView(final Context context) {

        super(context);

        /**
         * Added so that by default, Pull-to-Refresh refreshes the page
         */
        this.setOnRefreshListener(defaultOnRefreshListener);
        this.mRefreshableView.setWebChromeClient(this.defaultWebChromeClient);
    }

    public PullToRefreshWebView(final Context context, final AttributeSet attrs) {

        super(context, attrs);

        /**
         * Added so that by default, Pull-to-Refresh refreshes the page
         */
        this.setOnRefreshListener(defaultOnRefreshListener);
        this.mRefreshableView.setWebChromeClient(this.defaultWebChromeClient);
    }

    public PullToRefreshWebView(final Context context, final Mode mode) {

        super(context, mode);

        /**
         * Added so that by default, Pull-to-Refresh refreshes the page
         */
        this.setOnRefreshListener(defaultOnRefreshListener);
        this.mRefreshableView.setWebChromeClient(this.defaultWebChromeClient);
    }

    public PullToRefreshWebView(final Context context, final Mode mode, final AnimationStyle style) {

        super(context, mode, style);

        /**
         * Added so that by default, Pull-to-Refresh refreshes the page
         */
        this.setOnRefreshListener(defaultOnRefreshListener);
        this.mRefreshableView.setWebChromeClient(this.defaultWebChromeClient);
    }

    @Override
    public final Orientation getPullToRefreshScrollDirection() {

        return Orientation.VERTICAL;
    }

    @Override
    protected WebView createRefreshableView(final Context context, final AttributeSet attrs) {

        WebView webView;
        if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
            webView = new InternalWebViewSDK9(context, attrs);
        } else {
            webView = new WebView(context, attrs);
        }

        webView.setId(R.id.webview);
        return webView;
    }

    @Override
    protected boolean isReadyForPullEnd() {

        final float exactContentHeight = FloatMath.floor(this.mRefreshableView.getContentHeight()
                * this.mRefreshableView.getScale());
        return this.mRefreshableView.getScrollY() >= (exactContentHeight - this.mRefreshableView.getHeight());
    }

    @Override
    protected boolean isReadyForPullStart() {

        return this.mRefreshableView.getScrollY() == 0;
    }

    @Override
    protected void onPtrRestoreInstanceState(final Bundle savedInstanceState) {

        super.onPtrRestoreInstanceState(savedInstanceState);
        this.mRefreshableView.restoreState(savedInstanceState);
    }

    @Override
    protected void onPtrSaveInstanceState(final Bundle saveState) {

        super.onPtrSaveInstanceState(saveState);
        this.mRefreshableView.saveState(saveState);
    }

    @TargetApi(9)
    final class InternalWebViewSDK9 extends WebView {

        // WebView doesn't always scroll back to it's edge so we add some
        // fuzziness
        static final int OVERSCROLL_FUZZY_THRESHOLD = 2;

        // WebView seems quite reluctant to overscroll so we use the scale
        // factor to scale it's value
        static final float OVERSCROLL_SCALE_FACTOR = 1.5f;

        public InternalWebViewSDK9(final Context context, final AttributeSet attrs) {

            super(context, attrs);
        }

        private int getScrollRange() {

            return (int) Math.max(
                    0,
                    FloatMath.floor(PullToRefreshWebView.this.mRefreshableView.getContentHeight()
                            * PullToRefreshWebView.this.mRefreshableView.getScale())
                            - (this.getHeight() - this.getPaddingBottom() - this.getPaddingTop()));
        }

        @Override
        protected boolean overScrollBy(final int deltaX, final int deltaY, final int scrollX, final int scrollY,
                final int scrollRangeX, final int scrollRangeY, final int maxOverScrollX, final int maxOverScrollY,
                final boolean isTouchEvent) {

            final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                    scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

            // Does all of the hard work...
            OverscrollHelper.overScrollBy(PullToRefreshWebView.this, deltaX, scrollX, deltaY, scrollY,
                    this.getScrollRange(), OVERSCROLL_FUZZY_THRESHOLD, OVERSCROLL_SCALE_FACTOR, isTouchEvent);

            return returnValue;
        }
    }
}
