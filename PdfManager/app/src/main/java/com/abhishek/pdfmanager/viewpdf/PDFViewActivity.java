package com.abhishek.pdfmanager.viewpdf;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.IOException;

import io.objectbox.Box;


public class PDFViewActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_pdfconverted);
        super.onCreate(savedInstanceState);


        pdfView = findViewById(R.id.pdfView);
        pdfView.setBackground(getResources().getDrawable(R.drawable.gradient));

        pdfiumCore = new PdfiumCore(this);


        if(uri.length()>1){
            setDefault();
            new AddPDFAsync(this).execute();
        }
    }

    PdfiumCore pdfiumCore;

    PDFView pdfView;
    PDFView.Configurator configurator;

    void setDefault(){

        configurator = pdfView.fromFile(new File(uri));
        configurator
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .spacing(20)
                .scrollHandle(new DefaultScrollHandle(this));

        if(password != null && password.length()>0){
            configurator.password(password);
        }

    }


    public static String uri = "", password;



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);
        if(resultCode != RESULT_OK)return;


        if(requestCode == PDF_SELECT_CODE){
            if(intentData == null)return;
            uri = FIleChooser.getPath(this, intentData.getData());
            password = "";

            if(uri == null || uri.length()<1){
                Toast.makeText(this, "Select a valid PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            setDefault();

            new AddPDFAsync(this).execute();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("New PDF").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Fit Width");
        menu.add("Fit Height");
        menu.add("Fit Width and Height");
        menu.add("Horizontal Scroll");
        menu.add("Vertical Scroll");
        menu.add("Continuous Scrolling");
        menu.add("Single Page Scrolling");

        return super.onCreateOptionsMenu(menu);
    }


    int PDF_SELECT_CODE = 0;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getTitle().toString().equalsIgnoreCase("New PDF")){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

            try{
                startActivityForResult(
                        Intent.createChooser(intent, "Choose a PDF Document"),
                        PDF_SELECT_CODE
                );
            }catch (Exception e){
                Toast.makeText(this, "Cannot Open this File...", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if(pdfView == null || (!pdfView.isEnabled()) ){
            Toast.makeText(this, "Add a PDF First...", Toast.LENGTH_SHORT).show();
            return true;
        }

        if(item.getTitle().toString().equalsIgnoreCase("Fit Width")){
            int page = pdfView.getCurrentPage();
            boolean vertical = pdfView.isSwipeVertical();

            pdfView.recycle();
            pdfView.removeAllViews();

            setDefault();
            configurator.pageFitPolicy(FitPolicy.WIDTH)
                    .swipeHorizontal(!vertical)
                    .defaultPage(page);


            new ViewPDFAsync(this).execute();


        }else if(item.getTitle().toString().equalsIgnoreCase("Fit Height")){
            int page = pdfView.getCurrentPage();
            boolean vertical = pdfView.isSwipeVertical();
            pdfView.recycle();
            pdfView.removeAllViews();

            setDefault();
            configurator.pageFitPolicy(FitPolicy.HEIGHT)
                    .swipeHorizontal(!vertical)
                    .defaultPage(page);

            new ViewPDFAsync(this).execute();


        }else if(item.getTitle().toString().equalsIgnoreCase("Fit Width and Height")){
            int page = pdfView.getCurrentPage();
            boolean vertical = pdfView.isSwipeVertical();
            pdfView.recycle();
            pdfView.removeAllViews();

            setDefault();
            configurator.pageFitPolicy(FitPolicy.BOTH)
                    .swipeHorizontal(!vertical)
                    .defaultPage(page);

            new ViewPDFAsync(this).execute();


        }else if(item.getTitle().toString().equalsIgnoreCase("Horizontal Scroll")){

            if(! pdfView.isSwipeVertical()){
                Toast.makeText(this, "Already Set to Horizontal Scroll", Toast.LENGTH_SHORT).show();
                return true;
            }

            int page = pdfView.getCurrentPage();
            pdfView.recycle();
            pdfView.removeAllViews();

            setDefault();
            configurator.swipeHorizontal(true)
                    .defaultPage(page);

            new ViewPDFAsync(this).execute();


        }else if(item.getTitle().toString().equalsIgnoreCase("Vertical Scroll")){

            if(pdfView.isSwipeVertical()){
                Toast.makeText(this, "Already Set to Vertical Scroll", Toast.LENGTH_SHORT).show();
                return true;
            }

            int page = pdfView.getCurrentPage();
            pdfView.recycle();
            pdfView.removeAllViews();

            setDefault();
            configurator.swipeHorizontal(false)
                    .defaultPage(page);

            new ViewPDFAsync(this).execute();


        }else if(item.getTitle().toString().equalsIgnoreCase("Continuous Scrolling")){
            pdfView.setPageFling(false);
            pdfView.setPageSnap(false);
        }else if(item.getTitle().toString().equalsIgnoreCase("Single Page Scrolling")){
            pdfView.setPageFling(true);
            pdfView.setPageSnap(true);
        }


        return super.onOptionsItemSelected(item);
    }






    class ViewPDFAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;


        ViewPDFAsync(AppCompatActivity context){
            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Loading");
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            configurator.onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.cancel();
                        }
                    });
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            configurator.load();

            super.onPostExecute(aVoid);
        }
    }



    class AddPDFAsync extends AsyncTask<Void, Void, Void> {

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        AddPDFAsync(AppCompatActivity context){
            this.context = context;

            password = "";

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Adding PDF");

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }


        @Override
        protected Void doInBackground(Void... voids) {

            ParcelFileDescriptor fd;
            try{
                fd = context.getContentResolver().openFileDescriptor(Uri.parse("file://"+uri), "r");
            }catch (Exception e){
                e.printStackTrace();
                toast("File Not Found...");
                return null;
            }

            try{
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                pdfiumCore.closeDocument(pdfDocument);

            }catch (Exception e){

                password_dialog_is_showing = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            EnterPassword(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+uri), "r"), uri);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                    }
                });

                while (password_dialog_is_showing){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }



                try {
                    PdfDocument pdfDocument = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+uri), "r"), password);
                    pdfiumCore.closeDocument(pdfDocument);

                } catch (IOException e1) {
                    e1.printStackTrace();
                    toast("Cannot Access the PDF...");
                }

            }


            return null;
        }

        boolean password_dialog_is_showing;

        void EnterPassword(final ParcelFileDescriptor fd, final String uri){

            final Dialog dialog = new Dialog(context);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);

            final View view = context.getLayoutInflater().inflate(R.layout.enter_password_dialog, null);

            ((TextView)view.findViewById(R.id.enter_password_dialog_txt)).setText(uri);
            view.findViewById(R.id.enter_password_dialog_ok_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String str = ((EditText)view.findViewById(R.id.enter_password_dialog_edit_text)).getText().toString();
                    try{

                        PdfDocument pdfDocument = pdfiumCore.newDocument(fd, str);
                        pdfiumCore.closeDocument(pdfDocument);

                        dialog.cancel();

                        password = str;
                        password_dialog_is_showing = false;
                    }catch (Exception e){
                        ((TextView)view.findViewById(R.id.enter_password_dialog_result_txt)).setText("Cannot Open the File using this Password");
                    }
                }
            });
            view.findViewById(R.id.enter_password_dialog_cancel_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                    password_dialog_is_showing = false;
                }
            });

            dialog.setContentView(view);
            dialog.show();
        }

        void toast(final String msg){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            setDefault();
            new ViewPDFAsync(context).execute();
            mProgressDialog.cancel();

        }
    }

}





