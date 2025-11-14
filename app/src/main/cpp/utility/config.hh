#ifndef CONFIG_HEADER
#define CONFIG_HEADER

#include <jni.h>
#include <sys/mman.h>
#include <string>
#include <unordered_map>

#include "logger.hh"

class ConfigValue {
public:
    int delay_ = 3;
    int fps_ = 120;
    bool mod_opcode_ = true;
    float scale_ = 1.0f;

    ConfigValue() = default;
    
    ConfigValue(int delay, int fps, bool mod_opcode, float scale)
        : delay_(delay),
          fps_(fps),
          mod_opcode_(mod_opcode),
          scale_(scale) {}
          
    ConfigValue(const ConfigValue& lhs) {
        delay_ = lhs.delay_;
        fps_ = lhs.fps_;
        mod_opcode_ = lhs.mod_opcode_;
        scale_ = lhs.scale_;
    }

    void DebugPrint() const {
        LOG("[Config] delay: %d | fps: %d | mod_opcode: %d | scale: %f", 
            delay_, fps_, mod_opcode_, scale_);
    }
};

// 简单的状态类替代 absl::Status
class Status {
public:
    enum Code {
        OK = 0,
        CANCELLED = 1,
        UNKNOWN = 2,
        INVALID_ARGUMENT = 3,
        DEADLINE_EXCEEDED = 4,
        NOT_FOUND = 5,
        ALREADY_EXISTS = 6,
        PERMISSION_DENIED = 7,
        RESOURCE_EXHAUSTED = 8,
        FAILED_PRECONDITION = 9,
        ABORTED = 10,
        OUT_OF_RANGE = 11,
        UNIMPLEMENTED = 12,
        INTERNAL = 13,
        UNAVAILABLE = 14,
        DATA_LOSS = 15,
        UNAUTHENTICATED = 16
    };

    Status() : code_(OK) {}
    Status(Code code, const std::string& message = "") : code_(code), message_(message) {}

    bool ok() const { return code_ == OK; }
    Code code() const { return code_; }
    const std::string& message() const { return message_; }

    static Status OK() { return Status(); }

private:
    Code code_;
    std::string message_;
};

// 简单的 StatusOr 类替代 absl::StatusOr
template<typename T>
class StatusOr {
public:
    StatusOr() : status_(Status::UNKNOWN) {}
    StatusOr(const Status& status) : status_(status) {}
    StatusOr(const T& value) : status_(Status::OK()), value_(value) {}
    
    bool ok() const { return status_.ok(); }
    const Status& status() const { return status_; }
    const T& value() const { return value_; }
    T& value() { return value_; }

private:
    Status status_;
    T value_;
};

namespace Utility {
    // JNI工具函数
    jobject GetApplication(JNIEnv* env);
    StatusOr<jobject> GetApplicationInfo(JNIEnv* env);
    StatusOr<std::string> GetLibraryPath(JNIEnv* env, jobject application_info);
    StatusOr<JavaVM*> GetVM();
    
    // 内存操作函数
    int ChangeMemPermission(void* p, size_t n, int permission = PROT_READ | PROT_WRITE | PROT_EXEC);
    void NopFunc(unsigned char* ptr);
}; // namespace Utility

#endif // config.hh