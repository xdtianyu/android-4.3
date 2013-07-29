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

package android.security.cts;

import junit.framework.TestCase;

public class NativeCodeTest extends TestCase {

    static {
        System.loadLibrary("ctssecurity_jni");
    }

    public void testPerfEvent() throws Exception {
        assertFalse("Device is vulnerable to CVE-2013-2094. Please apply security patch "
                    + "at http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/"
                    + "commit/?id=8176cced706b5e5d15887584150764894e94e02f",
                    doPerfEventTest());
    }

    /**
     * Returns true iff this device is vulnerable to CVE-2013-2094.
     * A patch for CVE-2013-2094 can be found at
     * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=8176cced706b5e5d15887584150764894e94e02f
     */
    private static native boolean doPerfEventTest();
}
