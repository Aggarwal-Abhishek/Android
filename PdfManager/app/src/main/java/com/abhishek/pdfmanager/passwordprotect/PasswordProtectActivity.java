package com.abhishek.pdfmanager.passwordprotect;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.objectbox.Box;

public class PasswordProtectActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_protect);

        pdfiumCore = new PdfiumCore(this);
        PDFBoxResourceLoader.init(this);

        mybox = App.boxStore.boxFor(SettingDB.class);

    }

    PdfiumCore pdfiumCore;

    private int PDF_SELECT_CODE = 0;
    String pdfUri ;
    String password;
    boolean password_ok ;

    PDDocument document;
    AccessPermission permission;

    File parentDir;


    Box<SettingDB>mybox;

    public void Protect(View view) {

        if(!password_ok){
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            return;
        }

        parentDir = new File(Utility.PDF_Location(mybox));
        if(!parentDir.exists())parentDir.mkdirs();

        if(pdfUri == null || pdfUri.length()<1){
            Toast.makeText(this, "Select a PDF First...", Toast.LENGTH_SHORT).show();
            return;
        }

        if( !new File(pdfUri).exists() ){
            Toast.makeText(this, "File Not Exists..", Toast.LENGTH_SHORT).show();
        }else{

            new ProtectAsync(this).execute();

        }

    }


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



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Advance Parameters").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(item.getTitle().toString().equalsIgnoreCase("Advance Parameters")){
            try {

                if(document == null){
                    Toast.makeText(this, "Select a PDF First..", Toast.LENGTH_SHORT).show();
                    throw new Exception("PDDocument is null");
                }


                final Dialog builder = new Dialog(this);
                builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
                builder.setCanceledOnTouchOutside(true);

                View view = getLayoutInflater().inflate(R.layout.password_protect_advance_parameters, null);




                final CheckBox c0 = view.findViewById(R.id.password_protect_c0);
                final CheckBox c1 = view.findViewById(R.id.password_protect_c1);
                final CheckBox c2 = view.findViewById(R.id.password_protect_c2);
                final CheckBox c3 = view.findViewById(R.id.password_protect_c3);
                final CheckBox c4 = view.findViewById(R.id.password_protect_c4);
                final CheckBox c5 = view.findViewById(R.id.password_protect_c5);
                final CheckBox c6 = view.findViewById(R.id.password_protect_c6);
                final CheckBox c7 = view.findViewById(R.id.password_protect_c7);

                Button btnok = view.findViewById(R.id.password_protect_b0);
                Button btncancel = view.findViewById(R.id.password_protect_b1);

                c0.setChecked(permission.canAssembleDocument());
                c1.setChecked(permission.canExtractContent());
                c2.setChecked(permission.canExtractForAccessibility());
                c3.setChecked(permission.canFillInForm());
                c4.setChecked(permission.canModify());
                c5.setChecked(permission.canModifyAnnotations());
                c6.setChecked(permission.canPrint());
                c7.setChecked(permission.canPrintDegraded());

                btncancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        builder.cancel();
                    }
                });

                btnok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        permission.setCanAssembleDocument(c0.isChecked());
                        permission.setCanExtractContent(c1.isChecked());
                        permission.setCanExtractForAccessibility(c2.isChecked());
                        permission.setCanFillInForm(c3.isChecked());
                        permission.setCanModify(c4.isChecked());
                        permission.setCanModifyAnnotations(c5.isChecked());
                        permission.setCanPrint(c6.isChecked());
                        permission.setCanPrintDegraded(c7.isChecked());

                        builder.cancel();
                    }
                });




                builder.setContentView(view);
                builder.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                builder.show();
            }
            catch (Exception e){e.printStackTrace();}

        }


        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if(resultCode != RESULT_OK)return;

        if(requestCode == PDF_SELECT_CODE){

            if(intentData == null)return;

            pdfUri = FIleChooser.getPath(getApplicationContext(), intentData.getData());
            ((TextView)findViewById(R.id.password_protect_pdf_name_txt)).setText(pdfUri);


            if(pdfUri == null || pdfUri.length()<1){
                Toast.makeText(this, "Cannot Add This PDF...", Toast.LENGTH_SHORT).show();
                return;
            }

            new AddPDFAsync(this).execute();
        }
    }



    class ProtectAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        public ProtectAsync(AppCompatActivity context) {
            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Loading PDF");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }


        @Override
        protected Void doInBackground(Void... voids) {

            try{

                while (load_pdf_is_running){
                    try{
                        Thread.sleep(500);
                    }catch (Exception e){e.printStackTrace();}
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMessage("Adding Password");
                    }
                });

                String op = ((EditText)findViewById(R.id.password_protect_owner_password_field)).getText().toString();
                String up = ((EditText)findViewById(R.id.password_protect_user_password_field)).getText().toString();


                if(op.length()<1 && up.length()<1){

                    toast("At least One Password is Required");
                    return null;
                }

                if(op.length() < 1)op = up;
                if(up.length() < 1)up = op;

                StandardProtectionPolicy spp = new StandardProtectionPolicy(op, up, permission);

                if(op.length() > 0)spp.setOwnerPassword(op);
                if(up.length() > 0)spp.setUserPassword(up);
                spp.setPermissions(permission);
                spp.setEncryptionKeyLength(128);

                document.protect(spp);

                String name = new File(pdfUri).getName();
                if(name.contains(".")){
                    try{
                        name = name.substring(0, name.lastIndexOf('.'));
                    }catch (Exception e){e.printStackTrace();}
                }

                final File f = new File(parentDir,  name + "_" +  System.currentTimeMillis() + ".pdf");

                document.save(f);
                document.close();


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pdfUri = "";
                        OperationPerformedDialog.showPDFConvertedDialog(context, f.getAbsolutePath());
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
                toast("Failed...");
            }


            return null;
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

            if(password_ok){
                new LoadPDF().execute();
            }
        }
    }


    boolean load_pdf_is_running;
    class LoadPDF extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            load_pdf_is_running = true;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {

                if(password_ok){

                    if(password==null || password.length()<1){
                        document = PDDocument.load(new File(pdfUri), true);
                    }else {
                        document = PDDocument.load(new File(pdfUri), password, true);
                    }

                    permission = new AccessPermission();

                }
            }catch (Exception e){
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PasswordProtectActivity.this, "Failed Adding PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            load_pdf_is_running = false;
        }
    }

}
