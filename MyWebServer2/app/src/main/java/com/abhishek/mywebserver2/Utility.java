package com.abhishek.mywebserver2;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.abhishek.mywebserver2.services.ReceiveFilesService;
import com.abhishek.mywebserver2.services.ScreenStreamService;
import com.abhishek.mywebserver2.services.SendDataService;
import com.abhishek.mywebserver2.services.WebServerService;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.samskivert.mustache.Mustache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Abhishek on 1/19/2018.
 */




public class Utility {


    public static ArrayList<String>appName ;
    public static HashMap<String,String>appPath = new HashMap<String, String>();
    public static HashMap<String,byte[]>appIcon = new HashMap<String, byte[]>();
    public static String AppsHtmlPage ;



    public static String getHTML(Context context , final String path , final String password) {

        class ListItem{


            String onclick , icon , name , date , size ;

            public ListItem(String onclick, String icon, String name, long date, long size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = Utility.getSize(size);
            }
            public ListItem(String onclick, String icon, String name, long date, String size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = size ;
            }
        };

        String
                FileIconPath = "/"+password+"/assets/icons/file.png" ,
                FolderIconPath = "/"+password+"/assets/icons/folder.png" ,
                DirectoryPageText ;

        try {

            InputStream is = context.getAssets().open("htmlPages/directoryPage.html");
            byte[] data = new byte[is.available()] ;
            is.read(data) ;
            is.close();

            DirectoryPageText = new String(data) ;
            //            Toast.makeText(context,DirectoryPageText,Toast.LENGTH_LONG).show();

            final File file = new File(path) ;

            String text = new String(DirectoryPageText) , parent_dir="#";

            try {
                parent_dir = file.getParent();
                if(parent_dir == null)parent_dir = "#" ;
            }catch(Exception e) {parent_dir="#";}



            ArrayList<ListItem> itemList = new ArrayList<ListItem>();


            String[] childs = file.list() ;
            if(childs == null)childs = new String[]{} ;

            long size = 0;

            for(String child : childs) {

                File f = new File(file,child) ;


                if(f.isDirectory()) {

                    String[] temp = f.list() ;
                    String tstr ;
                    if(temp == null )tstr = " ";
                    else if(temp.length <= 1)tstr = temp.length + " file" ;
                    else tstr = temp.length + " files" ;




                    itemList.add(
                            new ListItem(URLEncoder.encode("/"+password + f.getAbsolutePath(),"utf-8").replace("%2F","/"), FolderIconPath, child, f.lastModified(), tstr)
                    );

                }else {

                    String ext = child.substring(child.lastIndexOf(".")+1);
                    ext = ext.toLowerCase();

                    try{
                        AssetFileDescriptor afd = context.getAssets().openFd("icons/"+ext+".png");
                        itemList.add(
                                new ListItem(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/"), "/"+password+"/assets/icons/"+ext+".png", child, f.lastModified(), f.length())
                        );

                    }catch (Exception e){
                        itemList.add(
                                new ListItem(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/"), FileIconPath, child, f.lastModified(), f.length())
                        );
                    }


                    size += f.length() ;
                }

            }




            final String pString = URLEncoder.encode("/"+password+"/"+parent_dir,"utf-8").replace("%2F","/");
            final String zString = URLEncoder.encode("/"+password+"/zip"+file.getAbsolutePath(),"utf-8").replace("%2F","/");
            final String gString = URLEncoder.encode("/"+password+"/gallery"+file.getAbsolutePath(),"utf-8").replace("%2F","/");
            final ArrayList<ListItem> temp_item_list = new ArrayList<ListItem>(itemList) ;


            final long size2 = size ;



            text = Mustache.compiler().compile(text).execute(new Object() {
                String parent = pString ;
                String zip =  zString;
                String gallery = gString ;
                String filesize = getSize(size2);
                String location = file.getAbsolutePath() ;
                ArrayList<ListItem>list_item = temp_item_list ;

            });

            //			return"";
            return text ;
        }catch(Exception e) {e.printStackTrace();}


        return "";

    }

    public static String getHTML(Context context  , final String password,String[] childs) {

        class ListItem{


            String onclick , icon , name , date , size ;

            public ListItem(String onclick, String icon, String name, long date, long size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = Utility.getSize(size);
            }
            public ListItem(String onclick, String icon, String name, long date, String size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = size ;
            }
        };

        String
                FileIconPath = "/"+password+"/assets/icons/file.png" ,
                FolderIconPath = "/"+password+"/assets/icons/folder.png" ,
                DirectoryPageText ;

        try {

            InputStream is = context.getAssets().open("htmlPages/directoryPage.html");
            byte[] data = new byte[is.available()] ;
            is.read(data) ;
            is.close();

            DirectoryPageText = new String(data) ;
            //            Toast.makeText(context,DirectoryPageText,Toast.LENGTH_LONG).show();



            String text = new String(DirectoryPageText) , parent_dir="#";




            ArrayList<ListItem> itemList = new ArrayList<ListItem>();



            if(childs == null)childs = new String[]{} ;

            long size = 0;

            for(String child : childs) {

                File f = new File(child) ;


                if(f.isDirectory()) {

                    String[] temp = f.list() ;
                    String tstr ;
                    if(temp == null )tstr = " ";
                    else if(temp.length <= 1)tstr = temp.length + " file" ;
                    else tstr = temp.length + " files" ;




                    itemList.add(
                            new ListItem(URLEncoder.encode("/"+password + f.getAbsolutePath(),"utf-8").replace("%2F","/"), FolderIconPath, child, f.lastModified(), tstr)
                    );

                }else {
                    String ext = child.substring(child.lastIndexOf(".")+1);
                    ext = ext.toLowerCase();

                    try{
                        AssetFileDescriptor afd = context.getAssets().openFd("icons/"+ext+".png");
                        itemList.add(
                                new ListItem(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/"), "/"+password+"/assets/icons/"+ext+".png", child, f.lastModified(), f.length())
                        );

                    }catch (Exception e){
                        itemList.add(
                                new ListItem(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/"), FileIconPath, child, f.lastModified(), f.length())
                        );
                    }


                    size += f.length() ;
                }

            }




            final String pString = "#";
            final String zString = "#";
            final ArrayList<ListItem> temp_item_list = new ArrayList<ListItem>(itemList) ;


            final long size2 = size ;



            text = Mustache.compiler().compile(text).execute(new Object() {
                String parent = pString ;
                String zip =  zString;
                String gallery = "#";
                String filesize = getSize(size2);
                String location = "Shared Files & Folders will Appear Here..." ;
                ArrayList<ListItem>list_item = temp_item_list ;

            });

        //			return"";
            return text ;
        }catch(Exception e) {e.printStackTrace();}


        return "";

    }

    public static String getGalleryHTML(Context context, String path, final String password) {

        class C1{
            String image;
            C1(String image){
                this.image = image ;
            }
        };

        String
                DirectoryPageText ;

        try {

            InputStream is = context.getAssets().open("htmlPages/gallery.html");
            byte[] data = new byte[is.available()] ;
            is.read(data) ;
            is.close();

            DirectoryPageText = new String(data) ;

            final File file = new File(path) ;

            String text = new String(DirectoryPageText) ;


            String[] childs = file.list() ;
            if(childs == null)childs = new String[]{} ;


            final ArrayList<C1>list = new ArrayList<C1>();

            for(String child : childs) {

                File f = new File(file,child) ;

                if(f.isFile()) {

                    String ext = child.substring(child.lastIndexOf(".")+1);
                    ext = ext.toLowerCase();

                    if(ext.equals("jpeg")||ext.equals("jpg")||ext.equals("png")||ext.equals("bitmap")||ext.equals("svg")){
                        list.add(new C1(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/")));
                    }
                }

            }



            text = Mustache.compiler().compile(text).execute(new Object(){
                String css = "/"+password+"/assets/css/w3.css" ;
                ArrayList<C1>imageItems = list;
            });

            return text ;
        }catch(Exception e) {e.printStackTrace();}


        return "";
    }

    public static String getServerHomeHtml(final Context context , String password){
        class ListItem{


            String onclick , icon , name , date , size ;

            public ListItem(String onclick, String icon, String name, long date, long size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = Utility.getSize(size);
            }
            public ListItem(String onclick, String icon, String name, long date, String size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = size ;
            }
        };

        String
                FileIconPath = "/"+password+"/assets/icons/file.png" ,
                FolderIconPath = "/"+password+"/assets/icons/folder.png" ,
                DirectoryPageText ;

        try {

            InputStream is = context.getAssets().open("htmlPages/serverhome.html");
            byte[] data = new byte[is.available()] ;
            is.read(data) ;
            is.close();

            DirectoryPageText = new String(data) ;
        //            Toast.makeText(context,DirectoryPageText,Toast.LENGTH_LONG).show();

            final File file = new File("/") ;

            String text = new String(DirectoryPageText) , parent_dir="#";


            ArrayList<ListItem> itemList = new ArrayList<ListItem>();


            String[] childs = file.list() ;
            if(childs == null)childs = new String[]{} ;

            long size = 0;

            for(String child : childs) {

                File f = new File(file,child) ;


                if(f.isDirectory()) {

                    String[] temp = f.list() ;
                    String tstr ;
                    if(temp == null )tstr = " ";
                    else if(temp.length <= 1)tstr = temp.length + " file" ;
                    else tstr = temp.length + " files" ;




                    itemList.add(
                            new ListItem(URLEncoder.encode("/"+password + f.getAbsolutePath(),"utf-8").replace("%2F","/"), FolderIconPath, child, f.lastModified(), tstr)
                    );

                }else {

                    String ext = child.substring(child.lastIndexOf(".")+1);
                    ext = ext.toLowerCase();

                    try{
                        AssetFileDescriptor afd = context.getAssets().openFd("icons/"+ext+".png");
                        itemList.add(
                                new ListItem(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/"), "/"+password+"/assets/icons/"+ext+".png", child, f.lastModified(), f.length())
                        );

                    }catch (Exception e){
                        itemList.add(
                                new ListItem(URLEncoder.encode("/"+password+f.getAbsolutePath(),"utf-8").replace("%2F","/"), FileIconPath, child, f.lastModified(), f.length())
                        );
                    }


                    size += f.length() ;
                }

            }




            final String pString = ".";
            final String zString = URLEncoder.encode("/"+password+"/zip"+file.getAbsolutePath(),"utf-8").replace("%2F","/");
            final String gString = URLEncoder.encode("/"+password+"/gallery"+file.getAbsolutePath(),"utf-8").replace("%2F","/");
            final ArrayList<ListItem> temp_item_list = new ArrayList<ListItem>(itemList) ;


            final long size2 = size ;



            text = Mustache.compiler().compile(text).execute(new Object() {
                String parent = pString ;
                String zip =  zString;
                String gallery = gString ;
                String filesize = getSize(size2);
                String location = file.getAbsolutePath() ;
                ArrayList<ListItem>list_item = temp_item_list ;
                ArrayList<ServiceClass>service_list = getRunningServices(context);
                List<UrlClass>ip_list = getUrlsList();
                String internal_path = Environment.getExternalStorageDirectory().getAbsolutePath();
                String external_path = Environment.getExternalStorageState();
                String screenstreamport = (isMyServiceRunning(ScreenStreamService.class,context)?ScreenStreamService.port:-1 ) +"";
                String senddataport = (isMyServiceRunning(ReceiveFilesService.class,context)?ReceiveFilesService.port:-1)+"";
                String receivedataport = (isMyServiceRunning(SendDataService.class,context)?SendDataService.port:-1)+"";
            });

        //			return"";
            return text ;
        }catch(Exception e) {e.printStackTrace();}


        return "";



    }

    public static String getAppListHtml(Context context , String password){


        if(AppsHtmlPage != null) return AppsHtmlPage ;


        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages =  pm.getInstalledApplications(PackageManager.GET_META_DATA);




            appName = new ArrayList<String >();


            for (ApplicationInfo packageInfo : packages) {
                String x = pm.getApplicationLabel(packageInfo).toString() ;

                appName.add(x);
                appPath.put(x+".apk",packageInfo.sourceDir) ;


                Log.d("abhi",packageInfo.sourceDir);

                try {



                    Drawable d = pm.getApplicationIcon(packageInfo.packageName);
                    Bitmap b = ((BitmapDrawable)d).getBitmap();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight())
                            .compress(Bitmap.CompressFormat.JPEG,100,baos);

                    appIcon.put(x,baos.toByteArray());

                } catch (Exception e) {
                    appIcon.put(x,null);
                    e.printStackTrace();
                }
            }


        class ListItem{


            String onclick , icon , name , date , size ;

            public ListItem(String onclick, String icon, String name, long date, long size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = Utility.getSize(size);
            }
            public ListItem(String onclick, String icon, String name, long date, String size) {

                this.onclick = onclick;
                this.icon = icon;
                this.name = name;
                this.date = Utility.getDate(date);
                this.size = size ;
            }
        };



        try{

            InputStream is = context.getAssets().open("htmlPages/directoryPage.html");
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();

            String DirectoryPageText = new String(data);

            final ArrayList<ListItem>itemList = new ArrayList<ListItem>();


            long size = 0;
            for(String name:appName){
                size += new File(appPath.get(name+".apk")).length();

                if(appIcon.get(name)==null){
                    itemList.add(new ListItem(
                                    URLEncoder.encode("/"+password+"/myapp/"+name+".apk","utf-8").replace("%2F","/"),
                                    URLEncoder.encode("/"+password+"/assets/icons/apk.png","utf-8").replace("%2F","/"),
                                    name,
                                    new File(appPath.get(name+".apk")).lastModified(),
                                    new File(appPath.get(name+".apk")).length()
                            )
                    );
                }else {
                    itemList.add(new ListItem(
                                    URLEncoder.encode("/"+password+"/myapp/"+name+".apk","utf-8").replace("%2F","/"),
                                    URLEncoder.encode("/"+password+"/myappicon/"+name,"utf-8").replace("%2F","/"),
                                    name,
                                    new File(appPath.get(name+".apk")).lastModified(),
                                    new File(appPath.get(name+".apk")).length()
                            )
                    );
                }


            }

            final long size2 = size ;

            DirectoryPageText = Mustache.compiler().compile(DirectoryPageText)
                    .execute(new Object(){
                        String parent = "#";
                        String zip = "#";
                        String gallery = "#";
                        String filesize = getSize(size2);
                        String location = "All Apps";
                        ArrayList<ListItem>list_item = itemList ;
                    });


            AppsHtmlPage = new String(DirectoryPageText) ;

            return DirectoryPageText ;
        }
        catch (Exception e){e.printStackTrace();}

        return "Error";

    }












































    public static ArrayList<ServiceClass> getRunningServices(Context context){
        ArrayList<ServiceClass>ret = new ArrayList<>();

        if(isMyServiceRunning(WebServerService.class,context))ret.add(new ServiceClass("Web Server"));
        if(isMyServiceRunning(ScreenStreamService.class,context))ret.add(new ServiceClass("Screen Stream Service"));
        if(isMyServiceRunning(SendDataService.class,context))ret.add(new ServiceClass("Receive Data"));
        if(isMyServiceRunning(ReceiveFilesService.class,context))ret.add(new ServiceClass("Send Data"));

        return  ret ;
    }
    public static boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager)context.getSystemService (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public static void send404(AsyncHttpServerResponse response,Context context){
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("htmlPages/filenotfound.html");
            response.sendStream(afd.createInputStream(),afd.getLength());
            response.end();
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void sendEnterPassword(AsyncHttpServerResponse response,Context context){
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("htmlPages/pwd.html");
            response.sendStream(afd.createInputStream(),afd.getLength());
            response.end();
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void sendPermissionDenied(AsyncHttpServerResponse response,Context context){
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("htmlPages/permission.html");
            response.sendStream(afd.createInputStream(),afd.getLength());
            response.end();
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getSize(long size) {
        String subs = " " ;

        if(size<1024)subs += size + " Bytes" ;
        else if(size<1048576)subs += (size/1024) + "." + ((size%1024)*10)/1024 + " KB" ;
        else if(size<1073741824)subs += (size/1048576) + "." + ((size%1048576)*10)/1048576 + " MB" ;
        else subs += (size/1073741824) + "." + ((size%1073741824)*10)/1073741824 + " GB" ;

        return subs ;
    }
    public static String getDate(long lastModified) {
        return DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT , DateFormat.DEFAULT).format(lastModified) ;
    }
    public static int getPortPreference(String s,Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String p = preferences.getString(s,"");
        int port ;
        try {
            port = Integer.parseInt(p) ;
            if(port>1024 && port<65535)return port;
            else return -1;
        }catch (Exception e){
            return -1;
        }
    }
    public static String getPasswordPreference(String s,Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String password = preferences.getString(s,"");

        if(password==null || password.length()<1)return null;
        for(int i=0;i<password.length();i++){
            char c = password.charAt(i);
            if((c>='a' && c<='z')||(c>='A' && c<='Z')||(c>='0' && c<='9'))continue;
            return null ;
        }
        return password ;
    }
    public static String[] getUrls(int port , String password){
        List<String> urls = new ArrayList<String>() ;

        try{
            for(Enumeration<NetworkInterface> enInterface = NetworkInterface.getNetworkInterfaces();
                enInterface.hasMoreElements();){
                NetworkInterface ni = enInterface.nextElement();

                for(Enumeration<InetAddress> enAddress = ni.getInetAddresses();
                    enAddress.hasMoreElements();){
                    InetAddress addr = enAddress.nextElement() ;

                    if(addr instanceof Inet4Address){

                        if(password == null || password.length()<1){
                            urls.add("http://"+addr.getHostAddress()+":"+port+"/") ;
                        }else{
                            urls.add("http://"+addr.getHostAddress()+":"+port+"/"+password+"/") ;
                        }

                    }else if(addr instanceof Inet6Address){
                        String tmp = addr.getHostAddress();
                        int x = tmp.lastIndexOf("%");
                        if(x!=-1)tmp = tmp.substring(0,x) ;

                        if(password == null || password.length()<1){
                            urls.add("http://["+tmp+"]:"+port+"/") ;
                        }else {
                            urls.add("http://["+tmp+"]:"+port+"/"+password+"/") ;
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return urls.toArray(new String[0]) ;
    }
    public static List<UrlClass> getUrlsList(){
        List<UrlClass> urls = new ArrayList<>() ;

        try{
            for(Enumeration<NetworkInterface> enInterface = NetworkInterface.getNetworkInterfaces();
                enInterface.hasMoreElements();){
                NetworkInterface ni = enInterface.nextElement();

                for(Enumeration<InetAddress> enAddress = ni.getInetAddresses();
                    enAddress.hasMoreElements();){
                    InetAddress addr = enAddress.nextElement() ;

                    if(addr instanceof Inet4Address){

                        urls.add(new UrlClass("http://"+addr.getHostAddress()+"/"));

                    }else if(addr instanceof Inet6Address){
                        String tmp = addr.getHostAddress();
                        int i = tmp.lastIndexOf("%");
                        if(i!=-1)tmp = tmp.substring(0,i);

                        urls.add(new UrlClass("http://["+tmp+"]/")) ;

                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return urls;
    }
}



class ServiceClass{
    String service_name ;
    ServiceClass(String x){
        service_name = x;
    }
}
class UrlClass{
    String url ;
    UrlClass(String x){
        url = x;
    }
}
