/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook.widget;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.wTagFuR;
import com.woTagFuacebook.AppEventsLogger;
import com.wooTagFucebook.FacebookException;
import com.wootTagFuebook.LoggingBehavior;
import com.wootaTagFubook.Request;
import com.wootagTagFuook.Session;
import com.wootag.TagFuok.internal.AnalyticsEvents;
import com.wootag.fTagFuk.internal.Logger;
import com.wootag.faTagFu.internal.Utility;
import com.wootag.facebook.model.GraphPlace;

public class PlacePickerFragment extends PickerFragment<GraphPlace> {

    /**
     * The key for an int parameter in the fragment's Intent bundle to indicate the radius in meters around the center
     * point to search. The default is 1000 meters.
     */
    public static final String RADIUS_IN_METERS_BUNDLE_KEY = "com.facebook.widget.PlacePickerFragment.RadiusInMeters";
    /**
     * The key for an int parameter in the fragment's Intent bundle to indicate what how many results to return at a
     * time. The default is 100 results.
     */
    public static final String RESULTS_LIMIT_BUNDLE_KEY = "com.facebook.widget.PlacePickerFragment.ResultsLimit";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate what search text should be sent to the
     * service. The default is to have no search text.
     */
    public static final String SEARCH_TEXT_BUNDLE_KEY = "com.facebook.widget.PlacePickerFragment.SearchText";
    /**
     * The key for a Location parameter in the fragment's Intent bundle to indicate what geographical location should be
     * the center of the search.
     */
    public static final String LOCATION_BUNDLE_KEY = "com.facebook.widget.PlacePickerFragment.Location";
    /**
     * The key for a boolean parameter in the fragment's Intent bundle to indicate that the fragment should display a
     * search box and automatically update the search text as it changes.
     */
    public static final String SHOW_SEARCH_BOX_BUNDLE_KEY = "com.facebook.widget.PlacePickerFragment.ShowSearchBox";

    /**
     * The default radius around the center point to search.
     */
    public static final int DEFAULT_RADIUS_IN_METERS = 1000;
    /**
     * The default number of results to retrieve.
     */
    public static final int DEFAULT_RESULTS_LIMIT = 100;

    private static final int searchTextTimerDelayInMilliseconds = 2 * 1000;

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String LOCATION = "location";
    private static final String CATEGORY = "category";
    private static final String WERE_HERE_COUNT = "were_here_count";
    private static final String TAG = "PlacePickerFragment";

    private Location location;
    private int radiusInMeters = DEFAULT_RADIUS_IN_METERS;
    private int resultsLimit = DEFAULT_RESULTS_LIMIT;
    private String searchText;
    private Timer searchTextTimer;
    private boolean hasSearchTextChangedSinceLastQuery;
    private boolean showSearchBox = true;
    private EditText searchBox;

    /**
     * Default constructor. Creates a Fragment with all default properties.
     */
    public PlacePickerFragment() {

        this(null);
    }

    /**
     * Constructor.
     *
     * @param args a Bundle that optionally contains one or more values containing additional configuration information
     *            for the Fragment.
     */
    public PlacePickerFragment(final Bundle args) {

        super(GraphPlace.class, R.layout.com_facebook_placepickerfragment, args);
        this.setPlacePickerSettingsFromBundle(args);
    }

    /**
     * Gets the location to search around. Either the location or the search text (or both) must be specified.
     *
     * @return the Location to search around
     */
    public Location getLocation() {

        return this.location;
    }

    /**
     * Gets the radius in meters around the location to search.
     *
     * @return the radius in meters
     */
    public int getRadiusInMeters() {

        return this.radiusInMeters;
    }

    /**
     * Gets the number of results to retrieve.
     *
     * @return the number of results to retrieve
     */
    public int getResultsLimit() {

        return this.resultsLimit;
    }

    /**
     * Gets the search text (e.g., category, name) to search for. Either the location or the search text (or both) must
     * be specified.
     *
     * @return the search text
     */
    public String getSearchText() {

        return this.searchText;
    }

    /**
     * Gets the currently-selected place.
     *
     * @return the currently-selected place, or null if there is none
     */
    public GraphPlace getSelection() {

        final Collection<GraphPlace> selection = this.getSelectedGraphObjects();
        return ((selection != null) && !selection.isEmpty()) ? selection.iterator().next() : null;
    }

