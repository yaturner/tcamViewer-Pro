// Client side C/C++ program to demonstrate Socket
// programming
#include <arpa/inet.h>
#include <cerrno>
#include <fcntl.h>
#include <iostream>
#include <cstdio>
#include <cstring>
#include <sys/epoll.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include "sys/ioctl.h"
#include "sys/types.h"
#include "sys/select.h"
#include <unistd.h>
#include <chrono>
#include <jni.h>
#include <pthread.h>
#include <sched.h>
#include <chrono>
#include <android/log.h>

#define PORT 5001
#define BUFFER_LENGTH 65535
#define TAG "Camera.cpp"
//#define DEBUG
#undef DEBUG
#ifdef DEBUG
#define LOGD(x...) do { \
  char buf[512]; \
  sprintf(buf, x); \
  __android_log_print(ANDROID_LOG_DEBUG,TAG, "%s | line %i", buf, __LINE__); \
} while (0)
#define LOGE(x...) do { \
  char buf[512]; \
  sprintf(buf, x); \
  __android_log_print(ANDROID_LOG_ERROR,TAG, "%s | line%i", buf, __LINE__); \
} while (0)
#else
#define LOGD(x...)
#define LOGE(x...)
#endif
using namespace std;

static JavaVM *jvm = nullptr;
JNIEnv *store_env;
jmethodID store_method;
jweak store_Wlistener;

struct sockaddr_in serv_addr;
//struct timeval timeout;
int sock_fd = -1;
int epoll_fd = -1;
bool running = false;
int bytes_read, totalBytesRead, responsePos;
char read_buffer[BUFFER_LENGTH];
char temp[2];
char response[BUFFER_LENGTH];
struct epoll_event event;
bool connected = false;
bool start_found, end_found;
pthread_t dataListenerThread;
jstring message;

bool sockConnect(const char *ipAddress);
void * isDataAvailable(void *pVoid);
void resetBuffers();

/**
 * connect
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_connect(JNIEnv *env, jobject MainActivity,
                                                           jobject jnilistener, jstring address) {
    bool ret = true;
    connected = false;

    env->GetJavaVM(&jvm); //store jvm reference for later call

    store_env = env;

    const jsize len = env->GetStringUTFLength(address);
    const char *ipAddress = env->GetStringUTFChars(address, nullptr);

    store_Wlistener = env->NewWeakGlobalRef(jnilistener);
    jclass clazz = env->GetObjectClass(store_Wlistener);

    store_method = env->GetMethodID(clazz, "onAcceptResponse", "(Ljava/lang/String;)V");

    if (!sockConnect(ipAddress)) {
        LOGD("Could not connect to socket\n");
        ret = false;
    } else {
        LOGD("Connected");
        end_found = false;
        start_found = false;
        connected = true;
        ret = true;
    }
    env->ReleaseStringUTFChars(address, ipAddress);
    return ret;
}

/**
 * startListening
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_startListening(JNIEnv *env, jobject thiz) {
    sched_param param;
    pthread_attr_t attr;
    int ret;

    running = true;
    LOGD("running = %s", running ? "true" : "false");
    ret = pthread_attr_init(&attr);
    ret = pthread_attr_getschedparam(&attr, &param);
    param.sched_priority = 20;
    ret = pthread_attr_setschedparam(&attr, &param);
    ret = pthread_create(&dataListenerThread, &attr, isDataAvailable, NULL);

    //env->DetachCurrentThread()
}

/**
 * stopListening
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_stopListening(JNIEnv *env, jobject thiz) {
    running = false;
    LOGD("running = %s", running ? "true" : "false");
}

/**
 * disconnect
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_disconnect(JNIEnv *env, jobject thiz) {
    running = false;
    connected = false;
    close(sock_fd);
    sock_fd = -1;

}
/**
 * isConnected
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_isConnected(JNIEnv *env, jobject thiz) {
    return connected;
}

void connect_alarm(int signum) {
//TODO throw an error can not connect
}

/**
 * socketConnect
 * @return
 */
bool sockConnect(const char *ipAddress) {

    if ((sock_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        LOGD("\n Socket creation error");
        return false;
    }
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);

    memset(&serv_addr, 0, sizeof(struct sockaddr_in));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = inet_addr(ipAddress);
    serv_addr.sin_port = htons(PORT);

    signal(SIGALRM, connect_alarm); /* connect_alarm is you signal handler */
    alarm(30); /* secs is your timeout in seconds */
    int b = connect(sock_fd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
    if (b < 0) {
        LOGE("Error: connect\n");
        close(sock_fd);
        alarm(0);
        return false;
    }
    alarm(0);

    epoll_fd = epoll_create1(0);
    if (epoll_fd == -1) {
        LOGE("Failed to create epoll file descriptor\n");
        close(sock_fd);
        return false;
    }

    event.events = EPOLLIN;
    event.data.fd = sock_fd;

    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, sock_fd, &event)) {
        LOGE("Failed to add file descriptor to epoll\n");
        close(epoll_fd);
        close(sock_fd);
        return false;
    }
    return true;
}

/**
 * sendCommand
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_sendCommand(JNIEnv *env, jobject thiz, jstring command) {
    bool ret = true;
    const char *cmd = env->GetStringUTFChars(command, nullptr);
    if (cmd == nullptr || strlen(cmd) == 0) {
        ret = false;
    }
    int bytes_sent = send(sock_fd, cmd, strlen(cmd), 0);
    if (bytes_sent < 0) {
        LOGE("Failed to send command");
        ret = false;
    } else {
        LOGD("'%s' sent", cmd);
        ret = true;
    }
    env->ReleaseStringUTFChars(command, cmd);
    return ret;
}

/**
 *
 */
void * isDataAvailable(void *pVoid) {
    jint res = jvm->AttachCurrentThread(&store_env, (void *) nullptr);
//    if (res < 0) {
//        return;
//    }
    totalBytesRead = 0;
    bytes_read = 0;
    response[0] = 0;
    temp[1] = '\0';
    while (connected && running) {
        /* read all of the available data */
//        auto time_start = std::chrono::high_resolution_clock::now();
        /* read a data packet only if we have extract all of the commands in the current packet */
        bytes_read = read(sock_fd/*events[i].data.fd*/, &read_buffer[0], 1024);
        if (bytes_read == 0) {
            sched_yield();
            usleep(10000);
            continue;
        }
        for (int index = 0; index < bytes_read; index++) {
            temp[0] = read_buffer[index];
            if (temp[0] == '\02') {
                if(start_found) {
                    //second in a row, we lost the '03', start over
                    responsePos = 0;
                } else {
                    start_found = true;
                }
                LOGD("found start temp[0] = %d", temp[0]);
            } else if (start_found && !end_found && temp[0] == '\03') {
                end_found = true;
                response[responsePos] = 0;
                message = store_env->NewStringUTF(&response[0]);
                store_env->CallVoidMethod(store_Wlistener, store_method, message);
                store_env->DeleteLocalRef(message);
                resetBuffers();
//                auto time_stop = std::chrono::high_resolution_clock::now();
//                auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
//                        time_stop - time_start);
//                __android_log_print(ANDROID_LOG_DEBUG, TAG, "duration = %d millis",
//                                    duration.count());
                sched_yield();
                usleep(10000);
            } else {
                if (start_found && !end_found) {
                    response[responsePos++] = temp[0];
                }
                totalBytesRead++;
            }
        }
    }
    return nullptr;
}

void resetBuffers() {
    response[0] = 0;
    responsePos = 0;
    end_found = false;
    start_found = false;
    totalBytesRead = 0;
}