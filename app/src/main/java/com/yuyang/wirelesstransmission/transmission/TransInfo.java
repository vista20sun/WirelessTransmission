package com.yuyang.wirelesstransmission.transmission;

import android.support.v4.provider.DocumentFile;

/**
 * Created by vista on 2017/9/28.
 */

public class TransInfo {
    private FileInfo info;
    private DocumentFile documentFile;
    private long finished;
    private long trans_size;

    public DocumentFile getDocumentFile() {
        return documentFile;
    }
    public String getName(){
        return info.getName();
    }
    public long getSize(){
        if(trans_size<0)
            return info.getSize();
        return trans_size;
    }
    public void setSize(long l){
        trans_size=l;
    }

    public void setDocumentFile(DocumentFile documentFile) {
        this.documentFile = documentFile;
    }

    public TransInfo(FileInfo info){
        trans_size=-1;
        this.info=info;
        finished=0;
        if(info.isSend())
            documentFile=((FileInfo_send)info).getDocumentFile();
        else
            documentFile=null;
    }

    public FileInfo getInfo() {
        return info;
    }

    public synchronized long getFinished() {
        return finished;
    }

    public synchronized void setFinished(long finished) {
        this.finished = finished;
    }

    public synchronized void finishOffest(long offset){
        finished+=offset;
    }
    public boolean isSend(){
        return info.isSend();
    }

    public int getProc(){
        return (int)(finished*100/info.getSize());
    }
}
