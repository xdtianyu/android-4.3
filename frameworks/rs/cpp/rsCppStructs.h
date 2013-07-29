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

#ifndef ANDROID_RSCPPSTRUCTS_H
#define ANDROID_RSCPPSTRUCTS_H

#include "rsCppUtils.h"
#ifndef RS_SERVER
#include "utils/RefBase.h"
#else
#include "RefBase.h"
#endif

// Every row in an RS allocation is guaranteed to be aligned by this amount
// Every row in a user-backed allocation must be aligned by this amount
#define RS_CPU_ALLOCATION_ALIGNMENT 16

namespace android {
namespace RSC {

typedef void (*ErrorHandlerFunc_t)(uint32_t errorNum, const char *errorText);
typedef void (*MessageHandlerFunc_t)(uint32_t msgNum, const void *msgData, size_t msgLen);

class RS;
class BaseObj;
class Element;
class Type;
class Allocation;
class Script;
class ScriptC;

class RS : public android::LightRefBase<RS> {

 public:
    RS();
    virtual ~RS();

    bool init(bool forceCpu = false, bool synchronous = false);

    void setErrorHandler(ErrorHandlerFunc_t func);
    ErrorHandlerFunc_t getErrorHandler() { return mErrorFunc; }

    void setMessageHandler(MessageHandlerFunc_t func);
    MessageHandlerFunc_t getMessageHandler() { return mMessageFunc; }

    void throwError(const char *err) const;

    RsContext getContext() { return mContext; }

    void finish();

 private:
    bool init(int targetApi, bool forceCpu, bool synchronous);
    static void * threadProc(void *);

    static bool gInitialized;
    static pthread_mutex_t gInitMutex;

    pthread_t mMessageThreadId;
    pid_t mNativeMessageThreadId;
    bool mMessageRun;

    RsDevice mDev;
    RsContext mContext;

    ErrorHandlerFunc_t mErrorFunc;
    MessageHandlerFunc_t mMessageFunc;

    struct {
        Element *U8;
        Element *I8;
        Element *U16;
        Element *I16;
        Element *U32;
        Element *I32;
        Element *U64;
        Element *I64;
        Element *F32;
        Element *F64;
        Element *BOOLEAN;

        Element *ELEMENT;
        Element *TYPE;
        Element *ALLOCATION;
        Element *SAMPLER;
        Element *SCRIPT;
        Element *MESH;
        Element *PROGRAM_FRAGMENT;
        Element *PROGRAM_VERTEX;
        Element *PROGRAM_RASTER;
        Element *PROGRAM_STORE;

        Element *A_8;
        Element *RGB_565;
        Element *RGB_888;
        Element *RGBA_5551;
        Element *RGBA_4444;
        Element *RGBA_8888;

        Element *FLOAT_2;
        Element *FLOAT_3;
        Element *FLOAT_4;

        Element *DOUBLE_2;
        Element *DOUBLE_3;
        Element *DOUBLE_4;

        Element *UCHAR_2;
        Element *UCHAR_3;
        Element *UCHAR_4;

        Element *CHAR_2;
        Element *CHAR_3;
        Element *CHAR_4;

        Element *USHORT_2;
        Element *USHORT_3;
        Element *USHORT_4;

        Element *SHORT_2;
        Element *SHORT_3;
        Element *SHORT_4;

        Element *UINT_2;
        Element *UINT_3;
        Element *UINT_4;

        Element *INT_2;
        Element *INT_3;
        Element *INT_4;

        Element *ULONG_2;
        Element *ULONG_3;
        Element *ULONG_4;

        Element *LONG_2;
        Element *LONG_3;
        Element *LONG_4;

        Element *MATRIX_4X4;
        Element *MATRIX_3X3;
        Element *MATRIX_2X2;
    } mElements;

};

class BaseObj : public android::LightRefBase<BaseObj> {
protected:
    void *mID;
    sp<RS> mRS;
    String8 mName;

    BaseObj(void *id, sp<RS> rs);
    void checkValid();

    static void * getObjID(sp<const BaseObj> o);

public:

