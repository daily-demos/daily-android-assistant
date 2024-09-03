cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_STANDARD 14)

project("native-lib")

find_library(log-lib log)

file(GLOB LUA_SOURCES "lua/*.c")

add_library(native-lib SHARED ${LUA_SOURCES} native-lib.cpp)

target_link_libraries(native-lib ${log-lib})
