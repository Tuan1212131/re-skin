cmake_minimum_required(VERSION 3.22.1)
project(skin)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(main SHARED
        core.cpp
        jni_bridge.cpp
)

target_include_directories(main PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(main android log)
