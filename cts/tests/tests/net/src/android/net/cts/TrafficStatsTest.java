/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.net.cts;

import android.net.TrafficStats;
import android.os.Process;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TrafficStatsTest extends AndroidTestCase {
    public void testValidMobileStats() {
        // We can't assume a mobile network is even present in this test, so
        // we simply assert that a valid value is returned.

        assertTrue(TrafficStats.getMobileTxPackets() >= 0);
        assertTrue(TrafficStats.getMobileRxPackets() >= 0);
        assertTrue(TrafficStats.getMobileTxBytes() >= 0);
        assertTrue(TrafficStats.getMobileRxBytes() >= 0);
    }

    public void testValidTotalStats() {
        assertTrue(TrafficStats.getTotalTxPackets() >= 0);
        assertTrue(TrafficStats.getTotalRxPackets() >= 0);
        assertTrue(TrafficStats.getTotalTxBytes() >= 0);
        assertTrue(TrafficStats.getTotalRxBytes() >= 0);
    }

    public void testThreadStatsTag() throws Exception {
        TrafficStats.setThreadStatsTag(0xf00d);
        assertTrue("Tag didn't stick", TrafficStats.getThreadStatsTag() == 0xf00d);

        final CountDownLatch latch = new CountDownLatch(1);

        new Thread("TrafficStatsTest.testThreadStatsTag") {
            @Override
            public void run() {
                assertTrue("Tag leaked", TrafficStats.getThreadStatsTag() != 0xf00d);
                TrafficStats.setThreadStatsTag(0xcafe);
                assertTrue("Tag didn't stick", TrafficStats.getThreadStatsTag() == 0xcafe);
                latch.countDown();
            }
        }.start();

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Tag lost", TrafficStats.getThreadStatsTag() == 0xf00d);

        TrafficStats.clearThreadStatsTag();
        assertTrue("Tag not cleared", TrafficStats.getThreadStatsTag() != 0xf00d);
    }

    long tcpPacketToIpBytes(long packetCount, long bytes) {
        // ip header + tcp header + data.
        // Tcp header is mostly 32. Syn has different tcp options -> 40. Don't care.
        return packetCount * (20 + 32 + bytes);
    }

    public void testTrafficStatsForLocalhost() throws IOException {
        long mobileTxPacketsBefore = TrafficStats.getMobileTxPackets();
        long mobileRxPacketsBefore = TrafficStats.getMobileRxPackets();
        long mobileTxBytesBefore = TrafficStats.getMobileTxBytes();
        long mobileRxBytesBefore = TrafficStats.getMobileRxBytes();
        long totalTxPacketsBefore = TrafficStats.getTotalTxPackets();
        long totalRxPacketsBefore = TrafficStats.getTotalRxPackets();
        long totalTxBytesBefore = TrafficStats.getTotalTxBytes();
        long totalRxBytesBefore = TrafficStats.getTotalRxBytes();
        long uidTxBytesBefore = TrafficStats.getUidTxBytes(Process.myUid());
        long uidRxBytesBefore = TrafficStats.getUidRxBytes(Process.myUid());
        long uidTxPacketsBefore = TrafficStats.getUidTxPackets(Process.myUid());
        long uidRxPacketsBefore = TrafficStats.getUidRxPackets(Process.myUid());

        // Transfer 1MB of data across an explicitly localhost socket.
        final int byteCount = 1024;
        final int packetCount = 1024;

        final ServerSocket server = new ServerSocket(0);
        new Thread("TrafficStatsTest.testTrafficStatsForLocalhost") {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("localhost", server.getLocalPort());
                    // Make sure that each write()+flush() turns into a packet:
                    // disable Nagle.
                    socket.setTcpNoDelay(true);
                    OutputStream out = socket.getOutputStream();
                    byte[] buf = new byte[byteCount];
                    for (int i = 0; i < packetCount; i++) {
                        out.write(buf);
                        out.flush();
                    }
                    out.close();
                    socket.close();
                } catch (IOException e) {
                }
            }
        }.start();

        try {
            Socket socket = server.accept();
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[byteCount];
            int read = 0;
            while (read < byteCount * packetCount) {
                int n = in.read(buf);
                assertTrue("Unexpected EOF", n > 0);
                read += n;
            }
        } finally {
            server.close();
        }

        // It's too fast to call getUidTxBytes function.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        long mobileTxPacketsAfter = TrafficStats.getMobileTxPackets();
        long mobileRxPacketsAfter = TrafficStats.getMobileRxPackets();
        long mobileTxBytesAfter = TrafficStats.getMobileTxBytes();
        long mobileRxBytesAfter = TrafficStats.getMobileRxBytes();
        long totalTxPacketsAfter = TrafficStats.getTotalTxPackets();
        long totalRxPacketsAfter = TrafficStats.getTotalRxPackets();
        long totalTxBytesAfter = TrafficStats.getTotalTxBytes();
        long totalRxBytesAfter = TrafficStats.getTotalRxBytes();
        long uidTxBytesAfter = TrafficStats.getUidTxBytes(Process.myUid());
        long uidRxBytesAfter = TrafficStats.getUidRxBytes(Process.myUid());
        long uidTxPacketsAfter = TrafficStats.getUidTxPackets(Process.myUid());
        long uidRxPacketsAfter = TrafficStats.getUidRxPackets(Process.myUid());
        long uidTxDeltaBytes = uidTxBytesAfter - uidTxBytesBefore;
        long uidTxDeltaPackets = uidTxPacketsAfter - uidTxPacketsBefore;
        long uidRxDeltaBytes = uidRxBytesAfter - uidRxBytesBefore;
        long uidRxDeltaPackets = uidRxPacketsAfter - uidRxPacketsBefore;

        // Localhost traffic *does* count against per-UID stats.
        /*
         * Calculations:
         *  - bytes
         *   bytes is approx: packets * data + packets * acks;
         *   but sometimes there are less acks than packets, so we set a lower
         *   limit of 1 ack.
         *  - setup/teardown
         *   + 7 approx.: syn, syn-ack, ack, fin-ack, ack, fin-ack, ack;
         *   but sometimes the last find-acks just vanish, so we set a lower limit of +5.
         */
        assertTrue("uidtxp: " + uidTxPacketsBefore + " -> " + uidTxPacketsAfter + " delta=" + uidTxDeltaPackets,
            uidTxDeltaPackets >= packetCount + 5 &&
            uidTxDeltaPackets <= packetCount + packetCount + 7);
        assertTrue("uidrxp: " + uidRxPacketsBefore + " -> " + uidRxPacketsAfter + " delta=" + uidRxDeltaPackets,
            uidRxDeltaPackets >= packetCount + 5 &&
            uidRxDeltaPackets <= packetCount + packetCount + 7);
        assertTrue("uidtxb: " + uidTxBytesBefore + " -> " + uidTxBytesAfter + " delta=" + uidTxDeltaBytes,
            uidTxDeltaBytes >= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(5, 0) &&
            uidTxDeltaBytes <= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(packetCount + 7, 0));
        assertTrue("uidrxb: " + uidRxBytesBefore + " -> " + uidRxBytesAfter + " delta=" + uidRxDeltaBytes,
            uidRxDeltaBytes >= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(5, 0) &&
            uidRxDeltaBytes <= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(packetCount + 7, 0));

        // Localhost traffic *does* count against total stats.
        // Fudge by 132 packets of 1500 bytes not related to the test.
        assertTrue("ttxp: " + totalTxPacketsBefore + " -> " + totalTxPacketsAfter,
            totalTxPacketsAfter >= totalTxPacketsBefore + uidTxDeltaPackets &&
            totalTxPacketsAfter <= totalTxPacketsBefore + uidTxDeltaPackets + 132);
        assertTrue("trxp: " + totalRxPacketsBefore + " -> " + totalRxPacketsAfter,
            totalRxPacketsAfter >= totalRxPacketsBefore + uidRxDeltaPackets &&
            totalRxPacketsAfter <= totalRxPacketsBefore + uidRxDeltaPackets + 132);
        assertTrue("ttxb: " + totalTxBytesBefore + " -> " + totalTxBytesAfter,
            totalTxBytesAfter >= totalTxBytesBefore + uidTxDeltaBytes &&
            totalTxBytesAfter <= totalTxBytesBefore + uidTxDeltaBytes + 132 * 1500);
        assertTrue("trxb: " + totalRxBytesBefore + " -> " + totalRxBytesAfter,
            totalRxBytesAfter >= totalRxBytesBefore + uidRxDeltaBytes &&
            totalRxBytesAfter <= totalRxBytesBefore + uidRxDeltaBytes + 132 * 1500);

        // Localhost traffic should *not* count against mobile stats,
        // There might be some other traffic, but nowhere near 1MB.
        assertTrue("mtxp: " + mobileTxPacketsBefore + " -> " + mobileTxPacketsAfter,
            mobileTxPacketsAfter >= mobileTxPacketsBefore &&
            mobileTxPacketsAfter <= mobileTxPacketsBefore + 500);
        assertTrue("mrxp: " + mobileRxPacketsBefore + " -> " + mobileRxPacketsAfter,
            mobileRxPacketsAfter >= mobileRxPacketsBefore &&
            mobileRxPacketsAfter <= mobileRxPacketsBefore + 500);
        assertTrue("mtxb: " + mobileTxBytesBefore + " -> " + mobileTxBytesAfter,
            mobileTxBytesAfter >= mobileTxBytesBefore &&
            mobileTxBytesAfter <= mobileTxBytesBefore + 200000);
        assertTrue("mrxb: " + mobileRxBytesBefore + " -> " + mobileRxBytesAfter,
            mobileRxBytesAfter >= mobileRxBytesBefore &&
            mobileRxBytesAfter <= mobileRxBytesBefore + 200000);

    }
}
