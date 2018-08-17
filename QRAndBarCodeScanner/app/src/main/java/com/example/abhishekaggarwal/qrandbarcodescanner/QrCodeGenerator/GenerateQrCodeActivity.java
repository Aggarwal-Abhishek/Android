package com.example.abhishekaggarwal.qrandbarcodescanner.QrCodeGenerator;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.abhishekaggarwal.qrandbarcodescanner.Data;
import com.example.abhishekaggarwal.qrandbarcodescanner.QrCodeGenerator.QRTypeFragments.SmsFragment;
import com.example.abhishekaggarwal.qrandbarcodescanner.QrCodeGenerator.QRTypeFragments.TextFragment;
import com.example.abhishekaggarwal.qrandbarcodescanner.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.encoder.QRCode;

public class GenerateQrCodeActivity extends AppCompatActivity implements FragmentChangeListener {

    Button QRTypeButton , QRSizeButton , QRErrorButton , QREncodingButton , QRGenerateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_code);

        ini();
    }


    void  ini(){

        QRTypeButton = (Button)findViewById(R.id.typeOfQRCodeButton) ;
        QRTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getBaseContext(), R.style.MyPopupMenu);

                PopupMenu pm = new PopupMenu(wrapper , QRTypeButton) ;
                pm.getMenuInflater().inflate(R.menu.qr_codes_types_menu , pm.getMenu());

                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        QRTypeButton.setText(item.getTitle());
                        Data.QrType = item.getTitle().toString() ;

      /*                  if(item.getTitle().equals("TEXT")){

                            //Toast.makeText(getBaseContext(),"TEXT",Toast.LENGTH_SHORT).show();

                            TextFragment tf = new TextFragment();
                            FragmentChangeListener fc = (FragmentChangeListener)GenerateQrCodeActivity.this ;
                            fc.replaceFragment(tf);


                        }
                        else if(item.getTitle().equals("SMS")){


                            TextFragment tf = new TextFragment();
                            FragmentChangeListener fc = (FragmentChangeListener)GenerateQrCodeActivity.this ;
                            fc.replaceFragment(tf);

                        }
            */

                        return  true;
                    }
                });
                pm.show();
            }
        });


        QRSizeButton = (Button)findViewById(R.id.barcodeSizeButton) ;
        QRSizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getBaseContext(), R.style.MyPopupMenu);

                PopupMenu pm = new PopupMenu(wrapper , QRSizeButton) ;
                pm.getMenuInflater().inflate(R.menu.barcode_size_menu , pm.getMenu());

                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        QRSizeButton.setText(item.getTitle());
                        return  true;
                    }
                });
                pm.show();
            }
        });



        QRErrorButton = (Button)findViewById(R.id.barcodeErrorButton) ;
        QRErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getBaseContext(), R.style.MyPopupMenu);

                PopupMenu pm = new PopupMenu(wrapper , QRErrorButton) ;
                pm.getMenuInflater().inflate(R.menu.barcode_error_menu , pm.getMenu());

                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        QRErrorButton.setText(item.getTitle());
                        return  true;
                    }
                });
                pm.show();
            }
        });



        QREncodingButton = (Button)findViewById(R.id.barcodeEncodeingButton) ;
        QREncodingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getBaseContext(), R.style.MyPopupMenu);

                PopupMenu pm = new PopupMenu(wrapper , QREncodingButton) ;
                pm.getMenuInflater().inflate(R.menu.barcode_encoding_menu , pm.getMenu());

                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        QREncodingButton.setText(item.getTitle());
                        return  true;
                    }
                });
                pm.show();
            }
        });


        QRGenerateButton = (Button)findViewById(R.id.generateButton) ;
        QRGenerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Bitmap b = TextToImageEncode(   ((EditText)findViewById(R.id.QRCodeText)).getText().toString()   );

                    ((ImageView)findViewById(R.id.QrImage)).setImageBitmap(b);


                } catch (WriterException e) {
                    e.printStackTrace();
                }


            }
        });





    }

    @Override
    public void replaceFragment(android.support.v4.app.Fragment fragment) {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();;
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //fragmentTransaction.replace(R.id.QrTypeFragment , fragment);
        //fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }



    private Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    500, 500, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }


}
