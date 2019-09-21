package com.yuyang.wirelesstransmission.transmission;

import android.app.Activity;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.yuyang.wirelesstransmission.transmission.ClientConnection.FileInfo_Sync.syncMode;

/**
 * Created by vista on 2018/1/18.
 */

public class ClientConnection {

    private final static int blockSize = 8192, cancelOffsect=20480 ;
    public ClientConnection(Client_t client_t, TransManage transManage, DocumentFile documentFile, ConnectionCompact clientConnectionCompact){
        client = client_t;
        manage = transManage;
        download=documentFile;
        connectionCompact = clientConnectionCompact;
        syncQueue=null;
        dir_sw=false;
        pause_all=false;
        rec_ready=false;
        cancel_ready=false;
        cancel_all=false;
        cancel_pause=false;
        canceling=false;
        linklost=false;
        sync_apply = false;
        taskType=0;
    }

    public ClientConnection(ConnectionCompact clientConnectionCompact){
        this(null,null,null, clientConnectionCompact);
    }

    public boolean isSync_apply() {
        return sync_apply;
    }

    public boolean isTask_wait() {
        return task_wait;
    }

    public void setTask_wait(boolean task_wait) {
        this.task_wait = task_wait;
    }

    public boolean isTask_cancel() {
        return task_cancel;
    }

    public void setTask_cancel(boolean task_cancel) {
        this.task_cancel = task_cancel;
    }

    public boolean isCancel_pause() {
        return cancel_pause;
    }

    public void setCancel_pause(boolean cancel_pause) {
        this.cancel_pause = cancel_pause;
    }

    public boolean isLinklost() {
        return linklost;
    }

    public void setLinklost(boolean linklost) {
        this.linklost = linklost;
        if(syncQueue!=null)
            syncQueue.add("finish");
    }

    public boolean isDir_sw() {
        return dir_sw;
    }

    public void setDir_sw(boolean dir_sw) {
        this.dir_sw = dir_sw;
    }

    public boolean isPause_all() {
        return pause_all;
    }

    public void setPause_all(boolean pause_all) {
        this.pause_all = pause_all;
    }

    public boolean isRec_ready() {
        return rec_ready;
    }

    public void setRec_ready(boolean rec_ready) {
        this.rec_ready = rec_ready;
    }

    public boolean isCancel_ready() {
        return cancel_ready;
    }

    public void setCancel_ready(boolean cancel_ready) {
        this.cancel_ready = cancel_ready;
    }

    public boolean isCancel_all() {
        return cancel_all;
    }

    public void setCancel_all(boolean cancel_all) {
        this.cancel_all = cancel_all;
    }

    public boolean isCanceling() {
        return canceling;
    }

    public void setCanceling(boolean canceling) {
        this.canceling = canceling;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }

    public TransManage getManage() {
        return manage;
    }

    public void bindTransManage(TransManage manage) {
        this.manage = manage;
    }

    public ConnectionCompact getConnectionCompact() {
        return connectionCompact;
    }

    public void setConnectionCompact(ConnectionCompact connectionCompact) {
        this.connectionCompact = connectionCompact;
    }

    public DocumentFile getDownload() {
        return download;
    }

    public void setDownload(DocumentFile download) {
        this.download = download;
    }

    public Client_t getClient() {
        return client;
    }

    public void setClient(Client_t client) {
        this.client = client;
    }
    public void buildClient(String ip, int port_s, int timeOut,Activity activity){
        client=Client_t.getClient_t(ip,port_s,timeOut,activity);
    }
    public int openClient(){
        if (client==null)
            return -1;
        return client.open();
    }
    public void closeClient(){
        if(client==null)
            return;
        client.close();
        client=null;
    }

    public void send(tranSignal t){
        client.send(t);
    }
    public void send(tranSignal t, String str){
        client.send(t,str);
    }

    public void setTrans_thread(Thread trans_thread) {
        this.trans_thread = trans_thread;
    }


    public synchronized void add_task(FileInfo info){
        manage.addTask(info);
        connectionCompact.onTaskAdd(manage.getTaskSize());
    }

