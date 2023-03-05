package bgu.spl.net.api;

import java.nio.charset.StandardCharsets;

public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<String>{
    private String resultMsg = "";

    @Override
    public String decodeNextByte(byte nextByte) {
        if (nextByte == ';'){
            String ans = resultMsg;
            resultMsg = "";
            return ans;
        }
        else {
            resultMsg += (char)nextByte;
            return null;
        }
    }

    @Override
    public byte[] encode(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }
}
