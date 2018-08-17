package com.abhishek.pdfmanager.removepassword;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.OperationPerformedDialog;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;

import io.objectbox.Box;

public class RemovePasswordActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_password);

        PDFBoxResourceLoader.init(this);
        pdfiumCore = new PdfiumCore(this);

        mybox = App.boxStore.boxFor(SettingDB.class);
    }

    PdfiumCore pdfiumCore;


    String pdfUri;
    private int PDF_SELECT_CODE = 0;

    Box<SettingDB> mybox;


    File parent;
    public void RemovePassword(View view) {

        parent = new File(Utility.PDF_Location(mybox));
        if(!parent.exists())parent.mkdirs();

        if(pdfUri == null || pdfUri.length()<1){
            Toast.makeText(this, "Select a PDF first", Toast.LENGTH_SHORT).show();
            return;
        }

        if( ! new File(pdfUri).exists()){
            Toast.makeText(this, "File Not Found", Toast.LENGTH_SHORT).show();
            return;
        }


        new RemoveProtectionAsync(this).execute();

    }




    public void AddPDF(View view) {

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if(resultCode != RESULT_OK)return;

        if(requestCode == PDF_SELECT_CODE){

            if(intentData == null)return;

            pdfUri = FIleChooser.getPath(getApplicationContext(), intentData.getData());
            ((TextView)findViewById(R.id.remove_password_pdf_name_txt)).setText(pdfUri);
        }
    }



    class RemoveProtectionAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        RemoveProtectionAsync(AppCompatActivity context){

            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Removing Protection");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{
                final String password = ((EditText)findViewById(R.id.remove_password_edit_text)).getText().toString();

                PDDocument document = null;

                try{
                    if(password.length() < 1){

                        PdfDocument pdfDocument = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r"));
                        pdfiumCore.closeDocument(pdfDocument);

                        document = PDDocument.load(new File(pdfUri), true);
                    }else{


                        PdfDocument pdfDocument = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r"), password);
                        pdfiumCore.closeDocument(pdfDocument);

                        document = PDDocument.load(new File(pdfUri), password, true);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    toast("Incorrect Password");
                    return null;
                }

                document.setAllSecurityToBeRemoved(true);

                String name = new File(pdfUri).getName();
                if(name.contains(".")){
                    try{
                        name = name.substring(0, name.lastIndexOf('.'));
                    }catch (Exception e){e.printStackTrace();}
                }

                final File f = new File(parent, name + "_" + System.currentTimeMillis()+".pdf");
                document.save(f);
                document.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OperationPerformedDialog.showPDFConvertedDialog(context, f.getAbsolutePath());
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
                toast("Failed");
            }


            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();
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