    public synchronized void signalHandel(){
        Signal signal;
        try {
            signal = client.receive();
        } catch (InterruptedException e) {
            connectionCompact.OnLinkLost();
            return;
        }
        Log.d("signalHandleDebug",signal.sig.name());
        switch (signal.getSig()){
            case Dir_info:
                if(taskType==0) {
                    taskType = GETDIR;
                    connectionCompact.onSwitchDir();
                }else{
                    taskType=0;
                    dir_sw=false;
                    connectionCompact.onDirSwitched();
                }
                return;
            case File_info:
                Log.d("siguri", signal.getData());
                String datas[]=signal.getData().split(FileInfo.split);
                connectionCompact.onAddFileInfo(new FileInfo_rec(datas[0],Long.parseLong(datas[1]),datas[2]));
                client.send(tranSignal.INFO_D);
                return;
            case Cancel_task:
                int idx=Integer.parseInt(signal.getData());
                if(idx==0){
                    task_cancel=true;
                    canceling=true;
                    return;
                }else{
                    manage.removeTask(idx);
                    connectionCompact.onTaskCancelAt(idx);
                    canceling=false;
                    client.send(tranSignal.Canceled,String.valueOf(idx));
                }
                return;
            case Cancel_at:
                Long at=Long.parseLong(signal.getData());
                manage.getTask(0).setSize(at);
                client.send(tranSignal.Cancel_Ready);
                cancel_ready=true;
                return;
            case Canceled:
                int idc=Integer.parseInt(signal.getData());
                if(idc!=0){
                    manage.removeTask(idc);
                    connectionCompact.onTaskCancelAt(idc);
                    canceling=false;
                    cancel_ready=true;
                }
                return;
            case Cancel_Ready:
                cancel_ready=true;
                return;
            case Cancel_all:
                if(manage.isEmptyTask())
                    return;
                cancel_all=true;
                task_cancel=true;
                if(manage.getTask(0).isSend())
                    canceling=true;
                else
                    cancel_pause=true;//add
                return;
            case Continue:
                connectionCompact.onTaskContinue();
                pause_all=false;
                return;
            case FT_Finish:
            case Send_Ready:
            case Get_Ready:
                rec_ready=true;
                return;
            case Stop:
                connectionCompact.onTaskStop();
                pause_all=true;
                return;
            case Close:
                pause_all=true;
                task_cancel=true;
                client.close();
                return;
            case Sync_info:
                if(syncQueue!=null)
                    syncQueue.add(signal.getData());
                client.send(tranSignal.INFO_D);
                syncReceived++;
                connectionCompact.onSecondarySyncProcessUpdate(syncReceived);
                return;
            case Sync_finish:
                syncQueue.add("Finish");
                sync_done=true;
                return;
            case Sync_count:
                syncTaskCheck(signal.data);
                return;
        }
    }

