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

import static com.google.mockwebserver.MockWebServer.ASCII;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base scripted response to be replayed by {@link MockWebServer}.
 */
abstract class BaseMockResponse<T extends BaseMockResponse<T>> {
    protected static final String CONTENT_LENGTH = "Content-Length";

    private String status = "HTTP/1.1 200 OK";
    private List<String> headers = new ArrayList<String>();
    private int bytesPerSecond = Integer.MAX_VALUE;
    private SocketPolicy socketPolicy = SocketPolicy.KEEP_OPEN;

    protected BaseMockResponse() {
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        final BaseMockResponse<?> result = (BaseMockResponse<?>) super.clone();
        result.headers = new ArrayList<String>(result.headers);
        return result;
    }

    /**
     * Returns the HTTP response line, such as "HTTP/1.1 200 OK".
     */
    public String getStatus() {
        return status;
    }

    public T setResponseCode(int code) {
        this.status = "HTTP/1.1 " + code + " OK";
        return self();
    }

    public T setStatus(String status) {
        this.status = status;
        return self();
    }

    /**
     * Returns the HTTP headers, such as "Content-Length: 0".
     */
    public List<String> getHeaders() {
        return headers;
    }

    public T clearHeaders() {
        headers.clear();
        return self();
    }

    public T addHeader(String header) {
        headers.add(header);
        return self();
    }

    public T addHeader(String name, Object value) {
        return addHeader(name + ": " + String.valueOf(value));
    }

    public T setHeader(String name, Object value) {
        removeHeader(name);
        return addHeader(name, value);
    }

    public T removeHeader(String name) {
        name += ": ";
        for (Iterator<String> i = headers.iterator(); i.hasNext();) {
            String header = i.next();
            if (name.regionMatches(true, 0, header, 0, name.length())) {
                i.remove();
            }
        }
        return self();
    }

    public SocketPolicy getSocketPolicy() {
        return socketPolicy;
    }

    public T setSocketPolicy(SocketPolicy socketPolicy) {
        this.socketPolicy = socketPolicy;
        return self();
    }

    public int getBytesPerSecond() {
        return bytesPerSecond;
    }

    /**
     * Set simulated network speed, in bytes per second.
     */
    public T setBytesPerSecond(int bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
        return self();
    }

    @Override public String toString() {
        return status;
    }

    /**
     * Write complete response, including all headers and the given body.
     * Handles applying {@link #setBytesPerSecond(int)} limits.
     */
    protected void writeResponse(InputStream body, OutputStream out) throws IOException {
        out.write((getStatus() + "\r\n").getBytes(ASCII));
        for (String header : getHeaders()) {
            out.write((header + "\r\n").getBytes(ASCII));
        }
        out.write(("\r\n").getBytes(ASCII));
        out.flush();

        // Stream data in MTU-sized increments
        final byte[] buffer = new byte[1452];
        final long delayMs;
        if (bytesPerSecond == Integer.MAX_VALUE) {
            delayMs = 0;
        } else {
            delayMs = (1000 * buffer.length) / bytesPerSecond;
        }

        int read;
        long sinceDelay = 0;
        while ((read = body.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            out.flush();

            sinceDelay += read;
            if (sinceDelay >= buffer.length && delayMs > 0) {
                sinceDelay %= buffer.length;
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    throw new AssertionError();
                }
            }
        }
    }

    /**
     * Write complete response. Usually implemented by calling
     * {@link #writeResponse(InputStream, OutputStream)} with the
     * implementation-specific body.
     */
    public abstract void writeResponse(OutputStream out) throws IOException;

    /**
     * Return concrete {@code this} to enable builder-style methods.
     */
    protected abstract T self();
}