    @Override
    public void onAttach(final Activity activity) {

        super.onAttach(activity);

        if (this.searchBox != null) {
            final InputMethodManager imm = (InputMethodManager) this.getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(this.searchBox, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();

        if (this.searchBox != null) {
            final InputMethodManager imm = (InputMethodManager) this.getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.searchBox.getWindowToken(), 0);
        }
    }

    @Override
    public void onInflate(final Activity activity, final AttributeSet attrs, final Bundle savedInstanceState) {

        super.onInflate(activity, attrs, savedInstanceState);
        final TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.com_facebook_place_picker_fragment);

        this.setRadiusInMeters(a.getInt(R.styleable.com_facebook_place_picker_fragment_radius_in_meters,
                this.radiusInMeters));
        this.setResultsLimit(a.getInt(R.styleable.com_facebook_place_picker_fragment_results_limit, this.resultsLimit));
        if (a.hasValue(R.styleable.com_facebook_place_picker_fragment_results_limit)) {
            this.setSearchText(a.getString(R.styleable.com_facebook_place_picker_fragment_search_text));
        }
        this.showSearchBox = a.getBoolean(R.styleable.com_facebook_place_picker_fragment_show_search_box,
                this.showSearchBox);

        a.recycle();
    }

    /**
     * Sets the search text and reloads the data in the control. This is used to provide search-box functionality where
     * the user may be typing or editing text rapidly. It uses a timer to avoid repeated requerying, preferring to wait
     * until the user pauses typing to refresh the data. Note that this method will NOT update the text in the search
     * box, if any, as it is intended to be called as a result of changes to the search box (and is public to enable
     * applications to provide their own search box UI instead of the default one).
     *
     * @param searchText the search text
     * @param forceReloadEventIfSameText if true, will reload even if the search text has not changed; if false,
     *            identical search text will not force a reload
     */
    public void onSearchBoxTextChanged(String searchText, final boolean forceReloadEventIfSameText) {

        if (!forceReloadEventIfSameText && Utility.stringsEqualOrEmpty(this.searchText, searchText)) {
            return;
        }

        if (TextUtils.isEmpty(searchText)) {
            searchText = null;
        }
        this.searchText = searchText;

        // If search text is being set in response to user input, it is wasteful to send a new request
        // with every keystroke. Send a request the first time the search text is set, then set up a 2-second timer
        // and send whatever changes the user has made since then. (If nothing has changed
        // in 2 seconds, we reset so the next change will cause an immediate re-query.)
        this.hasSearchTextChangedSinceLastQuery = true;
        if (this.searchTextTimer == null) {
            this.searchTextTimer = this.createSearchTextTimer();
        }
    }

    /**
     * Sets the location to search around. Either the location or the search text (or both) must be specified.
     *
     * @param location the Location to search around
     */
    public void setLocation(final Location location) {

        this.location = location;
    }

    /**
     * Sets the radius in meters around the location to search.
     *
     * @param radiusInMeters the radius in meters
     */
    public void setRadiusInMeters(final int radiusInMeters) {

        this.radiusInMeters = radiusInMeters;
    }

    /**
     * Sets the number of results to retrieve.
     *
     * @param resultsLimit the number of results to retrieve
     */
    public void setResultsLimit(final int resultsLimit) {

        this.resultsLimit = resultsLimit;
    }

    /**
     * Sets the search text (e.g., category, name) to search for. Either the location or the search text (or both) must
     * be specified. If a search box is displayed, this will update its contents to the specified text.
     *
     * @param searchText the search text
     */
    public void setSearchText(String searchText) {

        if (TextUtils.isEmpty(searchText)) {
            searchText = null;
        }
        this.searchText = searchText;
        if (this.searchBox != null) {
            this.searchBox.setText(searchText);
        }
    }

    @Override
    public void setSettingsFromBundle(final Bundle inState) {

        super.setSettingsFromBundle(inState);
        this.setPlacePickerSettingsFromBundle(inState);
    }

    private Request createRequest(final Location location, final int radiusInMeters, final int resultsLimit,
            final String searchText, final Set<String> extraFields, final Session session) {

        final Request request = Request.newPlacesSearchRequest(session, location, radiusInMeters, resultsLimit,
                searchText, null);

        final Set<String> fields = new HashSet<String>(extraFields);
        final String[] requiredFields = new String[] { ID, NAME, LOCATION, CATEGORY, WERE_HERE_COUNT };
        fields.addAll(Arrays.asList(requiredFields));

        final String pictureField = this.adapter.getPictureFieldSpecifier();
        if (pictureField != null) {
            fields.add(pictureField);
        }

        final Bundle parameters = request.getParameters();
        parameters.putString("fields", TextUtils.join(",", fields));
        request.setParameters(parameters);

        return request;
    }

