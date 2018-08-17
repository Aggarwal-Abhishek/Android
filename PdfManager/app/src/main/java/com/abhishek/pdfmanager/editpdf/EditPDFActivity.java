package com.abhishek.pdfmanager.editpdf;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
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
import com.abhishek.pdfmanager.OperationPerformedDialog;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tom_roush.pdfbox.io.IOUtils;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.common.PDStream;
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject;
import com.tom_roush.pdfbox.util.Matrix;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import io.objectbox.Box;

public class EditPDFActivity extends AppCompatActivity {


    public class Data{
        int document_no, page_no, rotation = 0;
        Uri bitmapUri;
        float a=-1, b, c, d;

        public Data(int document_no, int page_no, Uri bitmapUri) {
            this.document_no = document_no;
            this.page_no = page_no;
            this.bitmapUri = bitmapUri;
        }
    }

    int cropPosition = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pdf);

        ini();
        pdfiumCore = new PdfiumCore(this);
        PDFBoxResourceLoader.init(this);
    }

    PdfiumCore pdfiumCore;

    DragSortListView listView;
    ArrayAdapter adapter;
    ArrayList<Data> data;

    ArrayList<Pair<String, String>>document;

    Box<SettingDB> mybox;

    void ini(){
        data = new ArrayList<>();
        document = new ArrayList<>();

        mybox = App.boxStore.boxFor(SettingDB.class);

        File tempDir = new File(Utility.TMP_Location(mybox));
        if(tempDir.exists()){
            for(File f : tempDir.listFiles()){
                if(f.isFile())f.delete();
            }
        }


        listView = findViewById(R.id.edit_pdf_list_view);
        adapter = getAdapter();

        listView.setAdapter(adapter);
        listView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                if(from != to){
                    Data movedData = data.get(from);
                    data.remove(from);
                    data.add(to, movedData);

                    adapter.notifyDataSetChanged();
                }
            }
        });



        DragSortController controller = new DragSortController(listView);
        controller.setDragHandleId(R.id.edit_pdf_list_item_drag_img);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.FLING_REMOVE);

        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
        listView.setDragEnabled(true);
        listView.setMaxScrollSpeed(8f);
    }


    ArrayAdapter getAdapter(){

        adapter = new ArrayAdapter<Data>(this, R.layout.edit_pdf_list_item, R.id.edit_pdf_list_item_page_no, data){
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                final Data info = data.get(position);

                final ImageView imageView = v.findViewById(R.id.edit_pdf_list_item_image);

                imageView.setImageURI(info.bitmapUri);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayImageDialog(info.bitmapUri, info.rotation);
                    }
                });
                imageView.setRotation(info.rotation);

                ((TextView)v.findViewById(R.id.edit_pdf_list_item_page_no)).setText((info.document_no+1)+"."+(info.page_no+1));

                (v.findViewById(R.id.edit_pdf_list_item_btn_close)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.remove(info);
                        adapter.notifyDataSetChanged();
                    }
                });

                v.findViewById(R.id.edit_pdf_list_item_btn_rotate).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayRotateDialog(position);
                    }
                });
                v.findViewById(R.id.edit_pdf_list_item_btn_rotate_clockwise).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.get(position).rotation += 90;
                        adapter.notifyDataSetChanged();
                    }
                });
                v.findViewById(R.id.edit_pdf_list_item_btn_rotate_anticlockwise).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.get(position).rotation -= 90;
                        adapter.notifyDataSetChanged();
                    }
                });

                v.findViewById(R.id.edit_pdf_list_item_btn_save_page).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        savePage(info);
                    }
                });

                v.findViewById(R.id.edit_pdf_list_item_btn_crop).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        cropPosition = position;

                        int x = info.rotation;
                        if(x<0)x = x*-1;

                        if(x%90 != 0){
                            CropImage.activity(Uri.parse("file:///" + info.bitmapUri))
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setAllowRotation(false)
                                    .setAllowFlipping(false)
                                    .setNoOutputImage(true)
                                    .start(EditPDFActivity.this);

                        }else{
                            CropImage.activity(Uri.parse("file:///" + info.bitmapUri))
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setAllowRotation(false)
                                    .setAllowFlipping(false)
                                    .setInitialRotation(info.rotation)
                                    .setNoOutputImage(true)
                                    .start(EditPDFActivity.this);
                        }



                    }
                });



                return v;
            }

        };
        return adapter;
    }


    public void SavePDF(View view){

        File f = new File(Utility.PDF_Location(mybox));
        if(!f.exists())f.mkdirs();


        new SavePDFAsync(this, f).execute();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent intentData) {
        if(resultCode != RESULT_OK){
            Toast.makeText(this, "Failed...", Toast.LENGTH_SHORT).show();
            try{
                CropImage.getActivityResult(intentData).getError().printStackTrace();
            }catch (Exception e){e.printStackTrace();}
            return;
        }

        if(requestCode == PDF_SELECT_CODE){

            if(intentData == null)return;
            ArrayList<String>list = new ArrayList<>();

            try{

                for (int i=0; i<intentData.getClipData().getItemCount(); i++){
                    String uri = FIleChooser.getPath(getApplicationContext(), intentData.getClipData().getItemAt(i).getUri());
                    if(uri == null){
                        Toast.makeText(this, "File Not Found : "+uri+", Skipping", Toast.LENGTH_SHORT).show();
                    }else list.add(uri);
                }

            }catch (Exception e){
                String uri = FIleChooser.getPath(getApplicationContext(), intentData.getData());
                if(uri == null){
                    Toast.makeText(this, "File Not Found : "+uri+", Skipping", Toast.LENGTH_SHORT).show();
                }else list.add(uri);
            }

            File tempDir = new File(Utility.TMP_Location(mybox));
            if(!tempDir.exists())tempDir.mkdirs();

            LoadAllThumbs(this, list, tempDir);

        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(intentData);
            Rect i = result.getWholeImageRect();
            Rect r = result.getCropRect();

//            Log.d("Abhi", r.toString());

            float a = r.left, b = r.top, c = r.right, d = r.bottom;
            float x = i.width(), y = i.height();

            a = a/x; b = b/y; c = c/x; d = d/y;

            if(cropPosition == -1)return;
            Data info = data.get(cropPosition);

            info.a = a; info.b = b; info.c = c; info.d = d;
            Log.d("Abhi", print(a, b, c, d));

            Toast.makeText(this, "Applied CROP", Toast.LENGTH_SHORT).show();
        }

    }







    void displayImageDialog(Uri uri, int rotation){
        try {

        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setCanceledOnTouchOutside(true);


        View view = getLayoutInflater().inflate(R.layout.edit_pdf_list_image_dialog, null);
        ((ImageView) view.findViewById(R.id.edit_pdf_list_item_image_dialog_image)).setImageURI(uri);
        if (rotation != 0) {
            (view.findViewById(R.id.edit_pdf_list_item_image_dialog_image)).setRotation(rotation);
        }
        builder.setContentView(view);
        builder.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        builder.show();
        }
        catch (Exception e){e.printStackTrace();}
    }

    void displayRotateDialog(final int position){

        try{

            final Dialog builder = new Dialog(this);
            builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
            builder.setCanceledOnTouchOutside(true);

            final View view = getLayoutInflater().inflate(R.layout.edit_pdf_enter_value_dialog, null);

            final EditText editText = ((EditText)view.findViewById(R.id.edit_pdf_enter_value_dialog_edit_text));
            editText.setHint("Enter Rotation ");
            editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);


            editText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    ((TextView)view.findViewById(R.id.edit_pdf_enter_value_dialog_response)).setText("");
                    return false;
                }
            });

            view.findViewById(R.id.edit_pdf_enter_value_dialog_positive_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        int r = Integer.parseInt(editText.getText().toString());

                        ((TextView)view.findViewById(R.id.edit_pdf_enter_value_dialog_response)).setTextColor(Color.GREEN);
                        ((TextView)view.findViewById(R.id.edit_pdf_enter_value_dialog_response)).setText("Applied Rotation : "+r);
                        data.get(position).rotation = r;
                        adapter.notifyDataSetChanged();


                    }catch (Exception e){
                        ((TextView)view.findViewById(R.id.edit_pdf_enter_value_dialog_response)).setTextColor(Color.RED);
                        ((TextView)view.findViewById(R.id.edit_pdf_enter_value_dialog_response)).setText("Enter a Valid Rotation...");
                    }
                }
            });

            view.findViewById(R.id.edit_pdf_enter_value_dialog_negative_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    builder.cancel();
                }
            });

            builder.setContentView(view);
            builder.show();

        }
        catch (Exception e){e.printStackTrace();}
    }

    void savePage(final Data info){

        final File imgDir = new File(Utility.IMG_Location(mybox));
        if(!imgDir.exists())imgDir.mkdirs();

        new SavePageAsync(this, info, imgDir).execute();

    }

































    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_pdf_menu, menu);
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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

        return true;
    }


    String print(Object a, Object b, Object c, Object d){
        return a + ", " + b + ", " + c + ", " + d;
    }





    class SavePageAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        Data info;
        File parentDir;
        ProgressDialog mProgressDialog;


        SavePageAsync(AppCompatActivity context, Data info, File parentDir){
            this.context = context;
            this.info = info;
            this.parentDir = parentDir;

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
                PdfDocument doc = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+document.get(info.document_no).first), "r"));
                pdfiumCore.openPage(doc, info.page_no);

                int width = pdfiumCore.getPageWidth(doc, info.page_no);
                int height = pdfiumCore.getPageHeight(doc, info.page_no);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                pdfiumCore.renderPageBitmap(doc, bitmap, info.page_no, 0, 0, width, height);

                pdfiumCore.closePage(doc, info.page_no);
                pdfiumCore.closeDocument(doc);

                File f = new File(parentDir, System.currentTimeMillis()+".jpeg");
                FileOutputStream fos = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                toast("Image Saved : " + f.getAbsolutePath());

            }catch (Exception e){
                e.printStackTrace();
                toast("Error Occurred : "+document.get(info.document_no).first);
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


    class SavePDFAsync extends AsyncTask<Void, Void, Void>{


        AppCompatActivity context;
        File parentDir;
        ProgressDialog mProcessDialog;

        SavePDFAsync(AppCompatActivity context, File parentDir){
            this.context = context;
            this.parentDir = parentDir;

            mProcessDialog = new ProgressDialog(context);
            mProcessDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.setCancelable(false);
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

                PDDocument[] pdDocuments = new PDDocument[document.size()];
                PDDocument ret = new PDDocument(true);

                for(Data info : data){

                    if(pdDocuments[info.document_no] == null){
                        try{
                            pdDocuments[info.document_no] = PDDocument.load(new File(document.get(info.document_no).first), document.get(info.document_no).second, true);
                        }catch (Exception e){e.printStackTrace();}
                    }

                    PDPage pdPage = pdDocuments[info.document_no].getPage(info.page_no);


                    try{





                        if(info.a >= 0.0){  // Cropping

                            float x = pdPage.getCropBox().getWidth();
                            float y = pdPage.getCropBox().getHeight();

                            float a = info.a*x, b = info.b*y, c = info.c*x, d = info.d*y;

                            PDRectangle rectangle = new PDRectangle();
                            rectangle.setLowerLeftX(a + pdPage.getCropBox().getLowerLeftX());
                            rectangle.setLowerLeftY(y-d + pdPage.getCropBox().getLowerLeftY());
                            rectangle.setUpperRightX(c + pdPage.getCropBox().getLowerLeftX());
                            rectangle.setUpperRightY(y-b + pdPage.getCropBox().getLowerLeftY());


                            Log.d("Abhi", rectangle.toString());

                            pdPage.setMediaBox(rectangle);
                            pdPage.setCropBox(rectangle);
                        }

                        if(info.rotation != 0){

                            int x = info.rotation;
                            if(x<0) x = x*-1;

                            if(x % 90 == 0){    // rotate 90

                                pdPage.setRotation(info.rotation);

                            }else { // rotate x

                                PDPage targetPage = new PDPage();
                                targetPage.setResources(new PDResources());

                                PDFormXObject xObject = importAsXObject(ret, pdPage);
                                targetPage.getResources().add(xObject, "X");

                                PDPageContentStream content = new PDPageContentStream(ret, targetPage);

                                Matrix matrix = Matrix.getRotateInstance(Math.toRadians(-info.rotation), 0, 0);

                                content.drawXObject(xObject, matrix.createAffineTransform());
                                content.close();

                                PDRectangle cropBox = targetPage.getCropBox();
                                RectF rectangle = new RectF();
                                cropBox.transform(matrix).computeBounds(rectangle, true);
                                PDRectangle newBox = new PDRectangle(rectangle.left, rectangle.bottom, rectangle.right - rectangle.left, rectangle.top - rectangle.bottom);

                                targetPage.setCropBox(newBox);
                                targetPage.setMediaBox(newBox);


                                ret.addPage(targetPage);
                                continue;
                            }


                        }

                    }catch (Exception e){e.printStackTrace();}

                    ret.addPage(pdPage);

                }

                final File file = new File(parentDir, System.currentTimeMillis()+".pdf");
                ret.save(file);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OperationPerformedDialog.showPDFConvertedDialog(context, file.getAbsolutePath());
                    }
                });


                for(int i=0; i<pdDocuments.length; i++){
                    pdDocuments[i].close();
                }
                ret.close();

            }catch (Exception e){e.printStackTrace();}


            return null;
        }



        PDFormXObject importAsXObject(PDDocument target, PDPage page) throws IOException
        {
            Iterator<PDStream> srcs = page.getContentStreams();



            while(srcs.hasNext()) {
                PDStream src = srcs.next();


                if (src != null)
                {
                    final PDFormXObject xobject = new PDFormXObject(target);

                    OutputStream os = xobject.getPDStream().createOutputStream();
                    InputStream is = src.createInputStream();
                    try
                    {
                        IOUtils.copy(is, os);
                    }
                    finally
                    {
                        IOUtils.closeQuietly(is);
                        IOUtils.closeQuietly(os);
                    }

                    xobject.setResources(page.getResources());
                    xobject.setBBox(page.getCropBox());


                    return xobject;
                }


            }


            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProcessDialog.cancel();
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

                int dn = document.size();
                int n = pdfiumCore.getPageCount(pdfDocument), width, height;

                document.add(new Pair<>(uri, pwd));

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




    long back_pressed_time = 0;
    @Override
    public void onBackPressed() {

        if(System.currentTimeMillis() - back_pressed_time <= 2000){
            super.onBackPressed();
        }else{
            Toast.makeText(this, "Press back again to Exit...", Toast.LENGTH_SHORT).show();
            back_pressed_time = System.currentTimeMillis();
        }

    }
}