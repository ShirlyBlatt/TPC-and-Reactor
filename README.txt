Command to run server:
	TPC server:
		mvn exec:java -Dexec.mainClass="bgu.spl.net.srv.main" -Dexec.args="0 <port>"
	Reactor server: 
		mvn exec:java -Dexec.mainClass="bgu.spl.net.srv.main" -Dexec.args="1 <port> <number_of_threads>"

example:
	REGISTER: REGISTER momo 1234 01-01-1111
	LOGIN: LOGIN momo 1234 1
	LOGOUT: LOGOUT
	FOLLOW: FOLLOW 0 shushu
	UNFOLLOW: FOLLOW 1 shushu
	POST: POST hello world
	PM: PM shushu hey shushu
	LOGSTAT: LOGSTAT
	STAT: STAT momo shushu nini
	BLOCK: BLOCK nini

bad words array location: server\src\main\java\bgu\spl\net\srv\DataBase.java in function "addBadWords"
	