    public synchronized void cancelAllTask(){
        if(manage.isEmptyTask())
            return;
        client.send(tranSignal.Cancel_all);
        task_cancel=true;
        cancel_all=true;
        if(manage.getTask(0).isSend())
            canceling=true;//add
        else
            cancel_pause=true;

    }
    private synchronized void cancelAll(){
        if(!cancel_all)
            return;
        manage.clearTask();
        connectionCompact.onTaskCancelALL();
        cancel_all=false;
        task_cancel=false;
        cancel_pause=false;
        canceling=false;
    }
    Thread getTrans_thread(){
        return new Thread(){
            public void run(){
                trans_thread=this;
                connectionCompact.onMakeNotification();
                connectionCompact.onTaskStart();
                while (!manage.isEmptyTask()){
                    if(linklost)
                        break;
                    pause();
                    TransInfo info=manage.getTask(0);
                    connectionCompact.onNotificationInit();
                    try{
                        if(info.isSend())
                            send_file(info);
                        else
                            get_file(info);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(cancel_all) {
                        cancelAll();
                        break;
                    }
                }
                connectionCompact.onAllTaskFinished();
                trans_thread=null;
            }
        };
    }

    public synchronized void start_task(){
        if(trans_thread==null)
            getTrans_thread().start();
    }


    private void pause(){
        connectionCompact.onNotificationIndeterminate(task_wait||cancel_pause||pause_all);
        while (task_wait||pause_all||cancel_pause){
            if((task_cancel||canceling)&&!cancel_pause)
                return;
            if(task_wait&&rec_ready){
                task_wait=false;
                rec_ready=false;
                continue;
            }
            if(cancel_ready&&cancel_pause){
                cancel_ready=false;
                cancel_pause=false;
                continue;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void get_file(TransInfo info) throws IOException {
        FileInfo_rec info_rec=(FileInfo_rec) info.getInfo();
        DocumentFile tar = FileAccess.getEmptyFile(download,info_rec.getDirPath(),info_rec.getNewname());
        BufferedOutputStream outputStream = new BufferedOutputStream(connectionCompact.onRequireOutStream(tar));
        MD5Generator md5 = client.isVerification()?new MD5Generator():null;
        int len;
        byte[] buff = new byte[blockSize];
        client.send(tranSignal.Get_Ready,info.toString());
        task_wait=true;
        if(info.getSize()>0) {
            connectionCompact.onNotificationUpdate();
            while ((len = client.read(buff)) != -1) {
                if (linklost)
                    return;
                pause();
                outputStream.write(buff, 0, len);
                if(md5!=null)
                    md5.update(buff,len);
                info.finishOffest(len);
                connectionCompact.onProgressUpdate();
                if (info.getFinished() >= info.getSize())
                    break;
            }
        }

        try{
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(canceling)
            canceling=false;
        if(cancel_pause)
            cancel_pause=false;
        if(task_cancel) {
            task_cancel = false;
            manage.removeTask(0);
            //connectionCompact.onTaskCancelAt(0);
            tar.delete();
        }else{
            FileInfo finishInfo=manage.pollTask();
            finishInfo.setDocumentFile(tar);
            if(md5!=null) {
                String cmpMD5 = client.getMD5();
                finishInfo.setVerify(md5.getMD5().equals(cmpMD5));
            }
            connectionCompact.onAddFinishedTask(finishInfo);
        }
        outputStream.close();
        connectionCompact.onTaskFinished();
    }
    void send_file(TransInfo info) throws IOException {
        DocumentFile src = info.getDocumentFile();
        InputStream in;
        try {
            in= connectionCompact.onRequireInStream(src);
        } catch (FileNotFoundException e) {
            client.send(tranSignal.Cancel_task);
            return;
        }
        byte bytes[]=new byte[blockSize];
        MD5Generator md5 = client.isVerification()?new MD5Generator():null;

        int len;
        long size=src.length();
        client.send(tranSignal.Send_Ready,info.toString());
        task_wait=true;
        boolean canceled=false;
        connectionCompact.onNotificationUpdate();
        while((len=in.read(bytes))!=-1){
            if(linklost)
                return;
            if(task_cancel&&!canceled){
                canceled=true;
                long cat=info.getFinished()+cancelOffsect;
                if(cat<info.getSize())
                    info.setSize(cat);
                client.send(tranSignal.Cancel_at,String.valueOf(info.getSize()));
                cancel_pause=true;
                size=cat;
            }
            pause();
            if(task_cancel&&!canceled)
                continue;
            client.write(bytes,0,len);
            if(md5!=null){
                md5.update(bytes,len);
            }
            info.finishOffest(len);
            connectionCompact.onProgressUpdate();

            if(info.getFinished()>=size)
                break;
        }
        try{
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(canceling)
            canceling=false;
        if(cancel_pause)
            cancel_pause=false;
        if(task_cancel) {
            task_cancel = false;
            manage.removeTask(0);
        }else {
            if(md5!=null){
                client.write(md5.getMD5().getBytes());
            }
            client.send(tranSignal.Send);
            manage.pollTask();
        }
        in.close();
        connectionCompact.onTaskFinished();
    }
    synchronized void requestFile(FileInfo_rec rec){
        requestFile(rec,false);
    }
    synchronized void requestFile(FileInfo_rec rec,boolean sync){
        for(int i=0;i<manage.getTaskSize()&&!sync;i++){
            if(manage.getInfo(i).equals(rec)){
                connectionCompact.onTaskRepeat(rec,i);
                return;
            }
        }
        DocumentFile df=FileAccess.findFile(download,rec.getDirPath(),rec.getName(),false);
        if(!sync&&df!=null&&df.exists()&&df.isFile()){
            connectionCompact.onFileRepeat(rec,df);
            return;
        }
        add_task(rec);
        client.send(tranSignal.Get_File,rec.getPath());
        start_task();
    }

    public synchronized void switch_dir(FileInfo_rec info){
        if(dir_sw)
            return;
        String dir=info.getPath();
        connectionCompact.onRecordPath(dir);
        switch_dir(dir);
    }
    public synchronized void switch_dir(String path){
        client.send(tranSignal.Get_Dir,path);
        dir_sw=true;
        connectionCompact.onSwitchDirStart();
    }
    public synchronized void requestItem(FileInfo_rec info){
        if(info.isDir())
            switch_dir(info);
        else
            requestFile(info);
    }

    public synchronized void switchPause(){
        pause_all=!pause_all;

        client.send(pause_all?tranSignal.Stop:tranSignal.Continue);
        connectionCompact.onPauseSwitch(pause_all);
    }
    public synchronized void cancelItem(int pos){
        if(canceling||sync_apply)
            return;
        canceling=true;
        client.send(tranSignal.Cancel_task,String.valueOf(pos));
        if(pos==0)
            task_cancel = true;
        else
            cancel_pause=true;
    }

    public String getDocumentMD5(DocumentFile df){
        try {
            InputStream in = connectionCompact.onRequireInStream(df);
            int len;
            byte bytes[]=new byte[blockSize];
            MD5Generator md5 = new MD5Generator();
            while((len=in.read(bytes))!=-1){
                if(sync_stop)
                    return null;
                md5.update(bytes,len);
            }
            return md5.getMD5();
        } catch (IOException e) {
            return null;
        }
    }
    public void clearSyncList(){
        syncList.clear();
        syncList = null;
    }
    public void applySyncList(){
        if(sync_thread!=null)
            return;
        new Thread(){
            public void run(){
                if(syncList==null) {
                    return;
                }
                sync_thread=this;
                sync_apply = true;
                connectionCompact.onSyncApplying(true);
                client.send(tranSignal.Sync_apply);
                for (FileInfo_Sync x:syncList){
                    if(x.mode== syncMode.skip)
                        continue;
                    else if(x.mode == syncMode.download){
                        switch (x.fileInfo.getType()){
                            case missfile:
                            case conflict: requestFile((FileInfo_rec)x.fileInfo,true); break;//checked
                            case newfile: ((FileInfo_send)x.fileInfo).getDocumentFile().delete();break;
                        }
                    }else if(x.mode == syncMode.upload){
                        switch (x.fileInfo.getType()){
                            case newfile: addSyncUpLoadTask((FileInfo_send)x.fileInfo);break;//checked
                            case missfile: send(tranSignal.deleteFile,((FileInfo_rec)x.fileInfo).getPath());break;//checked
                            case conflict://checked
                                FileInfo_rec rec = (FileInfo_rec) x.fileInfo;
                                DocumentFile df=FileAccess.findFile(download,rec.getDirPath(),rec.getName(),false);
                                if(df!=null) {
                                    FileInfo_send send=new FileInfo_send(df);
                                    send.setPath(download);
                                    addSyncUpLoadTask(send);
                                }
                                break;
                        }
                    }
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                syncList.clear();
                client.send(tranSignal.Sync_apply);
                sync_apply = false;
                connectionCompact.onSyncApplying(false);
                syncList=null;
                sync_thread=null;
            }
        }.start();
    }
    private void addSyncUpLoadTask(FileInfo_send send){
        add_task(send);
        String path = send.getPath();
        path=path.substring(0,path.length()-send.getName().length());
        send(tranSignal.Send_File,String.format("%s:::%d:::%s",send.getName(),send.getSize(),path));
        start_task();
    }

    private void addServerMissingFile(HashSet<String> checked,DocumentFile file){
        DocumentFile list[]=file.listFiles();
        for (DocumentFile x:list){
            if(x.isDirectory()){
                addServerMissingFile(checked,x);
                continue;
            }
            String path = x.getUri().getPath();
            path=path.substring(download.getUri().getPath().length());
            if(checked.contains(path)) continue;
            FileInfo_send fileInfo = new FileInfo_send(x);
            fileInfo.setPath(path);
            fileInfo.setType(FileInfo.syncType.newfile);
            connectionCompact.onSyncAdd(new FileInfo_Sync(fileInfo));
            Log.d("SYNCTEST-ADD", fileInfo.getName());
        }
    }
    private void syncTaskCheck(String str){
            syncMax = Integer.parseInt(str);
            connectionCompact.onSyncProcessMaxSet(syncMax);
            syncReceived = 0;
    }
    private Thread getSync_thread(){
        return new Thread(){
            @Override
            public void run(){
                sync_thread=this;
                sync_stop=false;
                HashSet <String> checkedFiles= new HashSet<>();
                int syncChecked=0;
                while (!sync_stop&&!linklost){
                    String str;
                    try {
                        str = syncQueue.take();
                        Log.d("SYNCTEST-GET", str+"["+syncQueue.size()+"]");
                    } catch (InterruptedException e) {
                        continue;
                    }
                    syncChecked++;
                    connectionCompact.onSyncProcessUpdata(syncChecked);
                    if(str.equals("Finish"))
                        break;
                    String datas[]=str.split(FileInfo.split);
                    FileInfo_rec rec= new FileInfo_rec(datas[0],Long.parseLong(datas[1]),datas[2]);
                    DocumentFile localFile = FileAccess.findFile(download,rec.getDirPath(),rec.getName(),false);
                    if(localFile==null){
                        rec.setType(FileInfo_rec.syncType.missfile);
                        connectionCompact.onSyncAdd(new FileInfo_Sync(rec));
                        checkedFiles.add(rec.getPath());
                        Log.d("SYNCTEST-LMF", rec.getName());
                        continue;
                    }else{
                        String md5 = getDocumentMD5(localFile);
                        if(md5==null)
                            break;
                        if(md5.equals(datas[3].trim())) {
                            checkedFiles.add(rec.getPath());
                            Log.d("SYNCTEST-SKIP", rec.getName());
                            continue;
                        }
                        rec.setType(FileInfo_rec.syncType.conflict);
                        checkedFiles.add(rec.getPath());
                        connectionCompact.onSyncAdd(new FileInfo_Sync(rec));
                        Log.d("SYNCTEST-COF", rec.getName());
                    }
                }
                connectionCompact.onSyncProcessUpdata(syncMax);
                connectionCompact.onSecondarySyncProcessUpdate(syncMax);
                syncQueue=null;
                addServerMissingFile(checkedFiles,download);
                client.send(tranSignal.Sync_finish);
                sync_thread=null;
                connectionCompact.onSyncFinish();
            }
        };
    }

    public void syncStart(){
        if(sync_thread!=null||!manage.isEmptyTask()||dir_sw) {
            connectionCompact.onSyncStart(false);
            return;
        }
        syncList = new ArrayList<>();
        syncQueue= new LinkedBlockingQueue<>();
        sync_done=false;
        getSync_thread().start();
        send(tranSignal.Sync_start);
        connectionCompact.onSyncStart(true);
    }
    public void syncStop(){
        if(!sync_done)
            send(tranSignal.Sync_stop);
        else
            sync_stop=true;
        //sync_stop=true;
    }
    public ArrayList<FileInfo_Sync> getSyncList(){
        return syncList;
    }


    private DocumentFile download;
    private Client_t client;
    private Thread trans_thread,sync_thread;
    private boolean task_wait,task_cancel,cancel_pause,linklost,dir_sw,pause_all,rec_ready,cancel_ready,cancel_all,canceling,sync_stop,sync_done,sync_apply;
    private int taskType, syncReceived,syncMax;
    private final static int GETDIR=123;
    private TransManage manage;
    private ConnectionCompact connectionCompact;
    private BlockingQueue<String> syncQueue;
    private ArrayList<FileInfo_Sync> syncList;

    public interface ConnectionCompact {
        InputStream onRequireInStream(DocumentFile src) throws FileNotFoundException;
        OutputStream onRequireOutStream(DocumentFile tar) throws FileNotFoundException;
        void onTaskStart();
        void onProgressUpdate();
        void onTaskFinished();
        void onAllTaskFinished();
        void onMakeNotification();
        void onNotificationUpdate();
        void onAddFinishedTask(final FileInfo info);
        void onNotificationIndeterminate(boolean state);
        void onNotificationInit();
        void onTaskStop();
        void onTaskContinue();
        void onTaskCancelALL();
        void onTaskAdd(int pos);
        void onTaskCancelAt(int index);
        void onPauseSwitch(boolean state);
        void onAddFileInfo(FileInfo_rec file);
        void onSwitchDir();
        void onTaskRepeat(final FileInfo_rec rec,final int idx);
        void onFileRepeat(FileInfo_rec rec,DocumentFile df);
        void onRecordPath(String path);
        void onSwitchDirStart();
        void onDirSwitched();
        void OnLinkLost();
        void onSyncFinish();
        void onSyncStart(boolean success);
        void onSyncAdd(FileInfo_Sync sync);
        void onSyncProcessMaxSet(int p);
        void onSecondarySyncProcessUpdate(int p);
        void onSyncProcessUpdata(int p);
        void onSyncApplying(boolean apply);
    }

    public static class FileInfo_Sync{
        private FileInfo fileInfo;
        public enum syncMode{skip,upload,download};
        protected syncMode mode;
        public FileInfo_Sync (FileInfo info){
            fileInfo = info;
            mode=syncMode.skip;
        }
        public FileInfo getFileInfo(){
            return fileInfo;
        }
        public syncMode getMode(){
            return mode;
        }
        public void setMode(syncMode mode){
            this.mode=mode;
        }

    }

}
