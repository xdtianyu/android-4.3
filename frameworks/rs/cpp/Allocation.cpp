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

#include "RenderScript.h"
#include <rs.h>

using namespace android;
using namespace RSC;

void * Allocation::getIDSafe() const {
    return getID();
}

void Allocation::updateCacheInfo(sp<const Type> t) {
    mCurrentDimX = t->getX();
    mCurrentDimY = t->getY();
    mCurrentDimZ = t->getZ();
    mCurrentCount = mCurrentDimX;
    if (mCurrentDimY > 1) {
        mCurrentCount *= mCurrentDimY;
    }
    if (mCurrentDimZ > 1) {
        mCurrentCount *= mCurrentDimZ;
    }
}

Allocation::Allocation(void *id, sp<RS> rs, sp<const Type> t, uint32_t usage) :
    BaseObj(id, rs), mSelectedY(0), mSelectedZ(0), mSelectedLOD(0),
    mSelectedFace(RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X) {

    if ((usage & ~(RS_ALLOCATION_USAGE_SCRIPT |
                   RS_ALLOCATION_USAGE_GRAPHICS_TEXTURE |
                   RS_ALLOCATION_USAGE_GRAPHICS_VERTEX |
                   RS_ALLOCATION_USAGE_GRAPHICS_CONSTANTS |
                   RS_ALLOCATION_USAGE_GRAPHICS_RENDER_TARGET |
                   RS_ALLOCATION_USAGE_IO_INPUT |
                   RS_ALLOCATION_USAGE_IO_OUTPUT |
                   RS_ALLOCATION_USAGE_SHARED)) != 0) {
        ALOGE("Unknown usage specified.");
    }

    if ((usage & RS_ALLOCATION_USAGE_IO_INPUT) != 0) {
        mWriteAllowed = false;
        if ((usage & ~(RS_ALLOCATION_USAGE_IO_INPUT |
                       RS_ALLOCATION_USAGE_GRAPHICS_TEXTURE |
                       RS_ALLOCATION_USAGE_SCRIPT)) != 0) {
            ALOGE("Invalid usage combination.");
        }
    }

    mType = t;
    mUsage = usage;

    if (t.get() != NULL) {
        updateCacheInfo(t);
    }
}

void Allocation::validateIsInt32() {
    RsDataType dt = mType->getElement()->getDataType();
    if ((dt == RS_TYPE_SIGNED_32) || (dt == RS_TYPE_UNSIGNED_32)) {
        return;
    }
    ALOGE("32 bit integer source does not match allocation type %i", dt);
}

void Allocation::validateIsInt16() {
    RsDataType dt = mType->getElement()->getDataType();
    if ((dt == RS_TYPE_SIGNED_16) || (dt == RS_TYPE_UNSIGNED_16)) {
        return;
    }
    ALOGE("16 bit integer source does not match allocation type %i", dt);
}

void Allocation::validateIsInt8() {
    RsDataType dt = mType->getElement()->getDataType();
    if ((dt == RS_TYPE_SIGNED_8) || (dt == RS_TYPE_UNSIGNED_8)) {
        return;
    }
    ALOGE("8 bit integer source does not match allocation type %i", dt);
}

void Allocation::validateIsFloat32() {
    RsDataType dt = mType->getElement()->getDataType();
    if (dt == RS_TYPE_FLOAT_32) {
        return;
    }
    ALOGE("32 bit float source does not match allocation type %i", dt);
}

void Allocation::validateIsObject() {
    RsDataType dt = mType->getElement()->getDataType();
    if ((dt == RS_TYPE_ELEMENT) ||
        (dt == RS_TYPE_TYPE) ||
        (dt == RS_TYPE_ALLOCATION) ||
        (dt == RS_TYPE_SAMPLER) ||
        (dt == RS_TYPE_SCRIPT) ||
        (dt == RS_TYPE_MESH) ||
        (dt == RS_TYPE_PROGRAM_FRAGMENT) ||
        (dt == RS_TYPE_PROGRAM_VERTEX) ||
        (dt == RS_TYPE_PROGRAM_RASTER) ||
        (dt == RS_TYPE_PROGRAM_STORE)) {
        return;
    }
    ALOGE("Object source does not match allocation type %i", dt);
}

void Allocation::updateFromNative() {
    BaseObj::updateFromNative();

    const void *typeID = rsaAllocationGetType(mRS->getContext(), getID());
    if(typeID != NULL) {
        sp<const Type> old = mType;
        sp<Type> t = new Type((void *)typeID, mRS);
        t->updateFromNative();
        updateCacheInfo(t);
        mType = t;
    }
}

