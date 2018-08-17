package com.abhishek.pdfmanager.textwatermark;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.OperationPerformedDialog;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.github.barteksc.pdfviewer.util.Util;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;

import io.objectbox.Box;


public class TextWatermarkActivity extends AppCompatActivity {

    void addDefault(int i, String s){
        ((TextView)findViewById(i)).setText(s);
    }

    public void iniFonts(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                pdFonts = new PDFont[]{
                        PDType1Font.COURIER, PDType1Font.COURIER_BOLD, PDType1Font.COURIER_BOLD_OBLIQUE, PDType1Font.COURIER_OBLIQUE, PDType1Font.HELVETICA,
                        PDType1Font.HELVETICA_BOLD, PDType1Font.HELVETICA_BOLD_OBLIQUE, PDType1Font.HELVETICA_OBLIQUE, PDType1Font.SYMBOL,
                        PDType1Font.TIMES_BOLD, PDType1Font.TIMES_BOLD_ITALIC, PDType1Font.TIMES_ITALIC, PDType1Font.TIMES_ROMAN
                };
//                Log.d("Abhi", "Font Loaded : "+pdFonts.length);
                fontsLoaded = true;
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_watermark);

        pdfiumCore = new PdfiumCore(this);
        PDFBoxResourceLoader.init(this);
        iniFonts();

        addDefault(R.id.text_watermark_txt_rotation, rotation+"");
        addDefault(R.id.text_watermark_txt_font_size, fontsize+"");
        addDefault(R.id.text_watermark_txt_margin, margin+"");
        addDefault(R.id.text_watermark_txt_opacity, opacity+"");
        addDefault(R.id.text_watermark_txt_alignment, alignment);
        ((EditText)findViewById(R.id.text_watermark_edit_text)).setText(text);


        if(fontsLoaded)addDefault(R.id.text_watermark_txt_font, fonts[font]);
        else {

            addDefault(R.id.text_watermark_txt_font, "Loading Fonts...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){

                        if(fontsLoaded){

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addDefault(R.id.text_watermark_txt_font, fonts[font]);
                                }
                            });

