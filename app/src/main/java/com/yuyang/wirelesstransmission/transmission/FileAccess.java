package com.yuyang.wirelesstransmission.transmission;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by vista on 2017/9/30.
 */

public class FileAccess {
    public final static int RESULT_URI=114513,READ_REQUEST_CODE=42,FILE_REQUEST_CODE=1;
    public static void getDirActivity(Activity activity){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activity.startActivityForResult(intent, READ_REQUEST_CODE);
    }
    public static void getFileActivity(Activity activity){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent,FILE_REQUEST_CODE);
    }
    public static Uri getDir_Uri(Activity context){
        List<UriPermission> list= context.getContentResolver().getPersistedUriPermissions();
        if(list.size()>0)
            return list.get(0).getUri();
        return null;
    }
    public static void releaseDir_Doc(Activity context){
        context.getContentResolver().releasePersistableUriPermission(getDir_Uri(context), Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
    public static void getUri_premission(Context context, Uri uri){
        context.getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    public static DocumentFile getDir_Doc (Activity context){
        List<UriPermission> list= context.getContentResolver().getPersistedUriPermissions();
        if(list.size()>0)
            return DocumentFile.fromTreeUri(context,list.get(0).getUri());
        return null;
    }

    private static File[] getMount(Context context){
        File list[] = context.getExternalFilesDirs(null);
        for (int i=0;i<list.length;i++)
            list[i]=list[i].getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile();
        return list;
    }


    protected static DocumentFile findFile(DocumentFile root,String path){
        String paths[]=path.split("/");
        DocumentFile df=root;
        for (String x:paths) {
            if(x.length()<=0)
                continue;
            df = df.findFile(x);
            if (df == null)
                break;
        }
        return df;
    }
    protected static DocumentFile getEmptyFile(DocumentFile root,String path,String name){
        return findFile(root,path,name,true);
    }
    protected static DocumentFile findFile(DocumentFile root,String path,String name,boolean crate){
        DocumentFile df=root;
        if(path!=null){
            String paths[]=path.split("/");
            for (int i=0;i<paths.length;i++) {
                String x=paths[i];
                if(x.length()<=0)
                    continue;
                DocumentFile dfx = df.findFile(x);
                if (dfx == null||dfx.isFile()) {
                    if(crate) {
                        df = df.createDirectory(x);
                        Log.d("FileAcess", "findFile: "+df);
                    }

                    else
                        return null;
                }
                else
                    df=dfx;
            }
        }
        DocumentFile tar=df.findFile(name);
        if(!crate)
            return tar;
        if (tar!=null&&tar.isFile())
            tar.delete();
        return df.createFile("*/*",name);
    }
    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "Byte(s)";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result = new BigDecimal(Double.toString(kiloByte));
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }
        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result = new BigDecimal(Double.toString(megaByte));
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result = new BigDecimal(Double.toString(gigaByte));
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result = new BigDecimal(teraBytes);
        return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

    public static String getPathUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {

                    if(split.length>1)
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    return Environment.getExternalStorageDirectory().toString();
                }else{
                    try{
                        if(split.length>1)
                            return getMount(context)[1].getAbsolutePath()+"/"+split[1];
                        return getMount(context)[1].getAbsolutePath();
                    }catch (Exception e){
                        return null;
                    }
                }

                // TODO handle non-primary volumes
            }
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
