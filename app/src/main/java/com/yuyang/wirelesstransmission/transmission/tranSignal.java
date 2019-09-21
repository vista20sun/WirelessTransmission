package com.yuyang.wirelesstransmission.transmission;

/**
 * Created by vista on 2017/9/28.
 */

public enum tranSignal {
    Ready,Close,Stop,Canceled,Cancel_task,Cancel_all,Pause,Continue,Cancel_at,Cancel_Pause,Cancel_Ready,Get_Dir,Get_File,Send_File,Target_Found,INFO_D, Sync_stop,Sync_apply,FolderChanged,EMERGENCY_DONE,Get_port,Request_File,File_info,Send_port,Dir_info,FT_Finish,Send, Received,Send_Ready,Get_Ready,Sync_start,Sync_finish,Sync_info,Sync_count,deleteFile;

    public static boolean isRealTime(tranSignal t){
        if(t.ordinal()<=EMERGENCY_DONE.ordinal())
            return true;
        return false;
    }
    public static tranSignal getSig(char[] buff){
        try{
            return values()[buff[0]];
        }catch (Exception e){
            return null;
        }
    }


}
