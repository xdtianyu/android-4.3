package com.android.ex.photo;

import android.database.Cursor;
import android.support.v4.app.Fragment;

import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.fragments.PhotoViewFragment;

public interface PhotoViewCallbacks {
    /**
     * Listener to be invoked for screen events.
     */
    public static interface OnScreenListener {

        /**
         * The full screen state has changed.
         */
        public void onFullScreenChanged(boolean fullScreen);

        /**
         * A new view has been activated and the previous view de-activated.
         */
        public void onViewActivated();

        /**
         * Called when a right-to-left touch move intercept is about to occur.
         *
         * @param origX the raw x coordinate of the initial touch
         * @param origY the raw y coordinate of the initial touch
         * @return {@code true} if the touch should be intercepted.
         */
        public boolean onInterceptMoveLeft(float origX, float origY);

        /**
         * Called when a left-to-right touch move intercept is about to occur.
         *
         * @param origX the raw x coordinate of the initial touch
         * @param origY the raw y coordinate of the initial touch
         * @return {@code true} if the touch should be intercepted.
         */
        public boolean onInterceptMoveRight(float origX, float origY);
    }

    public static interface CursorChangedListener {
        /**
         * Called when the cursor that contains the photo list data
         * is updated. Note that there is no guarantee that the cursor
         * will be at the proper position.
         * @param cursor the cursor containing the photo list data
         */
        public void onCursorChanged(Cursor cursor);
    }

    public void addScreenListener(int position, OnScreenListener listener);

    public void removeScreenListener(int position);

    public void addCursorListener(CursorChangedListener listener);

    public void removeCursorListener(CursorChangedListener listener);

    public void setViewActivated(int position);

    public void onNewPhotoLoaded(int position);

    public void toggleFullScreen();

    public boolean isFragmentActive(Fragment fragment);

    public void onFragmentVisible(Fragment fragment);

    public boolean isFragmentFullScreen(Fragment fragment);

    public void onCursorChanged(PhotoViewFragment fragment, Cursor cursor);

    /**
     * Returns the adapter associated with this activity.
     */
    public PhotoPagerAdapter getAdapter();
}
