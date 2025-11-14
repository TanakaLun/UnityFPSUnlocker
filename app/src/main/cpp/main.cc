#include "main.hh"

#include <jni.h>
#include <thread>
#include <string>

#include "fpslimiter.hh"
#include "utility/logger.hh"

FPSUnlockerManager& FPSUnlockerManager::GetInstance() {
    static FPSUnlockerManager instance;
    return instance;
}

void FPSUnlockerManager::Initialize() {
    current_config_ = ConfigValue(3, 120, true, 1.0f);
    LOG("[FPSUnlocker] Manager initialized with default config");
}

void FPSUnlockerManager::SetConfig(const ConfigValue& config) {
    current_config_ = config;
    LOG("[FPSUnlocker] Config updated");
    current_config_.DebugPrint();
    
    std::thread([config]() {
        FPSLimiter::Start(config);
    }).detach();
}

ConfigValue FPSUnlockerManager::GetCurrentConfig() const {
    return current_config_;
}

// JNI方法实现
extern "C" {

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeInitialize(JNIEnv* env, jclass clazz) {
    LOG("[JNI] nativeInitialize called");
    FPSUnlockerManager::GetInstance().Initialize();
}

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeSetConfig(
    JNIEnv* env, jclass clazz, 
    jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    LOG("[JNI] nativeSetConfig called: delay=%d, fps=%d, mod_opcode=%d, scale=%.1f", 
        delay, fps, mod_opcode, scale);
    
    ConfigValue config(delay, fps, mod_opcode, scale);
    FPSUnlockerManager::GetInstance().SetConfig(config);
}

JNIEXPORT jint JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeGetDelay(JNIEnv* env, jclass clazz) {
    return FPSUnlockerManager::GetInstance().GetCurrentConfig().delay_;
}

JNIEXPORT jint JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeGetFPS(JNIEnv* env, jclass clazz) {
    return FPSUnlockerManager::GetInstance().GetCurrentConfig().fps_;
}

JNIEXPORT jboolean JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeGetModOpcode(JNIEnv* env, jclass clazz) {
    return FPSUnlockerManager::GetInstance().GetCurrentConfig().mod_opcode_;
}

JNIEXPORT jfloat JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeGetScale(JNIEnv* env, jclass clazz) {
    return FPSUnlockerManager::GetInstance().GetCurrentConfig().scale_;
}

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeStart(
    JNIEnv* env, jclass clazz, 
    jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    LOG("[JNI] nativeStart called");
    ConfigValue config(delay, fps, mod_opcode, scale);
    std::thread([config]() {
        FPSLimiter::Start(config);
    }).detach();
}

} // extern "C"