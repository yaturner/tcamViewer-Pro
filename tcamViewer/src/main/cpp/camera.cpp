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
#include <chrono>
#include <android/log.h>

#define PORT 5001
#define BUFFER_LENGTH 65535
#define MAX_EVENTS 5
#define APP_NAME "Camera.cpp"
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
char read_buffer[BUFFER_LENGTH + 1] = {0};
char response[BUFFER_LENGTH + 1] = {0};
struct epoll_event event, events[MAX_EVENTS];
bool connected = false;
bool start_found, end_found;
char *bufferPos, *startPos, *endPos;
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

/**
 * connect
 */
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
        LOGD("Could not connect to socket\n");
        ret = false;
    } else {
        LOGD("Connected");
        bufferPos = read_buffer;
        end_found = false;
        start_found = false;
        connected = true;
        ret = true;
    }
    return ret;
}

/**
 * startListening
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_startListening(JNIEnv *env, jobject thiz) {
    running = true;
    LOGD("running = %s", running?"true":"false");
    isDataAvailable();
}

/**
 * stopListening
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_darcangel_tcamViewer_JNI_CameraServiceJNI_stopListening(JNIEnv *env, jobject thiz) {
    running = false;
    LOGD("running = %s", running?"true":"false");
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
bool sockConnect() {
    if ((sock_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        LOGD("\n Socket creation error \n");
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
        LOGE("Error: connect\n");
        return false;
    }

    epoll_fd = epoll_create1(0);
    if (epoll_fd == -1) {
        LOGE("Failed to create epoll file descriptor\n");
        return false;
    }

    event.events = EPOLLIN;
    event.data.fd = sock_fd;

    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, sock_fd, &event)) {
        LOGE("Failed to add file descriptor to epoll\n");
        close(epoll_fd);
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
    jint res = jvm->AttachCurrentThread(&store_env, (void*)NULL);
    if(res < 0) {

    }
    int totalBytesRead = 0;
    bytes_read = 0;
    bufferPos = read_buffer;
    while (connected && running) {
        usleep(250);
        //LOGD("running = %s", running?"true":"false");
        /* wait for data to be available */
        event_count = epoll_wait(epoll_fd, events, MAX_EVENTS, 30000);
        if(event_count == 0) {
            LOGD("epoll timeout");
            continue;
        }

        //LOGD("%d ready events\n", event_count);
        /* read all of the available data */
        auto time_start = std::chrono::high_resolution_clock::now();
        for (i = 0; i < event_count; i++) {
            if(!running) {
                break;
            }
            //LOGD("Reading file descriptor '%d' -- ", events[i].data.fd);
            /* read a data packet */
            bytes_read = read(events[i].data.fd, bufferPos, BUFFER_LENGTH);
            /* if we read anything */
            if (bytes_read > 0) {
                totalBytesRead = totalBytesRead + bytes_read;
                LOGD("%d bytes read, total = %d.\n", bytes_read, totalBytesRead);
                /* next buffer read position */
                bufferPos = bufferPos + bytes_read;
                if(bufferPos > read_buffer + BUFFER_LENGTH -1) {
                    LOGE("BUFFER OVERRUN bufferPos = %x, read_buffer = %x, exiting while", bufferPos, read_buffer);
                    running = false;
                    break;
                }
                /* scan the buffer for start and end markers*/
                for (char *currentPos = bufferPos - bytes_read; currentPos < bufferPos; currentPos++) {
                    /* tjsn start */
                    if (*currentPos == '\02') {
                        LOGD("found start at buffer position %lx, value = %d\n",
                                            currentPos - read_buffer,
                               *currentPos);
                        start_found = true;
                        startPos = currentPos;
                    }
                    /* tjsn end */
                    if (*currentPos == '\03') {
                        endPos = currentPos;
                        responseLen = endPos - startPos;
                        LOGD("found end at buffer position %lx, len = %ld, value = %d\n",
                               endPos, responseLen, *currentPos);
                        end_found = true;
                        /* create the tjsn package and return it to java */
                        auto time_stop = std::chrono::high_resolution_clock::now();
                        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(
                                time_stop - time_start);
                        memcpy(response, startPos, responseLen + 1);
                        jstring message = store_env->NewStringUTF((const char*)&response);
                        store_env->CallVoidMethod(store_Wlistener, store_method, message);
                        LOGD("response starts with %d and ends with %d\n", response[0],
                               response[responseLen]);
                        std::cout << "duration = " << duration.count() << std::endl;
                        /***********************************************************************************/
                        /* if there is any thin left in the buffer after the end, move it to the beginning */
                        /***********************************************************************************/
                        LOGD("Comparing %x > %x", bufferPos, endPos+1);
                        if(bufferPos > endPos + 1) {
                            int len = bufferPos - endPos;
                            LOGD("partial response found: len = %d, endPos = %d", len, *(endPos));
                            bufferPos = endPos + 1;
                            LOGD("Preparing to move %d bytes from %x to %x", len, bufferPos,
                                 read_buffer);
                            LOGD("bufferPos[0] = %d\n", bufferPos[0]);
                            memcpy(read_buffer, bufferPos, len);
                            read_buffer[len] = '\0';
                            bufferPos = read_buffer;
                            LOGD("After memcpy/set read_buffer = %d/%d", read_buffer[0], read_buffer[len]);
                            LOGD("read_buffer = '%s'\n", read_buffer);
                        }
                        end_found = false;
                        start_found = false;
                        totalBytesRead = 0;
                        break;
                    }
                }
            } else {
                if(!running) {
                    break;
                }
                //LOGE("nothing read\n");
            }
        }
    };
    return;
}

