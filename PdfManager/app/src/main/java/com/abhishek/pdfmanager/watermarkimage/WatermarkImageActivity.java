package com.abhishek.pdfmanager.watermarkimage;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
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
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;

import io.objectbox.Box;

public class WatermarkImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watermark_image);

        mybox = App.boxStore.boxFor(SettingDB.class);


        PDFBoxResourceLoader.init(this);

        pdfiumCore = new PdfiumCore(this);

        SeekBar seekBar = (SeekBar) findViewById(R.id.watremark_image_seekbar);
        seekBar.setProgress(50);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTitle(progress+"");
                alpha = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }


    Box<SettingDB> mybox;

    PdfiumCore pdfiumCore;

    final int PDF_SELECT_CODE = 0;
    final int IMG_SELECT_CODE = 1;

    String pdfUri, imgUri, password;
    int alpha = 50;



    File parentDir;





    public void Watermark(View view) {
        if(pdfUri.length()<1){
            Toast.makeText(this, "Select PDF First", Toast.LENGTH_SHORT).show();
            return;
        }
        if(imgUri.length()<1){
            Toast.makeText(this, "Select Image First", Toast.LENGTH_SHORT).show();
            return;
        }

        parentDir = new File(Utility.PDF_Location(mybox));
        if(!parentDir.exists())parentDir.mkdirs();

        new WatermarkAsync(this).execute();
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
            ((TextView)findViewById(R.id.watremark_image_txt_image)).setText(uri);

            imgUri = uri;
        }
        if(requestCode == PDF_SELECT_CODE){
            if(intentData == null)return;
            String uri = FIleChooser.getPath(this, intentData.getData());
            ((TextView)findViewById(R.id.watremark_image_pdf_name_txt)).setText(uri);

            pdfUri = uri;
            new AddPDFAsync(this).execute();
        }
    }





    class WatermarkAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        WatermarkAsync(AppCompatActivity context){
            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Adding Watermark");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{

                PDDocument pdDocument;
                if(password == null || password.length()<1){
                    pdDocument = PDDocument.load(new File(pdfUri), true);
                }else {
                    pdDocument = PDDocument.load(new File(pdfUri), password, true);
                }
                pdDocument.setAllSecurityToBeRemoved(true);

                Bitmap bitmap = generateWatermarkPDF();

                PDImageXObject imageXObject = LosslessFactory.createFromImage(pdDocument, bitmap);


                for(PDPage page : pdDocument.getPages()){
                    try{
                        PDPageContentStream cs = new PDPageContentStream(pdDocument, page, true, true, true);
                        cs.drawImage(imageXObject, page.getCropBox().getLowerLeftX(), page.getCropBox().getLowerLeftY(), page.getCropBox().getWidth(), page.getCropBox().getHeight());
                        cs.close();
                    }catch (Exception e){e.printStackTrace();}
                }

                String name = new File(pdfUri).getName();
                if(name.contains(".")){
                    name = name.substring(0, name.lastIndexOf("."));
                }

                final File f = new File(parentDir, name + "_" + System.currentTimeMillis()+".pdf");
                pdDocument.save(f);
                pdDocument.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OperationPerformedDialog.showPDFConvertedDialog(context, f.getAbsolutePath());
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
            }



            return null;
        }



        Bitmap generateWatermarkPDF()throws Exception{

            Bitmap bitmap = BitmapFactory.decodeFile(imgUri);
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            Paint alphaPaint = new Paint();
            alphaPaint.setAlpha((int)(2.55*(float) alpha));
            canvas.drawBitmap(bitmap, 0, 0, alphaPaint);

            while (width*height > 1000000){
                width /= 2;
                height /= 2;
            }

            return Bitmap.createScaledBitmap(newBitmap, width, height, true);

        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();
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
                fd = context.getContentResolver().openFileDescriptor(Uri.parse("file://"+pdfUri), "r");
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
