CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lpthread

all: mainClient
	g++ -o bin/mainClient bin/connectionHandler.o bin/mainClient.o $(LDFLAGS) 

mainClient: bin/connectionHandler.o bin/mainClient.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/mainClient.o: src/mainClient.cpp
	g++ $(CFLAGS) -o bin/mainClient.o src/mainClient.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
