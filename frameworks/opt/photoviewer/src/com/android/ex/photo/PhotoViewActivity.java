/*
 * Copyright (C) 2011 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ex.photo;

import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.android.ex.photo.PhotoViewPager.InterceptType;
import com.android.ex.photo.PhotoViewPager.OnInterceptTouchListener;
import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.fragments.PhotoViewFragment;
import com.android.ex.photo.loaders.PhotoPagerLoader;
import com.android.ex.photo.provider.PhotoContract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Activity to view the contents of an album.
 */
public class PhotoViewActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, OnPageChangeListener, OnInterceptTouchListener,
        OnMenuVisibilityListener, PhotoViewCallbacks {

    private final static String STATE_ITEM_KEY =
            "com.google.android.apps.plus.PhotoViewFragment.ITEM";
    private final static String STATE_FULLSCREEN_KEY =
            "com.google.android.apps.plus.PhotoViewFragment.FULLSCREEN";
    private final static String STATE_ACTIONBARTITLE_KEY =
            "com.google.android.apps.plus.PhotoViewFragment.ACTIONBARTITLE";
    private final static String STATE_ACTIONBARSUBTITLE_KEY =
            "com.google.android.apps.plus.PhotoViewFragment.ACTIONBARSUBTITLE";

    private static final int LOADER_PHOTO_LIST = 1;

    /** Count used when the real photo count is unknown [but, may be determined] */
    public static final int ALBUM_COUNT_UNKNOWN = -1;

    /** Argument key for the dialog message */
    public static final String KEY_MESSAGE = "dialog_message";

    public static int sMemoryClass;

    /** The URI of the photos we're viewing; may be {@code null} */
    private String mPhotosUri;
    /** The URI of the initial photo to display */
    private String mInitialPhotoUri;
    /** The index of the currently viewed photo */
    private int mPhotoIndex;
    /** The query projection to use; may be {@code null} */
    private String[] mProjection;
    /** The total number of photos; only valid if {@link #mIsEmpty} is {@code false}. */
    private int mAlbumCount = ALBUM_COUNT_UNKNOWN;
    /** {@code true} if the view is empty. Otherwise, {@code false}. */
    private boolean mIsEmpty;
    /** the main root view */
    protected View mRootView;
    /** The main pager; provides left/right swipe between photos */
    protected PhotoViewPager mViewPager;
    /** Adapter to create pager views */
    protected PhotoPagerAdapter mAdapter;
    /** Whether or not we're in "full screen" mode */
    private boolean mFullScreen;
    /** The listeners wanting full screen state for each screen position */
    private Map<Integer, OnScreenListener>
            mScreenListeners = new HashMap<Integer, OnScreenListener>();
    /** The set of listeners wanting full screen state */
    private Set<CursorChangedListener> mCursorListeners = new HashSet<CursorChangedListener>();
    /** When {@code true}, restart the loader when the activity becomes active */
    private boolean mRestartLoader;
    /** Whether or not this activity is paused */
    private boolean mIsPaused = true;
    /** The maximum scale factor applied to images when they are initially displayed */
    private float mMaxInitialScale;
    /** The title in the actionbar */
    private String mActionBarTitle;
    /** The subtitle in the actionbar */
    private String mActionBarSubtitle;

    private final Handler mHandler = new Handler();
    // TODO Find a better way to do this. We basically want the activity to display the
    // "loading..." progress until the fragment takes over and shows it's own "loading..."
    // progress [located in photo_header_view.xml]. We could potentially have all status displayed
    // by the activity, but, that gets tricky when it comes to screen rotation. For now, we
    // track the loading by this variable which is fragile and may cause phantom "loading..."
    // text.
    private long mEnterFullScreenDelayTime;

    protected PhotoPagerAdapter createPhotoPagerAdapter(Context context,
            android.support.v4.app.FragmentManager fm, Cursor c, float maxScale) {
        return new PhotoPagerAdapter(context, fm, c, maxScale);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActivityManager mgr = (ActivityManager) getApplicationContext().
                getSystemService(Activity.ACTIVITY_SERVICE);
        sMemoryClass = mgr.getMemoryClass();

        Intent mIntent = getIntent();

        int currentItem = -1;
        if (savedInstanceState != null) {
            currentItem = savedInstanceState.getInt(STATE_ITEM_KEY, -1);
            mFullScreen = savedInstanceState.getBoolean(STATE_FULLSCREEN_KEY, false);
            mActionBarTitle = savedInstanceState.getString(STATE_ACTIONBARTITLE_KEY);
            mActionBarSubtitle = savedInstanceState.getString(STATE_ACTIONBARSUBTITLE_KEY);
        }

        // uri of the photos to view; optional
        if (mIntent.hasExtra(Intents.EXTRA_PHOTOS_URI)) {
            mPhotosUri = mIntent.getStringExtra(Intents.EXTRA_PHOTOS_URI);
        }

        // projection for the query; optional
        // If not set, the default projection is used.
        // This projection must include the columns from the default projection.
        if (mIntent.hasExtra(Intents.EXTRA_PROJECTION)) {
            mProjection = mIntent.getStringArrayExtra(Intents.EXTRA_PROJECTION);
        } else {
            mProjection = null;
        }

        // Set the current item from the intent if wasn't in the saved instance
        if (currentItem < 0) {
            if (mIntent.hasExtra(Intents.EXTRA_PHOTO_INDEX)) {
                currentItem = mIntent.getIntExtra(Intents.EXTRA_PHOTO_INDEX, -1);
            }
            if (mIntent.hasExtra(Intents.EXTRA_INITIAL_PHOTO_URI)) {
                mInitialPhotoUri = mIntent.getStringExtra(Intents.EXTRA_INITIAL_PHOTO_URI);
            }
        }
        // Set the max initial scale, defaulting to 1x
        mMaxInitialScale = mIntent.getFloatExtra(Intents.EXTRA_MAX_INITIAL_SCALE, 1.0f);

        // If we still have a negative current item, set it to zero
        mPhotoIndex = Math.max(currentItem, 0);
        mIsEmpty = true;

        setContentView(R.layout.photo_activity_view);

        // Create the adapter and add the view pager
        mAdapter =
                createPhotoPagerAdapter(this, getSupportFragmentManager(), null, mMaxInitialScale);
        final Resources resources = getResources();
        mRootView = findViewById(R.id.photo_activity_root_view);
        mViewPager = (PhotoViewPager) findViewById(R.id.photo_view_pager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setOnInterceptTouchListener(this);
        mViewPager.setPageMargin(resources.getDimensionPixelSize(R.dimen.photo_page_margin));

        // Kick off the loader
        getSupportLoaderManager().initLoader(LOADER_PHOTO_LIST, null, this);

        mEnterFullScreenDelayTime =
                resources.getInteger(R.integer.reenter_fullscreen_delay_time_in_millis);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.addOnMenuVisibilityListener(this);
            final int showTitle = ActionBar.DISPLAY_SHOW_TITLE;
            actionBar.setDisplayOptions(showTitle, showTitle);
            setActionBarTitles(actionBar);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullScreen(mFullScreen, false);

        mIsPaused = false;
        if (mRestartLoader) {
            mRestartLoader = false;
            getSupportLoaderManager().restartLoader(LOADER_PHOTO_LIST, null, this);
        }
    }

    @Override
    protected void onPause() {
        mIsPaused = true;

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // If in full screen mode, toggle mode & eat the 'back'
        if (mFullScreen) {
            toggleFullScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_ITEM_KEY, mViewPager.getCurrentItem());
        outState.putBoolean(STATE_FULLSCREEN_KEY, mFullScreen);
        outState.putString(STATE_ACTIONBARTITLE_KEY, mActionBarTitle);
        outState.putString(STATE_ACTIONBARSUBTITLE_KEY, mActionBarSubtitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
          case android.R.id.home:
             finish();
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    @Override
    public void addScreenListener(int position, OnScreenListener listener) {
        mScreenListeners.put(position, listener);
    }

    @Override
    public void removeScreenListener(int position) {
        mScreenListeners.remove(position);
    }

    @Override
    public synchronized void addCursorListener(CursorChangedListener listener) {
        mCursorListeners.add(listener);
    }

    @Override
    public synchronized void removeCursorListener(CursorChangedListener listener) {
        mCursorListeners.remove(listener);
    }

    @Override
    public boolean isFragmentFullScreen(Fragment fragment) {
        if (mViewPager == null || mAdapter == null || mAdapter.getCount() == 0) {
            return mFullScreen;
        }
        return mFullScreen || (mViewPager.getCurrentItem() != mAdapter.getItemPosition(fragment));
    }

    @Override
    public void toggleFullScreen() {
        setFullScreen(!mFullScreen, true);
    }

    public void onPhotoRemoved(long photoId) {
        final Cursor data = mAdapter.getCursor();
        if (data == null) {
            // Huh?! How would this happen?
            return;
        }

        final int dataCount = data.getCount();
        if (dataCount <= 1) {
            finish();
            return;
        }

        getSupportLoaderManager().restartLoader(LOADER_PHOTO_LIST, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_PHOTO_LIST) {
            return new PhotoPagerLoader(this, Uri.parse(mPhotosUri), mProjection);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final int id = loader.getId();
        if (id == LOADER_PHOTO_LIST) {
            if (data == null || data.getCount() == 0) {
                mIsEmpty = true;
            } else {
                mAlbumCount = data.getCount();

                if (mInitialPhotoUri != null) {
                    int index = 0;
                    int uriIndex = data.getColumnIndex(PhotoContract.PhotoViewColumns.URI);
                    while (data.moveToNext()) {
                        String uri = data.getString(uriIndex);
                        if (TextUtils.equals(uri, mInitialPhotoUri)) {
                            mInitialPhotoUri = null;
                            mPhotoIndex = index;
                            break;
                        }
                        index++;
                    }
                }

                // We're paused; don't do anything now, we'll get re-invoked
                // when the activity becomes active again
                // TODO(pwestbro): This shouldn't be necessary, as the loader manager should
                // restart the loader
                if (mIsPaused) {
                    mRestartLoader = true;
                    return;
                }
                boolean wasEmpty = mIsEmpty;
                mIsEmpty = false;

                mAdapter.swapCursor(data);
                if (mViewPager.getAdapter() == null) {
                    mViewPager.setAdapter(mAdapter);
                }
                notifyCursorListeners(data);

                // set the selected photo
                int itemIndex = mPhotoIndex;

                // Use an index of 0 if the index wasn't specified or couldn't be found
                if (itemIndex < 0) {
                    itemIndex = 0;
                }

                mViewPager.setCurrentItem(itemIndex, false);
                if (wasEmpty) {
                    setViewActivated(itemIndex);
                }
            }
            // Update the any action items
            updateActionItems();
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        // If the loader is reset, remove the reference in the adapter to this cursor
        // TODO(pwestbro): reenable this when b/7075236 is fixed
        // mAdapter.swapCursor(null);
    }

    protected void updateActionItems() {
        // Do nothing, but allow extending classes to do work
    }

    private synchronized void notifyCursorListeners(Cursor data) {
        // tell all of the objects listening for cursor changes
        // that the cursor has changed
        for (CursorChangedListener listener : mCursorListeners) {
            listener.onCursorChanged(data);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mPhotoIndex = position;
        setViewActivated(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public boolean isFragmentActive(Fragment fragment) {
        if (mViewPager == null || mAdapter == null) {
            return false;
        }
        return mViewPager.getCurrentItem() == mAdapter.getItemPosition(fragment);
    }

    @Override
    public void onFragmentVisible(Fragment fragment) {
        updateActionBar();
    }

    @Override
    public InterceptType onTouchIntercept(float origX, float origY) {
        boolean interceptLeft = false;
        boolean interceptRight = false;

        for (OnScreenListener listener : mScreenListeners.values()) {
            if (!interceptLeft) {
                interceptLeft = listener.onInterceptMoveLeft(origX, origY);
            }
            if (!interceptRight) {
                interceptRight = listener.onInterceptMoveRight(origX, origY);
            }
        }

        if (interceptLeft) {
            if (interceptRight) {
                return InterceptType.BOTH;
            }
            return InterceptType.LEFT;
        } else if (interceptRight) {
            return InterceptType.RIGHT;
        }
        return InterceptType.NONE;
    }

    /**
     * Updates the title bar according to the value of {@link #mFullScreen}.
     */
    protected void setFullScreen(boolean fullScreen, boolean setDelayedRunnable) {
        final boolean fullScreenChanged = (fullScreen != mFullScreen);
        mFullScreen = fullScreen;

        if (mFullScreen) {
            setLightsOutMode(true);
            cancelEnterFullScreenRunnable();
        } else {
            setLightsOutMode(false);
            if (setDelayedRunnable) {
                postEnterFullScreenRunnableWithDelay();
            }
        }

        if (fullScreenChanged) {
            for (OnScreenListener listener : mScreenListeners.values()) {
                listener.onFullScreenChanged(mFullScreen);
            }
        }
    }

    private void postEnterFullScreenRunnableWithDelay() {
        mHandler.postDelayed(mEnterFullScreenRunnable, mEnterFullScreenDelayTime);
    }

    private void cancelEnterFullScreenRunnable() {
        mHandler.removeCallbacks(mEnterFullScreenRunnable);
    }

    protected void setLightsOutMode(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int flags = enabled
                    ? View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    : View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

            // using mViewPager since we have it and we need a view
            mViewPager.setSystemUiVisibility(flags);
        } else {
            final ActionBar actionBar = getActionBar();
            if (enabled) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
            int flags = enabled
                    ? View.SYSTEM_UI_FLAG_LOW_PROFILE
                    : View.SYSTEM_UI_FLAG_VISIBLE;
            mViewPager.setSystemUiVisibility(flags);
        }
    }

    private Runnable mEnterFullScreenRunnable = new Runnable() {
        @Override
        public void run() {
            setFullScreen(true, true);
        }
    };

    @Override
    public void setViewActivated(int position) {
        OnScreenListener listener = mScreenListeners.get(position);
        if (listener != null) {
            listener.onViewActivated();
        }
    }

    /**
     * Adjusts the activity title and subtitle to reflect the photo name and count.
     */
    protected void updateActionBar() {
        final int position = mViewPager.getCurrentItem() + 1;
        final boolean hasAlbumCount = mAlbumCount >= 0;

        final Cursor cursor = getCursorAtProperPosition();
        if (cursor != null) {
            final int photoNameIndex = cursor.getColumnIndex(PhotoContract.PhotoViewColumns.NAME);
            mActionBarTitle = cursor.getString(photoNameIndex);
        } else {
            mActionBarTitle = null;
        }

        if (mIsEmpty || !hasAlbumCount || position <= 0) {
            mActionBarSubtitle = null;
        } else {
            mActionBarSubtitle =
                    getResources().getString(R.string.photo_view_count, position, mAlbumCount);
        }
        setActionBarTitles(getActionBar());
    }

    /**
     * Sets the Action Bar title to {@link #mActionBarTitle} and the subtitle to
     * {@link #mActionBarSubtitle}
     */
    private final void setActionBarTitles(ActionBar actionBar) {
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle(getInputOrEmpty(mActionBarTitle));
        actionBar.setSubtitle(getInputOrEmpty(mActionBarSubtitle));
    }

    /**
     * If the input string is non-null, it is returned, otherwise an empty string is returned;
     * @param in
     * @return
     */
    private static final String getInputOrEmpty(String in) {
        if (in == null) {
            return "";
        }
        return in;
    }

    /**
     * Utility method that will return the cursor that contains the data
     * at the current position so that it refers to the current image on screen.
     * @return the cursor at the current position or
     * null if no cursor exists or if the {@link PhotoViewPager} is null.
     */
    public Cursor getCursorAtProperPosition() {
        if (mViewPager == null) {
            return null;
        }

        final int position = mViewPager.getCurrentItem();
        final Cursor cursor = mAdapter.getCursor();

        if (cursor == null) {
            return null;
        }

        cursor.moveToPosition(position);

        return cursor;
    }

    public Cursor getCursor() {
        return (mAdapter == null) ? null : mAdapter.getCursor();
    }

    @Override
    public void onMenuVisibilityChanged(boolean isVisible) {
        if (isVisible) {
            cancelEnterFullScreenRunnable();
        } else {
            postEnterFullScreenRunnableWithDelay();
        }
    }

    @Override
    public void onNewPhotoLoaded(int position) {
        // do nothing
    }

    protected boolean isFullScreen() {
        return mFullScreen;
    }

    protected void setPhotoIndex(int index) {
        mPhotoIndex = index;
    }

    @Override
    public void onCursorChanged(PhotoViewFragment fragment, Cursor cursor) {
        // do nothing
    }

    @Override
    public PhotoPagerAdapter getAdapter() {
        return mAdapter;
    }
}
