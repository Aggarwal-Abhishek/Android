package com.abhishek.pdfmanager;

import android.os.Environment;

import java.io.File;

import io.objectbox.Box;

/**
 * Created by Abhishek on 7/4/2018.
 */

public class Utility {

    final static String AppName = "0_Abhi_0";

    // 0, 1, 2 : good, low, none
    public static int Thumb_Quality(Box<SettingDB> mybox){

        if(mybox.count() == 2){
            int id;

            try{
                id = Integer.parseInt(mybox.get(2).str);

                if(id == R.id.setting_thumb_checkbox_good)return 0;
                if(id == R.id.setting_thumb_checkbox_low)return 1;
                if(id == R.id.setting_thumb_checkbox_none)return 2;

                else throw new Exception();
            }catch (Exception e){e.printStackTrace(); return 1;}
        }

        return 1;
    }

    public static String PDF_Location(Box<SettingDB> mybox){

        if(mybox.count() == 2){

            try{
                String s = mybox.get(1).str;
                if(s.endsWith("/") || s.endsWith("\\")){}
                else s = s+"/";

                return s + AppName + "/pdf/";
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+AppName+"/pdf/";
    }

    public static String IMG_Location(Box<SettingDB> mybox){

        if(mybox.count() == 2){

            try{
                String s = mybox.get(1).str;
                if(s.endsWith("/") || s.endsWith("\\")){}
                else s = s+"/";

                return s + AppName + "/images/";
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+AppName+"/images/";
    }

    public static String TMP_Location(Box<SettingDB> mybox){

        if(mybox.count() == 2){

            try{
                String s = mybox.get(1).str;
                if(s.endsWith("/") || s.endsWith("\\")){}
                else s = s+"/";

                return s + AppName + "/temp/";
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+AppName+"/temp/";
    }
}
