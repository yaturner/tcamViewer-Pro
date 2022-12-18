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
#include <chrono>
#include <android/log.h>

#define PORT 5001
#define BUFFER_LENGTH 65535
#define MAX_EVENTS 100
#define APP_NAME "Camera.cpp"
#undef DEBUG
#ifdef DEBUG
#define LOGD(x...) do { \
  char buf[512]; \
  sprintf(buf, x); \
  __android_log_print(ANDROID_LOG_DEBUG,"camera.cpp", "%s | line %i", buf, __LINE__); \
} while (0)
#define LOGE(x...) do { \
  char buf[512]; \
  sprintf(buf, x); \
  __android_log_print(ANDROID_LOG_ERROR,"camera.cpp", "%s | line%i", buf, __LINE__); \
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
struct timeval timeout;
int sock_fd = -1;
int epoll_fd = -1;
bool running = false;
int event_count, i;
int bytes_read;
//char read_buffer[BUFFER_LENGTH + 1] = {0};
//char response[BUFFER_LENGTH + 1] = {0};
string read_buffer;
char temp[BUFFER_LENGTH];
string response;
struct epoll_event event, events[MAX_EVENTS];
bool connected = false;
bool start_found, end_found;
char *bufferPos, *startPos, *endPos;
int startPosition, endPosition; //relative to start of read_buffer
long responseLen;
pthread_t pthread;
int pid;
const char *cmd_get_image = "\02{\"cmd\":\"get_image\"}\03";
const char *cmd_stream = "\02{\"cmd\":\"stream_on\", "
                         "\"args\":{\"delay_msec\":0, \"num_frames\":0}}\03";

bool sockConnect(const char *ipAddress);
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

/**
 * connect
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_connect(JNIEnv *env, jobject MainActivity,
                                                           jobject jnilistener, jstring address) {
    bool ret = true;
    connected = false;
    timeout.tv_sec = 30;
    timeout.tv_usec = 0;

    env->GetJavaVM(&jvm); //store jvm reference for later call

    store_env = env;

    const jsize len = env->GetStringUTFLength(address);
    const char* ipAddress = env->GetStringUTFChars(address, 0);

    store_Wlistener = env->NewWeakGlobalRef(jnilistener);
    jclass clazz = env->GetObjectClass(store_Wlistener);

    store_method = env->GetMethodID(clazz, "onAcceptResponse", "(Ljava/lang/String;)V");

    if (!sockConnect(ipAddress)) {
        LOGD("Could not connect to socket\n");
        ret = false;
    } else {
        LOGD("Connected");
        read_buffer.reserve(BUFFER_LENGTH);
        response.reserve(BUFFER_LENGTH);
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
    running = true;
    LOGD("running = %s", running ? "true" : "false");
    isDataAvailable();
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

/**
 * socketConnect
 * @return
 */
bool sockConnect(const char *ipAddress) {
    if ((sock_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        LOGD("\n Socket creation error \n");
        return false;
    }
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);

    memset(&serv_addr, 0, sizeof(struct sockaddr_in));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = inet_addr(ipAddress);
    serv_addr.sin_port = htons(PORT);

    int b = connect(sock_fd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
    if (b < 0) {
        LOGE("Error: connect\n");
        close(sock_fd);
        return false;
    }

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
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_sendCommand(JNIEnv *env, jobject thiz,
                                                               jstring command) {
    // TODO: implement sendCommand()
    bool ret = true;
    const char *cmd = env->GetStringUTFChars(command, 0);
    if (cmd == NULL || strlen(cmd) == 0) {
        ret = false;
    }
    int bytes_sent = send(sock_fd, cmd, strlen(cmd), 0);
    if (bytes_sent < 0) {
        LOGE("Failed to send command");
        ret = false;
    } else {
        LOGD("'%s' sent\n", cmd);
        ret = true;
    }
    env->ReleaseStringUTFChars(command, cmd);
    return ret;
}

/**
 *
 */
void isDataAvailable() {
    jint res = jvm->AttachCurrentThread(&store_env, (void *) NULL);
    if (res < 0) {

    }
    int totalBytesRead = 0;
    bytes_read = 0;
    while (connected && running) {
        ////usleep(250);
        /* wait for data to be available */
        event_count = epoll_wait(epoll_fd, events, MAX_EVENTS, 30000);
        if (event_count == 0) {
            LOGD("epoll timeout");
            continue;
        }

        //LOGD("%d ready events\n", event_count);
        /* read all of the available data */
        auto time_start = std::chrono::high_resolution_clock::now();
        for (i = 0; i < event_count; i++) {
            if (!running) {
                break;
            }
            //LOGD("Reading file descriptor '%d' -- ", events[i].data.fd);
            /* read a data packet */
            bytes_read = recv(events[i].data.fd, temp, BUFFER_LENGTH-1, 0);

            /* if we read anything */
            if (bytes_read > 0) {
                temp[bytes_read] = 0;
                read_buffer = read_buffer + temp;
                totalBytesRead = totalBytesRead + bytes_read;
                //LOGD("%d bytes read, total = %d.\n", bytes_read, totalBytesRead);
                /* next buffer read position */
                /* scan the buffer for start and end markers*/
                /* tjsn start */
                if (!start_found) {
                    startPos = strchr((char *const) &read_buffer[0], '\02');
                    if (startPos != NULL) {
                        startPosition = startPos-&read_buffer[0];
                        LOGD("found start at buffer position %d, value = %d\n", startPosition, startPos[0]);
                        start_found = true;
                    }
                }
                /* tjsn end */
                if (start_found && !end_found) {
                    endPos = strchr((char *const) &read_buffer[0], '\03');
                    if (endPos != NULL) {
                        end_found = true;
                        endPosition = endPos-&read_buffer[0];
                        LOGD("found end at buffer position %d, value = %d\n", endPosition, endPos[0]);
                        /* create the tjsn package and return it to java */
                        auto time_stop = std::chrono::high_resolution_clock::now();
                        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(
                                time_stop - time_start);
                        responseLen = endPosition - startPosition;
                        response = read_buffer.substr(startPosition, responseLen+1);
                        response = response + '\0';
                        read_buffer = read_buffer.substr(responseLen+1);
                        jstring message = store_env->NewStringUTF(response.c_str());
                        store_env->CallVoidMethod(store_Wlistener, store_method, message);
                        LOGD("response starts with %d and ends with %d\n",
                             response[0],
                             response[responseLen]);
                        LOGD("duration = %d\n",duration.count());
                        end_found = false;
                        start_found = false;
                        startPosition = -1;
                        endPosition = -1;
                        totalBytesRead = 0;
                        break;
                    }
                }

            } else {
                if (!running) {
                    break;
                }
                //LOGE("nothing read\n");
            }
        }
    };
    return;
}

