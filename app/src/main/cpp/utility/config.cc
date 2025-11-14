#include "config.hh"

#include <dlfcn.h>
#include <jni.h>
#include <stdint.h>
#include <fstream>
#include <string>

#include <xdl.h>

#include "logger.hh"

namespace Utility {
    jobject GetApplication(JNIEnv* env) {
        jclass activity_thread_clz = env->FindClass("android/app/ActivityThread");
        if (activity_thread_clz != nullptr) {
            jmethodID currentApplicationId = env->GetStaticMethodID(
                activity_thread_clz,
                "currentApplication",
                "()Landroid/app/Application;");
            if (currentApplicationId != nullptr) {
                return env->CallStaticObjectMethod(activity_thread_clz, currentApplicationId);
            }
            else {
                ERROR("Cannot find method: currentApplication() in ActivityThread.");
            }
        }
        else {
            ERROR("Cannot find class: android.app.ActivityThread");
        }
        return nullptr;
    }

    StatusOr<jobject> GetApplicationInfo(JNIEnv* env) {
        jobject application = GetApplication(env);
        if (application == nullptr) {
            return Status(Status::NOT_FOUND, "No method id currentApplication");
        }
        jclass application_clazz = env->GetObjectClass(application);
        if (application_clazz == nullptr) {
            return Status(Status::NOT_FOUND, "No class application");
        }
        jmethodID get_application_info = env->GetMethodID(
            application_clazz,
            "getApplicationInfo",
            "()Landroid/content/pm/ApplicationInfo;");
        if (get_application_info) {
            return env->CallObjectMethod(application, get_application_info);
        }
        else {
            return Status(Status::NOT_FOUND, "No method id getApplicationInfo");
        }
    }

    StatusOr<std::string> GetLibraryPath(JNIEnv* env, jobject application_info) {
        if (!application_info) {
            return Status(Status::NOT_FOUND, "No method id");
        }
        jfieldID native_library_dir_id = env->GetFieldID(
            env->GetObjectClass(application_info),
            "nativeLibraryDir",
            "Ljava/lang/String;");

        if (native_library_dir_id) {
            jstring native_library_dir_jstring = (jstring)env->GetObjectField(application_info, native_library_dir_id);
            const char* path = env->GetStringUTFChars(native_library_dir_jstring, 0);
            std::string package_name(path);
            env->ReleaseStringUTFChars(native_library_dir_jstring, path);
            return package_name;
        }
        else {
            return Status(Status::NOT_FOUND, "No nativeLibraryDir");
        }
    }

    StatusOr<JavaVM*> GetVM() {
        void* art = xdl_open("libart.so", 0);
        if (!art) {
            return Status(Status::INTERNAL, "Cannot open libart.so.");
        }

        using JNIGetCreatedJavaVMs_t = int (*)(JavaVM** vmBuf, jsize bufLen, jsize* nVMs);
        static JNIGetCreatedJavaVMs_t JNIGetCreatedJavaVMsFunc = (JNIGetCreatedJavaVMs_t)xdl_sym(art, "JNI_GetCreatedJavaVMs", nullptr);

        if (!JNIGetCreatedJavaVMsFunc) {
            return Status(Status::NOT_FOUND, "Cannot get symbol JNIGetCreatedJavaVMsFunc");
        }

        jsize numVMs;
        JavaVM* vms = nullptr;
        if (JNIGetCreatedJavaVMsFunc(&vms, 1, &numVMs) != 0) {
            return Status(Status::NOT_FOUND, "Cannot get vms");
        }
        return vms;
    }

    int ChangeMemPermission(void* p, size_t n, int permission) {
        void* page_aligned_p = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(p) & ~(getpagesize() - 1));

        size_t protect_size = reinterpret_cast<uintptr_t>(p) + n - reinterpret_cast<uintptr_t>(page_aligned_p);
        if (protect_size % getpagesize() != 0) {
            protect_size += getpagesize();
        }

        return ::mprotect(page_aligned_p, protect_size, permission);
    }

    void NopFunc(unsigned char* ptr) {
        ChangeMemPermission(ptr, 4);
#ifdef __aarch64__
        ptr[0] = 0xC0;
        ptr[1] = 0x03;
        ptr[2] = 0x5F;
        ptr[3] = 0xD6;
#elif __ARM_ARCH_7A__
        if (0x00000001 == (reinterpret_cast<intptr_t>(ptr) & 0x00000001)) {
            ptr--;
            ChangeMemPermission(ptr, 4);
            ptr[0] = 0x70;
            ptr[1] = 0x47;
            ptr[2] = 0x00;
            ptr[3] = 0xBF;
        }
        else {
            ptr[0] = 0x1E;
            ptr[1] = 0xFF;
            ptr[2] = 0x2F;
            ptr[3] = 0xE1;
        }
#elif defined(__i386__) || defined(__x86_64__)
        ptr[0] = 0xC3;
#endif
    }
} // namespace Utility