    private Timer createSearchTextTimer() {

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                PlacePickerFragment.this.onSearchTextTimerTriggered();
            }
        }, 0, searchTextTimerDelayInMilliseconds);

        return timer;
    }

    private void setPlacePickerSettingsFromBundle(final Bundle inState) {

        // We do this in a separate non-overridable method so it is safe to call from the constructor.
        if (inState != null) {
            this.setRadiusInMeters(inState.getInt(RADIUS_IN_METERS_BUNDLE_KEY, this.radiusInMeters));
            this.setResultsLimit(inState.getInt(RESULTS_LIMIT_BUNDLE_KEY, this.resultsLimit));
            if (inState.containsKey(SEARCH_TEXT_BUNDLE_KEY)) {
                this.setSearchText(inState.getString(SEARCH_TEXT_BUNDLE_KEY));
            }
            if (inState.containsKey(LOCATION_BUNDLE_KEY)) {
                final Location location = inState.getParcelable(LOCATION_BUNDLE_KEY);
                this.setLocation(location);
            }
            this.showSearchBox = inState.getBoolean(SHOW_SEARCH_BOX_BUNDLE_KEY, this.showSearchBox);
        }
    }

    @Override
    PickerFragmentAdapter<GraphPlace> createAdapter() {

        final PickerFragmentAdapter<GraphPlace> adapter = new PickerFragmentAdapter<GraphPlace>(this.getActivity()) {

            @Override
            protected int getDefaultPicture() {

                return R.drawable.com_facebook_place_default_icon;
            }

            @Override
            protected int getGraphObjectRowLayoutId(final GraphPlace graphObject) {

                return R.layout.com_facebook_placepickerfragment_list_row;
            }

            @Override
            protected CharSequence getSubTitleOfGraphObject(final GraphPlace graphObject) {

                final String category = graphObject.getCategory();
                final Integer wereHereCount = (Integer) graphObject.getProperty(WERE_HERE_COUNT);

                String result = null;
                if ((category != null) && (wereHereCount != null)) {
                    result = PlacePickerFragment.this.getString(R.string.com_facebook_placepicker_subtitle_format,
                            category, wereHereCount);
                } else if ((category == null) && (wereHereCount != null)) {
                    result = PlacePickerFragment.this.getString(
                            R.string.com_facebook_placepicker_subtitle_were_here_only_format, wereHereCount);
                } else if ((category != null) && (wereHereCount == null)) {
                    result = PlacePickerFragment.this.getString(
                            R.string.com_facebook_placepicker_subtitle_catetory_only_format, category);
                }
                return result;
            }

        };
        adapter.setShowCheckbox(false);
        adapter.setShowPicture(this.getShowPictures());
        return adapter;
    }

    @Override
    LoadingStrategy createLoadingStrategy() {

        return new AsNeededLoadingStrategy();
    }

    @Override
    SelectionStrategy createSelectionStrategy() {

        return new SingleSelectionStrategy();
    }

    @Override
    String getDefaultTitleText() {

        return this.getString(R.string.com_facebook_nearby);
    }

    @Override
    Request getRequestForLoadData(final Session session) {

        return this.createRequest(this.location, this.radiusInMeters, this.resultsLimit, this.searchText,
                this.extraFields, session);
    }

    @Override
    void logAppEvents(final boolean doneButtonClicked) {

        final AppEventsLogger logger = AppEventsLogger.newLogger(this.getActivity(), this.getSession());
        final Bundle parameters = new Bundle();

        // If Done was clicked, we know this completed successfully. If not, we don't know (caller might have
        // dismissed us in response to selection changing, or user might have hit back button). Either way
        // we'll log the number of selections.
        final String outcome = doneButtonClicked ? AnalyticsEvents.PARAMETER_DIALOG_OUTCOME_VALUE_COMPLETED
                : AnalyticsEvents.PARAMETER_DIALOG_OUTCOME_VALUE_UNKNOWN;
        parameters.putString(AnalyticsEvents.PARAMETER_DIALOG_OUTCOME, outcome);
        parameters.putInt("num_places_picked", (this.getSelection() != null) ? 1 : 0);

        logger.logSdkEvent(AnalyticsEvents.EVENT_PLACE_PICKER_USAGE, null, parameters);
    }

    @Override
    void onLoadingData() {

        this.hasSearchTextChangedSinceLastQuery = false;
    }

    void onSearchTextTimerTriggered() {

        if (this.hasSearchTextChangedSinceLastQuery) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {

                    FacebookException error = null;
                    try {
                        PlacePickerFragment.this.loadData(true);
                    } catch (final FacebookException fe) {
                        error = fe;
                    } catch (final Exception e) {
                        error = new FacebookException(e);
                    } finally {
                        if (error != null) {
                            final OnErrorListener onErrorListener = PlacePickerFragment.this.getOnErrorListener();
                            if (onErrorListener != null) {
                                onErrorListener.onError(PlacePickerFragment.this, error);
                            } else {
                                Logger.log(LoggingBehavior.REQUESTS, TAG, "Error loading data : %s", error);
                            }
                        }
                    }
                }
            });
        } else {
            // Nothing has changed in 2 seconds. Invalidate and forget about this timer.
            // Next time the user types, we will fire a query immediately again.
            this.searchTextTimer.cancel();
            this.searchTextTimer = null;
        }
    }

    @Override
    void saveSettingsToBundle(final Bundle outState) {

        super.saveSettingsToBundle(outState);

        outState.putInt(RADIUS_IN_METERS_BUNDLE_KEY, this.radiusInMeters);
        outState.putInt(RESULTS_LIMIT_BUNDLE_KEY, this.resultsLimit);
        outState.putString(SEARCH_TEXT_BUNDLE_KEY, this.searchText);
        outState.putParcelable(LOCATION_BUNDLE_KEY, this.location);
        outState.putBoolean(SHOW_SEARCH_BOX_BUNDLE_KEY, this.showSearchBox);
    }

    @Override
    void setupViews(final ViewGroup view) {

        if (this.showSearchBox) {
            final ListView listView = (ListView) view.findViewById(R.id.com_facebook_picker_list_view);

            final View searchHeaderView = this.getActivity().getLayoutInflater()
                    .inflate(R.layout.com_facebook_picker_search_box, listView, false);

            listView.addHeaderView(searchHeaderView, null, false);

            this.searchBox = (EditText) view.findViewById(R.id.com_facebook_picker_search_text);

            this.searchBox.addTextChangedListener(new SearchTextWatcher());
            if (!TextUtils.isEmpty(this.searchText)) {
                this.searchBox.setText(this.searchText);
            }
        }
    }

    private class AsNeededLoadingStrategy extends LoadingStrategy {

        @Override
        public void attach(final GraphObjectAdapter<GraphPlace> adapter) {

            super.attach(adapter);

            this.adapter.setDataNeededListener(new GraphObjectAdapter.DataNeededListener() {

                @Override
                public void onDataNeeded() {

                    // Do nothing if we are currently loading data . We will get notified again when that load finishes
                    // if the adapter still
                    // needs more data. Otherwise, follow the next link.
                    if (!AsNeededLoadingStrategy.this.loader.isLoading()) {
                        AsNeededLoadingStrategy.this.loader.followNextLink();
                    }
                }
            });
        }

        @Override
        protected void onLoadFinished(final GraphObjectPagingLoader<GraphPlace> loader,
                final SimpleGraphObjectCursor<GraphPlace> data) {

            super.onLoadFinished(loader, data);

            // We could be called in this state if we are clearing data or if we are being re-attached
            // in the middle of a query.
            if ((data == null) || loader.isLoading()) {
                return;
            }

            PlacePickerFragment.this.hideActivityCircle();

            if (data.isFromCache()) {
                // Only the first page can be cached, since all subsequent pages will be round-tripped. Force
                // a refresh of the first page before we allow paging to begin. If the first page produced
                // no data, launch the refresh immediately, otherwise schedule it for later.
                loader.refreshOriginalRequest(data.areMoreObjectsAvailable() ? CACHED_RESULT_REFRESH_DELAY : 0);
            }
        }
    }

    private class SearchTextWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(final Editable s) {

        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

            PlacePickerFragment.this.onSearchBoxTextChanged(s.toString(), false);
        }
    }
}
