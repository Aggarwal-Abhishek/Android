package com.abhishek.mywebserver2.extra;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Abhishek on 1/27/2018.
 */

public class ContactsAPI {

    Context context;

    public ContactsAPI(Context context){this.context = context ;}


    private final String DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;
    private final String FILTER = DISPLAY_NAME + " NOT LIKE '%@%'";
    private final String ORDER = String.format("%1$s COLLATE NOCASE", DISPLAY_NAME);
    private final String[] PROJECTION = {
            ContactsContract.Contacts._ID,
            DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
    };

    static boolean ok = false ;


    private static ArrayList<String> nameList = new ArrayList<>();
    private static ArrayList<String> emailList = new ArrayList<>();
    private static ArrayList<String> numberList = new ArrayList<>();
    private static ArrayList<Bitmap> bitmapsList = new ArrayList<>();

    static String html ;


    public void getDetails() {

        if(ok == true){
            return;
        }

        ContentResolver cr = context.getContentResolver();

        Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                PROJECTION,
                FILTER, null, ORDER);

        if (cursor != null && cursor.moveToFirst()) {

            do{
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                Integer hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));


                String email = null ;
                Cursor ce = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);

                if (ce != null && ce.moveToFirst()) {
                    do{
                        email = ce.getString(ce.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        if(email!=null)break;
                    }while (ce.moveToNext());

                    ce.close();
                }


                String phone = null ;
                if (hasPhone > 0) {
                    Cursor cp = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    if (cp != null && cp.moveToFirst()) {
                        phone = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        cp.close();
                    }
                }


                Bitmap photo = null ;
                try{

                    InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                            cr,ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,new Long(id))
                    );

                    if(inputStream != null){
                        photo = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    }


                }catch (Exception e){
                    e.printStackTrace();
                }



                if((name!=null)||(email!=null)||(phone!=null)){
                    nameList.add(name);
                    emailList.add(email);
                    numberList.add(phone);
                    bitmapsList.add(photo);
                }


            }while (cursor.moveToNext());

            cursor.close();
        }
    }

    String encodeBase64(Bitmap image){
    if (image == null)return "";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b,Base64.DEFAULT);
    }

    public String getHtml(String password){

        if(ok == true )return  html ;

        try{

            InputStream is = context.getAssets().open("htmlPages/contacts.html");
            byte[] data = new byte[is.available()] ;
            is.read(data) ;
            is.close();

            String DirectoryPageText = new String(data) ;

            String row = "";

            for(int i=0;i<Math.max(nameList.size(),emailList.size());i++ ){
                Log.d("abhi",nameList.get(i));

                if(bitmapsList.get(i) == null){

                    row += getRow(new ListItem("/"+password+"/assets/icons/vcf.png",nameList.get(i),numberList.get(i),emailList.get(i)));

                }else{
                    row += getRow(new ListItem("data:image/jpeg;base64, "+encodeBase64(bitmapsList.get(i)),nameList.get(i),numberList.get(i),emailList.get(i)));
                }


            }


            html = DirectoryPageText.replace("{{list}}",row);

            ok = true ;

            return  html;

        }catch (Exception e){e.printStackTrace();return e.getMessage();}
    }


    String getRow(ListItem l){
        String ret = "<tr class=\"row_item\">\n" +
                "                <td><img src=\"{{icon}}\"></td>\n" +
                "                <td>{{name}}</td>\n" +
                "                <td>&nbsp;&nbsp;</td>\n" +
                "                <td>{{phone}}</td>\n" +
                "                <td>{{email}}</td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "                <td><hr></td>\n" +
                "                <td colspan=\"2\"><hr></td>\n" +
                "                <td><hr></td>\n" +
                "                <td><hr></td>\n" +
                "            </tr>"
                ;




        return  ret
                .replace("{{icon}}",l.icon)
                .replace("{{name}}",l.name)
                .replace("{{phone}}",l.phone)
                .replace("{{email}}",l.email)
                ;

    }

}



class ListItem{
    String  icon , name , phone , email ;

    public ListItem(String icon, String name, String phone, String email) {
        this.icon = icon==null?" ":icon;
        this.name = name==null?" ":name;
        this.phone = phone==null? " ":phone;
        this.email = email==null? " ":email;
    }
};
