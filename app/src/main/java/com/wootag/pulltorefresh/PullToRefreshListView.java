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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;

import com.TagFu.R;
import com.TagFu.pulltorefresh.internal.EmptyViewMethodAccessor;
import com.TagFu.pulltorefresh.internal.LoadingLayout;

public class PullToRefreshListView extends PullToRefreshAdapterViewBase<ListView> {

    protected static final Logger LOG = LoggerManager.getLogger();

    private LoadingLayout mHeaderLoadingView;
    private LoadingLayout mFooterLoadingView;

    protected FrameLayout mLvFooterLoadingFrame;

    private boolean mListViewExtrasEnabled;

    public PullToRefreshListView(final Context context) {

        super(context);
    }

    public PullToRefreshListView(final Context context, final AttributeSet attrs) {

        super(context, attrs);
    }

    public PullToRefreshListView(final Context context, final Mode mode) {

        super(context, mode);
    }

    public PullToRefreshListView(final Context context, final Mode mode, final AnimationStyle style) {

        super(context, mode, style);
    }

    @Override
    public final Orientation getPullToRefreshScrollDirection() {

        return Orientation.VERTICAL;
    }

    protected ListView createListView(final Context context, final AttributeSet attrs) {

        final ListView lv;
        if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
            lv = new InternalListViewSDK9(context, attrs);
        } else {
            lv = new InternalListView(context, attrs);
        }
        return lv;
    }

    @Override
    protected LoadingLayoutProxy createLoadingLayoutProxy(final boolean includeStart, final boolean includeEnd) {

        final LoadingLayoutProxy proxy = super.createLoadingLayoutProxy(includeStart, includeEnd);

        if (this.mListViewExtrasEnabled) {
            final Mode mode = this.getMode();

            if (includeStart && mode.showHeaderLoadingLayout()) {
                proxy.addLayout(this.mHeaderLoadingView);
            }
            if (includeEnd && mode.showFooterLoadingLayout()) {
                proxy.addLayout(this.mFooterLoadingView);
            }
        }

        return proxy;
    }

    @Override
    protected ListView createRefreshableView(final Context context, final AttributeSet attrs) {

        final ListView lv = this.createListView(context, attrs);

        // Set it to this so it can be used in ListActivity/ListFragment
        lv.setId(android.R.id.list);
        return lv;
    }

    @Override
    protected void handleStyledAttributes(final TypedArray a) {

        super.handleStyledAttributes(a);

        this.mListViewExtrasEnabled = a.getBoolean(R.styleable.PullToRefresh_ptrListViewExtrasEnabled, true);

        if (this.mListViewExtrasEnabled) {
            final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL);

            // Create Loading Views ready for use later
            final FrameLayout frame = new FrameLayout(this.getContext());
            this.mHeaderLoadingView = this.createLoadingLayout(this.getContext(), Mode.PULL_FROM_START, a);
            this.mHeaderLoadingView.setVisibility(View.GONE);
            frame.addView(this.mHeaderLoadingView, lp);
            this.mRefreshableView.addHeaderView(frame, null, false);

            this.mLvFooterLoadingFrame = new FrameLayout(this.getContext());
            this.mFooterLoadingView = this.createLoadingLayout(this.getContext(), Mode.PULL_FROM_END, a);
            this.mFooterLoadingView.setVisibility(View.GONE);
            this.mLvFooterLoadingFrame.addView(this.mFooterLoadingView, lp);

            /**
             * If the value for Scrolling While Refreshing hasn't been explicitly set via XML, enable Scrolling While
             * Refreshing.
             */
            if (!a.hasValue(R.styleable.PullToRefresh_ptrScrollingWhileRefreshingEnabled)) {
                this.setScrollingWhileRefreshingEnabled(true);
            }
        }
    }

    @Override
    protected void onRefreshing(final boolean doScroll) {

        /**
         * If we're not showing the Refreshing view, or the list is empty, the the header/footer views won't show so we
         * use the normal method.
         */
        final ListAdapter adapter = this.mRefreshableView.getAdapter();
        if (!this.mListViewExtrasEnabled || !this.getShowViewWhileRefreshing() || (null == adapter)
                || adapter.isEmpty()) {
            super.onRefreshing(doScroll);
            return;
        }

        super.onRefreshing(false);

        final LoadingLayout origLoadingView, listViewLoadingView, oppositeListViewLoadingView;
        final int selection, scrollToY;

        switch (this.getCurrentMode()) {
        case MANUAL_REFRESH_ONLY:
        case PULL_FROM_END:
            origLoadingView = this.getFooterLayout();
            listViewLoadingView = this.mFooterLoadingView;
            oppositeListViewLoadingView = this.mHeaderLoadingView;
            selection = this.mRefreshableView.getCount() - 1;
            scrollToY = this.getScrollY() - this.getFooterSize();
            break;
        case PULL_FROM_START:
        default:
            origLoadingView = this.getHeaderLayout();
            listViewLoadingView = this.mHeaderLoadingView;
            oppositeListViewLoadingView = this.mFooterLoadingView;
            selection = 0;
            scrollToY = this.getScrollY() + this.getHeaderSize();
            break;
        }

        // Hide our original Loading View
        origLoadingView.reset();
        origLoadingView.hideAllViews();

        // Make sure the opposite end is hidden too
        oppositeListViewLoadingView.setVisibility(View.GONE);

        // Show the ListView Loading View and set it to refresh.
        listViewLoadingView.setVisibility(View.VISIBLE);
        listViewLoadingView.refreshing();

        if (doScroll) {
            // We need to disable the automatic visibility changes for now
            this.disableLoadingLayoutVisibilityChanges();

            // We scroll slightly so that the ListView's header/footer is at the
            // same Y position as our normal header/footer
            this.setHeaderScroll(scrollToY);

            // Make sure the ListView is scrolled to show the loading
            // header/footer
            this.mRefreshableView.setSelection(selection);

            // Smooth scroll as normal
            this.smoothScrollTo(0);
        }
    }

    @Override
    protected void onReset() {

        /**
         * If the extras are not enabled, just call up to super and return.
         */
        if (!this.mListViewExtrasEnabled) {
            super.onReset();
            return;
        }

        final LoadingLayout originalLoadingLayout, listViewLoadingLayout;
        final int scrollToHeight, selection;
        final boolean scrollLvToEdge;

        switch (this.getCurrentMode()) {
        case MANUAL_REFRESH_ONLY:
        case PULL_FROM_END:
            originalLoadingLayout = this.getFooterLayout();
            listViewLoadingLayout = this.mFooterLoadingView;
            selection = this.mRefreshableView.getCount() - 1;
            scrollToHeight = this.getFooterSize();
            scrollLvToEdge = Math.abs(this.mRefreshableView.getLastVisiblePosition() - selection) <= 1;
            break;
        case PULL_FROM_START:
        default:
            originalLoadingLayout = this.getHeaderLayout();
            listViewLoadingLayout = this.mHeaderLoadingView;
            scrollToHeight = -this.getHeaderSize();
            selection = 0;
            scrollLvToEdge = Math.abs(this.mRefreshableView.getFirstVisiblePosition() - selection) <= 1;
            break;
        }

        // If the ListView header loading layout is showing, then we need to
        // flip so that the original one is showing instead
        if (listViewLoadingLayout.getVisibility() == View.VISIBLE) {

            // Set our Original View to Visible
            originalLoadingLayout.showInvisibleViews();

            // Hide the ListView Header/Footer
            listViewLoadingLayout.setVisibility(View.GONE);

            /**
             * Scroll so the View is at the same Y as the ListView header/footer, but only scroll if: we've pulled to
             * refresh, it's positioned correctly
             */
            if (scrollLvToEdge && (this.getState() != State.MANUAL_REFRESHING)) {
                this.mRefreshableView.setSelection(selection);
                this.setHeaderScroll(scrollToHeight);
            }
        }

        // Finally, call up to super
        super.onReset();
    }

    protected class InternalListView extends ListView implements EmptyViewMethodAccessor {

        private boolean addedLvFooter;

        public InternalListView(final Context context, final AttributeSet attrs) {

            super(context, attrs);
        }

        @Override
        public boolean dispatchTouchEvent(final MotionEvent event) {

            /**
             * This is a bit hacky, but Samsung's ListView has got a bug in it when using Header/Footer Views and the
             * list is empty. This masks the issue so that it doesn't cause an FC. See Issue #66.
             */
            try {
                return super.dispatchTouchEvent(event);
            } catch (final IndexOutOfBoundsException e) {
                LOG.e(e);
                return false;
            }
        }

        @Override
        public void setAdapter(final ListAdapter adapter) {

            // Add the Footer View at the last possible moment
            if ((null != PullToRefreshListView.this.mLvFooterLoadingFrame) && !this.addedLvFooter) {
                this.addFooterView(PullToRefreshListView.this.mLvFooterLoadingFrame, null, false);
                this.addedLvFooter = true;
            }

            super.setAdapter(adapter);
        }

        @Override
        public void setEmptyView(final View emptyView) {

            PullToRefreshListView.this.setEmptyView(emptyView);
        }

        @Override
        public void setEmptyViewInternal(final View emptyView) {

            super.setEmptyView(emptyView);
        }

        @Override
        protected void dispatchDraw(final Canvas canvas) {

            /**
             * This is a bit hacky, but Samsung's ListView has got a bug in it when using Header/Footer Views and the
             * list is empty. This masks the issue so that it doesn't cause an FC. See Issue #66.
             */
            try {
                super.dispatchDraw(canvas);
            } catch (final IndexOutOfBoundsException exception) {
                LOG.e(exception);

            }
        }

    }

    @TargetApi(9)
    final class InternalListViewSDK9 extends InternalListView {

        public InternalListViewSDK9(final Context context, final AttributeSet attrs) {

            super(context, attrs);
        }

        @Override
        protected boolean overScrollBy(final int deltaX, final int deltaY, final int scrollX, final int scrollY,
                final int scrollRangeX, final int scrollRangeY, final int maxOverScrollX, final int maxOverScrollY,
                final boolean isTouchEvent) {

            final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                    scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

            // Does all of the hard work...
            OverscrollHelper.overScrollBy(PullToRefreshListView.this, deltaX, scrollX, deltaY, scrollY, isTouchEvent);

            return returnValue;
        }
    }

}