    void * getID() const;
    virtual ~BaseObj();
    virtual void updateFromNative();
    virtual bool equals(const BaseObj *obj);
};


class Allocation : public BaseObj {
protected:
    android::sp<const Type> mType;
    uint32_t mUsage;
    android::sp<Allocation> mAdaptedAllocation;

    bool mConstrainedLOD;
    bool mConstrainedFace;
    bool mConstrainedY;
    bool mConstrainedZ;
    bool mReadAllowed;
    bool mWriteAllowed;
    uint32_t mSelectedY;
    uint32_t mSelectedZ;
    uint32_t mSelectedLOD;
    RsAllocationCubemapFace mSelectedFace;

    uint32_t mCurrentDimX;
    uint32_t mCurrentDimY;
    uint32_t mCurrentDimZ;
    uint32_t mCurrentCount;

    void * getIDSafe() const;
    void updateCacheInfo(sp<const Type> t);

    Allocation(void *id, sp<RS> rs, sp<const Type> t, uint32_t usage);

    void validateIsInt32();
    void validateIsInt16();
    void validateIsInt8();
    void validateIsFloat32();
    void validateIsObject();

    virtual void updateFromNative();

    void validate2DRange(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h);

public:
    android::sp<const Type> getType() {
        return mType;
    }

    void syncAll(RsAllocationUsageType srcLocation);
    void ioSendOutput();
    void ioGetInput();

    void generateMipmaps();

    void copy1DRangeFrom(uint32_t off, size_t count, const void *data);
    void copy1DRangeFrom(uint32_t off, size_t count, sp<const Allocation> data, uint32_t dataOff);

    void copy1DRangeTo(uint32_t off, size_t count, void *data);

    void copy1DFrom(const void* data);
    void copy1DTo(void* data);

    void copy2DRangeFrom(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                         const void *data);

    void copy2DRangeTo(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                       void *data);

    void copy2DRangeFrom(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                         sp<const Allocation> data, uint32_t dataXoff, uint32_t dataYoff);

    void copy2DStridedFrom(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                           const void *data, size_t stride);
    void copy2DStridedFrom(const void *data, size_t stride);

    void copy2DStridedTo(uint32_t xoff, uint32_t yoff, uint32_t w, uint32_t h,
                         void *data, size_t stride);
    void copy2DStridedTo(void *data, size_t stride);

    void resize(int dimX);
    void resize(int dimX, int dimY);

    static sp<Allocation> createTyped(sp<RS> rs, sp<const Type> type,
                                   RsAllocationMipmapControl mips, uint32_t usage);
    static sp<Allocation> createTyped(sp<RS> rs, sp<const Type> type,
                                   RsAllocationMipmapControl mips, uint32_t usage, void * pointer);

    static sp<Allocation> createTyped(sp<RS> rs, sp<const Type> type,
                                   uint32_t usage = RS_ALLOCATION_USAGE_SCRIPT);
    static sp<Allocation> createSized(sp<RS> rs, sp<const Element> e, size_t count,
                                   uint32_t usage = RS_ALLOCATION_USAGE_SCRIPT);
    static sp<Allocation> createSized2D(sp<RS> rs, sp<const Element> e,
                                        size_t x, size_t y,
                                        uint32_t usage = RS_ALLOCATION_USAGE_SCRIPT);


};

class Element : public BaseObj {
public:
    bool isComplex();
    size_t getSubElementCount() {
        return mVisibleElementMap.size();
    }

    sp<const Element> getSubElement(uint32_t index);
    const char * getSubElementName(uint32_t index);
    size_t getSubElementArraySize(uint32_t index);
    uint32_t getSubElementOffsetBytes(uint32_t index);
    RsDataType getDataType() const {
        return mType;
    }

    RsDataKind getDataKind() const {
        return mKind;
    }

    size_t getSizeBytes() const {
        return mSizeBytes;
    }

