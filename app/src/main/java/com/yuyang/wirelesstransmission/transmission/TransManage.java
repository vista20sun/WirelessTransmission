package com.yuyang.wirelesstransmission.transmission;

import com.yuyang.wirelesstransmission.transmission.FileInfo;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;
import com.yuyang.wirelesstransmission.transmission.TransInfo;

import java.util.ArrayList;

/**
 * Created by vista on 2017/9/28.
 */

public class TransManage {

    public TransManage(){
        task_list=new ArrayList<>();
        rec_list=new ArrayList<>();
        total=0;
        transmited=0;
    }
    public synchronized void addTask(FileInfo taskInfo){
        task_list.add(new TransInfo(taskInfo));
        total=0;
        for(TransInfo x:task_list){
            total+=x.getInfo().getSize();
        }
        transmited=0;
    }
    public synchronized FileInfo pollTask(){
        transmited+=task_list.get(0).getInfo().getSize();
        return task_list.remove(0).getInfo();
    }
    public synchronized void removeTask(int index){
        task_list.remove(index);
    }
    public synchronized void clearTask(){
        task_list.clear();
    }
    public synchronized int getTaskSize(){
        return task_list.size();
    }
    public FileInfo getInfo(int i){
        try{
            return task_list.get(i).getInfo();
        }catch (IndexOutOfBoundsException e){
            return null;
        }
    }
    public TransInfo getTask(int i){
        try{
            return task_list.get(i);
        }catch (IndexOutOfBoundsException e){
            return null;
        }
    }
    public long getProc(int i){
        try{
            return task_list.get(i).getFinished();
        }catch (IndexOutOfBoundsException e){
            return -1;
        }
    }

    public long getTransmited() {
        if(task_list.isEmpty())
            return transmited;
        return transmited+task_list.get(0).getFinished();
    }

    public long getTotal() {
        return total;
    }

    public void addREC(FileInfo_rec rec){
        rec_list.add(rec);
    }
    public FileInfo_rec getREC(int i){
        try{
            return rec_list.get(i);
        }catch (IndexOutOfBoundsException e){
            return null;
        }
    }
    public boolean isEmptyTask(){
        return task_list.isEmpty();
    }



    private ArrayList<TransInfo> task_list;
    private ArrayList<FileInfo_rec> rec_list;
    private long transmited,total;
}
