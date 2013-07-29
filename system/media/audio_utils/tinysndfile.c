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

#include <audio_utils/sndfile.h>
#include <audio_utils/primitives.h>
#include <stdio.h>
#include <string.h>

struct SNDFILE_ {
    int mode;
    uint8_t *temp;  // realloc buffer used for shrinking 16 bits to 8 bits and byte-swapping
    FILE *stream;
    size_t bytesPerFrame;
    size_t remaining;   // frames unread for SFM_READ, frames written for SFM_WRITE
    SF_INFO info;
};

static unsigned little2u(unsigned char *ptr)
{
    return (ptr[1] << 8) + ptr[0];
}

static unsigned little4u(unsigned char *ptr)
{
    return (ptr[3] << 24) + (ptr[2] << 16) + (ptr[1] << 8) + ptr[0];
}

static int isLittleEndian(void)
{
    static const short one = 1;
    return *((const char *) &one) == 1;
}

// "swab" conflicts with OS X <string.h>
static void my_swab(short *ptr, size_t numToSwap)
{
    while (numToSwap > 0) {
        *ptr = little2u((unsigned char *) ptr);
        --numToSwap;
        ++ptr;
    }
}

static SNDFILE *sf_open_read(const char *path, SF_INFO *info)
{
    FILE *stream = fopen(path, "rb");
    if (stream == NULL)
        return NULL;
    // don't attempt to parse all valid forms, just the most common one
    unsigned char wav[44];
    size_t actual;
    actual = fread(wav, sizeof(char), sizeof(wav), stream);
    if (actual != sizeof(wav))
        return NULL;
    for (;;) {
        if (memcmp(wav, "RIFF", 4))
            break;
        unsigned riffSize = little4u(&wav[4]);
        if (riffSize < 36)
            break;
        if (memcmp(&wav[8], "WAVEfmt ", 8))
            break;
        unsigned fmtsize = little4u(&wav[16]);
        if (fmtsize != 16)
            break;
        unsigned format = little2u(&wav[20]);
        if (format != 1)    // PCM
            break;
        unsigned channels = little2u(&wav[22]);
        if (channels != 1 && channels != 2)
            break;
        unsigned samplerate = little4u(&wav[24]);
        if (samplerate == 0)
            break;
        // ignore byte rate
        // ignore block alignment
        unsigned bitsPerSample = little2u(&wav[34]);
        if (bitsPerSample != 8 && bitsPerSample != 16)
            break;
        unsigned bytesPerFrame = (bitsPerSample >> 3) * channels;
        if (memcmp(&wav[36], "data", 4))
            break;
        unsigned dataSize = little4u(&wav[40]);
        SNDFILE *handle = (SNDFILE *) malloc(sizeof(SNDFILE));
        handle->mode = SFM_READ;
        handle->temp = NULL;
        handle->stream = stream;
        handle->bytesPerFrame = bytesPerFrame;
        handle->remaining = dataSize / bytesPerFrame;
        handle->info.frames = handle->remaining;
        handle->info.samplerate = samplerate;
        handle->info.channels = channels;
        handle->info.format = SF_FORMAT_WAV;
        if (bitsPerSample == 8)
            handle->info.format |= SF_FORMAT_PCM_U8;
        else
            handle->info.format |= SF_FORMAT_PCM_16;
        *info = handle->info;
        return handle;
    }
    return NULL;
}

static void write4u(unsigned char *ptr, unsigned u)
{
    ptr[0] = u;
    ptr[1] = u >> 8;
    ptr[2] = u >> 16;
    ptr[3] = u >> 24;
}

static SNDFILE *sf_open_write(const char *path, SF_INFO *info)
{
    if (!(
            (info->samplerate > 0) &&
            (info->channels == 1 || info->channels == 2) &&
            ((info->format & SF_FORMAT_TYPEMASK) == SF_FORMAT_WAV) &&
            ((info->format & SF_FORMAT_SUBMASK) == SF_FORMAT_PCM_16 ||
             (info->format & SF_FORMAT_SUBMASK) == SF_FORMAT_PCM_U8)
          )) {
        return NULL;
    }
    FILE *stream = fopen(path, "w+b");
    unsigned char wav[44];
    memset(wav, 0, sizeof(wav));
    memcpy(wav, "RIFF", 4);
    wav[4] = 36;    // riffSize
    memcpy(&wav[8], "WAVEfmt ", 8);
    wav[16] = 16;   // fmtsize
    wav[20] = 1;    // format = PCM
    wav[22] = info->channels;
    write4u(&wav[24], info->samplerate);
    unsigned bitsPerSample = (info->format & SF_FORMAT_SUBMASK) == SF_FORMAT_PCM_16 ? 16 : 8;
    unsigned blockAlignment = (bitsPerSample >> 3) * info->channels;
    unsigned byteRate = info->samplerate * blockAlignment;
    write4u(&wav[28], byteRate);
    wav[32] = blockAlignment;
    wav[34] = bitsPerSample;
    memcpy(&wav[36], "data", 4);
    // dataSize is initially zero
    (void) fwrite(wav, sizeof(wav), 1, stream);
    SNDFILE *handle = (SNDFILE *) malloc(sizeof(SNDFILE));
    handle->mode = SFM_WRITE;
    handle->temp = NULL;
    handle->stream = stream;
    handle->bytesPerFrame = blockAlignment;
    handle->remaining = 0;
    handle->info = *info;
    return handle;
}

