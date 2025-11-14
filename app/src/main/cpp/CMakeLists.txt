# CMakeList.txt: UnityFPSUnlocker 的 CMake 项目
cmake_minimum_required (VERSION 3.21)

project(UnityFPSUnlocker)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions -D_LIBCPP_DISABLE_VISIBILITY_ANNOTATIONS")

set(LibraryName UnityFPSUnlocker)

# 添加Debug和Release版本信息
string(TOUPPER ${CMAKE_BUILD_TYPE} CMAKE_BUILD_TYPE_UP)
if(CMAKE_BUILD_TYPE_UP STREQUAL DEBUG)
    message(STATUS "Debug Version")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -g")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -g")
else()
    message(STATUS "Release Version")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Oz -flto -fdata-sections -ffunction-sections")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Oz -flto -fdata-sections -ffunction-sections")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -s -Wl,--exclude-libs,ALL -flto")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -s -Wl,--exclude-libs,ALL -Wl,--gc-sections -flto")
endif()

# 添加头文件
include_directories(
    .
    "third/xdl"
)

# 文件写出位置
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/libs/${CMAKE_BUILD_TYPE}/)

# 源代码
aux_source_directory(${PROJECT_SOURCE_DIR}/${LibraryName} program-src)
aux_source_directory(unity unity-src)
aux_source_directory(utility utility-src)
aux_source_directory(third/xdl xdl-src)

add_library (
    ${LibraryName}
    SHARED
    ${program-src}
    ${unity-src}
    ${utility-src}
    ${xdl-src}
)

find_library(log-lib log)
target_link_libraries(${LibraryName} ${log-lib})

set_target_properties(${LibraryName} PROPERTIES PREFIX "")
# set_target_properties(${LibraryName} PROPERTIES OUTPUT_NAME ${ANDROID_ABI})