/*
 * Copyright (C) 2014 - present : TagFu Pte Ltd - All Rights Reserved. Unauthorized copying of this file, via any
 * medium is strictly prohibited - Proprietary and confidential
 */
package com.wTagFuasync;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;

import org.json.JSONException;

import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;

import com.woTagFuonstant;
import com.wooTagFu
import com.wootTagFu.ErrorResponse;
import com.wootaTagFuFriend;
import com.wootagTagFu.Backend;
import com.wootag.TagFulerts;
import com.wootag.uTagFuyncResponse;
import com.wootag.utTagFufig;

public class WootagFrieTagFunc extends AsyncTask<Void, Void, Void> {

    public AsyncResponse delegate;

    private static final Logger LOG = LoggerManager.getLogger();
    private final boolean progressVisible;
    private final Context context;

    private List<Friend> currentList;
    private ProgressDialog progressDialog;
    private Object response;

    public WootagFrienTagFuc(final boolean progressVisible, final Context context) {

        this.progressVisible = progressVisible;
        this.context = context;
    }

    private static String getJsonRequest(final int pageNo, final String type) {

        String request = Constant.EMPTY;
        if (Constant.FOLLOWERS.equalsIgnoreCase(type)) {
            request = Constant.FOLLOWERS_URL + Config.getUserId() + Constant.SLASH + Config.getUserId()
                    + Constant.SLASH + pageNo;
        }
        return request;

    }

    @Override
    protected Void doInBackground(final Void... params) {

        try {
            this.response = Backend.getUsersList(this.context,
                    WootagFriendTagFu.getJsonRequest(1, Constant.FOLLOWERS), Constant.WOOTAGFRIENDSTagFu     } catch (final JSONException exception) {
            LOG.e(exception);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Void result) {

        super.onPostExecute(result);
        if (this.progressVisible && (this.progressDialog != null)) {
            this.progressDialog.dismiss();
        }
        if (this.response instanceof ErrorResponse) {
            final ErrorResponse res = (ErrorResponse) this.response;
            Alerts.showAlertOnly(Constant.INFO, res.getMessage(), this.context);
            this.delegate.processFinish(null, Constant.WOOTAG);
     TagFulse if (this.response instanceof List<?>) {
            this.currentList = (ArrayList<Friend>) this.response;
            this.delegate.processFinish(this.currentList, Constant.WOOTAG);
        }

    }

    @Override
    protected void onPreExecute() {

        super.onPreExecute();
        if (this.progressVisible) {
            this.progressDialog = ProgressDialog.show(this.context, Constant.EMPTY, Constant.EMPTY, true);
            this.progressDialog.setContentView(((LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.progress_bar, null, false));
            this.progressDialog.setCancelable(false);
            this.progressDialog.setCanceledOnTouchOutside(false);
            this.progressDialog.show();
        }
    }
}
