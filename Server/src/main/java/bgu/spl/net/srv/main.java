package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoderImpl;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class main {
    public static void main(String[] args){
        // args[0] = 0/1 TPC/REACTOR
        // args[1] = port
        // args[2] = number of threads (for Reactor server only)
        if (args[0].equals("0")) {
            System.out.println("Tread-Per-Client SERVER");
            try (Server<String> server = Server.threadPerClient(Integer.parseInt(args[1]), () -> new BidiMessagingProtocolImpl(), MessageEncoderDecoderImpl::new);) {
                server.serve();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (args[0].equals("1")){
            System.out.println("REACTOR SERVER");
            try(Server<String> server = Server.reactor(Integer.parseInt(args[2]), Integer.parseInt(args[1]), () -> new BidiMessagingProtocolImpl() {}, MessageEncoderDecoderImpl::new);){
                server.serve();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        else {
            System.out.println("bad input");
        }
    }
}