    static sp<const Element> BOOLEAN(sp<RS> rs);
    static sp<const Element> U8(sp<RS> rs);
    static sp<const Element> I8(sp<RS> rs);
    static sp<const Element> U16(sp<RS> rs);
    static sp<const Element> I16(sp<RS> rs);
    static sp<const Element> U32(sp<RS> rs);
    static sp<const Element> I32(sp<RS> rs);
    static sp<const Element> U64(sp<RS> rs);
    static sp<const Element> I64(sp<RS> rs);
    static sp<const Element> F32(sp<RS> rs);
    static sp<const Element> F64(sp<RS> rs);
    static sp<const Element> ELEMENT(sp<RS> rs);
    static sp<const Element> TYPE(sp<RS> rs);
    static sp<const Element> ALLOCATION(sp<RS> rs);
    static sp<const Element> SAMPLER(sp<RS> rs);
    static sp<const Element> SCRIPT(sp<RS> rs);
    static sp<const Element> MESH(sp<RS> rs);
    static sp<const Element> PROGRAM_FRAGMENT(sp<RS> rs);
    static sp<const Element> PROGRAM_VERTEX(sp<RS> rs);
    static sp<const Element> PROGRAM_RASTER(sp<RS> rs);
    static sp<const Element> PROGRAM_STORE(sp<RS> rs);

    static sp<const Element> A_8(sp<RS> rs);
    static sp<const Element> RGB_565(sp<RS> rs);
    static sp<const Element> RGB_888(sp<RS> rs);
    static sp<const Element> RGBA_5551(sp<RS> rs);
    static sp<const Element> RGBA_4444(sp<RS> rs);
    static sp<const Element> RGBA_8888(sp<RS> rs);

    static sp<const Element> F32_2(sp<RS> rs);
    static sp<const Element> F32_3(sp<RS> rs);
    static sp<const Element> F32_4(sp<RS> rs);
    static sp<const Element> F64_2(sp<RS> rs);
    static sp<const Element> F64_3(sp<RS> rs);
    static sp<const Element> F64_4(sp<RS> rs);
    static sp<const Element> U8_2(sp<RS> rs);
    static sp<const Element> U8_3(sp<RS> rs);
    static sp<const Element> U8_4(sp<RS> rs);
    static sp<const Element> I8_2(sp<RS> rs);
    static sp<const Element> I8_3(sp<RS> rs);
    static sp<const Element> I8_4(sp<RS> rs);
    static sp<const Element> U16_2(sp<RS> rs);
    static sp<const Element> U16_3(sp<RS> rs);
    static sp<const Element> U16_4(sp<RS> rs);
    static sp<const Element> I16_2(sp<RS> rs);
    static sp<const Element> I16_3(sp<RS> rs);
    static sp<const Element> I16_4(sp<RS> rs);
    static sp<const Element> U32_2(sp<RS> rs);
    static sp<const Element> U32_3(sp<RS> rs);
    static sp<const Element> U32_4(sp<RS> rs);
    static sp<const Element> I32_2(sp<RS> rs);
    static sp<const Element> I32_3(sp<RS> rs);
    static sp<const Element> I32_4(sp<RS> rs);
    static sp<const Element> U64_2(sp<RS> rs);
    static sp<const Element> U64_3(sp<RS> rs);
    static sp<const Element> U64_4(sp<RS> rs);
    static sp<const Element> I64_2(sp<RS> rs);
    static sp<const Element> I64_3(sp<RS> rs);
    static sp<const Element> I64_4(sp<RS> rs);
    static sp<const Element> MATRIX_4X4(sp<RS> rs);
    static sp<const Element> MATRIX_3X3(sp<RS> rs);
    static sp<const Element> MATRIX_2X2(sp<RS> rs);

    Element(void *id, sp<RS> rs,
            android::Vector<sp<Element> > &elements,
            android::Vector<android::String8> &elementNames,
            android::Vector<uint32_t> &arraySizes);
    Element(void *id, sp<RS> rs, RsDataType dt, RsDataKind dk, bool norm, uint32_t size);
    Element(sp<RS> rs);
    virtual ~Element();

    void updateFromNative();
    static sp<const Element> createUser(sp<RS> rs, RsDataType dt);
    static sp<const Element> createVector(sp<RS> rs, RsDataType dt, uint32_t size);
    static sp<const Element> createPixel(sp<RS> rs, RsDataType dt, RsDataKind dk);
    bool isCompatible(sp<const Element>e);

