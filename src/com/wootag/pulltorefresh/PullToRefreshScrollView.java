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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

import com.wootag.R;

public class PullToRefreshScrollView extends PullToRefreshBase<ScrollView> {

    public PullToRefreshScrollView(final Context context) {

        super(context);
    }

    public PullToRefreshScrollView(final Context context, final AttributeSet attrs) {

        super(context, attrs);
    }

    public PullToRefreshScrollView(final Context context, final Mode mode) {

        super(context, mode);
    }

    public PullToRefreshScrollView(final Context context, final Mode mode, final AnimationStyle style) {

        super(context, mode, style);
    }

    @Override
    public final Orientation getPullToRefreshScrollDirection() {

        return Orientation.VERTICAL;
    }

    @Override
    protected ScrollView createRefreshableView(final Context context, final AttributeSet attrs) {

        ScrollView scrollView;
        if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
            scrollView = new InternalScrollViewSDK9(context, attrs);
        } else {
            scrollView = new ScrollView(context, attrs);
        }

        scrollView.setId(R.id.scrollview);
        return scrollView;
    }

    @Override
    protected boolean isReadyForPullEnd() {

        final View scrollViewChild = this.mRefreshableView.getChildAt(0);
        if (null != scrollViewChild) {
            return this.mRefreshableView.getScrollY() >= (scrollViewChild.getHeight() - this.getHeight());
        }
        return false;
    }

    @Override
    protected boolean isReadyForPullStart() {

        return this.mRefreshableView.getScrollY() == 0;
    }

    @TargetApi(9)
    final class InternalScrollViewSDK9 extends ScrollView {

        public InternalScrollViewSDK9(final Context context, final AttributeSet attrs) {

            super(context, attrs);
        }

        /**
         * Taken from the AOSP ScrollView source
         */
        private int getScrollRange() {

            int scrollRange = 0;
            if (this.getChildCount() > 0) {
                final View child = this.getChildAt(0);
                scrollRange = Math.max(0,
                        child.getHeight() - (this.getHeight() - this.getPaddingBottom() - this.getPaddingTop()));
            }
            return scrollRange;
        }

        @Override
        protected boolean overScrollBy(final int deltaX, final int deltaY, final int scrollX, final int scrollY,
                final int scrollRangeX, final int scrollRangeY, final int maxOverScrollX, final int maxOverScrollY,
                final boolean isTouchEvent) {

            final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                    scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

            // Does all of the hard work...
            OverscrollHelper.overScrollBy(PullToRefreshScrollView.this, deltaX, scrollX, deltaY, scrollY,
                    this.getScrollRange(), isTouchEvent);

            return returnValue;
        }
    }
}
