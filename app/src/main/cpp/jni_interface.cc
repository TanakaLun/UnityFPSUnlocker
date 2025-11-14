#include <jni.h>
#include "fpslimiter.hh"

extern "C" {

JNIEXPORT void JNICALL
Java_io_github_hexstr_UnityFPSUnlocker_UnityFPSUnlocker_startFPSLimiter(JNIEnv* env, jclass clazz) {
    // 使用默认配置启动，实际配置通过UpdateConfig设置
    ConfigValue default_config{5, 90, false, 1.0f};
    FPSLimiter::Start(default_config);
}

JNIEXPORT void JNICALL
Java_io_github_hexstr_UnityFPSUnlocker_UnityFPSUnlocker_updateConfig(
    JNIEnv* env, jclass clazz, jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    ConfigValue new_config{delay, fps, mod_opcode, scale};
    FPSLimiter::UpdateConfig(new_config);
}

JNIEXPORT void JNICALL
Java_io_github_hexstr_UnityFPSUnlocker_UnityFPSUnlocker_setFrameRate(
    JNIEnv* env, jclass clazz, jint fps) {
    
    FPSLimiter::SetFrameRate(fps);
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    return JNI_VERSION_1_6;
}

}