package com.abhishek.pdfmanager.pagetoimage;

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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.OperationPerformedDialog;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import io.objectbox.Box;

public class PageToImageActivity extends AppCompatActivity {

    class Data{
        int document_no, page_no;
        Uri bitmap_uri;

        public Data(int document_no, int page_no, Uri bitmap_uri) {
            this.document_no = document_no;
            this.page_no = page_no;
            this.bitmap_uri = bitmap_uri;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_to_image);

        pdfiumCore = new PdfiumCore(this);
        ini();
    }

    PdfiumCore pdfiumCore;

    ArrayList<Data>data;
    ArrayList<Pair<Uri, String>>documentUri;

    DragSortListView listView;
    ArrayAdapter<Data>adapter;

    File tempDir, parentDir;

    int quality = 1;


    Box<SettingDB>mybox;

    void ini(){
        data = new ArrayList<>();
        documentUri = new ArrayList<>();

        mybox = App.boxStore.boxFor(SettingDB.class);

        parentDir = new File(Utility.IMG_Location(mybox));
        tempDir = new File(Utility.TMP_Location(mybox));
        if(tempDir.exists()){
            for(File f : tempDir.listFiles()){
                if(f.isFile())f.delete();
            }
        }else{
            tempDir.mkdirs();
        }


        listView = findViewById(R.id.page_to_image_list_view);
        iniAdapter();
        listView.setAdapter(adapter);
        DragSortController controller = new DragSortController(listView);
        controller.setSortEnabled(false);
        listView.setFloatViewManager(controller);
        listView.setDragEnabled(false);
    }

    void iniAdapter(){

        adapter = new ArrayAdapter<Data>(this, R.layout.page_to_image_list_item, R.id.page_to_image_page_no, data){

            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                final Data info = data.get(position);

                ((ImageView)view.findViewById(R.id.page_to_image_imageview)).setImageURI(info.bitmap_uri);
                view.findViewById(R.id.page_to_image_imageview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayImageDialog(position);
                    }
                });

                ((TextView)view.findViewById(R.id.page_to_image_page_no)).setText((info.document_no+1)+"."+(info.page_no+1));

                view.findViewById(R.id.page_to_image_close_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });

                view.findViewById(R.id.page_to_image_save_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SaveImage(position);
                    }
                });
                view.findViewById(R.id.page_to_image_save_txtview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SaveImage(position);
                    }
                });

                view.findViewById(R.id.page_to_image_close_txtview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });



                return view;
            }
        };

    }


    void SaveImage(final int position){
        
        parentDir = new File(Utility.IMG_Location(mybox));
        if(!parentDir.exists())parentDir.mkdirs();

        new SaveImageAsync(this, position).execute();

    }


    void ExtractImages(){

        parentDir = new File(Utility.IMG_Location(mybox));
        if(!parentDir.exists())parentDir.mkdirs();

        new SaveAllImagesAsync(this).execute();
    }




    int prev = -1;
    PdfDocument prevDocument;

    void RenderImage(int position) throws Exception{
        Data info = data.get(position);


        if(info.document_no != prev){
            if(prevDocument != null)pdfiumCore.closeDocument(prevDocument);
            prevDocument = pdfiumCore.newDocument(getContentResolver().openFileDescriptor(documentUri.get(info.document_no).first, "r"), documentUri.get(info.document_no).second);
            prev = info.document_no;
        }
        pdfiumCore.openPage(prevDocument, info.page_no);

        int width = pdfiumCore.getPageWidth(prevDocument, info.page_no);
        int height = pdfiumCore.getPageHeight(prevDocument, info.page_no);

        if(quality == 0){
            while (width*height > 4000000){
                width = width/2;
                height = height/2;
            }
        }else if(quality == 1){
            while (width*height > 8000000){
                width = width/2;
                height = height/2;
            }
        }


        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        pdfiumCore.renderPageBitmap(prevDocument, bitmap, info.page_no, 0, 0, width, height);

        pdfiumCore.closePage(prevDocument, info.page_no);

        File f = new File(parentDir, System.currentTimeMillis()+".jpeg");
        FileOutputStream fos = new FileOutputStream(f);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();
    }



    void addPDF(final ArrayList<String>pdfUris){

        if(!tempDir.exists())tempDir.mkdirs();

        LoadAllThumbs(this, pdfUris, tempDir);

    }

    void displayImageDialog(int position){
        Data info = data.get(position);

        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setCanceledOnTouchOutside(true);

        View view = getLayoutInflater().inflate(R.layout.edit_pdf_list_image_dialog, null);
        ((ImageView)view.findViewById(R.id.edit_pdf_list_item_image_dialog_image)).setImageURI(info.bitmap_uri);
        builder.setContentView(view);
        builder.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        builder.show();
    }




    final int PDF_SELECT_CODE = 0;

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
        else if(item.getTitle().toString().equalsIgnoreCase("Save All Pages")){
            try{
                ExtractImages();
            }catch (Exception e){
                Toast.makeText(this, "Operation Failed", Toast.LENGTH_SHORT).show();
            }
        }
        else if(item.getTitle().toString().equalsIgnoreCase("Image Quality")){

            final Dialog builder = new Dialog(this);
            builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
            builder.setCanceledOnTouchOutside(false);

            View view = getLayoutInflater().inflate(R.layout.page_to_image_quality_dialog, null);

            final CheckBox b0 = view.findViewById(R.id.page_to_image_quality_checkbox_low);
            final CheckBox b1 = view.findViewById(R.id.page_to_image_quality_checkbox_good);
            final CheckBox b2 = view.findViewById(R.id.page_to_image_quality_checkbox_best);

            b0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        b1.setChecked(false);
                        b2.setChecked(false);
                    }
                }
            });

            b1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        b2.setChecked(false);
                        b0.setChecked(false);
                    }

                }
            });
            b2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        b1.setChecked(false);
                        b0.setChecked(false);
                    }
                }
            });

            b0.setChecked(false);
            b1.setChecked(false);
            b2.setChecked(false);

            if(quality == 0){
                b0.setChecked(true);
            }else if(quality == 1){
                b1.setChecked(true);
            }else if(quality == 2){
                b2.setChecked(true);
            }else {
                quality = 1;
                b1.setChecked(true);
            }

            view.findViewById(R.id.page_to_image_quality_checkbox_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(b0.isChecked())quality = 0;
                    else if(b1.isChecked())quality = 1;
                    else if(b2.isChecked())quality = 2;
                    else quality = 1;

                    builder.cancel();
                }
            });


            builder.setContentView(view);
            builder.show();
        }



        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add("Add PDF").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Save All Pages");
        menu.add("Image Quality");
        return super.onCreateOptionsMenu(menu);
    }






    class SaveAllImagesAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        SaveAllImagesAsync(AppCompatActivity context){
            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(data.size());
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage("Saving Images");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            for(int i=0; i<data.size(); i++){
                try {
                    RenderImage(i);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.setProgress(mProgressDialog.getProgress()+1);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();
        }
    }


    class SaveImageAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        int position;


        public SaveImageAsync(AppCompatActivity context, int position) {
            this.context = context;
            this.position = position;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage("Saving As Image");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }


        @Override
        protected Void doInBackground(Void... voids) {

            try{

                RenderImage(position);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Image Saved", Toast.LENGTH_SHORT).show();
                    }
                });

            }catch (Exception e){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }


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
                } catch (Exception e1) {
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

                documentUri.add(new Pair<>(Uri.parse("file://"+uri), pwd));

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


                }else{

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















