# BoardConfig.mk
#
# Product-specific compile-time definitions.
#

# same as mips except HAL
include device/generic/mips/BoardConfig.mk

# Build OpenGLES emulation libraries
BUILD_EMULATOR_OPENGL := true
BUILD_EMULATOR_OPENGL_DRIVER := true

# share the same one across all mini-emulators
BOARD_EGL_CFG := device/generic/goldfish/opengl/system/egl/egl.cfg

