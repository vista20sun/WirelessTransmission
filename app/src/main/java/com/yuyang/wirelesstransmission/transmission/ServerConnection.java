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

/**
 * Created by vista on 2018/2/14.
 */

public class ServerConnection {
    private Server_t server;
    private DocumentFile sharedir;
    private TransManage manage;
    private ConnectionCompact connectionCompact;
    private boolean task_wait,task_cancel,cancel_pause,linklost,pause_all,rec_ready,cancel_ready,cancel_all,canceling,closed,info_pause,target_found,sync_stop;
    private boolean verification;
    private Thread trans_thread,link_thread,sync_thread;
    private String rootPath;
    private final static int blockSize=8192;

    public ServerConnection(ConnectionCompact connectionCompact){
        this.connectionCompact = connectionCompact;
        initFlags();
        verification =false;
    }
    public ServerConnection(Server_t server,DocumentFile sharedir, TransManage manage,ConnectionCompact connectionCompact){
        this(connectionCompact);
        this.server=server;
        this.sharedir=sharedir;
        this.manage=manage;
    }
    public void initFlags(){
        task_cancel=false;
        task_wait=false;
        cancel_pause=false;
        linklost=false;
        pause_all=false;
        rec_ready=false;
        cancel_ready=false;
        canceling=false;
        cancel_all=false;
        closed=false;
        target_found=false;
        trans_thread=null;
        sync_thread=null;
        sync_stop=false;
    }

    public void buildServer(int port_s,int port_t, int timeOut,Activity activity){
        server = Server_t.getServer_t(port_s,port_t,timeOut,activity);
    }

    public void send(tranSignal t){
        server.send(t);
    }

    public void send(tranSignal t,String str){
        server.send(t,str);
    }

    public void closeServer(){
        if(server!=null)
            server.close();
    }

    public void openServer(final String ssid, final String ip,final boolean verification){
        this.verification=verification;
        new Thread(){
            public void run(){
                if(trans_thread!=null||link_thread!=null)
                    return;
                link_thread=this;
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try{
                    connectionCompact.log(String.format("使用WLAN %s建立服务",ssid));
                    connectionCompact.log(String.format("本机地址: %s",ip));
                    connectionCompact.log(String.format("监听端口:#%d, 等待连接",server.getPort_sig()));
                    connectionCompact.onSocketSet(ip,ssid,server.getPort_sig());
                    server.open_s(verification);
                    connectionCompact.log("校验功能:"+(verification?"已启用":"已禁用"));
                    connectionCompact.log("服务端已连接");
                    connectionCompact.log(String.format("文件传输通道建立于端口#%d",server.getPort_file()));
                    server.open_f();
                    connectionCompact.onStartServerSuccess();
                    connectionCompact.onSocketAccept();
                    connectionCompact.log("服务启动完成");
                    connectionCompact.log("开始工作");
                } catch (IOException e) {
                    connectionCompact.onStartServerFail();
                }finally {
                    link_thread=null;
                    return;
                }
            }
        }.start();
    }



    public Server_t getServer() {
        return server;
    }

    public void setServer(Server_t server) {
        this.server = server;
    }

    public DocumentFile getSharedir() {
        return sharedir;
    }

    public void setSharedir(DocumentFile sharedir) {
        if(sharedir==null)
            return;
        this.sharedir = sharedir;
        rootPath = sharedir.getUri().getPath();
    }

    public TransManage getManage() {
        return manage;
    }

    public void setManage(TransManage manage) {
        this.manage = manage;
    }

    public ConnectionCompact getConnectionCompact() {
        return connectionCompact;
    }

