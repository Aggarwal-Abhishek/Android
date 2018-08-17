package com.example.abhishekaggarwal.qrandbarcodescanner.Scan;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.abhishekaggarwal.qrandbarcodescanner.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);



        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("ScanActivity ... ");
        integrator.setOrientationLocked(false) ;

        integrator.initiateScan();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {




        IntentResult result = IntentIntegrator.parseActivityResult(requestCode , resultCode , data);

        if(result != null && result.getContents()!=null){

            new AlertDialog.Builder(this)
                    .setTitle("Result ... ")
                    .setMessage(result.getContents())
                    .setCancelable(true)
                    .show() ;

        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
