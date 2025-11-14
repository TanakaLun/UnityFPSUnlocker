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

# 明确列出所有源文件
set(SOURCES
    main.cc
    fpslimiter.cc
    unity/unity_engine.cc
    utility/config.cc
)

# XDL 源文件
set(XDL_SOURCES
    third/xdl/xdl.c
    third/xdl/xdl_iterate.c
    third/xdl/xdl_linker.c
    third/xdl/xdl_lzma.c
    third/xdl/xdl_util.c
)

add_library (
    ${LibraryName}
    SHARED
    ${SOURCES}
    ${XDL_SOURCES}
)

find_library(log-lib log)
target_link_libraries(${LibraryName} ${log-lib})