package com.yuyang.wirelesstransmission.transmission;

import android.app.Activity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by vista on 2017/9/28.
 */

public class Server_t {
    private int port_sig, port_file,timeout;
    private InputStream sig_instream, file_instream;
    private OutputStream sig_outstream, file_outstream;
    private InputStreamReader reader;
    private OutputStreamWriter writer;
    private Queue<Signal> send_queue, retime_queue;
    private BlockingQueue<Signal> rec_queue;
    private boolean close;

    private ServerSocket server_sig, server_file;
    private Socket socket_sig, socket_file;
    private onSignalDeliverListener listener;

    private static Server_t server_t;
    public static Server_t getServer_t(int port_s, int port_t,int time_o, Activity activity){
        if(server_t!=null)
            server_t.close();
        server_t=new Server_t(port_s,port_t,time_o,activity);
        return server_t;
    }

    public String getMD5(){
        byte[] temp = new byte[4096];
        try {
            int len=file_instream.read(temp);
            return new String(temp,0,len);
        } catch (IOException e) {
            return null;
        }
    }

    public interface onSignalDeliverListener {
        void OnSignalDeliver();

        void OnLinkLost();
    }

    public Server_t(int port_s, int port_t,int time_o, Activity activity) {
        port_sig = port_s;
        port_file = port_t;
        timeout=time_o;
        close = true;
        listener = (onSignalDeliverListener) activity;
    }

    public void open_s(boolean verification) throws IOException {
        server_sig = new ServerSocket(port_sig);
        socket_sig = server_sig.accept();
        sig_instream = socket_sig.getInputStream();
        sig_outstream = socket_sig.getOutputStream();
        writer = new OutputStreamWriter(sig_outstream, "UTF-8");
        reader = new InputStreamReader(sig_instream, "UTF-8");
        writer.write(tranSignal.Send_port.ordinal());
        send_queue = new LinkedList<>();
        rec_queue = new LinkedBlockingQueue<>();
        retime_queue = new LinkedList<>();
        writer.write(String.valueOf(port_file)+":"+(verification?"1":"0"));
        writer.flush();
    }

    public boolean isClose() {
        return close;
    }

    public void open_f() throws IOException {
        server_file = new ServerSocket(port_file);
        socket_file = server_file.accept();
        file_instream = socket_file.getInputStream();
        file_outstream = socket_file.getOutputStream();
        socket_sig.setSoTimeout(timeout+300);
        trans_t().start();
        close = false;
    }

    public String getClient_ip() {
        return socket_sig.getInetAddress().getHostAddress();
    }
    public String getHosts_ip(){
        InetAddress inetAddress= null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            return null;
        }
        return inetAddress.getHostAddress();
    }

    private Thread trans_t() {
        return new Thread() {
            public void run() {
                char[] in = new char[1024];
                while (!close) {
                    try {
                        int len = reader.read(in);
                        Signal sig = Signal.ParasChars(len, in);
                        handel_signal(sig);
                        if (!retime_queue.isEmpty()) {
                            Signal s = retime_queue.poll();
                            writer.write(s.sig.ordinal());
                            if(s.data!=null)
                                writer.write(s.data);
                        } else if (!send_queue.isEmpty()) {
                            Signal s = send_queue.poll();
                            writer.write(s.sig.ordinal());
                            if(s.data!=null)
                                writer.write(s.data);
                        } else {
                            writer.write(tranSignal.Ready.ordinal());
                        }
                        writer.flush();
                    } catch (IOException e) {
                        listener.OnLinkLost();
                        close();
                    }
                }
            }
        };
    }

    public int getPort_sig() {
        return port_sig;
    }

    public int getPort_file() {
        return port_file;
    }

    public synchronized void handel_signal(Signal signal) {
        switch (signal.sig) {
            case Ready:
                return;
            default:
                rec_queue.offer(signal);
                listener.OnSignalDeliver();

        }
    }

    public synchronized void send(tranSignal t) {
        if (tranSignal.isRealTime(t))
            retime_queue.offer(new Signal(t));
        else
            send_queue.offer(new Signal(t));
    }

    public synchronized void send(tranSignal t, String str){
        if (tranSignal.isRealTime(t))
            retime_queue.offer(new Signal(t, str));
        else
            send_queue.offer(new Signal(t, str));
    }

    public synchronized void write(byte[] bytes) throws IOException {
        file_outstream.write(bytes);
        file_outstream.flush();
    }

    public synchronized void write(byte[] bytes, int offest, int leng) throws IOException {
        file_outstream.write(bytes, offest, leng);
        file_outstream.flush();
    }

    public Signal receive() throws InterruptedException {
        return rec_queue.take();
    }

    public int read(byte[] bytes) throws IOException {
        return file_instream.read(bytes);
    }

    public static String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }


    public synchronized void close() {
        if (server_sig != null) {
            try {
                server_sig.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (sig_instream != null)
                try {
                    sig_instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (sig_outstream != null)
                try {
                    sig_outstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (socket_sig != null)
                try {
                    socket_sig.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        if (server_file != null) {
            try {
                server_file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (socket_file != null)
                try {
                    socket_file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (file_outstream != null)
                try {
                    file_outstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (file_instream != null)
                try {
                    file_instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            close = true;
        }
    }
}
