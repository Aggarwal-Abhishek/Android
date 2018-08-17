package com.abhishek.mywebserver2.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.support.v7.widget.ViewUtils;
import android.util.Log;
import android.view.WindowManager;

import com.abhishek.mywebserver2.Utility;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ScreenStreamService extends Service {
    public ScreenStreamService() {}
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    public static int port = 4444;
    public static String password  = "a";

    AsyncHttpServer server ;
    CopyOnWriteArrayList<WebSocket> webSockets = new CopyOnWriteArrayList<WebSocket>() ;

    public static String
        RESULT_CODE = "resultcode" ,
        INTENT_DATA = "intentdata" ;

    MediaProjectionManager mgr ;
    WindowManager wmgr ;
    MediaProjection projection ;
    VirtualDisplay vdisplay ;
    ImageReader imageReader ;
    int width,height ;

    static int VIRTUAL_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY|
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC|
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;

    HandlerThread handlerThread ;
    Handler handler ;

    AtomicReference<byte[]>jpeg = new AtomicReference<byte[]>() ;
    Bitmap latestBitmap ;
    public static int quality = 50,resize=50;


    @Override
    public void onCreate() {
        super.onCreate();

        setUpServer();

        mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE) ;
        wmgr = (WindowManager)getSystemService(WINDOW_SERVICE) ;

        handlerThread = new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_BACKGROUND) ;
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) ;
    }

    @Override
    public void onDestroy() {
        projection.stop();
        imageReader.close();
        vdisplay.release();

        server.stop();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        setUpMediaProjection(intent);

        setUpImageReader();

        setUpVirtualDisplay();

        return START_STICKY ;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        imageReader.close();
        vdisplay.release();

        setUpImageReader();
        setUpVirtualDisplay();
    }

    void setUpServer(){
        server = new AsyncHttpServer();

        if(Utility.getPortPreference("streamport",getApplicationContext())!=-1){
            port = Utility.getPortPreference("streamport",getApplicationContext());
        }
        if(Utility.getPasswordPreference("password",getApplicationContext())!=null){
            password = Utility.getPasswordPreference("password",getApplicationContext()) ;
        }




        server.get("/" + password + "/screen/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("image/jpeg",jpeg.get());
                response.end();
            }
        });

        server.get("/"+password + "/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    AssetFileDescriptor afd = getAssets().openFd("htmlPages/soc.html") ;
                    response.sendStream(afd.createInputStream(),afd.getLength());
                    response.end();

                } catch (IOException e) {
                    response.send("Error in assets/soc.html");
                    response.end();
                    e.printStackTrace();
                }
            }
        });

        server.websocket("/screenss", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                webSockets.add(webSocket);
//                webSocket.send("Abhi");

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if(ex != null){
                            ex.printStackTrace();
                        }else{
                            webSockets.remove(webSocket) ;
                        }
                    }
                });
            }
        });


        server.get("/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                String path = request.getPath();

                Utility.sendEnterPassword(response,getApplicationContext());

            }
        });






        server.listen(port) ;
    }

    void setUpMediaProjection(Intent intent){
        projection = mgr.getMediaProjection(
                intent.getIntExtra(RESULT_CODE,-1),
                (Intent)intent.getParcelableExtra(INTENT_DATA)
        );
        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                vdisplay.release();
                super.onStop();
            }
        },handler);
    }

    void setUpVirtualDisplay(){
        vdisplay = projection.createVirtualDisplay("ScreenProjection",
                width,height,getResources().getDisplayMetrics().densityDpi,
                VIRTUAL_DISPLAY_FLAGS,imageReader.getSurface(),
                null,handler) ;
    }

    void setUpImageReader(){
        Point size = new Point();
        wmgr.getDefaultDisplay().getSize(size);
        width = size.x ;
        height = size.y ;

        width = 1280;
        height = 720;


        Log.d("Abhi","width : "+width);
        Log.d("Abhi","Height : "+height);



        imageReader = ImageReader.newInstance(width,height, PixelFormat.RGBA_8888,2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                if(imageReader == null)return;

                final Image image;

                try {
                    image = imageReader.acquireLatestImage();
                }catch (Exception e){
                    return;
                }

                if(image == null)return;

                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();

                int pixelStride=planes[0].getPixelStride();
                int rowStride=planes[0].getRowStride();
                int rowPadding=rowStride - pixelStride * width;
                int bitmapWidth=width + rowPadding / pixelStride;


                if(latestBitmap == null || latestBitmap.getWidth()!=bitmapWidth|| latestBitmap.getHeight()!=height){
                    if(latestBitmap!=null)latestBitmap.recycle();
                    latestBitmap = Bitmap.createBitmap(width, height,Bitmap.Config.ARGB_8888) ;
                }
                latestBitmap.copyPixelsFromBuffer(buffer);

                image.close();

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                Bitmap.createScaledBitmap(latestBitmap,(width*resize)/100,(height*resize)/100,false)
//                Bitmap.createBitmap(latestBitmap,0,0,(width*resize)/100,(height*resize)/100)
                        .compress(Bitmap.CompressFormat.JPEG,quality,baos) ;

                if(password==null || password.length()<1){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            jpeg.set(baos.toByteArray());
                            for(WebSocket webSocket : webSockets){
                                webSocket.send("/screen/"+ Long.toString(SystemClock.uptimeMillis()));
                            }
                        }
                    }).start();
                }else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            jpeg.set(baos.toByteArray());
                            for(WebSocket webSocket : webSockets){
                                webSocket.send("/"+password+"/screen/"+ Long.toString(SystemClock.uptimeMillis()));
                            }
                        }
                    }).start();
                }


            }
        },handler);
    }

}

