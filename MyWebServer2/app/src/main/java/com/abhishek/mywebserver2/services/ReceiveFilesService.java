package com.abhishek.mywebserver2.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.icu.text.LocaleDisplayNames;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.abhishek.mywebserver2.Utility;
import com.codekidlabs.storagechooser.StorageChooser;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class ReceiveFilesService extends Service {
    public ReceiveFilesService() {}
    @Override
    public IBinder onBind(Intent intent) {throw new UnsupportedOperationException("Not yet implemented");}


    public static String password = "a";
    public static int port = 2222 ;
    public static String saveFileDirectory = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MyServer";

    public HashMap<Integer,WebSocket>webSockets = new HashMap<Integer, WebSocket>();
    public HashMap<Integer,Long> received = new HashMap<Integer, Long>();




    @Override
    public void onCreate() {
        super.onCreate();


        final AsyncHttpServer server = new AsyncHttpServer();

        server.post("/upload/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {

                if(!(new File(saveFileDirectory)).exists()){
                    (new File(saveFileDirectory)).mkdirs();
                }

                final MultipartFormDataBody body = (MultipartFormDataBody)request.getBody();
                body.setMultipartCallback(new MultipartFormDataBody.MultipartCallback() {
                    @Override
                    public void onPart(final Part part) {
                        if(part.isFile()){
                            try {

                                File f = new File(saveFileDirectory,part.getFilename());
                                final FileOutputStream fo = new FileOutputStream(f) ;


                                body.setDataCallback(new DataCallback() {
                                    @Override
                                    public void onDataAvailable(DataEmitter emitter, final ByteBufferList bb) {
                                        try {

                                            String path = request.getPath();
                                            if(!path.startsWith("/"))path="/"+path;
                                            path = path.substring("/upload/".length());

                                            try{


                                                final int i = Integer.parseInt(path) ;
                                                send(i,bb.remaining());

                                            }
                                            catch (Exception e){
                                                e.printStackTrace();
                                            }

                                            fo.write(bb.getAllByteArray());
                                            bb.recycle();

                                        } catch (IOException e) {e.printStackTrace();}
                                    }
                                });

                            } catch (FileNotFoundException e) {e.printStackTrace();}

                        }
                    }
                });



                request.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        response.send("Uploaded to : "+saveFileDirectory);
                    }
                });

            }
        });





        server.websocket("/register/.*", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {


                String path = request.getPath();
                if(path.startsWith("/")||path.startsWith("\\")){}
                else path="/"+path;

                path = path.substring("/register/".length());

                try{
                    int i = Integer.parseInt(path);

                    webSockets.put(i,webSocket);
                    received.put(i,0L);

                }catch (Exception e){
                    Log.d("SocketError : ",path);
                    e.printStackTrace();

                }

            }
        });


        server.get("/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    AssetFileDescriptor afd = getAssets().openFd("htmlPages/upload.html");
                    response.sendStream(afd.createInputStream(),afd.getLength());
                    response.end();
                } catch (IOException e) {
                    Utility.send404(response,getApplicationContext());e.printStackTrace();}
            }
        });







        server.listen(port);
    }

    void send(final int key, final long data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{

                    long rec = received.get(key);
                    rec+=data;
                    received.put(key,rec);

                    webSockets.get(key).send(rec+"");

                }catch (Exception e){e.printStackTrace();}
            }
        }).start();

    }

}
