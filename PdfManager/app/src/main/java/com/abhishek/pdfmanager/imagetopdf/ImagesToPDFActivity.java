package com.abhishek.pdfmanager.imagetopdf;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.abhishek.pdfmanager.App;
import com.abhishek.pdfmanager.FIleChooser;
import com.abhishek.pdfmanager.OperationPerformedDialog;
import com.abhishek.pdfmanager.R;
import com.abhishek.pdfmanager.SettingDB;
import com.abhishek.pdfmanager.Utility;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.shockwave.pdfium.PdfiumCore;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import io.objectbox.Box;

public class ImagesToPDFActivity extends AppCompatActivity {

    public class Data{

        Uri bitmapUri;
        String imagePath;

        public Data(Uri bitmapUri, String imagePath) {
            this.bitmapUri = bitmapUri;
            this.imagePath = imagePath;
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images_to_pdf);

        mybox = App.boxStore.boxFor(SettingDB.class);

        PDFBoxResourceLoader.init(this);
        ini();
    }

    Box<SettingDB> mybox;

    ArrayList<Data> data;

    DragSortListView listView;
    ArrayAdapter<Data> adapter;


    final int IMG_SELECT_CODE = 1;

    int cropPosition = -1;

    File tempPath;





    void ini(){
        tempPath = new File(Utility.TMP_Location(mybox));
        if(!tempPath.exists()) tempPath.mkdirs();


        data = new ArrayList<>();

        listView = findViewById(R.id.images_to_pdf_list_view);
        setAdapter();
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
        controller.setDragHandleId(R.id.images_to_pdf_drag_handle_image_view);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.FLING_REMOVE);

        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
        listView.setDragEnabled(true);
        listView.setMaxScrollSpeed(8f);
    }

    void setAdapter(){
        adapter = new ArrayAdapter<Data>(this, R.layout.image_to_pdf_list_item, R.id.images_to_pdf_txt_view, data){
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                final Data info = data.get(position);

                ((ImageView)view.findViewById(R.id.images_to_pdf_image_view)).setImageURI(info.bitmapUri);
                view.findViewById(R.id.images_to_pdf_close_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        data.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });
                view.findViewById(R.id.images_to_pdf_image_view).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayImageDialog(position);
                    }
                });
                view.findViewById(R.id.images_to_pdf_edit_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        cropPosition = position;

                        CropImage.activity(info.bitmapUri)
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setAllowRotation(true)
                                .setAllowFlipping(true)
                                .setAllowCounterRotation(true)
                                .start(ImagesToPDFActivity.this);

                    }
                });

                return view;
            }
        };
    }


    File parentPath;
    public void Convert(View view) {

        parentPath = new File(Utility.PDF_Location(mybox));
        if(!parentPath.exists())parentPath.mkdirs();

        new ConvertAsync(this).execute();
    }


    void addImage(final ArrayList<String> list){

        if(!tempPath.exists())tempPath.mkdirs();

        new AddImageAsync(this, list).execute();
    }
    

    void displayImageDialog(int position){
        try {

            Data info = data.get(position);

            Dialog builder = new Dialog(this);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Add Images").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Image Quality");
        return super.onCreateOptionsMenu(menu);
    }


    int imageQuality = 0;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getTitle().toString().equalsIgnoreCase("Add Images")){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            try{
                startActivityForResult(
                        Intent.createChooser(intent, "Choose an Image"),
                        IMG_SELECT_CODE
                );
            }catch (Exception e){
                Toast.makeText(this, "Cannot Open this File...", Toast.LENGTH_SHORT).show();
            }
        }else if(item.getTitle().toString().equalsIgnoreCase("Image Quality")){


            final Dialog builder = new Dialog(this);
            builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
            builder.setCanceledOnTouchOutside(false);

            View view = getLayoutInflater().inflate(R.layout.images_to_pdf_quality_dialog, null);

            final CheckBox c0 = view.findViewById(R.id.image_to_page_quality_checkbox_good);
            final CheckBox c1 = view.findViewById(R.id.image_to_page_quality_checkbox_best);
            Button btn = view.findViewById(R.id.image_to_page_quality_checkbox_button);


            if(imageQuality == 1){
                c1.setChecked(true);
                c0.setChecked(false);
            }else{
                imageQuality = 0;
                c0.setChecked(true);
                c1.setChecked(false);
            }

            c0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked)c1.setChecked(false);
                    else c1.setChecked(true);
                }
            });
            c1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked)c0.setChecked(false);
                    else c0.setChecked(true);
                }
            });
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(c1.isChecked())imageQuality = 1;
                    else imageQuality = 0;
                    builder.cancel();
                }
            });




            builder.setContentView(view);
            builder.show();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if(resultCode != RESULT_OK){
            Toast.makeText(this, "Failed..", Toast.LENGTH_SHORT).show();
            return;
        }

        if(requestCode == IMG_SELECT_CODE){

            if(intentData == null)return;
            ArrayList<String> list = new ArrayList<>();

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
                addImage(list);
//                adapter.notifyDataSetChanged();
            }
            catch (Exception e){
                Toast.makeText(getApplicationContext(), "Error Adding Pdf to List", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(intentData);
            Bitmap bitmap = BitmapFactory.decodeFile(result.getUri().getPath());

            try{
                File f = new File(tempPath, System.currentTimeMillis()+".jpg");
                FileOutputStream fos = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                String imgPath = data.get(cropPosition).imagePath;
                data.remove(cropPosition);
                data.add(cropPosition, new Data(Uri.parse("file:///"+f.getAbsolutePath()), imgPath));
                adapter.notifyDataSetChanged();
            }catch (Exception e){e.printStackTrace();}

        }
    }



    class ConvertAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;

        ConvertAsync(AppCompatActivity context){
            this.context = context;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Adding Images");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{

                PDDocument document = new PDDocument(true);
                for(Data info : data){

                    try{

                        PDPage page = new PDPage();
                        document.addPage(page);

                        PDImageXObject imageXObject;
                        if(imageQuality == 0){
                            imageXObject = PDImageXObject.createFromFile(info.bitmapUri.getPath(), document);
                        }else{
                            imageXObject = PDImageXObject.createFromFile(info.imagePath, document);
                        }

                        page.setMediaBox(new PDRectangle(imageXObject.getWidth(), imageXObject.getHeight()));

                        PDPageContentStream cs = new PDPageContentStream(document, page);
                        cs.drawImage(imageXObject, 0, 0);
                        cs.close();

                    }catch (Exception e){e.printStackTrace();}

                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMessage("Saving PDF");
                    }
                });

                final File f = new File(parentPath, System.currentTimeMillis()+".pdf");
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
            }



            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.cancel();
        }
    }

    class AddImageAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ArrayList<String>list;

        ProgressDialog mProgressDialog;

        public AddImageAsync(AppCompatActivity context, ArrayList<String> list) {
            this.context = context;
            this.list = list;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(list.size());
            mProgressDialog.setMessage("Loading Images");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            int i = 0;
            for(String s : list){
                if(s == null || s.length()<1)continue;

                try{
                    Bitmap bitmap = BitmapFactory.decodeFile(s);
                    int width = bitmap.getWidth(), height = bitmap.getHeight();

                    while (width*height > 1000000){
                        width = width/2;
                        height = height/2;
                    }
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                    File f = new File(tempPath, System.currentTimeMillis()+".jpeg");
                    FileOutputStream fos = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    data.add(new Data(Uri.parse("file://"+f.getAbsolutePath()), s));

                }catch (Exception e){
                    e.printStackTrace();
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mProgressDialog.setProgress(mProgressDialog.getProgress()+1);
                    }
                });
            }


            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adapter.notifyDataSetChanged();
            mProgressDialog.cancel();
        }
    }


}
