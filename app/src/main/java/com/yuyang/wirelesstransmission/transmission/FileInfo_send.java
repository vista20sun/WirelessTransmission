package com.yuyang.wirelesstransmission.transmission;

import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import java.io.OutputStream;

/**
 * Created by vista on 2017/9/28.
 */

public class FileInfo_send extends FileInfo {

    private String path;
    public FileInfo_send(DocumentFile document){
        super(document.getName(),document.isDirectory()?-1:document.length(),true);
        setDocumentFile(document);
        path=null;
    }
    public String getPath(){
        return path;
    }
    public void setPath(DocumentFile root){
        //String path = x.getUri().getPath();
        //path=path.substring(download.getUri().getPath().length());
        path = getUri().getPath().substring(root.getUri().getPath().length());
    }
    public void setPath(String str){
        path=str;
    }
    public DocumentFile getDocumentFile(){
        return super.getDocumentFile();
    }
    public Uri getUri(){
        return getDocumentFile().getUri();
    }
    public String toString(){
        return String.format("%s,%d,%s",getName(),getSize(),getUri());
    }

}
