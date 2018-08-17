package com.abhishek.pdfmanager.extractimages;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import io.objectbox.Box;

public class ExtractImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract_image);

        PDFBoxResourceLoader.init(this);
        pdfiumCore = new PdfiumCore(this);

        ini();
    }



    class Data{
        int documentNo, pageNo;
        Uri bitmapUri;
        boolean extracted = false;

        Data(int documentNo, int pageNo, Uri bitmapUri){
            this.documentNo = documentNo;
            this.pageNo = pageNo;
            this.bitmapUri = bitmapUri;
        }
    }

    PdfiumCore pdfiumCore;

    ArrayList<Data>data;
    ArrayList<Pair<String, String>>documentUri;

    DragSortListView listView;
    ArrayAdapter<Data>adapter;


    File tempDir;

    Box<SettingDB> mybox;

    void ini(){
        mybox = App.boxStore.boxFor(SettingDB.class);

        data = new ArrayList<>();
        documentUri = new ArrayList<>();
        listView = findViewById(R.id.extract_images_list_view);

        parentDir = new File(Utility.IMG_Location(mybox));
        tempDir = new File(Utility.TMP_Location(mybox));


        if(tempDir.exists()){
            for(File f : tempDir.listFiles()){
                if(f.isFile())f.delete();
            }
        }else{
            tempDir.mkdirs();
        }

        if(!parentDir.exists()){
            parentDir.mkdirs();
        }


        listView = findViewById(R.id.extract_images_list_view);
        iniAdapter();
        listView.setAdapter(adapter);

        DragSortController controller = new DragSortController(listView);
        controller.setSortEnabled(false);

        listView.setFloatViewManager(controller);
        listView.setDragEnabled(false);
    }


    void iniAdapter(){
        adapter = new ArrayAdapter<Data>(this, R.layout.extract_image_list_item, R.id.page_to_image_page_no, data){

            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                final Data info = data.get(position);

                ((ImageView)view.findViewById(R.id.page_to_image_imageview)).setImageURI(info.bitmapUri);
                view.findViewById(R.id.page_to_image_imageview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {

                            Dialog builder = new Dialog(ExtractImageActivity.this);
                            builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            builder.setCanceledOnTouchOutside(true);


                            View view = getLayoutInflater().inflate(R.layout.edit_pdf_list_image_dialog, null);
                            ((ImageView) view.findViewById(R.id.edit_pdf_list_item_image_dialog_image)).setImageURI(info.bitmapUri);
                            builder.setContentView(view);
                            builder.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);


                            builder.show();
                        }
                        catch (Exception e){e.printStackTrace();}
                    }
                });

                ((TextView)view.findViewById(R.id.page_to_image_page_no)).setText((info.documentNo+1)+"."+(info.pageNo+1));

                (view.findViewById(R.id.page_to_image_close_btn)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.remove(info);
                        adapter.notifyDataSetChanged();
                    }
                });

                view.findViewById(R.id.page_to_image_save_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ExtractImage(position);
                    }
                });

                view.findViewById(R.id.page_to_image_save_txtview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ExtractImage(position);
                    }
                });

                view.findViewById(R.id.page_to_image_close_txtview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.remove(info);
                        adapter.notifyDataSetChanged();
                    }
                });

                return view;
            }

        };

    }


    void addPDF(final ArrayList<String>list){

        File tempDir = new File(Utility.TMP_Location(mybox));
        if(!tempDir.exists())tempDir.mkdirs();


        LoadAllThumbs(this, list, tempDir);
    }





    void ExtractImages(){

        if(!parentDir.exists())parentDir.mkdirs();

        new ExtractAllImages(this).execute();

    }

    void ExtractImage(int position){
        final Data info = data.get(position);

        if(info.extracted){
            Toast.makeText(this, "Already Extracted", Toast.LENGTH_SHORT).show();
            return;
        }

        new ExtractImageAsync(this, info).execute();


    }

    File parentDir;
    int ExtractImages(PDResources resources){

        int ret = 0;

        try{

            for(COSName name : resources.getXObjectNames()){
                PDXObject xObject = resources.getXObject(name);

                if(xObject instanceof PDFormXObject){

                    ret += ExtractImages(((PDFormXObject)xObject).getResources());

                }else if(xObject instanceof PDImageXObject){
                    Bitmap bitmap = ((PDImageXObject)xObject).getImage();
                    FileOutputStream fos = new FileOutputStream(new File(parentDir, System.currentTimeMillis()+".jpeg"));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    ret += 1;
                }

            }

            return ret;

        }catch (Exception e){e.printStackTrace();return ret;}
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Add PDF").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Extract Images from All Pages");
        return super.onCreateOptionsMenu(menu);
    }


    final int PDF_SELECT_CODE = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(item.getTitle().toString().equalsIgnoreCase("Add PDF")){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            try{
                startActivityForResult(
                        Intent.createChooser(intent, "Choose a PDF Document"),
                        PDF_SELECT_CODE
                );
            }catch (Exception e){
                Toast.makeText(this, "Cannot Open this File...", Toast.LENGTH_SHORT).show();
            }
        }
        else if(item.getTitle().toString().equalsIgnoreCase("Extract Images from All Pages")){
            ExtractImages();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if(resultCode != RESULT_OK)return;
        if(requestCode == PDF_SELECT_CODE){

            if(intentData == null)return;
            ArrayList<String>list = new ArrayList<>();

            try{

                for (int i=0; i<intentData.getClipData().getItemCount(); i++){
                    String uri = FIleChooser.getPath(getApplicationContext(), intentData.getClipData().getItemAt(i).getUri());
                    list.add(uri);
                }

            }catch (Exception e){
                String uri = FIleChooser.getPath(getApplicationContext(), intentData.getData());
                list.add(uri);
            }

            try{
                addPDF(list);
            }
            catch (Exception e){
                Toast.makeText(getApplicationContext(), "Error Adding Pdf to List", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

    }




    class ExtractAllImages extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        public ExtractAllImages(AppCompatActivity context) {
            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Saved : 0 Images");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{
                int num = 0, x;

                PDDocument[] pdDocument = new PDDocument[documentUri.size()];

                for(Data info : data){
                    try{

                        if(pdDocument[info.documentNo] == null){
                            pdDocument[info.documentNo] = PDDocument.load(new File(documentUri.get(info.documentNo).first), documentUri.get(info.documentNo).second, true);
                            pdDocument[info.documentNo].setAllSecurityToBeRemoved(true);
                        }

                        PDPage pdPage = pdDocument[info.documentNo].getPage(info.pageNo);
                        x = ExtractImages(pdPage.getResources());
                        info.extracted = true;

                        if(x > 0){
                            num += x;

                            final int p = num;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.setMessage("Extracted : " + p + " Images");
                                }
                            });
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }

                final int p = num;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new MaterialDialog.Builder(context)
                                .title("Extracted : " + p + " Images")
                                .content("Image Location :\n" + parentDir.getAbsolutePath())
                                .positiveText("OK")
                                .show();
                    }
                });

                for(PDDocument pdDocument1 : pdDocument){
                    pdDocument1.close();
                }

            }catch (Exception e){e.printStackTrace();}




            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();
        }
    }

    class ExtractImageAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        Data info;

        ProgressDialog mProgressDialog;

        ExtractImageAsync(AppCompatActivity context, Data info){
            this.context = context;
            this.info = info;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Saving");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{

                PDDocument document = PDDocument.load(new File(documentUri.get(info.documentNo).first), documentUri.get(info.documentNo).second, true);
                document.setAllSecurityToBeRemoved(true);

                final int num = ExtractImages(document.getPage(info.pageNo).getResources());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Extracted : "+num+" Images", Toast.LENGTH_SHORT).show();
                    }
                });

                document.close();

                info.extracted = true;
            }catch (Exception e){e.printStackTrace();}

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();
        }
    }


    void LoadAllThumbs(final AppCompatActivity context, final ArrayList<String> list, final File tempDir){

        new Thread(new Runnable() {
            @Override
            public void run() {

                for(final String uri : list){

                    if(uri == null || uri.length()<1)continue;

                    load_thumbs_is_running = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new LoadThumbs(context, uri, tempDir).execute();
                        }
                    });

                    while (load_thumbs_is_running){
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }


            }
        }).start();

    }

    boolean load_thumbs_is_running;
    class LoadThumbs extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        Pair<String, String>pair;

        ProgressDialog mProgressDialog;
        boolean progress_dialog_is_showing;

        File tempDir;

        LoadThumbs(AppCompatActivity context, String uri, File tempDir){
            this.context = context;
            this.pair = new Pair<>(uri, "");
            this.tempDir = tempDir;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage("Generating Previews");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgress(0);

            progress_dialog_is_showing = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pair.first), "r");
                fd.close();
            } catch (Exception e) {
                e.printStackTrace();
                toast("File not Found : "+pair.first);
                return null;
            }

            try{

                final PdfDocument pdfDocument = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pair.first), "r"));
                pdfiumCore.closeDocument(pdfDocument);

                generate(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pair.first), "r"), pair.first, pair.second);
            }catch (Exception e){

                progress_dialog_is_showing = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mProgressDialog.isShowing())mProgressDialog.hide();


                        try{
                            EnterPassword(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pair.first), "r"), pair.first);
                        }catch (Exception e1){
                            e1.printStackTrace();
                            progress_dialog_is_showing = false;
                        }
                    }
                });

                while (progress_dialog_is_showing){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                try {
                    generate(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pair.first), "r"), pair.first, pair.second);
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                    toast("Cannot Access File : " + pair.first);
                }
            }


            return null;
        }



        void generate(ParcelFileDescriptor fd, String uri, String pwd){
            try{
                final PdfDocument pdfDocument;

                if(pwd.length()<1){
                    pdfDocument = pdfiumCore.newDocument(fd);
                }else{
                    pdfDocument = pdfiumCore.newDocument(fd, pwd);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMax(pdfiumCore.getPageCount(pdfDocument));
                        mProgressDialog.show();
                    }
                });

                int dn = documentUri.size();
                int n = pdfiumCore.getPageCount(pdfDocument), width, height;

                documentUri.add(new Pair<>(uri, pwd));

                int thumb_quality = Utility.Thumb_Quality(mybox);

                if(thumb_quality == 0){

                    for(int i=0; i<n; i++){
                        try{
                            pdfiumCore.openPage(pdfDocument, i);

                            width = pdfiumCore.getPageWidth(pdfDocument, i);
                            height = pdfiumCore.getPageHeight(pdfDocument, i);

                            while (width*height > 1000000){
                                width = (width<<1)/3;
                                height = (height<<1)/3;
                            }

                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, i, 0, 0, width, height);
                            pdfiumCore.closePage(pdfDocument, i);

                            File f = new File(tempDir, System.currentTimeMillis()+".jpeg");
                            FileOutputStream fos = new FileOutputStream(f);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();

                            data.add(new Data(dn, i, Uri.parse(f.getAbsolutePath())));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.setProgress(mProgressDialog.getProgress()+1);
                                }
                            });

                        }catch (Exception e){e.printStackTrace();}
                    }

                }else if(thumb_quality == 1){

                    for(int i=0; i<n; i++){
                        try{
                            pdfiumCore.openPage(pdfDocument, i);

                            width = pdfiumCore.getPageWidth(pdfDocument, i);
                            height = pdfiumCore.getPageHeight(pdfDocument, i);

                            while (width*height > 500000){
                                width = width/2;
                                height = height/2;
                            }

                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, i, 0, 0, width, height);
                            pdfiumCore.closePage(pdfDocument, i);

                            File f = new File(tempDir, System.currentTimeMillis()+".jpeg");
                            FileOutputStream fos = new FileOutputStream(f);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();

                            data.add(new Data(dn, i, Uri.parse(f.getAbsolutePath())));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.setProgress(mProgressDialog.getProgress()+1);
                                }
                            });

                        }catch (Exception e){e.printStackTrace();}
                    }

                }else {

                    for(int i=0; i<n; i++){
                        try{
                            data.add(new Data(dn, i, null));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.setProgress(mProgressDialog.getProgress()+1);
                                }
                            });

                        }catch (Exception e){e.printStackTrace();}
                    }

                }


                pdfiumCore.closeDocument(pdfDocument);

            }catch (Exception e){
                e.printStackTrace();
                toast("File Cannot be added ... Skipping");
            }

        }

        void EnterPassword(final ParcelFileDescriptor fd, final String uri){

            final Dialog dialog = new Dialog(context);
            dialog.setCanceledOnTouchOutside(false);

            final View view = context.getLayoutInflater().inflate(R.layout.enter_password_dialog, null);

            ((TextView)view.findViewById(R.id.enter_password_dialog_txt)).setText(uri);
            view.findViewById(R.id.enter_password_dialog_ok_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String str = ((EditText)view.findViewById(R.id.enter_password_dialog_edit_text)).getText().toString();
                    try{

                        PdfDocument pdfDocument = pdfiumCore.newDocument(fd, str);
                        pdfiumCore.closeDocument(pdfDocument);

                        pair = new Pair<>(uri, str);
                        dialog.cancel();

                        progress_dialog_is_showing = false;
                    }catch (Exception e){
                        ((TextView)view.findViewById(R.id.enter_password_dialog_result_txt)).setText("Cannot Open the File using this Password");
                    }
                }
            });
            view.findViewById(R.id.enter_password_dialog_cancel_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                    progress_dialog_is_showing = false;
                }
            });

            dialog.setContentView(view);
            dialog.show();
        }



        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mProgressDialog.cancel();
            load_thumbs_is_running = false;
            adapter.notifyDataSetChanged();
        }


        void toast(final String msg){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
