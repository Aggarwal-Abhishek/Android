package com.abhishek.pdfmanager.splitpdf;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.abhishek.pdfmanager.viewpdf.PDFViewActivity;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.objectbox.Box;

public class SplitPDFActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_pdf_normal);

        PDFBoxResourceLoader.init(this);
        pdfiumCore = new PdfiumCore(this);

        mybox = App.boxStore.boxFor(SettingDB.class);
    }

    PdfiumCore pdfiumCore;

    private int PDF_SELECT_CODE = 0;
    String pdfUri, password;
    boolean password_ok;

    File parentDir;

    Box<SettingDB>mybox;
    

    public void SelectPDF(View view) {
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
    }

    public void Split(View view) {
        String str = ((EditText)findViewById(R.id.split_pdf_ranges)).getText().toString();
        if(str.length()<1){
            Toast.makeText(this, "Enter ranges First...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        

        
        if(pdfUri == null || pdfUri.length()<1){
            Toast.makeText(this, "Select a File First", Toast.LENGTH_SHORT).show();
            return;
        }
        final File file = new File(pdfUri);
        if(!file.exists()){
            Toast.makeText(this, "File Not Found", Toast.LENGTH_SHORT).show();
            return;
        }

        parentDir = new File(Utility.PDF_Location(mybox));
        if(!parentDir.exists())parentDir.mkdirs();


        new SplitPDFAsync(this, str).execute();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if(resultCode != RESULT_OK)return;

        if(requestCode == PDF_SELECT_CODE){

            if(intentData == null)return;

            pdfUri = FIleChooser.getPath(getApplicationContext(), intentData.getData());
            ((TextView)findViewById(R.id.split_pdf_pdf_name_txt)).setText(pdfUri);

            new AddPDFAsync(this).execute();
        }
    }



    class SplitPDFAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;
        String str;

        MaterialSimpleListAdapter listDialogAdapter;

        SplitPDFAsync(AppCompatActivity context, String str){
            this.context = context;
            this.str = str;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Splitting");

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            listDialogAdapter = new MaterialSimpleListAdapter(new MaterialSimpleListAdapter.Callback() {
                @Override
                public void onMaterialListItemSelected(MaterialDialog dialog, int index, MaterialSimpleListItem item) {
                    PDFViewActivity.uri = item.getContent().toString();
                    PDFViewActivity.password = "";
                    startActivity(new Intent(context, PDFViewActivity.class));
                }
            });

            mProgressDialog.show();
        }


        @Override
        protected Void doInBackground(Void... voids) {

            final String[] ranges = str.split(",");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.setMax(ranges.length);
                }
            });


            try{
                PDDocument pdDocument;
                if(password.length()<1)pdDocument = PDDocument.load(new File(pdfUri));
                else if(password_ok) pdDocument = PDDocument.load(new File(pdfUri), password);
                else pdDocument = PDDocument.load(new File(pdfUri), password);

                int n = pdDocument.getNumberOfPages();
                for(String range : ranges){

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.setProgress(mProgressDialog.getProgress()+1);
                        }
                    });

                    if(range.contains("-")){
                        String[] pages = range.split("-");
                        if(pages.length != 2){
                            toast("Skipping : "+range);
                            continue;
                        }

                        try{
                            int a = Integer.parseInt(pages[0]);
                            int b = Integer.parseInt(pages[1]);

                            if(a <= b){

                                if(a<=0 || b>n)throw new Exception();

                                PDDocument ret = new PDDocument();
                                while (a<=b){
                                    ret.addPage(pdDocument.getPage(a-1));
                                    ++a;
                                }

                                File f = new File(parentDir, System.currentTimeMillis()+".pdf");
                                ret.save(f);
                                ret.close();

                                listDialogAdapter.add(new MaterialSimpleListItem.Builder(context)
                                    .content(f.getAbsolutePath())
                                    .build());
                            }
                            else if(b < a){// a = 10 , b = 5
                                if(a>n || b<=0)throw new Exception();


                                PDDocument ret = new PDDocument();
                                while (a>=b){
                                    ret.addPage(pdDocument.getPage(a-1));
                                    --a;
                                }
                                File f = new File(parentDir, System.currentTimeMillis()+".pdf");
                                ret.save(f);
                                ret.close();

                                listDialogAdapter.add(new MaterialSimpleListItem.Builder(context)
                                        .content(f.getAbsolutePath())
                                        .build());
                            }



                        }catch (Exception e){
                            e.printStackTrace();
                            toast("Skipping : "+range);
                            continue;
                        }



                    }else{

                        try {
                            int a = Integer.parseInt(range);

                            if(a<=0 || a>n)throw new Exception();

                            PDDocument ret = new PDDocument();
                            ret.addPage(pdDocument.getPage(a-1));

                            File f = new File(parentDir, System.currentTimeMillis()+".pdf");
                            ret.save(f);
                            ret.close();

                            listDialogAdapter.add(new MaterialSimpleListItem.Builder(context)
                                    .content(f.getAbsolutePath())
                                    .build());

                        }catch (Exception e){
                            e.printStackTrace();
                            toast("Skipping : "+range);
                            continue;
                        }

                    }

                }

                pdDocument.close();


            }catch (Exception e){
                e.printStackTrace();
                toast("Failed");
            }

            toast("Done...");



            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();

            new MaterialDialog.Builder(context)
                    .title("Splitted PDF : " + listDialogAdapter.getItemCount())
                    .adapter(listDialogAdapter, null)
                    .show();


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


    class AddPDFAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        AddPDFAsync(AppCompatActivity context){
            this.context = context;

            password = "";
            password_ok = false;

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
                fd = context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r");
            }catch (Exception e){
                e.printStackTrace();
                toast("File Not Found...");
                return null;
            }

            try{
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                pdfiumCore.closeDocument(pdfDocument);

                password_ok = true;

            }catch (Exception e){

                password_dialog_is_showing = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            EnterPassword(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r"), pdfUri);
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
                    PdfDocument pdfDocument = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r"), password);
                    pdfiumCore.closeDocument(pdfDocument);
                    password_ok = true;

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
            mProgressDialog.cancel();
        }
    }
}
