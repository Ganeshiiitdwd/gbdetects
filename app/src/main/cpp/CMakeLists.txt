# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("gbdetects")

# Set the path to the OpenCV SDK module. Since you named your module 'sdk',
# we use that in the path. This path is relative to this file's location.
set(OpenCV_DIR ../../../../sdk/native/jni)
find_package(OpenCV REQUIRED)

# Creates and names our shared library.
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

# Link our library against OpenCV and Android's log library.
target_link_libraries(${CMAKE_PROJECT_NAME}
        ${OpenCV_LIBS}
        android
        log)