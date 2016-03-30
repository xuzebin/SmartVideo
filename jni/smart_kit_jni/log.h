
#ifndef SMART_KIT_UTIL_LOG_H_
#define SMART_KIT_UTIL_LOG_H_

#if defined DEBUG || defined _DEBUG
#if defined _ANDROID_
#include <android/log.h>
#define  SMART_DEBUG(x...)  __android_log_print(ANDROID_LOG_INFO, "SMART_KIT", x);
#else
#define  SMART_DEBUG(x,...) do{printf(x, ##__VA_ARGS__);} while(0)
#endif
#else
#define  SMART_DEBUG(...)  do {} while (0)
#endif


#endif /* SMART_KIT_UTIL_LOG_H_ */
