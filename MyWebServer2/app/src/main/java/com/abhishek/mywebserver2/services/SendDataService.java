package com.abhishek.mywebserver2.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.IBinder;
import android.util.Log;

import com.abhishek.mywebserver2.Utility;
import com.abhishek.mywebserver2.tools.CircularByteBuffer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SendDataService extends Service {
    public SendDataService() {}
    @Override
    public IBinder onBind(Intent intent) {throw new UnsupportedOperationException("Not yet implemented");}


    public static List<String> uris = new ArrayList<String>();
    public static int port = 3333;
    public static String password = "a";
    AsyncHttpServer server ;


    @Override
    public void onCreate() {
        super.onCreate();

        if(Utility.getPasswordPreference("password",getApplicationContext())!=null){
            password = Utility.getPasswordPreference("password",getApplicationContext());
        }
        if(Utility.getPortPreference("senddataport",getApplicationContext())!=-1){
            port = Utility.getPortPreference("senddataport",getApplicationContext());
        }


        server = new AsyncHttpServer();

        server.get("/"+  password + "/assets/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath();
                if(!path.startsWith("/"))path="/"+path;

                try {
                    if(path.startsWith("/"+ password + "/assets/"))path=path.substring(("/"+ password + "/assets/").length());
                    else {
                        throw new Exception("Asset File Not Found");
                    }
                    AssetFileDescriptor afd = getAssets().openFd(path);
                    response.sendStream(afd.createInputStream(),afd.getLength());
                    response.end();
                }

                catch (Exception e){
                    Utility.send404(response,getApplicationContext());
                    e.printStackTrace();
                }
            }
        });


        server.get("/" + password + "/zip/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getPath();
                try{
                    path = URLDecoder.decode(path,"utf-8");
                } catch (UnsupportedEncodingException e) {
                    response.send("Cannot Decode URL : "+path);
                    response.end();

                    e.printStackTrace();
                    return;
                }
                if(!path.startsWith("/"))path="/"+path;
                path = path.substring(  ("/"+password+"/zip").length() );

                final File f = new File(path);
                if(!f.exists()){
                    Utility.send404(response,getApplicationContext());
                    return;
                }

                for(String s : uris){
                    if(path.startsWith(s)){

                        sendZip(f,response);


                        return;
                    }
                }
                Utility.sendPermissionDenied(response,getApplicationContext());
            }
        });



        server.get("/" + password + "/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                String path = request.getPath();
                try {
                    path = URLDecoder.decode(path,"utf-8");
                } catch (UnsupportedEncodingException e) {
                    response.send("Cannot Decode URL : "+path);
                    response.end();

                    e.printStackTrace();
                    return;
                }


                if(!path.startsWith("/"))path="/"+path;

                if(path.startsWith("/" + password + "/"))path=path.substring(("/" + password + "/").length());

                if(path==null || path.length()<1)path="/";

                try{
                    File f = new File(path);
                    if(f.exists()){

                        if(!path.startsWith("/"))path = "/"+path;

                        for(String s : uris){
                            if(path.startsWith(s)){

                                if(f.isDirectory()){
                                    response.send("text/html" , Utility.getHTML(getApplicationContext(),path,password));
                                }else{
                                    response.sendStream(new FileInputStream(f),f.length());
                                }
                                response.end();

                                return;
                            }
                        }

                        response.send(Utility.getHTML(getApplicationContext(),password,uris.toArray(new String[0])));
                        response.end();
                    }else {
                        throw new Exception("File Not Found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Utility.send404(response,getApplicationContext());
                }
            }
        });





        server.get("/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Utility.sendEnterPassword(response,getApplicationContext());
            }
        });
        server.listen(port);


    }



    void sendZip(final File f, final AsyncHttpServerResponse response){
        try{

            String mPath = f.getAbsolutePath();
            if(  mPath.endsWith("/")  ||  mPath.endsWith("\\")  ){}
            else mPath = mPath+"/";


            final String inputPath = new String(mPath) ;




            int BUF_LEN = 512*1024 ; //
            final CircularByteBuffer cbb = new CircularByteBuffer(BUF_LEN);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        ZipOutputStream zos = new ZipOutputStream(cbb.getOutputStream());
                        WriteToOutputStream(f,zos);
                        zos.close();
                    }
                    catch (Exception e){e.printStackTrace();}
                }


                void WriteToOutputStream(File file,ZipOutputStream zos) throws Exception{
//                    if(!response.isOpen())return;

                    if(file.isFile()){

                        if(!file.getAbsolutePath().startsWith(inputPath))return;

                        ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(inputPath.length()));
                        zos.putNextEntry(ze);

                        FileInputStream fis = new FileInputStream(file);
                        int len;
                        byte[] buffer = new byte[32*1024];
                        while(  (len = fis.read(buffer)) > 0){
                            zos.write(buffer,0,len);
                        }
                        fis.close();
                        zos.closeEntry();

                    }else{

                        File[] subFiles = file.listFiles();
                        for(File f : subFiles){
                            WriteToOutputStream(f,zos);
                        }

                    }

                }

            }).start();





            int len ;
            byte[] buffer = new byte[BUF_LEN];

            while ((len = cbb.getInputStream().read(buffer))>0){

                Log.d(" , ",len+"");
//                if(!response.isOpen())return;

                if(len == BUF_LEN){
                    Log.d("Abhi","Hell Yeah !!!");
                    response.write(new ByteBufferList(buffer));

                }
                else {

                    byte[] buf = new byte[len];
                    for (int i = 0; i < len; i++) {
                        buf[i] = buffer[i];
                    }

                    response.write(new ByteBufferList(buf));
                }

            }
            cbb.getInputStream().close();
            cbb.getOutputStream().close();
            response.end();
        }
        catch (Exception e){e.printStackTrace();}
    }

}
