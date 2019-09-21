package com.yuyang.wirelesstransmission.transmission;


import android.app.Activity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by vista on 2017/9/28.
 */

public class Client_t {
    private int port_sig,port_file;
    private String address;
    private InputStream sigInStream,fileInStream;
    private OutputStream sigOutStream, fileOutStream;
    private InputStreamReader reader;
    private OutputStreamWriter writer;
    private Queue<Signal> send_queue, realTime_queue;
    private BlockingQueue<Signal>rec_queue;
    private boolean close,verification;
    public boolean isVerification(){
        return verification;
    }

    private static Client_t client_t;
    public static Client_t getClient_t(String ip, int port_s, int t,Activity activity){
        if(client_t!=null)
            client_t.close();
        client_t=new Client_t(ip,port_s,t,activity);
        return client_t;
    }


    private Socket socket_sig,socket_file;
    private int time_out;
    private onSignalDeliverListener listener;

    public interface onSignalDeliverListener{
        void OnSignalDeliver();
        void OnLinkLost();
    }

    public Client_t(String ip, int port_s, int t,Activity activity){
        port_sig=port_s;
        port_file=-1;
        address=ip;
        time_out=t;
        close=true;
        listener=(onSignalDeliverListener)activity;
    }

    private void open_s() throws IOException {
        socket_sig=new Socket();
        socket_sig.connect(new InetSocketAddress(address,port_sig),time_out);
        sigOutStream =socket_sig.getOutputStream();
        sigInStream =socket_sig.getInputStream();
        writer=new OutputStreamWriter(sigOutStream,"UTF-8");
        reader=new InputStreamReader(sigInStream,"UTF-8");
        send_queue=new LinkedList<>();
        realTime_queue =new LinkedList<>();
        rec_queue=new LinkedBlockingQueue<>();
    }
    private void open_f() throws IOException {
        socket_file=new Socket();
        socket_file.connect(new InetSocketAddress(address,port_file));
        fileInStream=socket_file.getInputStream();
        fileOutStream =socket_file.getOutputStream();
        socket_sig.setSoTimeout(time_out);
    }
    public int open(){
        try{
            open_s();
        }catch (IOException e){
            e.printStackTrace();
            return -1;
        }
        char temp[]=new char[100];
        try {
            int len = reader.read(temp);
            if (tranSignal.getSig(temp)!=tranSignal.Send_port)
                throw new IOException("suspension signal");
            String data[] = String.valueOf(temp,1,len-1).split(":");
            port_file=Integer.parseInt(data[0].trim());
            verification = data[1].equals("1");
        }catch (IOException e){
            e.printStackTrace();
            return -2;
        }catch (NumberFormatException e2){
            e2.printStackTrace();
            return -3;
        }
        try{
            sleep(300);
        }catch (Exception e){
            ;
        }
        try{
            open_f();
        }catch (IOException e){
            e.printStackTrace();
            return -4;
        }
        close=false;
        trans_t().start();
        return 0;
    }


    private Thread trans_t(){
        return new Thread(){
            public void run(){
                char[] in=new char[1024];
                send(tranSignal.Get_Dir,"");
                while (!close){
                    try {
                        if(!realTime_queue.isEmpty()){
                            Signal s = realTime_queue.poll();
                            writer.write(s.sig.ordinal());
                            if(s.data!=null)
                                writer.write(s.data);
                            writer.flush();
                        }else if(!send_queue.isEmpty()){
                            Signal s = send_queue.poll();
                            writer.write(s.sig.ordinal());
                            if(s.data!=null)
                                writer.write(s.data);
                            writer.flush();
                        }else{
                            writer.write(tranSignal.Ready.ordinal());
                            writer.flush();
                            try{
                                sleep(200);
                            }catch (InterruptedException e) {
                                ;
                            }
                        }
                        int len=reader.read(in);
                        Signal sig = Signal.ParasChars(len,in);
                        handel_signal(sig);
                    }catch (IOException e){
                        listener.OnLinkLost();
                        close();
                        return;
                    }
                }
            }
        };
    }

    public synchronized void send(tranSignal t){
        if(tranSignal.isRealTime(t))
            realTime_queue.offer(new Signal(t));
        else
            send_queue.offer(new Signal(t));
    }
    public synchronized void send(tranSignal t, String str){
        if(tranSignal.isRealTime(t))
            realTime_queue.offer(new Signal(t,str));
        else
            send_queue.offer(new Signal(t,str));
    }

    public synchronized void write(byte[] bytes) throws IOException {
        fileOutStream.write(bytes);
        fileOutStream.flush();
    }
    public synchronized void write(byte[] bytes,int offest,int leng) throws IOException {
        fileOutStream.write(bytes,offest,leng);
        fileOutStream.flush();
    }
    public synchronized void handel_signal(Signal signal){
        switch (signal.sig){
            case Ready:return;
            default:
                rec_queue.offer(signal);
                listener.OnSignalDeliver();
        }
    }
    public Signal receive() throws InterruptedException {
        return rec_queue.take();
    }
    public int read(byte[] bytes) throws IOException {
        return fileInStream.read(bytes);
    }
    public String getMD5(){
        byte[] temp = new byte[4096];
        try {
            int len=fileInStream.read(temp);
            return new String(temp,0,len);
        } catch (IOException e) {
            return null;
        }
    }

    public synchronized void close(){
        if(socket_file!=null)
            try {
                socket_file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(socket_sig!=null)
            try{
                socket_sig.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        if(writer!=null)
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(reader!=null)
            try{
                reader.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        if(sigInStream !=null)
            try {
                sigInStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(sigOutStream !=null)
            try {
                sigOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(fileOutStream !=null)
            try {
                fileOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(fileInStream!=null)
            try {
                fileInStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        close=true;
    }

}
