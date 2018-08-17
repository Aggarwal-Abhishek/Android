package com.abhishek.mywebserver2.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.IBinder;
import android.util.Log;

import com.abhishek.mywebserver2.Utility;
import com.abhishek.mywebserver2.extra.ContactsAPI;
import com.abhishek.mywebserver2.tools.CircularByteBuffer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WebServerService extends Service {
    public WebServerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static int port = 5555;
    public static String password = "a" ;
    AsyncHttpServer server  ;

    @Override
    public void onCreate() {
        super.onCreate();

        if(Utility.getPasswordPreference("password",getApplicationContext())!=null){
            password = Utility.getPasswordPreference("password",getApplicationContext());
        }
        if(Utility.getPortPreference("serverport",getApplicationContext())!=-1){
            port = Utility.getPortPreference("serverport",getApplicationContext());
        }

        server = new AsyncHttpServer();


//assets

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
//                    response.send("Asset File Not Found ... : "+e.getMessage());
//                    response.end();
                    e.printStackTrace();
                }
            }
        });

//zip

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
                path = path.substring(  ("/"+password+"/zip/").length() );

                final File f = new File(path);
                if(!f.exists()){
                    Utility.send404(response,getApplicationContext());
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendZip(f,response);
                        response.end();
                    }
                }).start();

//                sendZip(f,response);
//                response.end();
            }
        });
//gallery
        server.get("/" + password + "/gallery/.*", new HttpServerRequestCallback() {
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

                if(path.startsWith("/" + password + "/gallery/"))path=path.substring(("/" + password + "/gallery/").length());

                if(path==null || path.length()<1)path="/";

                try{
                    File f = new File(path);
                    if(f.exists()){
                        if(f.isDirectory()){
                            response.send("text/html" , Utility.getGalleryHTML(getApplicationContext(),path,password));
                        }else{
                            response.sendStream(new FileInputStream(f),f.length());
                        }
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
//contacts
        server.get("/" + password + "/contacts/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                try {
                    ContactsAPI contactsAPI = new ContactsAPI(getApplicationContext());
                    contactsAPI.getDetails();
                    response.send(contactsAPI.getHtml(password));
                    response.end();
                }catch (Exception e){
                    e.printStackTrace();
                    response.send("Error : "+e.toString());
                    response.end();
                }
            }
        });



//apps
        server.get("/" + password + "/myapps/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                response.send(Utility.getAppListHtml(getApplicationContext(), password));
                response.end();
            }
        });

//app
        server.get("/" + password + "/myapp/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath();


                path = path.substring(("/"+password+"/myapp/").length());


                if(path.endsWith("/")||path.endsWith("\\"))path = path.substring(0,path.length()-1);



                try{

                    path = URLDecoder.decode(path,"utf-8");

                    if(path == null||path.length()<1)throw new Exception();


                    response.sendFile(new File(Utility.appPath.get(path)));
                    response.end();

                }catch (Exception e){

                    response.send(e.toString());

//                    Utility.send404(response,getApplicationContext());
                    response.end();
                }
            }
        });


//appicon
        server.get("/" + password + "/myappicon/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath();

                path = path.substring(("/"+password+"/myappicon/").length());


                if(path.endsWith("/")||path.endsWith("\\"))path = path.substring(0,path.length()-1);



                try{

                    path = URLDecoder.decode(path,"utf-8");

                    if(path == null||path.length()<1)throw new Exception();



                    response.send("image/jpeg",Utility.appIcon.get(path));
                    response.end();

                }catch (Exception e){

                    response.send(e.toString());
                    e.printStackTrace();
//                    Utility.send404(response,getApplicationContext());
                    response.end();
                }
            }
        });






//files

        server.get("/" + password + "/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                String path = request.getPath();
                try {
                    path = URLDecoder.decode(path,"utf-8");
                } catch (UnsupportedEncodingException e) {
                    response.send("<h1>Cannot Decode URL : "+path+"</h1>");
                    response.end();

                    e.printStackTrace();
                    return;
                }


                if(!path.startsWith("/"))path="/"+path;

                if(path.startsWith("/" + password + "/"))path=path.substring(("/" + password + "/").length());

                if(path==null || path.length()<1)path="/";


                if("/".equals(path)){
                    response.send("text/html",Utility.getServerHomeHtml(getApplicationContext(),password));
                    response.end();
                    return;
                }

                try{
                    File f = new File(path);
                    if(f.exists()){
                        if(f.isDirectory()){
                            response.send("text/html" , Utility.getHTML(getApplicationContext(),path,password));
                        }else{
                            response.sendStream(new FileInputStream(f),f.length());
                        }
                        response.end();
                    }else {
                        throw new Exception("File Not Found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
//                    response.send("File Not Found : "+path);
//                    response.end();
                    Utility.send404(response,getApplicationContext());
                }
            }
        });