SNDFILE *sf_open(const char *path, int mode, SF_INFO *info)
{
    if (path == NULL || info == NULL)
        return NULL;
    switch (mode) {
    case SFM_READ:
        return sf_open_read(path, info);
    case SFM_WRITE:
        return sf_open_write(path, info);
    default:
        return NULL;
    }
}

void sf_close(SNDFILE *handle)
{
    if (handle == NULL)
        return;
    free(handle->temp);
    if (handle->mode == SFM_WRITE) {
        (void) fflush(handle->stream);
        rewind(handle->stream);
        unsigned char wav[44];
        (void) fread(wav, sizeof(wav), 1, handle->stream);
        unsigned dataSize = handle->remaining * handle->bytesPerFrame;
        write4u(&wav[4], dataSize + 36);    // riffSize
        write4u(&wav[40], dataSize);        // dataSize
        rewind(handle->stream);
        (void) fwrite(wav, sizeof(wav), 1, handle->stream);
    }
    (void) fclose(handle->stream);
    free(handle);
}

sf_count_t sf_readf_short(SNDFILE *handle, short *ptr, sf_count_t desiredFrames)
{
    if (handle == NULL || handle->mode != SFM_READ || ptr == NULL || !handle->remaining ||
            desiredFrames <= 0) {
        return 0;
    }
    if (handle->remaining < (size_t) desiredFrames)
        desiredFrames = handle->remaining;
    size_t desiredBytes = desiredFrames * handle->bytesPerFrame;
    // does not check for numeric overflow
    size_t actualBytes = fread(ptr, sizeof(char), desiredBytes, handle->stream);
    size_t actualFrames = actualBytes / handle->bytesPerFrame;
    handle->remaining -= actualFrames;
    switch (handle->info.format & SF_FORMAT_SUBMASK) {
    case SF_FORMAT_PCM_U8:
        memcpy_to_i16_from_u8(ptr, (unsigned char *) ptr, actualFrames * handle->info.channels);
        break;
    case SF_FORMAT_PCM_16:
        if (!isLittleEndian())
            my_swab(ptr, actualFrames * handle->info.channels);
        break;
    }
    return actualFrames;
}

sf_count_t sf_writef_short(SNDFILE *handle, const short *ptr, sf_count_t desiredFrames)
{
    if (handle == NULL || handle->mode != SFM_WRITE || ptr == NULL || desiredFrames <= 0)
        return 0;
    size_t desiredBytes = desiredFrames * handle->bytesPerFrame;
    size_t actualBytes = 0;
    switch (handle->info.format & SF_FORMAT_SUBMASK) {
    case SF_FORMAT_PCM_U8:
        handle->temp = realloc(handle->temp, desiredBytes);
        memcpy_to_u8_from_i16(handle->temp, ptr, desiredBytes);
        actualBytes = fwrite(handle->temp, sizeof(char), desiredBytes, handle->stream);
        break;
    case SF_FORMAT_PCM_16:
        // does not check for numeric overflow
        if (isLittleEndian()) {
            actualBytes = fwrite(ptr, sizeof(char), desiredBytes, handle->stream);
        } else {
            handle->temp = realloc(handle->temp, desiredBytes);
            memcpy(handle->temp, ptr, desiredBytes);
            my_swab((short *) handle->temp, desiredFrames * handle->info.channels);
            actualBytes = fwrite(handle->temp, sizeof(char), desiredBytes, handle->stream);
        }
        break;
    }
    size_t actualFrames = actualBytes / handle->bytesPerFrame;
    handle->remaining += actualFrames;
    return actualFrames;
}
