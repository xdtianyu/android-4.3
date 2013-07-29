# BoardConfig.mk
#
# Product-specific compile-time definitions.
#

# same sa mini-armv7-a-neon except HAL
include device/generic/armv7-a-neon/BoardConfig.mk

# Build OpenGLES emulation libraries
BUILD_EMULATOR_OPENGL := true
BUILD_EMULATOR_OPENGL_DRIVER := true

BOARD_EGL_CFG := device/generic/goldfish/opengl/system/egl/egl.cfg

