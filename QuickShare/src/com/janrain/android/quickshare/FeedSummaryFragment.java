/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 Copyright (c) 2011, Janrain, Inc.

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
   other materials provided with the distribution.
 * Neither the name of the Janrain, Inc. nor the names of its
   contributors may be used to endorse or promote products derived from this
   software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 File:   FeedSummaryActivity.java
 Author: Lilli Szafranski - lilli@janrain.com
 Date:   April 22, 2011
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package com.janrain.android.quickshare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Config;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.android.imageloader.ImageLoader;
import com.janrain.android.engage.ui.JRPublishFragment;

import java.util.ArrayList;

public class FeedSummaryFragment extends ListFragment implements FeedData.FeedReaderListener {
    private static final String TAG = FeedSummaryFragment.class.getSimpleName();

    private ArrayList<Story> mStories;
    private StoryAdapter mAdapter;
    private FeedData mFeedData;
    private boolean mDualPane;
    private int mCurCheckPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedData = FeedData.getInstance();

        mStories = new ArrayList<Story>();
        getUpdatedStoriesList();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.details);
        mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            // Make sure our UI is in the correct state.
            showDetails(mCurCheckPosition);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View retval = inflater.inflate(R.layout.feed_summary_listview, container, false);

        mAdapter = new StoryAdapter(getActivity(), R.layout.feed_summary_listview_row, mStories);
        setListAdapter(mAdapter);

        return retval;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        if (pos == 0) {
            ((TextView) v.findViewById(R.id.row_story_title)).setText("Loading new articles...");
            mFeedData.asyncLoadJanrainBlog(this);
        } else {
            showDetails(pos);
        }
    }

    private void showDetails(int index) {
        mCurCheckPosition = index;

        if (mDualPane) {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            getListView().setItemChecked(index, true);

            // Check what fragment is currently shown, pop if needed, then replace
            Fragment f = getFragmentManager().findFragmentById(R.id.details);
            if (f instanceof JRPublishFragment) getFragmentManager().popBackStackImmediate();
            f = getFragmentManager().findFragmentById(R.id.details);

            StoryDetailFragment details = (StoryDetailFragment) f;
            if (details == null || details.getShownIndex() != index) {
                // Make new fragment to show this selection.
                details = StoryDetailFragment.newInstance(index);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.replace(R.id.details, details);
                ft.commit();
            }
        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(getActivity(), StoryDetailActivity.class);
            intent.putExtra("index", index);
            startActivity(intent);
        }
    }

    public void asyncFeedReadSucceeded() {
        if (Config.LOGD)
            Log.d(TAG, "[asyncFeedReadSucceeded]");


        getUpdatedStoriesList();
        mAdapter.notifyDataSetChanged();
    }

    public void asyncFeedReadFailed() {
        if (Config.LOGD)
            Log.d(TAG, "[asyncFeedReadFailed]");

        getUpdatedStoriesList();
        mAdapter.notifyDataSetChanged();
    }

    private void getUpdatedStoriesList() {
        mStories.clear();
        mStories.addAll(mFeedData.getFeed());

        if (Config.LOGD) Log.d(TAG, "[getUpdatedStoriesList] " + mStories.size() + " stories remain");

        mStories.add(0, Story.dummyStory());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.story_managing_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Config.LOGD)
            Log.d(TAG, "[onOptionsItemSelected] here");

        switch (item.getItemId()) {
            case R.id.delete_all_stories:
                if (Config.LOGD) Log.d(TAG, "[onOptionsItemSelected] delete all stories option selected");

                mFeedData.deleteAllStories();
                getUpdatedStoriesList();
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class StoryAdapter extends ArrayAdapter<Story> {
        private int mResourceId;
        private Context mContext;

        public StoryAdapter(Context context, int resId, ArrayList<Story> items) {
            super(context, -1, items);

            mResourceId = resId;
            mContext = context;
        }

        private View getInflatedView() {
            Log.i(TAG, "[getView] with null or dummy convertView");
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return li.inflate(mResourceId, null);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = getInflatedView();
            } else if (v.getTag().equals("DUMMY_ROW")) {
                v = getInflatedView();
            }

            TextView title = (TextView) v.findViewById(R.id.row_story_title);
            ImageView icon = (ImageView) v.findViewById(R.id.row_story_icon);
            TextView text = (TextView) v.findViewById(R.id.row_story_text);
            TextView date = (TextView) v.findViewById(R.id.row_story_date);

            Story story = getItem(position);

            /* This is the row that contains dummy (empty) story, really to be used as a "Refresh"
                button that is clickable/focusable like other listview rows, and looks nicer than
                a real button. */
            if (position == 0) {
                v.setTag("DUMMY_ROW");

                icon.setVisibility(View.GONE);
                text.setVisibility(View.GONE);
                date.setVisibility(View.GONE);

                title.setText("Refresh");
                title.setGravity(Gravity.CENTER_HORIZONTAL);
            } else {
                if (Config.LOGD) Log.d(TAG, "[getView] for row " + position + ": " + story.getTitle());

                v.setTag("STORY_ROW");

                title.setGravity(Gravity.LEFT);

                if (story.getImageUrls().isEmpty()) {
                    icon.setVisibility(View.GONE);
                } else {
                    icon.setVisibility(View.VISIBLE);
                    ImageLoader il = mFeedData.getImageLoader();
                    ImageLoader.BindResult br = il.bind(this, icon, story.getThumbnailUrl());
                    if (Config.LOGD) Log.d(TAG, "bind result: " + br);
                }

                title.setText(story.getTitle());
                text.setText(story.getPlainText());
                date.setText(story.getDate());
            }

            return v;
        }
    }
}