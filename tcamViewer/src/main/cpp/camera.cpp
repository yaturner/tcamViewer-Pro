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
#include <unistd.h>
#include <chrono>
#include <jni.h>
#include <pthread.h>
#include <android/log.h>

#define PORT 5001
#define BUFFER_LENGTH 65535
#define MAX_EVENTS 5
#define APP_NAME "Camera.cpp"

static JavaVM *jvm = nullptr;
JNIEnv *store_env;
jmethodID store_method;
jweak store_Wlistener;

struct sockaddr_in serv_addr;
struct timeval timeout;
int sock_fd = -1;
int epoll_fd = -1;
int running = 1, event_count, i;
int bytes_read;
char read_buffer[BUFFER_LENGTH + 1] = {0};
char response[BUFFER_LENGTH + 1] = {0};
struct epoll_event event, events[MAX_EVENTS];
bool connected = false;
bool start_found, end_found;
char *pos, *start, *end;
long responseLen;
pthread_t pthread;
int pid;
const char *cmd_get_image = "\02{\"cmd\":\"get_image\"}\03";
const char *cmd_stream = "\02{\"cmd\":\"stream_on\", "
                         "\"args\":{\"delay_msec\":0, \"num_frames\":0}}\03";

bool sockConnect();
bool sendCommand(const char *cmd);
void isDataAvailable();
int init();

/**
 * main
 */
//int main(int argc, char *argv[]) {
//  exit(init());
//}

void responseCallback(JNIEnv *env, const _jstring *message_);

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_connect(JNIEnv *env, jobject MainActivity, jobject jnilistener) {
    bool ret = true;
    connected = false;
    timeout.tv_sec = 30;
    timeout.tv_usec = 0;

    env->GetJavaVM(&jvm); //store jvm reference for later call

    store_env = env;

    store_Wlistener = env->NewWeakGlobalRef(jnilistener);
    jclass clazz = env->GetObjectClass(store_Wlistener);

    store_method = env->GetMethodID(clazz, "onAcceptResponse", "(Ljava/lang/String;)V");

    if (!sockConnect()) {
        __android_log_print( ANDROID_LOG_ERROR, APP_NAME, "Could not connect to socket\n");
        ret = false;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "Connected");
        pos = read_buffer;
        end_found = false;
        start_found = false;
        connected = true;
        ret = true;
    }
    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_startListening(JNIEnv *env, jobject thiz) {
    ///pid = pthread_create(&pthread, NULL, isDataAvailable, (void*)"dataAvailThread");
    isDataAvailable();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_isConnected(JNIEnv *env, jobject thiz) {
    return connected;
}

bool sockConnect() {
    if ((sock_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "\n Socket creation error \n");
        return false;
    }

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);

    memset(&serv_addr, 0, sizeof(struct sockaddr_in));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = inet_addr("192.168.0.32");
    serv_addr.sin_port = htons(PORT);

    int b = connect(sock_fd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
    if (b < 0) {
        __android_log_print(ANDROID_LOG_ERROR, APP_NAME, "Error: connect\n");
        return false;
    }

    epoll_fd = epoll_create1(0);
    if (epoll_fd == -1) {
        __android_log_print(ANDROID_LOG_ERROR, APP_NAME, "Failed to create epoll file descriptor\n");
        return false;
    }

    event.events = EPOLLIN;
    event.data.fd = sock_fd;

    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, sock_fd, &event)) {
        __android_log_print(ANDROID_LOG_ERROR, APP_NAME, "Failed to add file descriptor to epoll\n");
        close(epoll_fd);
        return false;
    }

    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_sendCommand(JNIEnv *env, jobject thiz, jstring command) {
    // TODO: implement sendCommand()
    bool ret = true;
    const char *cmd = env->GetStringUTFChars(command, 0);
    if (cmd == NULL || strlen(cmd) == 0) {
        ret = false;
    }
    int bytes_sent = send(sock_fd, cmd, strlen(cmd), 0);
    if (bytes_sent < 0) {
        __android_log_print(ANDROID_LOG_ERROR, APP_NAME, "Failed to send command");
        ret = false;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "'%s' sent\n", cmd);
        ret = true;
    }
    env->ReleaseStringUTFChars(command, cmd);
    return ret;
}

void isDataAvailable() {
    jint res = jvm->AttachCurrentThread(&store_env, (void*)NULL);
    if(res < 0) {

    }

    bytes_read = 0;
    pos = read_buffer;
    while (running) {
        printf("\nPolling for input...\n");
        /* wait for data to be available */
        event_count = epoll_wait(epoll_fd, events, MAX_EVENTS, 30000);
        if(event_count == 0) {
            __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "epoll timeout");
            continue;
        }

        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "%d ready events\n", event_count);
        /* read all of the available data */
        auto time_start = std::chrono::high_resolution_clock::now();
        for (i = 0; i < event_count; i++) {
            __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "Reading file descriptor '%d' -- ", events[i].data.fd);
            /* read a data packet */
            bytes_read = read(events[i].data.fd, pos, BUFFER_LENGTH);
            /* if we read anything */
            if (bytes_read > 0) {
                __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "%d bytes read.\n", bytes_read);
                /* next buffer read position */
                pos = pos + bytes_read;
                /* scan the buffer for start and end markers*/
                for (char *p = pos - bytes_read; p < pos; p++) {
                    /* tjsn start */
                    if (*p == '\02') {
                        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "found start at buffer position %lx, value = %d\n", p - read_buffer,
                               *p);
                        start_found = true;
                        start = p;
                    }
                    /* tjsn end */
                    if (*p == '\03') {
                        end = p;
                        responseLen = end - start;
                        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "found end at buffer position %lx, len = %ld, value = %d\n",
                               p - read_buffer,
                               responseLen, *p);
                        end_found = true;
                        /* create the tjsn package and return it to java */
                        auto time_stop = std::chrono::high_resolution_clock::now();
                        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(
                                time_stop - time_start);
                        memcpy(response, start, responseLen + 1);
                        jstring message = store_env->NewStringUTF((const char*)&response);
                        store_env->CallVoidMethod(store_Wlistener, store_method, message);
                        __android_log_print(ANDROID_LOG_DEBUG, APP_NAME, "response starts with %d and ends with %d\n", response[0],
                               response[responseLen]);
                        std::cout << "duration = " << duration.count() << std::endl;
                        pos = read_buffer;
                        /* if there is any thin left in the bufeer after the end, nove it to the beginning */

                        /////break;
                    }
                }
            } else {
                __android_log_print(ANDROID_LOG_ERROR, APP_NAME, "nothing read\n");
            }
        }
    };
}

