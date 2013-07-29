/*
 * Copyright (C) 2011 Google Inc.
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

import static com.google.mockwebserver.MockWebServer.ASCII;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * A scripted response to be replayed by {@link MockWebServer}.
 */
public class MockResponse extends BaseMockResponse<MockResponse> implements Cloneable {
    private static final String CHUNKED_BODY_HEADER = "Transfer-encoding: chunked";

    private byte[] body;

    public MockResponse() {
        this.body = new byte[0];
        addHeader(CONTENT_LENGTH, 0);
    }

    @Override
    public MockResponse clone() {
        try {
            return (MockResponse) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public MockResponse setBody(byte[] body) {
        this.body = body;
        setHeader(CONTENT_LENGTH, body.length);
        return this;
    }

    public MockResponse setBody(String body) {
        try {
            return setBody(body.getBytes(ASCII));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    public byte[] getBody() {
        return body;
    }

    public MockResponse setChunkedBody(byte[] body, int maxChunkSize) throws IOException {
        removeHeader(CONTENT_LENGTH);
        addHeader(CHUNKED_BODY_HEADER);

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int pos = 0;
        while (pos < body.length) {
            int chunkSize = Math.min(body.length - pos, maxChunkSize);
            bytesOut.write(Integer.toHexString(chunkSize).getBytes(ASCII));
            bytesOut.write("\r\n".getBytes(ASCII));
            bytesOut.write(body, pos, chunkSize);
            bytesOut.write("\r\n".getBytes(ASCII));
            pos += chunkSize;
        }
        bytesOut.write("0\r\n\r\n".getBytes(ASCII)); // last chunk + empty trailer + crlf

        this.body = bytesOut.toByteArray();
        return this;
    }

    public MockResponse setChunkedBody(String body, int maxChunkSize) throws IOException {
        return setChunkedBody(body.getBytes(ASCII), maxChunkSize);
    }

    @Override
    protected MockResponse self() {
        return this;
    }

    @Override
    public void writeResponse(OutputStream out) throws IOException {
        super.writeResponse(new ByteArrayInputStream(body), out);
    }
}
