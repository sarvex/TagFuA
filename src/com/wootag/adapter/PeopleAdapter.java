/*
 * Copyright (C) 2014 - present : TagFu Pte Ltd - All Rights Reserved. Unauthorized copying of this file, via any
 * medium is strictly prohibited - Proprietary and confidential
 */
package com.wTagFuadapter;

import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;

import com.woTagFuonstant;
import com.wooTagFu
import com.wootTagFunc.FollowAsyncTask;
import com.wootaTagFuPeople;
import com.wootagTagFuents.BaseFragment;
import com.wootag.TagFunts.OtherUserFragment;
import com.wootag.uTagFue;
import com.wootag.utTagFurts;
import com.wootag.utiTagFuig;
import com.wootag.utilTagFuwInterface;
import com.wootag.util.MainManager;

public class PeopleAdapter extends ArrayAdapter<People> implements OnClickListener, FollowInterface {

    private static final String BROWSE = "browse";

    private static final String NO_ID_FOR_THIS_USER = "No Id for this user";

    private static final String EMPTY = "";

    private static final String UNFOLLOWED_SUCCESSFULLY = "Unfollowed successfully.";

    private static final String FOLLOWED_SUCCESSFULLY = "Followed successfully.";

    private static final String NO = "no";

    private static final String UNFOLLOW = "unfollow";

    private static final String YES = "yes";

    private static final String FOLLOW = "follow";

    private static final Logger LOG = LoggerManager.getLogger();

    private static final String USERID = "userid";

    private final Context context;
    private final Fragment currentFragment;
    private ImageView currentImageview;
    private int id;
    private final List<People> people;
    private People peopleDetails;
    private final String screenType;

    public PeopleAdapter(final Context context, final int textViewResourceId, final List<People> peopleList,
            final String screenType, final Fragment currentFragment) {

        super(context, textViewResourceId, peopleList);
        this.people = peopleList;
        this.context = context;
        this.screenType = screenType;
        this.currentFragment = currentFragment;
    }

    @Override
    public void follow(final String type) {

        if (FOLLOW.equalsIgnoreCase(type)) {
            this.currentImageview.setImageResource(R.drawable.add1);
            this.peopleDetails.setIsFollow(YES);
            this.notifyDataSetChanged();
            Alerts.showInfoOnly(FOLLOWED_SUCCESSFULLY, this.context);
        }
        if (UNFOLLOW.equalsIgnoreCase(type)) {
            this.currentImageview.setImageResource(R.drawable.unfollow);
            this.peopleDetails.setIsFollow(NO);
            this.notifyDataSetChanged();
            Alerts.showInfoOnly(UNFOLLOWED_SUCCESSFULLY, this.context);
        }

    }

    @Override
    public int getCount() {

        return this.people.size();
    }