                            return;
                        }else {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                }
            }).start();

        }

        mybox = App.boxStore.boxFor(SettingDB.class);


    }


    Box<SettingDB> mybox;


    PdfiumCore pdfiumCore;

    String pdfUri, password;
    final int PDF_SELECT_CODE = 0;
    static boolean fontsLoaded = false;

    String[] fonts = { "COURIER", "COURIER BOLD", "COURIER BOLD OBLIQUE", "COURIER OBLIQUE",
            "HELVETICA", "HELVETICA BOLD", "HELVETICA BOLD OBLIQUE", "HELVETICA OBLIQUE", "SYMBOL",
            "TIMES BOLD", "TIMES BOLD ITALIC", "TIMES ITALIC", "TIMES ROMAN"
    };

    static PDFont[] pdFonts = null;



    int rotation = 45, font = 5, fontsize = 25, fontcolor = Color.BLACK, margin=0;
    float opacity = (float) 0.5;
    String alignment = "CENTER";
    String text = "My Watermark";



    File parentDir;
    public void Watermark(View v){

        parentDir = new File(Utility.PDF_Location(mybox));
        if(!parentDir.exists())parentDir.mkdirs();

        new WatermarkAsync(this).execute();

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
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if(resultCode != RESULT_OK)return;


        if(requestCode == PDF_SELECT_CODE){
            if(intentData == null)return;
            String uri = FIleChooser.getPath(this, intentData.getData());
            ((TextView)findViewById(R.id.text_watermark_pdf_name_txt)).setText(uri);

            pdfUri = uri;

            new AddPDFAsync(this).execute();
        }


    }





    public void SelectRotation(View view) {

        PopupMenu popupMenu = new PopupMenu(this, view);


        for(int i=0; i<=360; i+=5){
            popupMenu.getMenu().add(i+"");
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                try{
                    int r = Integer.parseInt(item.getTitle().toString());
                    rotation = r;
                    ((TextView)findViewById(R.id.text_watermark_txt_rotation)).setText(r + "°");

                }
                catch (Exception e){e.printStackTrace();}

                return true;
            }
        });

        popupMenu.show();
    }

    public void SelectFont(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);

        for(String s : fonts){
            popupMenu.getMenu().add(s);
        }


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                String s = item.getTitle().toString();
                for(int i=0; i<fonts.length; i++){
                    if(fonts[i].equalsIgnoreCase(s)){
                        font = i;
                        break;
                    }
                }


                ((TextView)findViewById(R.id.text_watermark_txt_font)).setText(s);
                return true;
            }
        });

        popupMenu.show();
    }

    public void SelectAlignment(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);

        popupMenu.getMenu().add("CENTER");
        popupMenu.getMenu().add("TOP");
        popupMenu.getMenu().add("BOTTOM");
        popupMenu.getMenu().add("LEFT");
        popupMenu.getMenu().add("RIGHT");



        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                alignment = item.getTitle().toString();

                ((TextView)findViewById(R.id.text_watermark_txt_alignment)).setText(alignment);

                return true;
            }
        });

        popupMenu.show();
    }

    public void SelectFontSize(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);


        for(int i=1; i<=100; i++){
            popupMenu.getMenu().add(i+"");
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                try{
                    int r = Integer.parseInt(item.getTitle().toString());
                    fontsize = r;

                    ((TextView)findViewById(R.id.text_watermark_txt_font_size)).setText(r + "");

                }
                catch (Exception e){}

                return true;
            }
        });

        popupMenu.show();
    }

    public void SelectOpacity(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);


        for(float i=0; i<=100; i+= 5){
            popupMenu.getMenu().add((i/100)+"");
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                try{
                    float o = Float.parseFloat(item.getTitle().toString());
                    opacity = o;

                    ((TextView)findViewById(R.id.text_watermark_txt_opacity)).setText(opacity + "");

                }
                catch (Exception e){}

                return true;
            }
        });

        popupMenu.show();
    }

    public void SelectFontColor(View view) {

        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(fontcolor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        findViewById(R.id.text_watermark_txt_fontcolor).setBackgroundColor(selectedColor);
                        fontcolor = selectedColor;
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .build()
                .show();
    }

    public void SelectMargin(View view) {

        PopupMenu popupMenu = new PopupMenu(this, view);


        for(int i=500; i>=-500; i-=10){
            popupMenu.getMenu().add(i+"");
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                try{
                    int r = Integer.parseInt(item.getTitle().toString());
                    margin = r;
                    ((TextView)findViewById(R.id.text_watermark_txt_margin)).setText(r + "");

                }
                catch (Exception e){}

                return true;
            }
        });

        popupMenu.show();
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
                final String op = Watermark(parentDir);
                if(op.length()>1){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OperationPerformedDialog.showPDFConvertedDialog(context, op);
                        }
                    });
                }
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



        public String Watermark(File path) throws Exception{


            if(!fontsLoaded){
                toast("Please Wait for the Fonts to Load");
                return "";
            }


            text = ((EditText)findViewById(R.id.text_watermark_edit_text)).getText().toString();

            if(text.length()<1){
                toast("Please Enter a valid Watermark");
                return "";
            }




            PDDocument document;
            if(password == null || password.length()<1){
                document = PDDocument.load(new File(pdfUri), true);
            }else{
                document = PDDocument.load(new File(pdfUri), password, true);
            }

            document.setAllSecurityToBeRemoved(true);

            float titleWidth = ( pdFonts[font].getStringWidth(text) / 1000 )* (float) fontsize;
            float titleHeight = (pdFonts[font].getFontDescriptor().getFontBoundingBox().getHeight()/ 1000) * (float) fontsize;
            float x = 0, y = 0;

            int rv = (fontcolor >> 16) & 0xFF;
            int gv = (fontcolor >> 8) & 0xFF;
            int bv = (fontcolor >> 0) & 0xFF;

            double angle = Math.toRadians(rotation);

            if(alignment.equalsIgnoreCase("CENTER")){

                for(PDPage page : document.getPages()){

                    PDPageContentStream cs = new PDPageContentStream(document, page, true, true, true);


                    PDExtendedGraphicsState r = new PDExtendedGraphicsState();
                    r.setNonStrokingAlphaConstant(opacity);




                    x = page.getCropBox().getWidth()/2;
                    y = page.getCropBox().getHeight()/2;

                    x = (float) (x - (titleWidth/2)*Math.cos(angle));
                    y = (float) (y - (titleWidth/2)*Math.sin(angle));

                    cs.setGraphicsStateParameters(r);
                    cs.setNonStrokingColor(rv, gv, bv);
                    cs.beginText();
                    cs.setFont(pdFonts[font], fontsize);
                    cs.setTextRotation(angle, x + page.getCropBox().getLowerLeftX(), y + page.getCropBox().getLowerLeftY());
                    cs.showText(text);
                    cs.endText();
                    cs.close();
                }
            }
            else if(alignment.equalsIgnoreCase("TOP")){

                for(PDPage page : document.getPages()){

                    PDPageContentStream cs = new PDPageContentStream(document, page, true, true, true);


                    PDExtendedGraphicsState r = new PDExtendedGraphicsState();
                    r.setNonStrokingAlphaConstant(opacity);


                    x = (float) (page.getCropBox().getWidth()/2 - (titleWidth/2)*Math.cos(angle));
                    y = (float) (page.getCropBox().getHeight() - titleWidth*Math.sin(angle)) - titleHeight/2;


                    cs.setGraphicsStateParameters(r);
                    cs.setNonStrokingColor(rv, gv, bv);
                    cs.beginText();
                    cs.setFont(pdFonts[font], fontsize);
                    cs.setTextRotation(angle, x + page.getCropBox().getLowerLeftX(), y + page.getCropBox().getLowerLeftY() - margin);
                    cs.showText(text);
                    cs.endText();
                    cs.close();
                }
            }
            else if(alignment.equalsIgnoreCase("BOTTOM")){

                for(PDPage page : document.getPages()){

                    PDPageContentStream cs = new PDPageContentStream(document, page, true, true, true);

                    PDExtendedGraphicsState r = new PDExtendedGraphicsState();
                    r.setNonStrokingAlphaConstant(opacity);



                    x = (float) (page.getCropBox().getWidth()/2 - (titleWidth/2)*Math.cos(angle));
                    y = margin;

                    cs.setGraphicsStateParameters(r);
                    cs.setNonStrokingColor(rv, gv, bv);
                    cs.beginText();
                    cs.setFont(pdFonts[font], fontsize);
                    cs.setTextRotation(angle, x + page.getCropBox().getLowerLeftX(), y + page.getCropBox().getLowerLeftY());
                    cs.showText(text);
                    cs.endText();
                    cs.close();
                }
            }
            else if(alignment.equalsIgnoreCase("LEFT")){


                for(PDPage page : document.getPages()){

                    PDPageContentStream cs = new PDPageContentStream(document, page, true, true, true);


                    PDExtendedGraphicsState r = new PDExtendedGraphicsState();
                    r.setNonStrokingAlphaConstant(opacity);



                    x = margin + titleHeight/2;
                    y = (float) (page.getCropBox().getHeight()/2 - (titleWidth/2)*Math.sin(angle));

                    cs.setGraphicsStateParameters(r);
                    cs.setNonStrokingColor(rv, gv, bv);
                    cs.beginText();
                    cs.setFont(pdFonts[font], fontsize);
                    cs.setTextRotation(angle, x + page.getCropBox().getLowerLeftX(), y + page.getCropBox().getLowerLeftY());
                    cs.showText(text);
                    cs.endText();
                    cs.close();
                }
            }
            else if(alignment.equalsIgnoreCase("Right")){


                for(PDPage page : document.getPages()){

                    PDPageContentStream cs = new PDPageContentStream(document, page, true, true, true);


                    PDExtendedGraphicsState r = new PDExtendedGraphicsState();
                    r.setNonStrokingAlphaConstant(opacity);



                    x = (float) (page.getCropBox().getWidth()-titleWidth*Math.cos(angle));
                    y = (float) (page.getCropBox().getHeight()/2 - (titleWidth/2)*Math.sin(angle));

                    cs.setGraphicsStateParameters(r);
                    cs.setNonStrokingColor(rv, gv, bv);
                    cs.beginText();
                    cs.setFont(pdFonts[font], fontsize);
                    cs.setTextRotation(angle, x + page.getCropBox().getLowerLeftX() - margin, y + page.getCropBox().getLowerLeftY());
                    cs.showText(text);
                    cs.endText();
                    cs.close();
                }
            }

            String name = new File(pdfUri).getName();
            if(name.contains(".")){
                try{
                    name = name.substring(0, name.lastIndexOf('.'));
                }catch (Exception e){e.printStackTrace();}
            }


            File f = new File(path, name + "_" + System.currentTimeMillis()+".pdf");
            document.save(f);
            document.close();

            return f.getAbsolutePath();
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
