/*
 * Copyright (C) 2008-2012 The Android Open Source Project
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

#include "RenderScript.h"
#include <rs.h>
#include "rsDefines.h"

using namespace android;
using namespace RSC;

ScriptIntrinsic::ScriptIntrinsic(sp<RS> rs, int id, sp<const Element> e)
    : Script(NULL, rs) {
    mID = rsScriptIntrinsicCreate(rs->getContext(), id, e->getID());
}

ScriptIntrinsicBlend::ScriptIntrinsicBlend(sp<RS> rs, sp<const Element> e)
    : ScriptIntrinsic(rs, RS_SCRIPT_INTRINSIC_ID_BLEND, e) {

}

void ScriptIntrinsicBlend::blendClear(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(0, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendSrc(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(1, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendDst(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(2, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendSrcOver(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(3, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendDstOver(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(4, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendSrcIn(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(5, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendDstIn(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(6, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendSrcOut(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(7, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendDstOut(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(8, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendSrcAtop(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(9, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendDstAtop(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(10, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendXor(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(11, in, out, NULL, 0);
}

// Numbering jumps here
void ScriptIntrinsicBlend::blendMultiply(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(14, in, out, NULL, 0);
}

// Numbering jumps here
void ScriptIntrinsicBlend::blendAdd(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(34, in, out, NULL, 0);
}

void ScriptIntrinsicBlend::blendSubtract(sp<Allocation> in, sp<Allocation> out) {
    Script::forEach(35, in, out, NULL, 0);
}

ScriptIntrinsicBlur::ScriptIntrinsicBlur(sp<RS> rs, sp<const Element> e)
    : ScriptIntrinsic(rs, RS_SCRIPT_INTRINSIC_ID_BLUR, e) {

}

void ScriptIntrinsicBlur::blur(sp<Allocation> in, sp<Allocation> out) {
    Script::setVar(1, in);
    Script::forEach(0, NULL, out, NULL, 0);
}

void ScriptIntrinsicBlur::setRadius(float radius) {
    Script::setVar(0, &radius, sizeof(float));
}
