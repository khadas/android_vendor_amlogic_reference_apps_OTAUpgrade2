LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
# Build all java files in the java subdirectory
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_LIBRARIES := org.apache.http.legacy
# Any libraries that this library depends on
# The name of the jar file to create
LOCAL_MODULE := libotaupgrade-static
# Build a static jar file.
include $(BUILD_STATIC_JAVA_LIBRARY)

