target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32-n8:16:32-S128"
target triple = "i386-unknown-linux-gnu"

declare float @llvm.sqrt.f32(float) nounwind readnone
declare <2 x float> @llvm.sqrt.v2f32(<2 x float>) nounwind readnone
declare <3 x float> @llvm.sqrt.v3f32(<3 x float>) nounwind readnone
declare <4 x float> @llvm.sqrt.v4f32(<4 x float>) nounwind readnone
declare float @llvm.exp.f32(float) nounwind readonly
declare float @llvm.pow.f32(float, float) nounwind readonly

define float @_Z4sqrtf(float %in) nounwind readnone alwaysinline {
  %1 = tail call float @llvm.sqrt.f32(float %in) nounwind readnone
  ret float %1
}

define <2 x float> @_Z4sqrtDv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = tail call <2 x float> @llvm.sqrt.v2f32(<2 x float> %in) nounwind readnone
  ret <2 x float> %1
}

define <3 x float> @_Z4sqrtDv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = tail call <3 x float> @llvm.sqrt.v3f32(<3 x float> %in) nounwind readnone
  ret <3 x float> %1
}

define <4 x float> @_Z4sqrtDv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = tail call <4 x float> @llvm.sqrt.v4f32(<4 x float> %in) nounwind readnone
  ret <4 x float> %1
}

define float @_Z3expf(float %in) nounwind readnone {
  %1 = tail call float @llvm.exp.f32(float %in) nounwind readnone
  ret float %1
}

define float @_Z3powff(float %v1, float %v2) nounwind readnone {
  %1 = tail call float @llvm.pow.f32(float %v1, float %v2) nounwind readnone
  ret float %1
}