void Allocation::syncAll(RsAllocationUsageType srcLocation) {
    switch (srcLocation) {
    case RS_ALLOCATION_USAGE_SCRIPT:
    case RS_ALLOCATION_USAGE_GRAPHICS_CONSTANTS:
    case RS_ALLOCATION_USAGE_GRAPHICS_TEXTURE:
    case RS_ALLOCATION_USAGE_GRAPHICS_VERTEX:
        break;
    default:
        ALOGE("Source must be exactly one usage type.");
    }
    rsAllocationSyncAll(mRS->getContext(), getIDSafe(), srcLocation);
}

void Allocation::ioSendOutput() {
#ifndef RS_COMPATIBILITY_LIB
    if ((mUsage & RS_ALLOCATION_USAGE_IO_OUTPUT) == 0) {
        ALOGE("Can only send buffer if IO_OUTPUT usage specified.");
    }
    rsAllocationIoSend(mRS->getContext(), getID());
#endif
}

void Allocation::ioGetInput() {
#ifndef RS_COMPATIBILITY_LIB
    if ((mUsage & RS_ALLOCATION_USAGE_IO_INPUT) == 0) {
        ALOGE("Can only send buffer if IO_OUTPUT usage specified.");
    }
    rsAllocationIoReceive(mRS->getContext(), getID());
#endif
}

void Allocation::generateMipmaps() {
    rsAllocationGenerateMipmaps(mRS->getContext(), getID());
}

void Allocation::copy1DRangeFrom(uint32_t off, size_t count, const void *data) {

    if(count < 1) {
        ALOGE("Count must be >= 1.");
        return;
    }
    if((off + count) > mCurrentCount) {
        ALOGE("Overflow, Available count %zu, got %zu at offset %zu.", mCurrentCount, count, off);
        return;
    }

    rsAllocation1DData(mRS->getContext(), getIDSafe(), off, mSelectedLOD, count, data,
                       count * mType->getElement()->getSizeBytes());
}

void Allocation::copy1DRangeTo(uint32_t off, size_t count, void *data) {
    if(count < 1) {
        ALOGE("Count must be >= 1.");
        return;
    }
    if((off + count) > mCurrentCount) {
        ALOGE("Overflow, Available count %zu, got %zu at offset %zu.", mCurrentCount, count, off);
        return;
    }

    rsAllocation1DRead(mRS->getContext(), getIDSafe(), off, mSelectedLOD, count, data,
                       count * mType->getElement()->getSizeBytes());
}

void Allocation::copy1DRangeFrom(uint32_t off, size_t count, sp<const Allocation> data,
                                 uint32_t dataOff) {

    rsAllocationCopy2DRange(mRS->getContext(), getIDSafe(), off, 0,
                            mSelectedLOD, mSelectedFace,
                            count, 1, data->getIDSafe(), dataOff, 0,
                            data->mSelectedLOD, data->mSelectedFace);
}

void Allocation::copy1DFrom(const void* data) {
    copy1DRangeFrom(0, mCurrentCount, data);
}

void Allocation::copy1DTo(void* data) {
    copy1DRangeTo(0, mCurrentCount, data);
}


void Allocation::validate2DRange(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h) {
    if (mAdaptedAllocation != NULL) {

    } else {
        if (((xoff + w) > mCurrentDimX) || ((yoff + h) > mCurrentDimY)) {
            ALOGE("Updated region larger than allocation.");
        }
    }
}

void Allocation::copy2DRangeFrom(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                                 const void *data) {
    validate2DRange(xoff, yoff, w, h);
    rsAllocation2DData(mRS->getContext(), getIDSafe(), xoff, yoff, mSelectedLOD, mSelectedFace,
                       w, h, data, w * h * mType->getElement()->getSizeBytes(), w * mType->getElement()->getSizeBytes());
}

void Allocation::copy2DRangeFrom(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                                 sp<const Allocation> data, uint32_t dataXoff, uint32_t dataYoff) {
    validate2DRange(xoff, yoff, w, h);
    rsAllocationCopy2DRange(mRS->getContext(), getIDSafe(), xoff, yoff,
                            mSelectedLOD, mSelectedFace,
                            w, h, data->getIDSafe(), dataXoff, dataYoff,
                            data->mSelectedLOD, data->mSelectedFace);
}

