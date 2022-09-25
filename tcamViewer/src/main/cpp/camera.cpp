//
// Created by ahjim on 9/25/22.
//
#include <arpa/inet.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#define PORT 5001
#define BUFFER_LENGTH 64565

  int sock = 0, valread, client_fd;
	struct sockaddr_in serv_addr;
	char* cmd;
	char* pos, *start, *end;
	char buffer[BUFFER_LENGTH] = { 0 };
	char response[BUFFER_LENGTH] = { 0 };
	long responseLen;

int  listen()
{
	if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
		printf("\n Socket creation error \n");
		return -1;
	}

	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(PORT);

	// Convert IPv4 and IPv6 addresses from text to binary
	// form
	if (inet_pton(AF_INET, "192.168.0.18", &serv_addr.sin_addr)
		<= 0) {
		printf(
			"\nInvalid address/ Address not supported \n");
		return -1;
	}

	if ((client_fd
		= connect(sock, (struct sockaddr*)&serv_addr,
				sizeof(serv_addr)))
		< 0) {
		printf("\nConnection Failed \n");
		return -1;
	}
	// Test if the socket is in non-blocking mode:
	if(fcntl(sock, F_GETFL) & O_NONBLOCK) {
	  // socket is non-blocking
	  printf("fd is non blocking\n");
	}

	// Put the socket in non-blocking mode:
	if(fcntl(sock, F_SETFL,
		 fcntl(sock, F_GETFL) | O_NONBLOCK) < 0) {
	  // handle error
	  printf("failed to set to non blocking\n");
	} else {
	  printf("set fd to non blocking\n");
	}

	pos = buffer;
	bool foundEnd = false;
	do {
	  valread = recv(sock, pos, BUFFER_LENGTH, 0);
	  if(valread > 0) {
	    pos = pos + valread;
	    printf("read %d bytes\n", valread);
	    for(char* p = pos-valread; p < pos; p++) {
	      if(*p == '\02') {
		printf("found start at %lx\n", p-buffer);
		start = p;
	      }
	      if(*p == '\03') {
		end = p;
		responseLen = end-start;
		printf("found end at %lx, len = %ld\n", p-buffer, responseLen);
		foundEnd = true;
		memcpy(&response[0], &start[0], responseLen+1);
		//		if(end < pos) {

	      }
	    }
	  }
	} while(!foundEnd);
	printf("start = %x, end = %x\n", response[0], response[responseLen]);
	// closing the connected socket
	close(client_fd);
	return 0;
}
