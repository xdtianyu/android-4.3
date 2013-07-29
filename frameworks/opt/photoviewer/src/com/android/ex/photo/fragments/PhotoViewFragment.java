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

package com.android.ex.photo.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.ex.photo.Intents;
import com.android.ex.photo.PhotoViewCallbacks;
import com.android.ex.photo.PhotoViewCallbacks.CursorChangedListener;
import com.android.ex.photo.PhotoViewCallbacks.OnScreenListener;
import com.android.ex.photo.R;
import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.loaders.PhotoBitmapLoader;
import com.android.ex.photo.loaders.PhotoBitmapLoader.BitmapResult;
import com.android.ex.photo.util.ImageUtils;
import com.android.ex.photo.views.PhotoView;
import com.android.ex.photo.views.ProgressBarWrapper;

/**
 * Displays a photo.
 */
public class PhotoViewFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<BitmapResult>,
        OnClickListener,
        OnScreenListener,
        CursorChangedListener {
    /**
     * Interface for components that are internally scrollable left-to-right.
     */
    public static interface HorizontallyScrollable {
        /**
         * Return {@code true} if the component needs to receive right-to-left
         * touch movements.
         *
         * @param origX the raw x coordinate of the initial touch
         * @param origY the raw y coordinate of the initial touch
         */

        public boolean interceptMoveLeft(float origX, float origY);

        /**
         * Return {@code true} if the component needs to receive left-to-right
         * touch movements.
         *
         * @param origX the raw x coordinate of the initial touch
         * @param origY the raw y coordinate of the initial touch
         */
        public boolean interceptMoveRight(float origX, float origY);
    }

    protected final static String STATE_INTENT_KEY =
            "com.android.mail.photo.fragments.PhotoViewFragment.INTENT";

    private final static String ARG_INTENT = "arg-intent";
    private final static String ARG_POSITION = "arg-position";
    private final static String ARG_SHOW_SPINNER = "arg-show-spinner";

    // Loader IDs
    protected final static int LOADER_ID_PHOTO = 1;
    protected final static int LOADER_ID_THUMBNAIL = 2;

    /** The size of the photo */
    public static Integer sPhotoSize;

    /** The URL of a photo to display */
    protected String mResolvedPhotoUri;
    protected String mThumbnailUri;
    /** The intent we were launched with */
    protected Intent mIntent;
    protected PhotoViewCallbacks mCallback;
    protected PhotoPagerAdapter mAdapter;

    protected PhotoView mPhotoView;
    protected ImageView mPhotoPreviewImage;
    protected TextView mEmptyText;
    protected ImageView mRetryButton;
    protected ProgressBarWrapper mPhotoProgressBar;

    protected int mPosition;

    /** Whether or not the fragment should make the photo full-screen */
    protected boolean mFullScreen;

    /** Whether or not this fragment will only show the loading spinner */
    protected boolean mOnlyShowSpinner;

    /** Whether or not the progress bar is showing valid information about the progress stated */
    protected boolean mProgressBarNeeded = true;

    protected View mPhotoPreviewAndProgress;
    protected boolean mThumbnailShown;

    /** Whether or not there is currently a connection to the internet */
    protected boolean mConnected;

    /** Public no-arg constructor for allowing the framework to handle orientation changes */
    public PhotoViewFragment() {
        // Do nothing.
    }

    /**
     * Create a {@link PhotoViewFragment}.
     * @param intent
     * @param position
     * @param onlyShowSpinner
     */
    public static PhotoViewFragment newInstance(
            Intent intent, int position, boolean onlyShowSpinner) {
        final Bundle b = new Bundle();
        b.putParcelable(ARG_INTENT, intent);
        b.putInt(ARG_POSITION, position);
        b.putBoolean(ARG_SHOW_SPINNER, onlyShowSpinner);
        final PhotoViewFragment f = new PhotoViewFragment();
        f.setArguments(b);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCallback = (PhotoViewCallbacks) getActivity();
        if (mCallback == null) {
            throw new IllegalArgumentException(
                    "Activity must be a derived class of PhotoViewActivity");
        }
        mAdapter = mCallback.getAdapter();
        if (mAdapter == null) {
            throw new IllegalStateException("Callback reported null adapter");
        }

        if (hasNetworkStatePermission()) {
            getActivity().registerReceiver(new InternetStateBroadcastReceiver(),
                    new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }
        // Don't call until we've setup the entire view
        setViewVisibility();
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sPhotoSize == null) {
            final DisplayMetrics metrics = new DisplayMetrics();
            final WindowManager wm =
                    (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            final ImageUtils.ImageSize imageSize = ImageUtils.sUseImageSize;
            wm.getDefaultDisplay().getMetrics(metrics);
            switch (imageSize) {
                case EXTRA_SMALL:
                    // Use a photo that's 80% of the "small" size
                    sPhotoSize = (Math.min(metrics.heightPixels, metrics.widthPixels) * 800) / 1000;
                    break;
                case SMALL:
                    // Fall through.
                case NORMAL:
                    // Fall through.
                default:
                    sPhotoSize = Math.min(metrics.heightPixels, metrics.widthPixels);
                    break;
            }
        }

        final Bundle bundle = getArguments();
        if (bundle == null) {
            return;
        }
        mIntent = bundle.getParcelable(ARG_INTENT);
        mPosition = bundle.getInt(ARG_POSITION);
        mOnlyShowSpinner = bundle.getBoolean(ARG_SHOW_SPINNER);
        mProgressBarNeeded = true;

        if (savedInstanceState != null) {
            final Bundle state = savedInstanceState.getBundle(STATE_INTENT_KEY);
            if (state != null) {
                mIntent = new Intent().putExtras(state);
            }
        }

        if (mIntent != null) {
            mResolvedPhotoUri = mIntent.getStringExtra(Intents.EXTRA_RESOLVED_PHOTO_URI);
            mThumbnailUri = mIntent.getStringExtra(Intents.EXTRA_THUMBNAIL_URI);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.photo_fragment_view, container, false);
        initializeView(view);
        return view;
    }

    protected void initializeView(View view) {
        mPhotoView = (PhotoView) view.findViewById(R.id.photo_view);
        mPhotoView.setMaxInitialScale(mIntent.getFloatExtra(Intents.EXTRA_MAX_INITIAL_SCALE, 1));
        mPhotoView.setOnClickListener(this);
        mPhotoView.setFullScreen(mFullScreen, false);
        mPhotoView.enableImageTransforms(false);

        mPhotoPreviewAndProgress = view.findViewById(R.id.photo_preview);
        mPhotoPreviewImage = (ImageView) view.findViewById(R.id.photo_preview_image);
        mThumbnailShown = false;
        final ProgressBar indeterminate =
                (ProgressBar) view.findViewById(R.id.indeterminate_progress);
        final ProgressBar determinate =
                (ProgressBar) view.findViewById(R.id.determinate_progress);
        mPhotoProgressBar = new ProgressBarWrapper(determinate, indeterminate, true);
        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        mRetryButton = (ImageView) view.findViewById(R.id.retry_button);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.addScreenListener(mPosition, this);
        mCallback.addCursorListener(this);

        if (hasNetworkStatePermission()) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo != null) {
                mConnected = activeNetInfo.isConnected();
            } else {
                // Best to set this to false, since it won't stop us from trying to download,
                // only allow us to try re-download if we get notified that we do have a connection.
                mConnected = false;
            }
        }

        if (!isPhotoBound()) {
            mProgressBarNeeded = true;
            mPhotoPreviewAndProgress.setVisibility(View.VISIBLE);

            getLoaderManager().initLoader(LOADER_ID_THUMBNAIL, null, this);
            getLoaderManager().initLoader(LOADER_ID_PHOTO, null, this);
        }
    }

    @Override
    public void onPause() {
        // Remove listeners
        mCallback.removeCursorListener(this);
        mCallback.removeScreenListener(mPosition);
        resetPhotoView();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // Clean up views and other components
        if (mPhotoView != null) {
            mPhotoView.clear();
            mPhotoView = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mIntent != null) {
            outState.putParcelable(STATE_INTENT_KEY, mIntent.getExtras());
        }
    }

    @Override
    public Loader<BitmapResult> onCreateLoader(int id, Bundle args) {
        if(mOnlyShowSpinner) {
            return null;
        }
        switch (id) {
            case LOADER_ID_PHOTO:
                return new PhotoBitmapLoader(getActivity(), mResolvedPhotoUri);
            case LOADER_ID_THUMBNAIL:
                return new PhotoBitmapLoader(getActivity(), mThumbnailUri);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<BitmapResult> loader, BitmapResult result) {
        Bitmap data = result.bitmap;
        // If we don't have a view, the fragment has been paused. We'll get the cursor again later.
        if (getView() == null) {
            return;
        }

        final int id = loader.getId();
        switch (id) {
            case LOADER_ID_THUMBNAIL:
                if (isPhotoBound()) {
                    // There is need to do anything with the thumbnail
                    // image, as the full size image is being shown.
                    return;
                }

                if (data == null) {
                    // no preview, show default
                    mPhotoPreviewImage.setImageResource(R.drawable.default_image);
                    mThumbnailShown = false;
                } else {
                    // show preview
                    mPhotoPreviewImage.setImageBitmap(data);
                    mThumbnailShown = true;
                }
                mPhotoPreviewImage.setVisibility(View.VISIBLE);
                if (getResources().getBoolean(R.bool.force_thumbnail_no_scaling)) {
                    mPhotoPreviewImage.setScaleType(ImageView.ScaleType.CENTER);
                }
                enableImageTransforms(false);
                break;
            case LOADER_ID_PHOTO:

                if (result.status == BitmapResult.STATUS_EXCEPTION) {
                    mProgressBarNeeded = false;
                    mEmptyText.setText(R.string.failed);
                    mEmptyText.setVisibility(View.VISIBLE);
                } else {
                    bindPhoto(data);
                }
                break;
            default:
                break;
        }

        if (mProgressBarNeeded == false) {
            // Hide the progress bar as it isn't needed anymore.
            mPhotoProgressBar.setVisibility(View.GONE);
        }

        if (data != null) {
            mCallback.onNewPhotoLoaded(mPosition);
        }
        setViewVisibility();
    }

    /**
     * Binds an image to the photo view.
     */
    private void bindPhoto(Bitmap bitmap) {
        if (bitmap != null) {
            if (mPhotoView != null) {
                mPhotoView.bindPhoto(bitmap);
            }
            enableImageTransforms(true);
            mPhotoPreviewAndProgress.setVisibility(View.GONE);
            mProgressBarNeeded = false;
        }
    }

    private boolean hasNetworkStatePermission() {
        final String networkStatePermission = "android.permission.ACCESS_NETWORK_STATE";
        int result = getActivity().checkCallingOrSelfPermission(networkStatePermission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Enable or disable image transformations. When transformations are enabled, this view
     * consumes all touch events.
     */
    public void enableImageTransforms(boolean enable) {
        mPhotoView.enableImageTransforms(enable);
    }

    /**
     * Resets the photo view to it's default state w/ no bound photo.
     */
    private void resetPhotoView() {
        if (mPhotoView != null) {
            mPhotoView.bindPhoto(null);
        }
    }

    @Override
    public void onLoaderReset(Loader<BitmapResult> loader) {
        // Do nothing
    }

    @Override
    public void onClick(View v) {
        mCallback.toggleFullScreen();
    }

    @Override
    public void onFullScreenChanged(boolean fullScreen) {
        setViewVisibility();
    }

    @Override
    public void onViewActivated() {
        if (!mCallback.isFragmentActive(this)) {
            // we're not in the foreground; reset our view
            resetViews();
        } else {
            if (!isPhotoBound()) {
                // Restart the loader
                getLoaderManager().restartLoader(LOADER_ID_THUMBNAIL, null, this);
            }
            mCallback.onFragmentVisible(this);
        }
    }

    /**
     * Reset the views to their default states
     */
    public void resetViews() {
        if (mPhotoView != null) {
            mPhotoView.resetTransformations();
        }
    }

    @Override
    public boolean onInterceptMoveLeft(float origX, float origY) {
        if (!mCallback.isFragmentActive(this)) {
            // we're not in the foreground; don't intercept any touches
            return false;
        }

        return (mPhotoView != null && mPhotoView.interceptMoveLeft(origX, origY));
    }

    @Override
    public boolean onInterceptMoveRight(float origX, float origY) {
        if (!mCallback.isFragmentActive(this)) {
            // we're not in the foreground; don't intercept any touches
            return false;
        }

        return (mPhotoView != null && mPhotoView.interceptMoveRight(origX, origY));
    }

    /**
     * Returns {@code true} if a photo has been bound. Otherwise, returns {@code false}.
     */
    public boolean isPhotoBound() {
        return (mPhotoView != null && mPhotoView.isPhotoBound());
    }

    /**
     * Sets view visibility depending upon whether or not we're in "full screen" mode.
     */
    private void setViewVisibility() {
        final boolean fullScreen = mCallback == null ? false : mCallback.isFragmentFullScreen(this);
        setFullScreen(fullScreen);
    }

    /**
     * Sets full-screen mode for the views.
     */
    public void setFullScreen(boolean fullScreen) {
        mFullScreen = fullScreen;
    }

    @Override
    public void onCursorChanged(Cursor cursor) {
        if (mAdapter == null) {
            // The adapter is set in onAttach(), and is guaranteed to be non-null. We have magically
            // received an onCursorChanged without attaching to an activity. Ignore this cursor
            // change.
            return;
        }
        if (cursor.moveToPosition(mPosition) && !isPhotoBound()) {
            mCallback.onCursorChanged(this, cursor);

            final LoaderManager manager = getLoaderManager();
            final Loader<BitmapResult> fakePhotoLoader = manager.getLoader(LOADER_ID_PHOTO);
            if (fakePhotoLoader != null) {
                final PhotoBitmapLoader loader = (PhotoBitmapLoader) fakePhotoLoader;
                mResolvedPhotoUri = mAdapter.getPhotoUri(cursor);
                loader.setPhotoUri(mResolvedPhotoUri);
                loader.forceLoad();
            }

            if (!mThumbnailShown) {
                final Loader<BitmapResult> fakeThumbnailLoader = manager.getLoader(
                        LOADER_ID_THUMBNAIL);
                if (fakeThumbnailLoader != null) {
                    final PhotoBitmapLoader loader = (PhotoBitmapLoader) fakeThumbnailLoader;
                    mThumbnailUri = mAdapter.getThumbnailUri(cursor);
                    loader.setPhotoUri(mThumbnailUri);
                    loader.forceLoad();
                }
            }
        }
    }

    public ProgressBarWrapper getPhotoProgressBar() {
        return mPhotoProgressBar;
    }

    public TextView getEmptyText() {
        return mEmptyText;
    }

    public ImageView getRetryButton() {
        return mRetryButton;
    }

    public boolean isProgressBarNeeded() {
        return mProgressBarNeeded;
    }

    private class InternetStateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // This is only created if we have the correct permissions, so
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo == null) {
                mConnected = false;
                return;
            }
            if (mConnected == false && activeNetInfo.isConnected() && !isPhotoBound()) {
                if (mThumbnailShown == false) {
                    getLoaderManager().restartLoader(LOADER_ID_THUMBNAIL, null,
                            PhotoViewFragment.this);
                }
                getLoaderManager().restartLoader(LOADER_ID_PHOTO, null, PhotoViewFragment.this);
                mConnected = true;
                mPhotoProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }
}