    @Override
    public People getItem(final int position) {

        return this.people.get(position);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        ViewHolder holder;
        final People peopleDetails = this.getItem(position);

        if (convertView == null) {
            convertView = ((LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.suggested_friends, parent, false);
            holder = this.initHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (!Strings.isNullOrEmpty(peopleDetails.getUrl())) {
            Image.displayImage(peopleDetails.getUrl(), (Activity) this.context, holder.profileImageView, 0);
        } else {
            holder.profileImageView.setImageResource(R.drawable.member);
        }

        if (peopleDetails.getUserName() != null) {
            holder.nameTextView.setText(peopleDetails.getUserName());
        } else {
            holder.nameTextView.setText(EMPTY);
        }
        holder.profileDetailsTextView.setText(this.getDescription(peopleDetails));

        if (NO.equalsIgnoreCase(peopleDetails.getIsFollow())) {
            holder.addImageView.setImageResource(R.drawable.add1);
        } else {
            holder.addImageView.setImageResource(R.drawable.unfollow);
        }

        if (peopleDetails.getId().equalsIgnoreCase(MainManager.getInstance().getUserId())) {
            holder.addImageView.setVisibility(View.GONE);
        } else {
            holder.addImageView.setVisibility(View.VISIBLE);
        }

        holder.nameTextView.setOnClickListener(this);
        holder.nameTextView.setTag(peopleDetails);
        holder.addImageView.setOnClickListener(this);
        holder.addImageView.setTag(peopleDetails);

        holder.nameTextView.setOnClickListener(this);
        holder.nameTextView.setTag(peopleDetails);
        holder.userImage.setOnClickListener(this);
        holder.userImage.setTag(peopleDetails);
        return convertView;
    }

    @Override
    public void onClick(final View view) {

        switch (view.getId()) {
        case R.id.addImageView:
            this.onAddImageClick(view);
            break;
        case R.id.profileImageRL:
            this.onProfileImageClick(view);
            break;
        case R.id.nameTextView:
            this.onProfileImageClick(view);
            break;
        default:
            break;
        }

    }

    private String getDescription(final People peopleDetails) {

        String description = EMPTY;

        if (peopleDetails.getPosition() != null) {
            description += peopleDetails.getPosition() + " | ";
        }
        if (peopleDetails.getCountry() != null) {
            description += peopleDetails.getCountry() + " | ";
        }
        if (peopleDetails.getEmailId() != null) {
            description += peopleDetails.getEmailId();
        }
        return description;
    }

    private void gotToOtherPage(final int id) {

        final OtherUserFragment fragment = new OtherUserFragment(); // object of next fragment
        final Bundle bundle = new Bundle();

        if (Constant.MY_PAGE.equalsIgnoreCase(this.screenType)) {
            bundle.putString(Constant.ROOT_FRAGMENT, Constant.MY_PAGE);
            bundle.putString(USERID, String.valueOf(id));
            fragment.setArguments(bundle);
            BaseFragment.tabActivity.pushFragments(R.id.mypageTab, fragment, Constant.OTHERS_PAGE,
                    this.currentFragment, Constant.MYPAGE);

        } else if (Constant.MY_PAGE_MORE_FEEDS.equalsIgnoreCase(this.screenType)) {
            bundle.putString(Constant.ROOT_FRAGMENT, Constant.MY_PAGE);
            bundle.putString(USERID, String.valueOf(id));
            fragment.setArguments(bundle);
            BaseFragment.tabActivity.pushFragments(R.id.mypageTab, fragment, Constant.OTHERS_PAGE,
                    this.currentFragment, Constant.MYPAGE);

        } else if (Constant.MORE_VIDEOS.equalsIgnoreCase(this.screenType)) {
            bundle.putString(Constant.ROOT_FRAGMENT, Constant.MY_PAGE);
            bundle.putString(USERID, String.valueOf(id));
            fragment.setArguments(bundle);
            BaseFragment.tabActivity.pushFragments(R.id.mypageTab, fragment, Constant.OTHERS_PAGE,
                    this.currentFragment, Constant.MYPAGE);

        } else if (Constant.VIDEO_FEEDS.equalsIgnoreCase(this.screenType)) {
            bundle.putString(Constant.ROOT_FRAGMENT, Constant.VIDEO_FEEDS);
            bundle.putString(USERID, String.valueOf(id));
            fragment.setArguments(bundle);
            BaseFragment.tabActivity.pushFragments(R.id.feedTab, fragment, Constant.OTHERS_PAGE, this.currentFragment,
                    Constant.HOME);

        } else if (Constant.NOTIFICATIONS_PAGE.equalsIgnoreCase(this.screenType)) {
            bundle.putString(Constant.ROOT_FRAGMENT, Constant.NOTIFICATIONS_PAGE);
            bundle.putString(USERID, String.valueOf(id));
            fragment.setArguments(bundle);
            BaseFragment.tabActivity.pushFragments(R.id.notificationTab, fragment, Constant.OTHERS_PAGE,
                    this.currentFragment, Constant.NOTIFICATIONS);

        } else if (Constant.BROWSE_PAGE.equalsIgnoreCase(this.screenType) || (BROWSE.equalsIgnoreCase(this.screenType))) {
            bundle.putString(Constant.ROOT_FRAGMENT, Constant.BROWSE_PAGE);
            bundle.putString(USERID, String.valueOf(id));
            fragment.setArguments(bundle);
            BaseFragment.tabActivity.pushFragments(R.id.browsTab, fragment, Constant.OTHERS_PAGE, this.currentFragment,
                    Constant.BROWSE);
        }
    }

    private ViewHolder initHolder(final View convertView) {

        final ViewHolder holder = new ViewHolder();
        holder.profileImageView = (ImageView) convertView.findViewById(R.id.profileImageView);
        holder.addImageView = (ImageView) convertView.findViewById(R.id.addImageView);
        holder.nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        holder.profileDetailsTextView = (TextView) convertView.findViewById(R.id.profileDetailsTextView);
        holder.userImage = (RelativeLayout) convertView.findViewById(R.id.profileImageRL);
        holder.details = (LinearLayout) convertView.findViewById(R.id.browseuserDetails);
        return holder;
    }

    /**
     * @param view
     * @throws NumberFormatException
     */
    private void onAddImageClick(final View view) throws NumberFormatException {

        this.peopleDetails = (People) view.getTag();
        final ImageView imageView = (ImageView) view;
        this.currentImageview = imageView;
        final String userId = this.peopleDetails.getId();
        if (userId != null) {
            this.id = Integer.parseInt(userId);
        }
        if (this.id > 0) {
            if (NO.equalsIgnoreCase(this.peopleDetails.getIsFollow())) {
                final FollowAsyncTask task = new FollowAsyncTask(String.valueOf(this.id), Config.getUserId(), FOLLOW,
                        this.context);
                task.delegate = PeopleAdapter.this;
                task.execute();
            } else {
                final FollowAsyncTask task = new FollowAsyncTask(String.valueOf(this.id), Config.getUserId(), UNFOLLOW,
                        this.context);
                task.delegate = PeopleAdapter.this;
                task.execute();
            }
        } else {
            Alerts.showInfoOnly(NO_ID_FOR_THIS_USER, this.context);
        }
    }

    /**
     * @param view
     * @throws NumberFormatException
     */
    private void onProfileImageClick(final View view) throws NumberFormatException {

        this.peopleDetails = (People) view.getTag();

        final String Id = this.peopleDetails.getId();
        if (!Config.getUserId().equalsIgnoreCase(Id)) {
            if (Id != null) {
                this.id = Integer.parseInt(Id);
            }
            if (this.id > 0) {
                this.gotToOtherPage(this.id);

            } else {
                Alerts.showInfoOnly(NO_ID_FOR_THIS_USER, this.context);
            }
        }
    }

    public class ViewHolder {

        public ImageView addImageView;
        public LinearLayout details;
        public TextView nameTextView;
        public TextView profileDetailsTextView;
        public ImageView profileImageView;
        public RelativeLayout userImage;

    }

}
