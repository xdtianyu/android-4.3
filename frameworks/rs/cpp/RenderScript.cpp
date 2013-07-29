/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <malloc.h>
#include <string.h>
#include <pthread.h>

#include "RenderScript.h"
#include "rs.h"

using namespace android;
using namespace RSC;

bool RS::gInitialized = false;
pthread_mutex_t RS::gInitMutex = PTHREAD_MUTEX_INITIALIZER;

RS::RS() {
    mDev = NULL;
    mContext = NULL;
    mErrorFunc = NULL;
    mMessageFunc = NULL;
    mMessageRun = false;

    memset(&mElements, 0, sizeof(mElements));
}

RS::~RS() {
    mMessageRun = false;

    rsContextDeinitToClient(mContext);

    void *res = NULL;
    int status = pthread_join(mMessageThreadId, &res);

    rsContextDestroy(mContext);
    mContext = NULL;
    rsDeviceDestroy(mDev);
    mDev = NULL;
}

bool RS::init(bool forceCpu, bool synchronous) {
    return RS::init(RS_VERSION, forceCpu, synchronous);
}

bool RS::init(int targetApi, bool forceCpu, bool synchronous) {
    mDev = rsDeviceCreate();
    if (mDev == 0) {
        ALOGE("Device creation failed");
        return false;
    }

    mContext = rsContextCreate(mDev, 0, targetApi, RS_CONTEXT_TYPE_NORMAL, forceCpu, synchronous);
    if (mContext == 0) {
        ALOGE("Context creation failed");
        return false;
    }

    pid_t mNativeMessageThreadId;

    int status = pthread_create(&mMessageThreadId, NULL, threadProc, this);
    if (status) {
        ALOGE("Failed to start RS message thread.");
        return false;
    }
    // Wait for the message thread to be active.
    while (!mMessageRun) {
        usleep(1000);
    }

    return true;
}

void RS::throwError(const char *err) const {
    ALOGE("RS CPP error: %s", err);
    int * v = NULL;
    v[0] = 0;
}


void * RS::threadProc(void *vrsc) {
    RS *rs = static_cast<RS *>(vrsc);
    size_t rbuf_size = 256;
    void * rbuf = malloc(rbuf_size);

    rsContextInitToClient(rs->mContext);
    rs->mMessageRun = true;

    while (rs->mMessageRun) {
        size_t receiveLen = 0;
        uint32_t usrID = 0;
        uint32_t subID = 0;
        RsMessageToClientType r = rsContextPeekMessage(rs->mContext,
                                                       &receiveLen, sizeof(receiveLen),
                                                       &usrID, sizeof(usrID));

        if (receiveLen >= rbuf_size) {
            rbuf_size = receiveLen + 32;
            rbuf = realloc(rbuf, rbuf_size);
        }
        if (!rbuf) {
            ALOGE("RS::message handler realloc error %zu", rbuf_size);
            // No clean way to recover now?
        }
        rsContextGetMessage(rs->mContext, rbuf, rbuf_size, &receiveLen, sizeof(receiveLen),
                            &subID, sizeof(subID));

        switch(r) {
        case RS_MESSAGE_TO_CLIENT_ERROR:
            ALOGE("RS Error %s", (const char *)rbuf);

            if(rs->mMessageFunc != NULL) {
                rs->mErrorFunc(usrID, (const char *)rbuf);
            }
            break;
        case RS_MESSAGE_TO_CLIENT_NONE:
        case RS_MESSAGE_TO_CLIENT_EXCEPTION:
        case RS_MESSAGE_TO_CLIENT_RESIZE:
            // teardown. But we want to avoid starving other threads during
            // teardown by yielding until the next line in the destructor can
            // execute to set mRun = false. Note that the FIFO sends an
            // empty NONE message when it reaches its destructor.
            usleep(1000);
            break;
        case RS_MESSAGE_TO_CLIENT_USER:
            if(rs->mMessageFunc != NULL) {
                rs->mMessageFunc(usrID, rbuf, receiveLen);
            } else {
                ALOGE("Received a message from the script with no message handler installed.");
            }
            break;

        default:
            ALOGE("RS unknown message type %i", r);
        }
    }

    if (rbuf) {
        free(rbuf);
    }
    ALOGE("RS Message thread exiting.");
    return NULL;
}

void RS::setErrorHandler(ErrorHandlerFunc_t func) {
    mErrorFunc = func;
}

void RS::setMessageHandler(MessageHandlerFunc_t func) {
    mMessageFunc  = func;
}

void RS::finish() {
    rsContextFinish(mContext);
}