    public void setConnectionCompact(ConnectionCompact connectionCompact) {
        this.connectionCompact = connectionCompact;
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

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isInfo_pause() {
        return info_pause;
    }

    public void setInfo_pause(boolean info_pause) {
        this.info_pause = info_pause;
    }


    public synchronized void signalHandel(){
        final Signal signal;
        try {
            signal = server.receive();
        } catch (InterruptedException e) {
            connectionCompact.OnLinkLost();
            return;
        }
        Log.d("signalHandleDebug",signal.sig.name());
        switch (signal.getSig()){
            case Get_Dir:
                if (signal.getData() == null) {
                    Send_dirInfo(sharedir);
                    connectionCompact.log("请求目录:\"\"");
                }else {
                    Send_dirInfo(signal.getData());
                    connectionCompact.log(String.format("请求目录:\"%s\"", signal.getData()));
                }
                return;
            case Send_File:
                String datas[]=signal.getData().split(FileInfo.split);
                FileInfo_rec rec=new FileInfo_rec(datas[0],Long.parseLong(datas[1]),datas[2]);
                manage.addTask(rec);
                connectionCompact.onTaskAdded(manage.getTaskSize());
                start_task();
                connectionCompact.log(String.format("接收文件: %s",signal.getData().split(":::")[0]));
                return;
            case Get_File:
                DocumentFile df= FileAccess.findFile(sharedir,signal.getData());
                manage.addTask(new FileInfo_send(df));
                connectionCompact.onTaskAdded(manage.getTaskSize());
                start_task();
                connectionCompact.log(String.format("发送文件: %s",signal.getData()));
                return;
            case Cancel_task:
                int idx=Integer.parseInt(signal.getData());
                if(idx==0){
                    canceling=true;
                    task_cancel=true;
                    return;
                }else{
                    manage.removeTask(idx);
                    server.send(tranSignal.Canceled,String.valueOf(idx));
                    connectionCompact.onTaskRemoved(idx);
                    canceling=false;
                }
                return;
            case Cancel_at:
                Long at=Long.parseLong(signal.getData());
                manage.getTask(0).setSize(at);
                server.send(tranSignal.Cancel_Ready);
                cancel_ready=true;
                return;
            case Canceled:
                int idc=Integer.parseInt(signal.getData());
                if(idc!=0){
                    manage.removeTask(idc);
                    connectionCompact.onTaskRemoved(idc);
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
                pause_all=false;
                connectionCompact.onTaskContinued();
                return;
            case FT_Finish:
            case Send_Ready:
            case Get_Ready:
                rec_ready=true;
                return;
            case Stop:
                pause_all=true;
                connectionCompact.onTaskPaused();
                return;
            case Close:
                pause_all=true;
                task_cancel=true;
                server.close();
                return;
            case Target_Found:
                target_found=true;
                Log.d("target_found","rec");
                return;
            case INFO_D:
                info_pause=false;
                return;
            case deleteFile:
                String path = signal.data;
                DocumentFile delf = FileAccess.findFile(sharedir,path);
                if(delf!=null) {
                    delf.delete();
                }
                return;

            case Sync_start:
                syncDir();
                return;
            case Sync_stop:
                sync_stop=true;
                return;
            case Sync_finish:
                connectionCompact.onSyncFinish();
                return;
            case Sync_apply:
                connectionCompact.onSyncApplying();
                return;

        }

    }

    private Thread getTrans_thread(){
        return new Thread(){
            public void run(){
                trans_thread=this;
                connectionCompact.onTransStart();
                while (!manage.isEmptyTask()){
                    if(linklost) {
                        trans_thread=null;
                        return;
                    }
                    pause();
                    TransInfo info=manage.getTask(0);
                    connectionCompact.onRunningTaskChanged(info);
                    try{
                        if(info.isSend())
                            send_file(info);
                        else
                            get_file(info);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(cancel_all) {
                        cancelAll();
                        break;
                    }
                    connectionCompact.onProcessUpdate();
                }
                connectionCompact.onAllTaskFinished();
                connectionCompact.onTransFinish();
                connectionCompact.onProcessUpdate();
                trans_thread=null;
            }
        };
    }

    public synchronized void cancel_all_task(){
        if(manage.isEmptyTask())
            return;
        server.send(tranSignal.Cancel_all);
        task_cancel=true;
        cancel_all=true;
        if(manage.getTask(0).isSend())
            canceling=true;//add
        else
            cancel_pause=true;
    }

    private void get_file(TransInfo info) throws IOException {
        DocumentFile tar=info.getDocumentFile();
        FileInfo_rec info_rec=(FileInfo_rec) info.getInfo();
        tar=FileAccess.getEmptyFile(sharedir,info_rec.getPath(),info_rec.getNewname());
        BufferedOutputStream outputStream = new BufferedOutputStream(connectionCompact.onRequireOutStream(tar));
        MD5Generator md5 = verification?new MD5Generator():null;
        int len;
        byte[] buff = new byte[blockSize];
        server.send(tranSignal.Get_Ready,info.toString());
        task_wait=true;
        if(info.getSize()>0) {
            connectionCompact.onNotificationUpdate(manage.getTask(0));
            while ((len = server.read(buff)) != -1) {
                if (linklost)
                    return;
                pause();
                outputStream.write(buff, 0, len);
                if(md5!=null)
                    md5.update(buff,len);
                info.finishOffest(len);
                connectionCompact.onRunningTaskProcessUpdate();
                connectionCompact.onProcessUpdate();
                connectionCompact.onNotificationUpdate(manage.getTask(0));
                if (info.getFinished() >= info.getSize())
                    break;
            }
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
            tar.delete();
        }else{
            FileInfo finishInfo=manage.pollTask();
            finishInfo.setDocumentFile(tar);
            if(md5!=null) {
                String cmpMD5 = server.getMD5();
                finishInfo.setVerify(md5.getMD5().equals(cmpMD5));
            }
            connectionCompact.onTaskFinish((FileInfo_rec)finishInfo);
        }
        outputStream.close();
        connectionCompact.onProcessUpdate();
        connectionCompact.onTaskRemoved(0);
    }
    private void send_file(TransInfo info) throws IOException {
        DocumentFile src = info.getDocumentFile();
        InputStream in;
        try {
            in= connectionCompact.onRequireInStream(src);
        } catch (FileNotFoundException e) {
            server.send(tranSignal.Cancel_task);
            return;
        }
        byte bytes[]=new byte[blockSize];
        MD5Generator md5 = verification?new MD5Generator():null;
        int len;
        long size=src.length();
        server.send(tranSignal.Send_Ready,info.toString());
        task_wait=true;
        boolean canceled=false;
        connectionCompact.onNotificationUpdate(manage.getTask(0));
        while((len=in.read(bytes))!=-1){
            if(linklost)
                return;
            if(task_cancel&&!canceled){
                canceled=true;
                long cat=info.getFinished()+20480;
                if(cat<info.getSize())
                    info.setSize(cat);
                server.send(tranSignal.Cancel_at,String.valueOf(info.getSize()));
                cancel_pause=true;
                size=cat;
            }
            pause();
            server.write(bytes,0,len);
            if(md5!=null){
                md5.update(bytes,len);
            }
            info.finishOffest(len);
            connectionCompact.onRunningTaskProcessUpdate();
            connectionCompact.onNotificationUpdate(manage.getTask(0));
            connectionCompact.onProcessUpdate();
            if(info.getFinished()>=size)
                break;//need wait?
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
        }else{
            if(md5!=null){
                server.write(md5.getMD5().getBytes());
            }
            manage.pollTask();
            server.send(tranSignal.Send);
        }
        in.close();
        connectionCompact.onTaskRemoved(0);
    }

    private void Send_dirInfo(String path){
        DocumentFile df=FileAccess.findFile(sharedir,path);
        Send_dirInfo(df);
    }
    private void Send_dirInfo(final DocumentFile df){
        new Thread(){
            public void run(){
                server.send(tranSignal.Dir_info);
                DocumentFile[] list=df.listFiles();
                for(DocumentFile x:list) {
                    if(target_found){
                        target_found=false;
                        Log.d("target_found","found");
                        server.send(tranSignal.Dir_info);
                        return;
                    }
                    Send_fileInfo(x);
                    info_pause=true;
                }
                server.send(tranSignal.Dir_info);
            }
        }.start();
    }
    private void Send_fileInfo(DocumentFile df){
        String name=df.getName();
        Long size=df.isDirectory()?-1:df.length();
        String path=df.getUri().getPath();
        path=path.substring(rootPath.length());
        server.send(tranSignal.File_info,String.format("%s:::%d:::%s",name,size,path));
    }

    private void start_task(){
        connectionCompact.onProcessUpdate();
        if(trans_thread==null)
            getTrans_thread().start();
    }

    private void cancelAll(){
        if(!cancel_all)
            return;
        manage.clearTask();
        connectionCompact.onCancelAllTask();
        cancel_all=false;
        task_cancel=false;
        cancel_pause=false;
        canceling=false;
    }


    private void pause(){
        connectionCompact.onPause(task_wait||cancel_pause||pause_all);
        while (task_wait||cancel_pause||pause_all){
            if((task_cancel||canceling)&&!cancel_pause)
                return;
            Log.d("pauseDebug",String.format("tp:%b,cp:%b,pa:%b",task_wait,cancel_pause,pause_all));
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

    public synchronized void cancelItem(int pos){
        if(canceling)
            return;
        canceling=true;
        server.send(tranSignal.Cancel_task,String.valueOf(pos));
        if(pos==0)
            task_cancel=true;
        else
            cancel_pause=true;
    }
    private void Send_syncInfo(DocumentFile df,String MD5){
        String name=df.getName();
        Long size=df.isDirectory()?-1:df.length();
        String path=df.getUri().getPath();
        path=path.substring(rootPath.length());
        server.send(tranSignal.Sync_info,String.format("%s:::%d:::%s:::%s",name,size,path,MD5));
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

    public ArrayList<DocumentFile> getSyncItems(DocumentFile root){
        DocumentFile[] list = root.listFiles();
        ArrayList<DocumentFile> arrayList = new ArrayList<>();
        for (DocumentFile x:list){
            if(x.isDirectory())
                arrayList.addAll(getSyncItems(x));
            else
                arrayList.add(x);
        }
        return arrayList;
    }

    public void syncDir(DocumentFile root){
        ArrayList<DocumentFile> list = getSyncItems(root);
        connectionCompact.onSyncStart(list.size());
        server.send(tranSignal.Sync_count,String.valueOf(list.size()));
        int proc = 0;
        for(DocumentFile file:list){
            if(sync_stop||linklost)
                break;
            else{
                connectionCompact.log(String.format("检查文件同步信息:%s",file.getName()));
                String md5 =getDocumentMD5(file);
                if(md5==null) continue;
                Send_syncInfo(file,md5);
                connectionCompact.onSyncUpdata(++proc);
            }
        }
        connectionCompact.onSyncUpdata(list.size());
        server.send(tranSignal.Sync_finish);
    }

    public void syncDir(){
        if(sync_thread!=null)
            return;
        sync_stop=false;
        new Thread(){
            @Override
            public void run(){
                sync_thread=this;
                syncDir(sharedir);
                sync_stop=false;
                sync_thread=null;
            }
        }.start();
        //server.send(tranSignal.Sync_start);

    }

    public interface ConnectionCompact {
        InputStream onRequireInStream(DocumentFile src) throws FileNotFoundException;
        OutputStream onRequireOutStream(DocumentFile tar) throws FileNotFoundException;
        void log(final String info);
        void OnLinkLost();
        void onProcessUpdate();
        void onTaskAdded(int pos);
        void onAllTaskFinished();
        void onTaskRemoved(int idx);
        void onTaskPaused();
        void onTaskContinued();
        void onTransStart();
        void onRunningTaskChanged(final TransInfo info);
        void onTransFinish();
        void onRunningTaskProcessUpdate();
        void onNotificationUpdate(final TransInfo info);
        void onTaskFinish(final FileInfo_rec info);
        void onPause(boolean state);
        void onCancelAllTask();
        void onStartServerFail();
        void onStartServerSuccess();
        void onSyncStart(int size);
        void onSyncFinish();
        void onSyncUpdata(int proc);
        void onSyncApplying();
        void onSocketSet(String ip,String ssid,int port);
        void onSocketAccept();
    }

}
