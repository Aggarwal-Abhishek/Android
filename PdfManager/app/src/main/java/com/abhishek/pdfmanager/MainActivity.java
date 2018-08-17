package com.abhishek.pdfmanager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.abhishek.pdfmanager.backgroundimage.AddBackgroundImageActivity;
import com.abhishek.pdfmanager.editpdf.EditPDFActivity;
import com.abhishek.pdfmanager.extractimages.ExtractImageActivity;
import com.abhishek.pdfmanager.imagetopdf.ImagesToPDFActivity;
import com.abhishek.pdfmanager.mergepdf.MergePDFActivity;
import com.abhishek.pdfmanager.pagetoimage.PageToImageActivity;
import com.abhishek.pdfmanager.passwordprotect.PasswordProtectActivity;
import com.abhishek.pdfmanager.removepassword.RemovePasswordActivity;
import com.abhishek.pdfmanager.splitpdf.SplitPDFActivity;
import com.abhishek.pdfmanager.textwatermark.TextWatermarkActivity;
import com.abhishek.pdfmanager.viewpdf.PDFViewActivity;
import com.abhishek.pdfmanager.watermarkimage.WatermarkImageActivity;

import io.objectbox.BoxStore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1
        );


        try {
            App.boxStore = MyObjectBox.builder().androidContext(this).build();
        }catch (Exception e){}
    }


    public void ViewPDF(View view) {startActivity(new Intent(this, PDFViewActivity.class));}
    public void EditPDF(View view) {
        startActivity(new Intent(this, EditPDFActivity.class));
    }
    public void ProtectWithPassword(View view) {startActivity(new Intent(this, PasswordProtectActivity.class));}
    public void RemovePassword(View view) {startActivity(new Intent(this, RemovePasswordActivity.class));}
    public void ExtractImages(View view) {startActivity(new Intent(this, ExtractImageActivity.class));}
    public void SplitPDF(View view) {startActivity(new Intent(this, SplitPDFActivity.class));}
    public void MergePDF(View view) {startActivity(new Intent(this, MergePDFActivity.class));}
    public void WatermarkImage(View view) {startActivity(new Intent(this, WatermarkImageActivity.class));}
    public void WatermarkText(View view) {startActivity(new Intent(this, TextWatermarkActivity.class));}
    public void AddBackgroundImage(View view) {startActivity(new Intent(this, AddBackgroundImageActivity.class));}
    public void ImageToPDF(View view) {startActivity(new Intent(this, ImagesToPDFActivity.class));}
    public void PageToImage(View view) {startActivity(new Intent(this, PageToImageActivity.class));}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add("Settings");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getTitle().toString().equalsIgnoreCase("Settings")){
            startActivity(new Intent(this, SettingActivity.class));
        }


        return super.onOptionsItemSelected(item);
    }
}