    class Builder {
    private:
        sp<RS> mRS;
        android::Vector<sp<Element> > mElements;
        android::Vector<android::String8> mElementNames;
        android::Vector<uint32_t> mArraySizes;
        bool mSkipPadding;

    public:
        Builder(sp<RS> rs);
        ~Builder();
        void add(sp<Element>, android::String8 &name, uint32_t arraySize = 1);
        sp<const Element> create();
    };

private:
    void updateVisibleSubElements();

    android::Vector<sp</*const*/ Element> > mElements;
    android::Vector<android::String8> mElementNames;
    android::Vector<uint32_t> mArraySizes;
    android::Vector<uint32_t> mVisibleElementMap;
    android::Vector<uint32_t> mOffsetInBytes;

    RsDataType mType;
    RsDataKind mKind;
    bool mNormalized;
    size_t mSizeBytes;
    size_t mVectorSize;
};

class FieldPacker {
protected:
    unsigned char* mData;
    size_t mPos;
    size_t mLen;

public:
    FieldPacker(size_t len)
        : mPos(0),
          mLen(len) {
        mData = new unsigned char[len];
    }

    virtual ~FieldPacker() {
        delete [] mData;
    }

    void align(size_t v) {
        if ((v & (v - 1)) != 0) {
            ALOGE("Non-power-of-two alignment: %zu", v);
            return;
        }

        while ((mPos & (v - 1)) != 0) {
            mData[mPos++] = 0;
        }
    }

    void reset() {
        mPos = 0;
    }

    void reset(size_t i) {
        if (i >= mLen) {
            ALOGE("Out of bounds: i (%zu) >= len (%zu)", i, mLen);
            return;
        }
        mPos = i;
    }

    void skip(size_t i) {
        size_t res = mPos + i;
        if (res > mLen) {
            ALOGE("Exceeded buffer length: i (%zu) > len (%zu)", i, mLen);
            return;
        }
        mPos = res;
    }

    void* getData() const {
        return mData;
    }

    size_t getLength() const {
        return mLen;
    }

    template <typename T>
    void add(T t) {
        align(sizeof(t));
        if (mPos + sizeof(t) <= mLen) {
            memcpy(&mData[mPos], &t, sizeof(t));
            mPos += sizeof(t);
        }
    }

    /*
    void add(rs_matrix4x4 m) {
        for (size_t i = 0; i < 16; i++) {
            add(m.m[i]);
        }
    }

    void add(rs_matrix3x3 m) {
        for (size_t i = 0; i < 9; i++) {
            add(m.m[i]);
        }
    }

    void add(rs_matrix2x2 m) {
        for (size_t i = 0; i < 4; i++) {
            add(m.m[i]);
        }
    }
    */

    void add(BaseObj* obj) {
        if (obj != NULL) {
            add((uint32_t) (uintptr_t) obj->getID());
        } else {
            add((uint32_t) 0);
        }
    }
};

class Type : public BaseObj {
protected:
    friend class Allocation;

    uint32_t mDimX;
    uint32_t mDimY;
    uint32_t mDimZ;
    bool mDimMipmaps;
    bool mDimFaces;
    size_t mElementCount;
    sp<const Element> mElement;

    void calcElementCount();
    virtual void updateFromNative();

public:

    sp<const Element> getElement() const {
        return mElement;
    }

    uint32_t getX() const {
        return mDimX;
    }

    uint32_t getY() const {
        return mDimY;
    }

    uint32_t getZ() const {
        return mDimZ;
    }

    bool hasMipmaps() const {
        return mDimMipmaps;
    }

    bool hasFaces() const {
        return mDimFaces;
    }

    size_t getCount() const {
        return mElementCount;
    }

    size_t getSizeBytes() const {
        return mElementCount * mElement->getSizeBytes();
    }

    Type(void *id, sp<RS> rs);

    static sp<const Type> create(sp<RS> rs, sp<const Element> e, uint32_t dimX, uint32_t dimY, uint32_t dimZ);

