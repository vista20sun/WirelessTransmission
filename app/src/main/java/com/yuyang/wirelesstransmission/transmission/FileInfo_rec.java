package com.yuyang.wirelesstransmission.transmission;

import android.util.Log;


/**
 * Created by vista on 2017/9/28.
 */

public class FileInfo_rec extends FileInfo {
    private String path; // a part of uri String;
    private String newname;



    public FileInfo_rec(String name, long size, String path) {
        super(name, size, false);
        this.path = path;
        newname=null;
    }
    public FileInfo_rec(String name, long size, String path,int ver){
        this(name,size,path);
        super.setVerify(verification.values()[ver]);
    }

    public String getDirPath(){
        return path.substring(0,path.length()-getName().length());
    }
    public void setNewname(String newname) {
        this.newname = newname;
    }

    public String getNewname(){
        if(newname==null)
            return getName();
        return newname;

    }


    public boolean isDone(){
        return super.getDocumentFile()!=null;
    }

    public String getPath() {
        Log.d("urid", path);
        return path;
    }


}