void Allocation::copy2DRangeTo(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                               void* data) {
    validate2DRange(xoff, yoff, w, h);
    rsAllocation2DRead(mRS->getContext(), getIDSafe(), xoff, yoff, mSelectedLOD, mSelectedFace,
                       w, h, data, w * h * mType->getElement()->getSizeBytes(), w * mType->getElement()->getSizeBytes());
}

void Allocation::copy2DStridedFrom(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                                   const void *data, size_t stride) {
    validate2DRange(xoff, yoff, w, h);
    rsAllocation2DData(mRS->getContext(), getIDSafe(), xoff, yoff, mSelectedLOD, mSelectedFace,
                       w, h, data, w * h * mType->getElement()->getSizeBytes(), stride);
}

void Allocation::copy2DStridedFrom(const void* data, size_t stride) {
    copy2DStridedFrom(0, 0, mCurrentDimX, mCurrentDimY, data, stride);
}

void Allocation::copy2DStridedTo(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                                 void *data, size_t stride) {
    validate2DRange(xoff, yoff, w, h);
    rsAllocation2DRead(mRS->getContext(), getIDSafe(), xoff, yoff, mSelectedLOD, mSelectedFace,
                       w, h, data, w * h * mType->getElement()->getSizeBytes(), stride);
}

void Allocation::copy2DStridedTo(void* data, size_t stride) {
    copy2DStridedTo(0, 0, mCurrentDimX, mCurrentDimY, data, stride);
}


/*
void resize(int dimX) {
    if ((mType.getY() > 0)|| (mType.getZ() > 0) || mType.hasFaces() || mType.hasMipmaps()) {
        throw new RSInvalidStateException("Resize only support for 1D allocations at this time.");
    }
    mRS.nAllocationResize1D(getID(), dimX);
    mRS.finish();  // Necessary because resize is fifoed and update is async.

    int typeID = mRS.nAllocationGetType(getID());
    mType = new Type(typeID, mRS);
    mType.updateFromNative();
    updateCacheInfo(mType);
}

void resize(int dimX, int dimY) {
    if ((mType.getZ() > 0) || mType.hasFaces() || mType.hasMipmaps()) {
        throw new RSInvalidStateException(
            "Resize only support for 2D allocations at this time.");
    }
    if (mType.getY() == 0) {
        throw new RSInvalidStateException(
            "Resize only support for 2D allocations at this time.");
    }
    mRS.nAllocationResize2D(getID(), dimX, dimY);
    mRS.finish();  // Necessary because resize is fifoed and update is async.

    int typeID = mRS.nAllocationGetType(getID());
    mType = new Type(typeID, mRS);
    mType.updateFromNative();
    updateCacheInfo(mType);
}
*/


android::sp<Allocation> Allocation::createTyped(sp<RS> rs, sp<const Type> type,
                                                RsAllocationMipmapControl mips, uint32_t usage) {
    void *id = rsAllocationCreateTyped(rs->getContext(), type->getID(), mips, usage, 0);
    if (id == 0) {
        ALOGE("Allocation creation failed.");
        return NULL;
    }
    return new Allocation(id, rs, type, usage);
}

android::sp<Allocation> Allocation::createTyped(sp<RS> rs, sp<const Type> type,
                                                RsAllocationMipmapControl mips, uint32_t usage,
                                                void *pointer) {
    void *id = rsAllocationCreateTyped(rs->getContext(), type->getID(), mips, usage,
                                       (uintptr_t)pointer);
    if (id == 0) {
        ALOGE("Allocation creation failed.");
    }
    return new Allocation(id, rs, type, usage);
}

android::sp<Allocation> Allocation::createTyped(sp<RS> rs, sp<const Type> type,
                                                uint32_t usage) {
    return createTyped(rs, type, RS_ALLOCATION_MIPMAP_NONE, usage);
}

android::sp<Allocation> Allocation::createSized(sp<RS> rs, sp<const Element> e,
                                                size_t count, uint32_t usage) {
    Type::Builder b(rs, e);
    b.setX(count);
    sp<const Type> t = b.create();

    return createTyped(rs, t, usage);
}

android::sp<Allocation> Allocation::createSized2D(sp<RS> rs, sp<const Element> e,
                                                  size_t x, size_t y, uint32_t usage) {
    Type::Builder b(rs, e);
    b.setX(x);
    b.setY(y);
    sp<const Type> t = b.create();

    return createTyped(rs, t, usage);
}
