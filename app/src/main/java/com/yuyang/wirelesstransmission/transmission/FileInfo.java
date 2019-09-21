package com.yuyang.wirelesstransmission.transmission;

import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.yuyang.wirelesstransmission.R;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Created by vista on 2017/9/28.
 */

public class FileInfo {
    public final static String split=":::";
    private String name;
    private long size;
    private boolean send;
    private DocumentFile documentFile;
    public enum verification {verified,unverified,uncheck}
    private verification verify;
    public enum syncType{synced,conflict,missfile,newfile};
    private syncType type;

    public syncType getType() {
        return type;
    }

    public void setType(syncType type) {
        this.type = type;
    }

    public FileInfo(String name, long size,boolean send) {
        this.name = name;
        this.size = size;
        this.send=send;
        documentFile=null;
        verify=verification.uncheck;
    }

    public DocumentFile getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(DocumentFile documentFile) {
        this.documentFile = documentFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (size != fileInfo.size) return false;
        if (send != fileInfo.send) return false;
        return name.equals(fileInfo.name);

    }
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (send ? 1 : 0);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getExtName(){
        int split=name.lastIndexOf(".");
        if(split==-1){
            return null;
        }
        return name.substring(split+1).toLowerCase(Locale.US);
    }
    public String getMime() {
        if(isDir())
            return "Folder/*";
        return getMime(getExtName());
    }
    public String getMimeType(){
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtName());
    }

    public long getSize() {
        return size;
    }
    public boolean isDir(){
        return size<0;
    }

    public void setSize(long size) {
        this.size = size;
    }
    public String getMime(String ext){
        String type=getOtherMime(ext);
        if(type!=null)
            return type;
        type= MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if(type!=null&&!type.isEmpty())
            return type;
        return "file/*";
    }
    public boolean isSend(){
        return send;
    }
    public int getTypeIconID(){
        String mime = getMime();
        String type[]=mime.split("/");
        int resID;
        if(isDir())
            resID=R.drawable.icon_folder;
        else
            switch (type[0]){
                case "text":resID=R.drawable.icon_text;break;
                case "image":resID=R.drawable.icon_pic;break;
                case "video":resID=R.drawable.icon_video;break;
                case "audio":resID=R.drawable.icon_audio;break;
                case "application": resID=getApplicationTypeID(type[1]);break;
                default:resID=R.drawable.icon_unknow;
            }

        return resID;
    }
    public static String getOtherMime(String ext){
        if(ext==null)
            return "file/*";
        switch (ext){
            case "7z":
            case "gz":
            case "tar.gz":
                return "application/compressed";
            case "rmvb":
                return "video/x-video";
            case "ape":
            case "ogg":
                return "audio/x-audio";
            case "ai":
                return "application/pdf";
            default: return null;
        }
    }
    public static int getApplicationTypeID(String type){
        if(type.contains("word")
                ||type.contains("powerpoint")
                ||type.contains("excel")
                ||type.contains("officedocument"))
            return R.drawable.icon_office;
        else if(type.contains("pdf"))
            return R.drawable.icon_pdf;
        else if(type.contains("vnd.android.package"))
            return R.drawable.icon_apk;
        else if(type.contains("zip")
                ||type.contains("rar")
                ||type.contains("tar")
                ||type.contains("compressed"))
            return R.drawable.icon_pack;
        else if(type.equals("ogg"))
            return R.drawable.icon_audio;
        return R.drawable.icon_unknow;
    }
    public void setVerify(boolean verify){
        this.verify = verify?verification.verified:verification.unverified;
        Log.d("debugging",""+verify);
    }
    public void setVerify(verification ver){
        verify=ver;
    }
    public verification getVerify(){
        return verify;
    }
}
