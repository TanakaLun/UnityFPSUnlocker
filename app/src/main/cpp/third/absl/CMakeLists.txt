#
# Copyright 2017 The Abseil Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# 只添加实际使用的组件
add_subdirectory(base)        # 基础库，status 依赖
add_subdirectory(container)   # flat_hash_map 容器
add_subdirectory(status)      # status 和 statusor

if (ABSL_BUILD_DLL)
  absl_make_dll()
  if ((BUILD_TESTING AND ABSL_BUILD_TESTING) OR ABSL_BUILD_TEST_HELPERS)
    absl_make_dll(TEST ON)
  endif()
endif()