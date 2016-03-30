APP_ABI := armeabi-v7a
APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions -march=armv7-a -mfloat-abi=softfp -mfpu=neon -D__ARM_NEON__
APP_OPTIM := release
NDK_TOOLCHAIN_VERSION = 4.8