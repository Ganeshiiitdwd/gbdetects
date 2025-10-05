# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("gbdetects")

# Set the path to the OpenCV SDK module.
set(OpenCV_DIR ../../../../sdk/native/jni)
find_package(OpenCV REQUIRED)

# Find the GLESv2 library
find_library(GLESv2-lib GLESv2)

# Creates and names our shared library.
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

# Link our library against OpenCV, Android's log library, and GLESv2.
target_link_libraries(${CMAKE_PROJECT_NAME}
        ${OpenCV_LIBS}
        ${GLESv2-lib}
        android
        log)