    class Builder {
    protected:
        sp<RS> mRS;
        uint32_t mDimX;
        uint32_t mDimY;
        uint32_t mDimZ;
        bool mDimMipmaps;
        bool mDimFaces;
        sp<const Element> mElement;

    public:
        Builder(sp<RS> rs, sp<const Element> e);

        void setX(uint32_t value);
        void setY(int value);
        void setMipmaps(bool value);
        void setFaces(bool value);
        sp<const Type> create();
    };

};

class Script : public BaseObj {
private:

protected:
    Script(void *id, sp<RS> rs);
    void forEach(uint32_t slot, sp<const Allocation> in, sp<const Allocation> out,
            const void *v, size_t) const;
    void bindAllocation(sp<Allocation> va, uint32_t slot) const;
    void setVar(uint32_t index, const void *, size_t len) const;
    void setVar(uint32_t index, sp<const BaseObj> o) const;
    void invoke(uint32_t slot, const void *v, size_t len) const;


    void invoke(uint32_t slot) const {
        invoke(slot, NULL, 0);
    }
    void setVar(uint32_t index, float v) const {
        setVar(index, &v, sizeof(v));
    }
    void setVar(uint32_t index, double v) const {
        setVar(index, &v, sizeof(v));
    }
    void setVar(uint32_t index, int32_t v) const {
        setVar(index, &v, sizeof(v));
    }
    void setVar(uint32_t index, int64_t v) const {
        setVar(index, &v, sizeof(v));
    }
    void setVar(uint32_t index, bool v) const {
        setVar(index, &v, sizeof(v));
    }

public:
    class FieldBase {
    protected:
        sp<const Element> mElement;
        sp<Allocation> mAllocation;

        void init(sp<RS> rs, uint32_t dimx, uint32_t usages = 0);

    public:
        sp<const Element> getElement() {
            return mElement;
        }

        sp<const Type> getType() {
            return mAllocation->getType();
        }

        sp<const Allocation> getAllocation() {
            return mAllocation;
        }

        //void updateAllocation();
    };
};

class ScriptC : public Script {
protected:
    ScriptC(sp<RS> rs,
            const void *codeTxt, size_t codeLength,
            const char *cachedName, size_t cachedNameLength,
            const char *cacheDir, size_t cacheDirLength);

};

class ScriptIntrinsic : public Script {
 protected:
    ScriptIntrinsic(sp<RS> rs, int id, sp<const Element> e);
};

class ScriptIntrinsicBlend : public ScriptIntrinsic {
 public:
    ScriptIntrinsicBlend(sp<RS> rs, sp <const Element> e);
    void blendClear(sp<Allocation> in, sp<Allocation> out);
    void blendSrc(sp<Allocation> in, sp<Allocation> out);
    void blendDst(sp<Allocation> in, sp<Allocation> out);
    void blendSrcOver(sp<Allocation> in, sp<Allocation> out);
    void blendDstOver(sp<Allocation> in, sp<Allocation> out);
    void blendSrcIn(sp<Allocation> in, sp<Allocation> out);
    void blendDstIn(sp<Allocation> in, sp<Allocation> out);
    void blendSrcOut(sp<Allocation> in, sp<Allocation> out);
    void blendDstOut(sp<Allocation> in, sp<Allocation> out);
    void blendSrcAtop(sp<Allocation> in, sp<Allocation> out);
    void blendDstAtop(sp<Allocation> in, sp<Allocation> out);
    void blendXor(sp<Allocation> in, sp<Allocation> out);
    void blendMultiply(sp<Allocation> in, sp<Allocation> out);
    void blendAdd(sp<Allocation> in, sp<Allocation> out);
    void blendSubtract(sp<Allocation> in, sp<Allocation> out);
};

class ScriptIntrinsicBlur : public ScriptIntrinsic {
 public:
    ScriptIntrinsicBlur(sp<RS> rs, sp <const Element> e);
    void blur(sp<Allocation> in, sp<Allocation> out);
    void setRadius(float radius);
};

}

}

#endif
