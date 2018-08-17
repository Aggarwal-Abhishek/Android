package com.example.abhishekaggarwal.qrandbarcodescanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.abhishekaggarwal.qrandbarcodescanner.FileScan.FileScanActivity;
import com.example.abhishekaggarwal.qrandbarcodescanner.QrCodeGenerator.GenerateQrCodeActivity;
import com.example.abhishekaggarwal.qrandbarcodescanner.Scan.ScanActivity;


public class MainActivity extends AppCompatActivity  {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    public void scan(View view){
        startActivity(new Intent(getBaseContext() , ScanActivity.class));
    }


    public void fileScan(View view){
        startActivity(new Intent(getBaseContext() , FileScanActivity.class));

    }

    public void generateQR(View view){
        startActivity(new Intent(getBaseContext() , GenerateQrCodeActivity.class));
    }






}
