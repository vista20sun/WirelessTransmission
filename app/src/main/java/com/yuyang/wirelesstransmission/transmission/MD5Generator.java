package com.yuyang.wirelesstransmission.transmission;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by vista on 2018/3/9.
 */

public class MD5Generator {
    MessageDigest digest;
    public MD5Generator(){
        try {
            digest=MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            digest = null;
        }
    }
    public void update(byte[] buffer,int len){
        if(digest==null)
            return;
        digest.update(buffer,0,len);
    }
    public String getMD5(){
        return new BigInteger(1,digest.digest()).toString();
    }


}
