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

package android.permission.cts;

import android.os.RemoteException;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify permissions are enforced.
 */
public class ContactsProviderTest extends AndroidTestCase {
    private ContentProviderClient mProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProvider = getContext()
                .getContentResolver().acquireContentProviderClient(Contacts.AUTHORITY);
        assertNotNull(mProvider);
    }

    /**
     * Verifies that query(ContactsContract.Contacts.CONTENT_URI) requires
     * Permission.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_CONTACTS}.
     */
    @SmallTest
    public void testQueryContacts() {
        try {
            mProvider.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
            fail("query(ContactsContract.Contacts.CONTENT_URI) did not throw SecurityException as expected");
        } catch (SecurityException se) {
            // Expected Exception
        } catch (RemoteException e) {
            fail("testQueryContacts throws unexpected exception");
        }
    }

    /**
     * Verifies that query(ContactsContract.Profile.CONTENT_URI) requires
     * Permission.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PROFILE}.
     */
    @SmallTest
    public void testQueryProfile() {
        try {
            Cursor cursor = mProvider.query(ContactsContract.Profile.CONTENT_URI,
                    null, null, null, null);
            fail("query(ContactsContract.Profile.CONTENT_URI) did not throw SecurityException as expected");
        } catch (SecurityException se) {
            // Expected Exception
        } catch (RemoteException e) {
            fail("testQueryProfile throws unexpected exception");
        }
    }
}
