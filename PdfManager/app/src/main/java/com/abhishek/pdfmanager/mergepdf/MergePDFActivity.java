package com.abhishek.pdfmanager.mergepdf;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import io.objectbox.Box;

public class MergePDFActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);

        PDFBoxResourceLoader.init(this);
        pdfiumCore = new PdfiumCore(this);
        ini();
    }

    final int PDF_SELECT_CODE = 0;

    PdfiumCore pdfiumCore;

    ArrayList<Pair<String, String>> document;
    ArrayList<Pair<Integer, String>> data;


    DragSortListView listView;
    ArrayAdapter<Pair<Integer, String>> adapter;


    Box<SettingDB> mybox;

    void ini() {
        document = new ArrayList<>();
        data = new ArrayList<>();

        mybox = App.boxStore.boxFor(SettingDB.class);

        listView = findViewById(R.id.merge_pdf_list_view);
        setAdapter();
        listView.setAdapter(adapter);

        listView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                if (from != to) {
                    Pair movedData = data.get(from);
                    data.remove(from);
                    data.add(to, movedData);
                    adapter.notifyDataSetChanged();
                }
            }
        });


        DragSortController controller = new DragSortController(listView);
        controller.setDragHandleId(R.id.merge_pdf_reorder_handle_btn);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.FLING_REMOVE);

        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
        listView.setDragEnabled(true);
        listView.setMaxScrollSpeed(8f);
    }


    void setAdapter() {
        adapter = new ArrayAdapter<Pair<Integer, String>>(this, R.layout.merge_pdf_list_item, R.id.merge_pdf_document_id, data) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                Pair<Integer, String> info = data.get(position);

                ((TextView) view.findViewById(R.id.merge_pdf_document_id)).setText(((Integer) info.first + 1) + "");
                ((TextView) view.findViewById(R.id.merge_pdf_text_view)).setText((String) info.second);
                view.findViewById(R.id.merge_pdf_close_btn).setOnClickListener(new View.OnClickListener() {
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


    public void MergePDF(View view) {

        File parent = new File(Utility.PDF_Location(mybox));
        if (!parent.exists()) parent.mkdirs();

        new MergePDFAsync(this, parent).execute();

    }


    void addPDF(ArrayList<String> list) {
        new AddPDFAsync(this, list).execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add("Add PDF").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getTitle().toString().equalsIgnoreCase("Add PDF")) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            try {
                startActivityForResult(
                        Intent.createChooser(intent, "Choose a PDF Document"),
                        PDF_SELECT_CODE
                );
            } catch (Exception e) {
                Toast.makeText(this, "Cannot Open this File...", Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if (resultCode != RESULT_OK) return;

        if (requestCode == PDF_SELECT_CODE) {

            if (intentData == null) return;
            ArrayList<String> list = new ArrayList<>();

            try {

                for (int i = 0; i < intentData.getClipData().getItemCount(); i++) {
                    String uri = FIleChooser.getPath(getApplicationContext(), intentData.getClipData().getItemAt(i).getUri());
                    list.add(uri);
                }

            } catch (Exception e) {
                String uri = FIleChooser.getPath(getApplicationContext(), intentData.getData());
                list.add(uri);
            }

            try {
                addPDF(list);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error Adding Pdf to List", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }


    class MergePDFAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ProgressDialog mProgressDialog;
        File parentDir;

        MergePDFAsync(AppCompatActivity context, File parentDir){
            this.context = context;
            this.parentDir = parentDir;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Merging");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{

                PDDocument ret = new PDDocument(true);

                ArrayList<PDDocument>pdDocuments = new ArrayList<>();

                for(Pair<Integer, String> info : data){

                    try{

                        PDDocument pdDocument = PDDocument.load(new File(document.get(info.first).first), document.get(info.first).second, true);
                        pdDocuments.add(pdDocument);

                        for(PDPage page : pdDocument.getPages()){
                            ret.addPage(page);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                        toast("Cannot add : "+info.first);
                    }
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMessage("Saving...");
                    }
                });

                try{
                    final File f = new File(parentDir, System.currentTimeMillis()+".pdf");
                    ret.save(f);
                    ret.close();


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OperationPerformedDialog.showPDFConvertedDialog(context, f.getAbsolutePath());
                        }
                    });

                }catch (Exception e){e.printStackTrace();toast("Failed Saving...");}



                for(PDDocument d : pdDocuments){
                    try{
                        d.close();
                    }catch (Exception e){e.printStackTrace();}
                }


            }catch (Exception e){
                e.printStackTrace();
                toast("Failed....");
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


    class AddPDFAsync extends AsyncTask<Void, Void, Void>{

        AppCompatActivity context;
        ArrayList<String>list;

        ProgressDialog mProgressDialog;

        Pair<String, String>pair;

        public AddPDFAsync(AppCompatActivity context, ArrayList<String> list) {
            this.context = context;
            this.list = list;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(list.size());
            mProgressDialog.setMessage("Adding PDF");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }


        @Override
        protected Void doInBackground(Void... voids) {

            for(final String uri : list){
                if(uri == null || uri.length()<1)continue;

                ParcelFileDescriptor fd;
                try{
                    fd = context.getContentResolver().openFileDescriptor(Uri.parse("file://"+uri), "r");
                }catch (Exception e){
                    e.printStackTrace();
                    toast("File not Found : "+uri+", Skipping");
                    continue;
                }

                try{
                    PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                    pdfiumCore.closeDocument(pdfDocument);

                    data.add(new Pair<>(document.size(), new File(uri).getName()));
                    document.add(new Pair<>(uri, ""));
                }catch (Exception e){
                    e.printStackTrace();

                    password_dialog_is_showing = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                ShowPasswordDialog(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+uri), "r"), uri);
                            }catch (Exception e1){
                                e1.printStackTrace();
                            }
                        }
                    });

                    while (password_dialog_is_showing){
                        try {
                            Thread.sleep(500);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }

                    try{
                        PdfDocument pdfDocument = pdfiumCore.newDocument(context.getContentResolver().openFileDescriptor(Uri.parse("file://"+uri), "r"), pair.second);
                        pdfiumCore.closeDocument(pdfDocument);

                        data.add(new Pair<>(document.size(), new File(uri).getName()));
                        document.add(new Pair<>(uri, pair.second));

                    }catch (Exception e1){
                        e1.printStackTrace();
                        toast("Skipping : " + uri);
                    }
                }
            }


            return null;
        }


        boolean password_dialog_is_showing;

        void ShowPasswordDialog(final ParcelFileDescriptor fd, final String uri){
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

                        pair = new Pair<>(uri, str);

                        password_dialog_is_showing = false;
                        dialog.cancel();

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


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adapter.notifyDataSetChanged();
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

