package com.abhishek.pdfmanager.backgroundimage;

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
import com.abhishek.pdfmanager.OperationPerformedDialog;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.multipdf.Overlay;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import io.objectbox.Box;

public class AddBackgroundImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_background_image);

        PDFBoxResourceLoader.init(this);
        pdfiumCore = new PdfiumCore(this);

        mybox = App.boxStore.boxFor(SettingDB.class);
    }

    final int PDF_SELECT_CODE = 0;
    final int IMG_SELECT_CODE = 1;

    String pdfUri, imgUri;

    PdfiumCore pdfiumCore;

    Box<SettingDB> mybox;



    public void SetBackground(View view) {
        if(pdfUri==null || pdfUri.length()<1){
            Toast.makeText(this, "Select PDF First", Toast.LENGTH_SHORT).show();
            return;
        }
        if(imgUri==null || imgUri.length()<1){
            Toast.makeText(this, "Select Image First", Toast.LENGTH_SHORT).show();
            return;
        }

        File parentDir = new File(Utility.PDF_Location(mybox));
        File tempDir = new File(Utility.TMP_Location(mybox));

        if(!parentDir.exists())parentDir.mkdirs();
        if(!tempDir.exists())tempDir.mkdirs();

        new SetBackgroundAsync(this, pdfUri, imgUri, parentDir, tempDir).execute();
    }

    public void AddImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        try{
            startActivityForResult(
                    Intent.createChooser(intent, "Choose an Image"),
                    IMG_SELECT_CODE
            );
        }catch (Exception e){
            Toast.makeText(this, "Cannot Open this File...", Toast.LENGTH_SHORT).show();
        }
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

        if(requestCode == IMG_SELECT_CODE){
            if(intentData == null)return;
            String uri = FIleChooser.getPath(this, intentData.getData());
            ((TextView)findViewById(R.id.add_background_image_txt_image)).setText(uri);

            imgUri = uri;
        }
        if(requestCode == PDF_SELECT_CODE){
            if(intentData == null)return;
            String uri = FIleChooser.getPath(this, intentData.getData());
            ((TextView)findViewById(R.id.add_background_image_pdf_name_txt)).setText(uri);

            pdfUri = uri;
        }
    }



    class SetBackgroundAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProcessDialog;
        String pdfUri, imgUri, password;
        File parentDir, tempDir;

        public SetBackgroundAsync(AppCompatActivity context, String pdfUri, String imgUri, File parentDir, File tempDir) {
            this.context = context;
            this.pdfUri = pdfUri;
            this.imgUri = imgUri;
            this.password = "";
            this.parentDir = parentDir;
            this.tempDir = tempDir;

            mProcessDialog = new ProgressDialog(context);
            mProcessDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.setMessage("Saving PDF");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProcessDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{
                ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r");
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                pdfiumCore.closeDocument(pdfDocument);

                addWatermark();
            }catch (Exception e){
                e.printStackTrace();


                password_dialog_is_showing = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(mProcessDialog.isShowing())mProcessDialog.hide();
                        try{
                            showPasswordDialog(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r"), pdfUri);
                        }catch (Exception e1){
                            e1.printStackTrace();
                            password_dialog_is_showing = false;
                        }

                    }
                });

                while (password_dialog_is_showing){
                    try{
                        Thread.sleep(500);
                    }catch (Exception e1){
                        e1.printStackTrace();
                    }
                }

                try{
                    addWatermark();
                }catch (Exception e1){
                    e1.printStackTrace();
                    toast("Failed...");
                }

            }
            return null;
        }



        boolean password_dialog_is_showing;

        void showPasswordDialog(final ParcelFileDescriptor fd, final String uri){

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

                        password = str;
                        dialog.cancel();

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



        void addWatermark(){

            try{
                PDDocument pdDocument;
                try {
                    pdDocument = PDDocument.load(new File(pdfUri), password, true);
                } catch (IOException e) {
                    toast("Cannot open the File...");
                    e.printStackTrace();
                    return ;
                }

                String watermark_pdf = getWatermarkPDF();
                if(watermark_pdf==null || watermark_pdf.length()<1){
                    toast("Failed");
                    return ;
                }

                String name = new File(pdfUri).getName();
                if(name.contains(".")){
                    try{
                        name = name.substring(0, name.lastIndexOf('.'));
                    }catch (Exception e){e.printStackTrace();}
                }

                final Overlay overlay = new Overlay();
                overlay.setInputPDF(pdDocument);
                overlay.setOutputFile(new File(parentDir, name + "_" + System.currentTimeMillis()+".pdf").getAbsolutePath());
                overlay.setDefaultOverlayFile(watermark_pdf);
                overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
                overlay.overlay(new HashMap<Integer, String>());
                pdDocument.close();

                toast("Done...");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OperationPerformedDialog.showPDFConvertedDialog(context, overlay.getOutputFile());
                    }
                });
            }catch (Exception e){
                toast("Operation Failed");
                e.printStackTrace();
            }
        }

        String getWatermarkPDF(){
            try{
                PDDocument document = new PDDocument();
                PDPage page = new PDPage();
                document.addPage(page);

                PDImageXObject imageXObject = PDImageXObject.createFromFile(imgUri, document);
                page.setMediaBox(new PDRectangle(imageXObject.getWidth(), imageXObject.getHeight()));

                PDPageContentStream cs = new PDPageContentStream(document, page);
                cs.drawImage(imageXObject, 0, 0);
                cs.close();

                File f = new File(tempDir, System.currentTimeMillis()+".pdf");
                document.save(f);
                document.close();

                return f.getAbsolutePath();
            }catch (Exception e){e.printStackTrace();}
            return "";
        }



        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProcessDialog.cancel();
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
