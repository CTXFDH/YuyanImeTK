# Install script for directory: C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/src/main/jni/third_party/knf/kissfft

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "C:/Program Files (x86)/yuyan_asr")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "RelWithDebInfo")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "C:/Users/wen/AppData/Local/Android/Sdk/ndk/25.1.8937393/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-objdump.exe")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/.cxx/RelWithDebInfo/5t2u114k/arm64-v8a/lib/libkissfft-float.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  list(APPEND CMAKE_ABSOLUTE_DESTINATION_FILES
   "/kissfft/kiss_fft.h;/kissfft/kissfft.hh;/kissfft/kiss_fftnd.h;/kissfft/kiss_fftndr.h;/kissfft/kiss_fftr.h")
  if(CMAKE_WARN_ON_ABSOLUTE_INSTALL_DESTINATION)
    message(WARNING "ABSOLUTE path INSTALL DESTINATION : ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
  endif()
  if(CMAKE_ERROR_ON_ABSOLUTE_INSTALL_DESTINATION)
    message(FATAL_ERROR "ABSOLUTE path INSTALL DESTINATION forbidden (by caller): ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
  endif()
  file(INSTALL DESTINATION "/kissfft" TYPE FILE FILES
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/src/main/jni/third_party/knf/kissfft/kiss_fft.h"
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/src/main/jni/third_party/knf/kissfft/kissfft.hh"
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/src/main/jni/third_party/knf/kissfft/kiss_fftnd.h"
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/src/main/jni/third_party/knf/kissfft/kiss_fftndr.h"
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/src/main/jni/third_party/knf/kissfft/kiss_fftr.h"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}/cmake/kissfft/kissfft-float-static-targets.cmake")
    file(DIFFERENT EXPORT_FILE_CHANGED FILES
         "$ENV{DESTDIR}/cmake/kissfft/kissfft-float-static-targets.cmake"
         "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/.cxx/RelWithDebInfo/5t2u114k/arm64-v8a/_deps/kissfft-build/CMakeFiles/Export/_cmake/kissfft/kissfft-float-static-targets.cmake")
    if(EXPORT_FILE_CHANGED)
      file(GLOB OLD_CONFIG_FILES "$ENV{DESTDIR}/cmake/kissfft/kissfft-float-static-targets-*.cmake")
      if(OLD_CONFIG_FILES)
        message(STATUS "Old export file \"$ENV{DESTDIR}/cmake/kissfft/kissfft-float-static-targets.cmake\" will be replaced.  Removing files [${OLD_CONFIG_FILES}].")
        file(REMOVE ${OLD_CONFIG_FILES})
      endif()
    endif()
  endif()
  list(APPEND CMAKE_ABSOLUTE_DESTINATION_FILES
   "/cmake/kissfft/kissfft-float-static-targets.cmake")
  if(CMAKE_WARN_ON_ABSOLUTE_INSTALL_DESTINATION)
    message(WARNING "ABSOLUTE path INSTALL DESTINATION : ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
  endif()
  if(CMAKE_ERROR_ON_ABSOLUTE_INSTALL_DESTINATION)
    message(FATAL_ERROR "ABSOLUTE path INSTALL DESTINATION forbidden (by caller): ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
  endif()
  file(INSTALL DESTINATION "/cmake/kissfft" TYPE FILE FILES "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/.cxx/RelWithDebInfo/5t2u114k/arm64-v8a/_deps/kissfft-build/CMakeFiles/Export/_cmake/kissfft/kissfft-float-static-targets.cmake")
  if("${CMAKE_INSTALL_CONFIG_NAME}" MATCHES "^([Rr][Ee][Ll][Ww][Ii][Tt][Hh][Dd][Ee][Bb][Ii][Nn][Ff][Oo])$")
    list(APPEND CMAKE_ABSOLUTE_DESTINATION_FILES
     "/cmake/kissfft/kissfft-float-static-targets-relwithdebinfo.cmake")
    if(CMAKE_WARN_ON_ABSOLUTE_INSTALL_DESTINATION)
      message(WARNING "ABSOLUTE path INSTALL DESTINATION : ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
    endif()
    if(CMAKE_ERROR_ON_ABSOLUTE_INSTALL_DESTINATION)
      message(FATAL_ERROR "ABSOLUTE path INSTALL DESTINATION forbidden (by caller): ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
    endif()
    file(INSTALL DESTINATION "/cmake/kissfft" TYPE FILE FILES "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/.cxx/RelWithDebInfo/5t2u114k/arm64-v8a/_deps/kissfft-build/CMakeFiles/Export/_cmake/kissfft/kissfft-float-static-targets-relwithdebinfo.cmake")
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  list(APPEND CMAKE_ABSOLUTE_DESTINATION_FILES
   "/cmake/kissfft/kissfft-config.cmake;/cmake/kissfft/kissfft-config-version.cmake")
  if(CMAKE_WARN_ON_ABSOLUTE_INSTALL_DESTINATION)
    message(WARNING "ABSOLUTE path INSTALL DESTINATION : ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
  endif()
  if(CMAKE_ERROR_ON_ABSOLUTE_INSTALL_DESTINATION)
    message(FATAL_ERROR "ABSOLUTE path INSTALL DESTINATION forbidden (by caller): ${CMAKE_ABSOLUTE_DESTINATION_FILES}")
  endif()
  file(INSTALL DESTINATION "/cmake/kissfft" TYPE FILE FILES
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/.cxx/RelWithDebInfo/5t2u114k/arm64-v8a/_deps/kissfft-build/kissfft-config.cmake"
    "C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/app/.cxx/RelWithDebInfo/5t2u114k/arm64-v8a/_deps/kissfft-build/kissfft-config-version.cmake"
    )
endif()

