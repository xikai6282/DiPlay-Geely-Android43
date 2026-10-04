#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <linux/i2c-dev.h>
#include <linux/i2c.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

#define MAX_I2C_MESSAGE_LENGTH 0xffff

static void throw_native_error(JNIEnv *env, int error_number, const char *operation) {
    char message[160];
    if (error_number == 0) {
        snprintf(message, sizeof(message), "%s returned an incomplete I2C transfer", operation);
    } else {
        snprintf(message, sizeof(message), "%s failed: errno=%d (%s)", operation, error_number,
                 strerror(error_number));
    }

    jclass exception_class = (*env)->FindClass(
            env, "com/shilapi/xcertplay/transport/LinuxI2cNativeException");
    if (exception_class == NULL) return;

    jmethodID constructor = (*env)->GetMethodID(env, exception_class, "<init>", "(ILjava/lang/String;)V");
    if (constructor == NULL) return;

    jstring detail = (*env)->NewStringUTF(env, message);
    if (detail == NULL) return;

    jobject exception = (*env)->NewObject(env, exception_class, constructor, error_number, detail);
    if (exception != NULL) (*env)->Throw(env, exception);
}

JNIEXPORT jint JNICALL
Java_com_shilapi_xcertplay_transport_LinuxI2cNative_open(
        JNIEnv *env, jobject receiver, jstring device_path) {
    (void) receiver;
    if (device_path == NULL) {
        throw_native_error(env, EINVAL, "open");
        return -1;
    }

    const char *path = (*env)->GetStringUTFChars(env, device_path, NULL);
    if (path == NULL) return -1;

    int file_descriptor;
    do {
        file_descriptor = open(path, O_RDWR | O_CLOEXEC);
    } while (file_descriptor < 0 && errno == EINTR);

    int error_number = file_descriptor < 0 ? errno : 0;
    (*env)->ReleaseStringUTFChars(env, device_path, path);
    if (file_descriptor < 0) throw_native_error(env, error_number, "open");
    return file_descriptor;
}

JNIEXPORT jbyteArray JNICALL
Java_com_shilapi_xcertplay_transport_LinuxI2cNative_transaction(
        JNIEnv *env,
        jobject receiver,
        jint file_descriptor,
        jint address,
        jbyteArray write_data,
        jint read_length) {
    (void) receiver;
    if (file_descriptor < 0 || write_data == NULL || address < 0 || address > 0x7f || read_length < 0) {
        throw_native_error(env, EINVAL, "I2C_RDWR");
        return NULL;
    }

    jsize write_length = (*env)->GetArrayLength(env, write_data);
    if (write_length > MAX_I2C_MESSAGE_LENGTH || read_length > MAX_I2C_MESSAGE_LENGTH ||
        (write_length == 0 && read_length == 0)) {
        throw_native_error(env, EINVAL, "I2C_RDWR");
        return NULL;
    }

    jbyte *write_bytes = NULL;
    uint8_t *read_bytes = NULL;
    struct i2c_msg messages[2];
    uint32_t message_count = 0;

    if (write_length > 0) {
        write_bytes = (*env)->GetByteArrayElements(env, write_data, NULL);
        if (write_bytes == NULL) return NULL;
        messages[message_count++] = (struct i2c_msg) {
                .addr = (uint16_t) address,
                .flags = 0,
                .len = (uint16_t) write_length,
                .buf = (uint8_t *) write_bytes,
        };
    }

    if (read_length > 0) {
        read_bytes = malloc((size_t) read_length);
        if (read_bytes == NULL) {
            if (write_bytes != NULL) (*env)->ReleaseByteArrayElements(env, write_data, write_bytes, JNI_ABORT);
            throw_native_error(env, ENOMEM, "I2C read buffer allocation");
            return NULL;
        }
        messages[message_count++] = (struct i2c_msg) {
                .addr = (uint16_t) address,
                .flags = I2C_M_RD,
                .len = (uint16_t) read_length,
                .buf = read_bytes,
        };
    }

    struct i2c_rdwr_ioctl_data request = {
            .msgs = messages,
            .nmsgs = message_count,
    };
    int result;
    do {
        result = ioctl(file_descriptor, I2C_RDWR, &request);
    } while (result < 0 && errno == EINTR);

    if (write_bytes != NULL) (*env)->ReleaseByteArrayElements(env, write_data, write_bytes, JNI_ABORT);
    if (result != (int) message_count) {
        int error_number = result < 0 ? errno : 0;
        free(read_bytes);
        throw_native_error(env, error_number, "I2C_RDWR");
        return NULL;
    }

    jbyteArray response = (*env)->NewByteArray(env, read_length);
    if (response != NULL && read_length > 0) {
        (*env)->SetByteArrayRegion(env, response, 0, read_length, (const jbyte *) read_bytes);
    }
    free(read_bytes);
    return response;
}

JNIEXPORT void JNICALL
Java_com_shilapi_xcertplay_transport_LinuxI2cNative_close(
        JNIEnv *env, jobject receiver, jint file_descriptor) {
    (void) receiver;
    if (file_descriptor < 0) {
        throw_native_error(env, EINVAL, "close");
        return;
    }
    if (close(file_descriptor) != 0) {
        throw_native_error(env, errno, "close");
    }
}
