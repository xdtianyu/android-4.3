/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.mockwebserver;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A scripted response to be replayed by {@link MockWebServer}. This specific
 * variant uses an {@link InputStream} as its data source. Each instance can
 * only be consumed once.
 */
public class MockStreamResponse extends BaseMockResponse<MockStreamResponse> {
    private InputStream body;

    public MockStreamResponse() {
        body = new ByteArrayInputStream(new byte[0]);
        addHeader(CONTENT_LENGTH, 0);
    }

    public MockStreamResponse setBody(InputStream body, long bodyLength) {
        // Release any existing body
        if (this.body != null) {
            closeQuietly(this.body);
        }

        this.body = body;
        setHeader(CONTENT_LENGTH, bodyLength);
        return this;
    }

    @Override
    public void writeResponse(OutputStream out) throws IOException {
        if (body == null) {
            throw new IllegalStateException("Stream already consumed");
        }

        try {
            super.writeResponse(body, out);
        } finally {
            closeQuietly(body);
        }
        body = null;
    }

    @Override
    protected MockStreamResponse self() {
        return this;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
}
