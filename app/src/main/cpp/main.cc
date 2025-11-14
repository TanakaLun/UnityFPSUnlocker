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
    // 初始化默认配置
    current_config_ = ConfigValue(3, 120, true, 1.0f);
    LOG("[FPSUnlocker] Manager initialized with default config");
}

void FPSUnlockerManager::SetConfig(const ConfigValue& config) {
    current_config_ = config;
    LOG("[FPSUnlocker] Config updated");
    current_config_.DebugPrint();
    
    // 应用新配置
    std::thread([config]() {
        FPSLimiter::Start(config);
    }).detach();
}

void FPSUnlockerManager::SetConfigForPackage(const std::string& package_name, const ConfigValue& config) {
    current_config_ = config;
    LOG("[FPSUnlocker] Config updated for package: %s", package_name.c_str());
    current_config_.DebugPrint();
    
    // 应用新配置
    std::thread([config]() {
        FPSLimiter::Start(config);
    }).detach();
}

ConfigValue FPSUnlockerManager::GetCurrentConfig() const {
    return current_config_;
}

// JNI方法 - 供Java层调用
extern "C" {

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeInitialize(JNIEnv* env, jclass clazz) {
    FPSUnlockerManager::GetInstance().Initialize();
}

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeSetConfig(
    JNIEnv* env, jclass clazz, 
    jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    ConfigValue config(delay, fps, mod_opcode, scale);
    FPSUnlockerManager::GetInstance().SetConfig(config);
}

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeSetConfigForPackage(
    JNIEnv* env, jclass clazz, jstring package_name,
    jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    const char* native_package_name = env->GetStringUTFChars(package_name, 0);
    ConfigValue config(delay, fps, mod_opcode, scale);
    FPSUnlockerManager::GetInstance().SetConfigForPackage(native_package_name, config);
    env->ReleaseStringUTFChars(package_name, native_package_name);
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

// 直接启动FPS解锁（兼容旧版本）
JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeStart(
    JNIEnv* env, jclass clazz, 
    jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    ConfigValue config(delay, fps, mod_opcode, scale);
    std::thread([config]() {
        FPSLimiter::Start(config);
    }).detach();
}

JNIEXPORT void JNICALL Java_io_github_hexstr_UnityFPSUnlocker_Main_nativeStartForPackage(
    JNIEnv* env, jclass clazz, jstring package_name,
    jint delay, jint fps, jboolean mod_opcode, jfloat scale) {
    
    const char* native_package_name = env->GetStringUTFChars(package_name, 0);
    ConfigValue config(delay, fps, mod_opcode, scale);
    
    LOG("[FPSUnlocker] Starting FPS unlock for package: %s", native_package_name);
    
    std::thread([config]() {
        FPSLimiter::Start(config);
    }).detach();
    
    env->ReleaseStringUTFChars(package_name, native_package_name);
}

}