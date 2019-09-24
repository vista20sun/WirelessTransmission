package com.yuyang.wirelesstransmission;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.yuyang.wirelesstransmission.transmission.FileAccess;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by vista on 2017/11/25.
 */

public class DataHelper extends SQLiteOpenHelper {
    private final static int VERSION = 1;
    private SQLiteDatabase db;
    private final static String DB_File="history.db";
    private final static String CRT_History_Table="CREATE TABLE history" +
            "(path text  PRIMARY KEY ,name text,type text,size real,time TEXT,verified int)";
    private Context context;
    public DataHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
        this.context=context;
    }
    public DataHelper(Context context,String name,int version){
        this(context,name,null,version);
    }
    public DataHelper(Context context){
        this(context,DB_File,VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db=db;
        db.execSQL(CRT_History_Table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Toast.makeText(context, "database update", Toast.LENGTH_SHORT).show();
    }

    public void insert(SQLiteDatabase db, ContentValues values){
        if(db==null)
            db=getWritableDatabase();
        db.insert("history",null,values);
    }
    public void clear(SQLiteDatabase db){
        if(db==null)
            db=getWritableDatabase();
        db.delete("history",null,null);
    }
    public void insert_history(SQLiteDatabase db, FileInfo_rec info){
        String path= FileAccess.getPathUri(context,info.getDocumentFile().getUri());
        if(isExist(path))
            delete(db,path);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ContentValues values = new ContentValues();
        values.put("path",path);
        values.put("type",info.getMimeType());
        values.put("name",info.getName());
        values.put("size",info.getSize());
        values.put("time",df.format(new Date()));
        values.put("verified",info.getVerify().ordinal());
        insert(db,values);
    }

    public Cursor getHistory(SQLiteDatabase db){
        if(db==null)
            db=getWritableDatabase();
        return db.query("history",null,null,null,null,null,null,null);//get all records
    }

    public ArrayList<FileInfo_rec> getHistory(){
        Cursor c = getHistory(null);
        ArrayList<FileInfo_rec> list = new ArrayList<>();
        while(c.moveToNext()){
            list.add(new FileInfo_rec(
                    c.getString(c.getColumnIndex("name")),
                    c.getLong(c.getColumnIndex("size")),
                    c.getString(c.getColumnIndex("path")),
                    c.getInt(c.getColumnIndex("verified"))));
        }
        c.close();
        return list;
    }
    public boolean isExist(String path){
        Cursor c = query(null,path);
        boolean exist = false;
        if(c.moveToNext())
            exist=true;
        c.close();
        return exist;
    }





    public Cursor query(SQLiteDatabase db,String path){
        if(db==null)
            db=getWritableDatabase();
        return db.query("history",null,"path=?",new String[]{path},null,null,null,null);
    }

    public void close(){
        super.close();
        if(db!=null)
            db.close();
        if(helper==this)
            helper=null;
    }

    public void delete(SQLiteDatabase db,String path){
        if(db==null)
            db=getWritableDatabase();
        db.delete("history","path=?",new String[]{path});
    }
    private static DataHelper helper=null;
    public static DataHelper getHelper(Context c){
        if(helper==null)
            helper=new DataHelper(c);
        return helper;
    }




}