//Enter Password

        server.get("/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Utility.sendEnterPassword(response,getApplicationContext());
//                response.send("Enter Password  : "+request.getPath() );
//                response.end();

            }
        });
        server.listen(port);



    }

    void sendZip(final File f, final AsyncHttpServerResponse response){
        try{

            String mPath = f.getAbsolutePath();
            if(  mPath.endsWith("/")  ||  mPath.endsWith("\\")  ){}
            else mPath = mPath+"/";


            final String inputPath = mPath ;




            int BUF_LEN = 128*1024 ; // 1 M.B. buffer
            final CircularByteBuffer cbb = new CircularByteBuffer(BUF_LEN);
            final ZipOutputStream zos = new ZipOutputStream(cbb.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{

                        WriteToOutputStream(f,zos);
                        zos.close();
                    }
                    catch (Exception e){e.printStackTrace();}
                }


                void WriteToOutputStream(File file,ZipOutputStream zos) throws Exception{

                    if(!response.isOpen())return;

                    if(file.isFile()){


                        ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(inputPath.length()));
                        Log.d("Zip Entry : ",file.getAbsolutePath().substring(inputPath.length() ) );
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

                if(!response.isOpen())return;

                byte[] buf = new byte[len];
                for (int i = 0; i < len; i++) {
                    buf[i] = buffer[i];
                }


                response.write(new ByteBufferList(buf));

            }


            cbb.getInputStream().close();
            cbb.getOutputStream().close();


        }
        catch (Exception e){e.printStackTrace();}
    }

//
//    void sendZip(final File f, final AsyncHttpServerResponse response){
//        try{
//
//            String mPath = f.getAbsolutePath();
//            if(  mPath.endsWith("/")  ||  mPath.endsWith("\\")  ){}
//            else mPath = mPath+"/";
//
//
//            final String inputPath = new String(mPath) ;
//
//            final PipedOutputStream pos = new PipedOutputStream();
//            final PipedInputStream pis = new PipedInputStream();
//            pos.connect(pis);
//
//
//
//
//
//            final Thread t1 = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try{
//                        if(Thread.currentThread().isInterrupted()){
//                            pos.close();
//                            return;
//                        }
//                        ZipOutputStream zos = new ZipOutputStream(pos)
//                        WriteToOutputStream(f,zos);
//                        zos.close();
//                        pos.close();
//                    }
//                    catch (Exception e){e.printStackTrace();}
//                }
//
//                void WriteToOutputStream(File file,ZipOutputStream zos) throws Exception{
//
//                    if(file.isFile()){
//
//                        ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(inputPath.length()));
//                        zos.putNextEntry(ze);
//
//                        FileInputStream fis = new FileInputStream(file);
//                        int len;
//                        byte[] buffer = new byte[8192];
//                        while(  (len = fis.read(buffer)) > 0){
//                            zos.write(buffer,0,len);
//                        }
//                        fis.close();
//                        zos.closeEntry();
//
//                    }else{
//
//                        File[] subFiles = file.listFiles();
//                        for(File f : subFiles){
//                            WriteToOutputStream(f,zos);
//                        }
//
//                    }
//
//                }
//            });
//            t1.setPriority(Thread.MAX_PRIORITY);
//            t1.start();
//
//
//
//            response.setClosedCallback(new CompletedCallback() {
//                @Override
//                public void onCompleted(Exception ex) {
//                    t1.interrupt();
//                }
//            });
//
//
//            int len ;
//            byte[] buffer = new byte[32768];
//
//            while ((len = pis.read(buffer))>0){
//
//                Log.d("len : ",len+"");
//
//                byte[] buf = new byte[len];
//                for(int i=0;i<len;i++){
//                    buf[i] = buffer[i];
//                }
//                response.write(new ByteBufferList(buf));
//            }
//            response.end();
//        }
//        catch (Exception e){e.printStackTrace();}
//    }
//


}
