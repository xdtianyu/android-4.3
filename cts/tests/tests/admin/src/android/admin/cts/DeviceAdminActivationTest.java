/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.admin.cts;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver2;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver3;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver4;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver5;
import android.deviceadmin.cts.CtsDeviceAdminReceiver;
import android.deviceadmin.cts.CtsDeviceAdminActivationTestActivity;
import android.deviceadmin.cts.CtsDeviceAdminActivationTestActivity.OnActivityResultListener;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests for the standard way of activating a Device Admin: by starting system UI via an
 * {@link Intent} with {@link DevicePolicyManager#ACTION_ADD_DEVICE_ADMIN}. The test requires that
 * the {@code CtsDeviceAdmin.apk} be installed.
 */
public class DeviceAdminActivationTest
    extends ActivityInstrumentationTestCase2<CtsDeviceAdminActivationTestActivity> {

    // IMPLEMENTATION NOTE: Because Device Admin activation requires the use of
    // Activity.startActivityForResult, this test creates an empty Activity which then invokes
    // startActivityForResult.

    private static final int REQUEST_CODE_ACTIVATE_ADMIN = 1;

    /**
     * Maximum duration of time (milliseconds) after which the effects of programmatic actions in
     * this test should have affected the UI.
     */
    private static final int UI_EFFECT_TIMEOUT_MILLIS = 5000;

    /**
     * Monitor guarding access to {@link #mLastOnActivityResultResultCode} and which is notified
     * every time {@code onActivityResult} of the {@code CtsDeviceAdminActivationTestActivity} is
     * invoked.
     */
    private final Object mOnActivityResultListenerLock = new Object();

    /**
     * Result code of the most recent invocation of
     * {@code CtsDeviceAdminActivationTestActivity.onActivityResult} or {@code null} if no
     * invocations have occured yet.
     *
     * @GuardedBy {@link #mOnActivityResultListenerLock}
     */
    private Integer mLastOnActivityResultResultCode;

    public DeviceAdminActivationTest() {
        super(CtsDeviceAdminActivationTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity().setOnActivityResultListener(new OnActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode != REQUEST_CODE_ACTIVATE_ADMIN) {
                    return;
                }
                synchronized (mOnActivityResultListenerLock) {
                    mLastOnActivityResultResultCode = resultCode;
                    mOnActivityResultListenerLock.notifyAll();
                }
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            finishActivateDeviceAdminActivity();
        } finally {
            super.tearDown();
        }
    }

    public void testActivateGoodReceiverDisplaysActivationUi() throws Exception {
        startAddDeviceAdminActivityForResult(CtsDeviceAdminReceiver.class);
        assertWithTimeoutOnActivityResultNotInvoked();
        // The UI is up and running. Assert that dismissing the UI returns the corresponding result
        // to the test activity.
        finishActivateDeviceAdminActivity();
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
    }

    public void testActivateBrokenReceiverFails() throws Exception {
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
    }

    public void testActivateBrokenReceiver2Fails() throws Exception {
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver2.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
    }

    public void testActivateBrokenReceiver3Fails() throws Exception {
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver3.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
    }

    public void testActivateBrokenReceiver4Fails() throws Exception {
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver4.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
    }

    public void testActivateBrokenReceiver5Fails() throws Exception {
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver5.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
    }

    private void startAddDeviceAdminActivityForResult(Class<?> receiverClass) {
        getActivity().startActivityForResult(
                getAddDeviceAdminIntent(receiverClass),
                REQUEST_CODE_ACTIVATE_ADMIN);
    }

    private Intent getAddDeviceAdminIntent(Class<?> receiverClass) {
        return new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    new ComponentName(
                            getInstrumentation().getTargetContext(),
                            receiverClass));
    }

    private void assertWithTimeoutOnActivityResultNotInvoked() {
        SystemClock.sleep(UI_EFFECT_TIMEOUT_MILLIS);
        synchronized (mOnActivityResultListenerLock) {
            assertNull(mLastOnActivityResultResultCode);
        }
    }

    private void assertWithTimeoutOnActivityResultInvokedWithResultCode(int expectedResultCode)
            throws Exception {
        long deadlineMillis = SystemClock.elapsedRealtime() + UI_EFFECT_TIMEOUT_MILLIS;
        synchronized (mOnActivityResultListenerLock) {
            while (true) {
                if (mLastOnActivityResultResultCode != null) {
                    // onActivityResult has been invoked -- check the arguments
                    assertEquals(expectedResultCode, (int) mLastOnActivityResultResultCode);
                    break;
                }

                // onActivityResult has not yet been invoked -- wait until it is
                long millisTillDeadline = deadlineMillis - SystemClock.elapsedRealtime();
                if (millisTillDeadline <= 0) {
                    fail("onActivityResult not invoked within " + UI_EFFECT_TIMEOUT_MILLIS + " ms");
                    break;
                }

                mOnActivityResultListenerLock.wait(millisTillDeadline);
            }
        }
    }

    private void finishActivateDeviceAdminActivity() {
        getActivity().finishActivity(REQUEST_CODE_ACTIVATE_ADMIN);
    }
}
