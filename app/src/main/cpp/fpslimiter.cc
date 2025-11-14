#include "fpslimiter.hh"

#include <chrono>
#include <string>
#include <thread>
#include <mutex>

#include "unity/unity_engine.hh"
#include "utility/logger.hh"

#include <xdl.h>

namespace FPSLimiter {
    static ConfigValue current_config{5, 90, false, 1.0f};
    static std::mutex config_mutex;
    static bool is_initialized = false;
    
    void Start(const ConfigValue& cfg) {
        std::lock_guard<std::mutex> lock(config_mutex);
        
        if (is_initialized) {
            return;
        }
        
        current_config = cfg;

#ifdef __aarch64__
        LOG("[UnityFPSUnlocker][arm64] Starting...");
#elif defined(__ARM_ARCH_7A__)
        LOG("[UnityFPSUnlocker][armv7] Starting...");
#elif defined(__i386__)
        LOG("[UnityFPSUnlocker][x86] Starting...");
#elif defined(__x86_64__)
        LOG("[UnityFPSUnlocker][x86_64] Starting...");
#endif
        current_config.DebugPrint();
        std::chrono::seconds sleep_duration(current_config.delay_);
        std::this_thread::sleep_for(sleep_duration);

        LOG("***** begin *****");
        void* handle = nullptr;
        for (int i = 0; i < 10; ++i) {
            if ((handle = xdl_open("libil2cpp.so", 0))) {
                break;
            }
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }
        if (handle) {
            Unity::GetInstance().Init(handle);
            Unity::GetInstance().SetFrameRate(current_config.fps_, current_config.mod_opcode_);
            Unity::GetInstance().SetResolution(current_config.scale_);

            LOG("***** finished *****");
            is_initialized = true;
            return;
        }
        else {
            ERROR("Failed to open libil2cpp.so");
        }
    }
    
    void UpdateConfig(const ConfigValue& new_config) {
        std::lock_guard<std::mutex> lock(config_mutex);
        current_config = new_config;
        
        LOG("Config updated - Delay: %d, FPS: %d, ModOpcode: %d, Scale: %.1f", 
            current_config.delay_, current_config.fps_, current_config.mod_opcode_, current_config.scale_);
        
        if (is_initialized) {
            // 实时更新帧率和分辨率
            Unity::GetInstance().SetFrameRate(current_config.fps_, current_config.mod_opcode_);
            Unity::GetInstance().SetResolution(current_config.scale_);
        }
    }
    
    void SetFrameRate(int fps) {
        std::lock_guard<std::mutex> lock(config_mutex);
        current_config.fps_ = fps;
        
        LOG("Frame rate updated to: %d", fps);
        
        if (is_initialized) {
            Unity::GetInstance().SetFrameRate(fps, current_config.mod_opcode_);
        }
    }
} // namespace FPSLimiter