#include <jni.h>
#include <string>
#include <unistd.h>
#include <iostream>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>

#include <android/log.h>
#define LOG_TAG "NativeApp"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define MAXLINE 39
#define HOSTPORT     8080
#define HOSTIP        "127.0.0.1"

char hello [39] = {0};
int sockfd;
struct sockaddr_in     servaddr;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_simpleservercpp_BLEServer_initUdp(JNIEnv *env, jobject thiz) {
    LOGI("[BlueTooth] init udp");

    if ( (sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0 ) {
        perror("socket creation failed");
        exit(EXIT_FAILURE);
    }

    memset(&servaddr, 0, sizeof(servaddr));
    // Filling server information
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(HOSTPORT);
    // servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_addr.s_addr = inet_addr(HOSTIP);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_simpleservercpp_BLEServer_sendOverUdp(JNIEnv *env, jobject /* this */, jbyteArray byteArray) {

    LOGI("[BlueTooth] Send bluetooth buffer over udp socket");

    // Convert jbyteArray to native C++ buffer
    jbyte *tempBuffer = env->GetByteArrayElements(byteArray, nullptr);
    jsize length = env->GetArrayLength(byteArray);
    // Copy data from tempBuffer (jbyte) to buffer (char)
    memcpy(hello, &tempBuffer[2], MAXLINE);

    sendto(sockfd, (const char *)hello, MAXLINE,
           MSG_CONFIRM, (const struct sockaddr *) &servaddr,
           sizeof(servaddr));

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_simpleservercpp_BLEServer_deInitUdp(JNIEnv *env, jobject thiz) {
    close(sockfd);
}
