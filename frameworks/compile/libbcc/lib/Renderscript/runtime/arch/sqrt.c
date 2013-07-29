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


#include "rs_types.rsh"

#define FN_FUNC_FN(fnc)                                         \
extern float2 __attribute__((overloadable)) fnc(float2 v) { \
    float2 r;                                                   \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    return r;                                                   \
}                                                               \
extern float3 __attribute__((overloadable)) fnc(float3 v) { \
    float3 r;                                                   \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    r.z = fnc(v.z);                                             \
    return r;                                                   \
}                                                               \
extern float4 __attribute__((overloadable)) fnc(float4 v) { \
    float4 r;                                                   \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    r.z = fnc(v.z);                                             \
    r.w = fnc(v.w);                                             \
    return r;                                                   \
}

extern float __attribute__((overloadable)) sqrt(float);

FN_FUNC_FN(sqrt)
