#include <stdlib.h>
#include "../include/connectionHandler.h"
#include <thread>

std::string currentDateTime(){
    std::string dateTime;
    boost::posix_time::ptime timeUTC = boost::posix_time::second_clock::universal_time();
    std::string day = std::to_string(timeUTC.date().day());
    std::string month = std::to_string(timeUTC.date().month().as_number());
    std::string year = std::to_string(timeUTC.date().year());
    std::string hour = std::to_string(timeUTC.time_of_day().hours());
    std::string minute = std::to_string(timeUTC.time_of_day().minutes());
    dateTime = day + "-" + month + "-" + year + " " + hour + ":" + minute;
    return dateTime;
}

std::string parserOutput(std::string line){
    std::string answer;
    // handle ACK message
    if (line.substr(0, 2) == "10"){
        line = line.erase(0, 2);
        answer = "ACK " + std::to_string(std::stoi(line.substr(0, 2))) + " ";
        if(line.substr(0, 2) == "04"){
            line = line.erase(0, 2);
            answer += line.substr(0, line.find('\0'));
        } else if (line.substr(0, 2) == "07" || line.substr(0, 2) == "08"){
            line = line.erase(0, 2);
            answer = answer.erase(0, 6);
            answer += line.substr(0, line.length() - 2);
        }
    } else if (line.substr(0, 2) == "09"){ // handle NOTIFICATION message
        line = line.erase(0, 2);
        answer = "NOTIFICATION ";
        if (line[0] == '0'){
            answer += "PM ";

        }
        else if(line[0] == '1'){
            answer += "Public ";
        }
        line = line.erase(0,1);
        // get posting user
        answer += line.substr(0, line.find('\0')) + " ";
        line = line.erase(0, line.find('\0') + 1);
        // get content
        answer += line.substr(0, line.find('\0'));
    } else if (line.substr(0, 2) == "11") { // handle ERROR message
        line = line.erase(0, 2);
        answer = "ERROR " + std::to_string(std::stoi(line.substr(0, 2)));

    }
    return answer;
}

std::string parserInput(std::string line){
    std::string answer;
    if (line.find("REGISTER") < line.size()){
        // add opcode
        answer += "01";
        line = line.erase(0, 9);
        // add username
        answer += line.substr(0, line.find(" ")) + '\0';
        line = line.erase(0, line.find(" ") + 1);
        // add password
        answer += line.substr(0, line.find(" ")) + '\0';
        line = line.erase(0, line.find(" ") + 1);
        // add birthday
        answer += line + '\0';

    } else if (line.find("LOGIN") < line.size()){
        // add opcode
        answer += "02";
        line = line.erase(0, 6);
        // add username
        answer += line.substr(0, line.find(" ")) + '\0';
        line = line.erase(0, line.find(" ") + 1);
        // add password
        answer += line.substr(0, line.find(" ")) + '\0';
        line = line.erase(0, line.find(" ") + 1);
        // add capcha
        answer += line;

    }else if (line.find("LOGOUT") < line.size()){
        answer += "03";

    }else if (line.find("FOLLOW") < line.size()){
        answer += "04";
        line = line.erase(0, 7);
        answer += line[0];
        line = line.erase(0, 2);
        answer += line;

    }else if (line.find("POST") < line.size()){
        answer += "05";
        line = line.erase(0, 5);
        answer += line + " " + '\0';
    }else if (line.find("PM") < line.size()){
        answer += "06";
        line = line.erase(0, 3);
        // add username
        answer += line.substr(0, line.find(" ")) + '\0';
        line = line.erase(0, line.find(" ") + 1);
        // add content
        answer += line + '\0';
        // add timestamp
        answer += currentDateTime() + '\0';
    }else if (line.find("LOGSTAT") < line.size()){
        answer += "07";

    }else if (line.find("STAT") < line.size()){
        answer += "08";
        line = line.erase(0, 5);
        while(line.find(" ") < line.size()){
            answer += line.substr(0, line.find(" ")) + '|';
            line = line.erase(0, line.find(" ") + 1);
        }
        answer += line + '|' + '\0';

    }else if(line.find("BLOCK") < line.size()){
        answer += "12";
        line = line.erase(0, 6);
        answer += line + '\0';
    }
    return answer;
}

void static serverToKeyboard(ConnectionHandler& connectionHandler, std::string& canWrite) {
    while (true) {
        std::string answer;

        if (!connectionHandler.getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        std::string result = parserOutput(answer);

        std::cout << result << std::endl;
        canWrite = "1";
        if (result[4] == '3') {
            connectionHandler.close();
            break;
        }
    }
}

void static keyboardToServer(ConnectionHandler& connectionHandler, std::string& canWrite){
    while (true) {
        if (canWrite == "1") {
            const short bufsize = 4096;
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            std::string answer(parserInput(line));
            canWrite = "0";
            if (!connectionHandler.sendLine(answer)) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                break;
            }
            if (answer == "03"){
                break;
            }
            std::cout << "Sent to server" << std::endl;
        }
    }
}

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    std::cout << "Connected\n";
    std::string canWrite("1");
	std::thread sendThread(keyboardToServer, std::ref(connectionHandler), std::ref(canWrite));
    serverToKeyboard(connectionHandler, canWrite);

    return 0;
}
