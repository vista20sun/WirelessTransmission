package com.yuyang.wirelesstransmission.transmission;

import java.io.IOException;

/**
 * Created by vista on 2017/9/28.
 */
 public class Signal{
    String data;
    tranSignal sig;
    Signal (tranSignal s){
        sig=s;
        data=null;
    }
    Signal (tranSignal s,String str){
        sig=s;
        data=str;
    }
    static Signal ParasChars(int len,char[] chars) throws IOException  {
        if(len<=0)
            throw new IOException("disconnect");
        if (len==1){
            return new Signal(tranSignal.getSig(chars));
        }
        return new Signal(tranSignal.getSig(chars),String.copyValueOf(chars,1,len-1));
    }

    public String getData() {
        return data;
    }

    public tranSignal getSig() {
        return sig;
    }
}