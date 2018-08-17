package com.example.abhishekaggarwal.qrandbarcodescanner.QrCodeGenerator.QRTypeFragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.abhishekaggarwal.qrandbarcodescanner.Data;
import com.example.abhishekaggarwal.qrandbarcodescanner.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class TextFragment extends Fragment {


    public TextFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.qr_type_text_layout , container , false) ;

        if(Data.QrType.equals("SMS"))return inflater.inflate(R.layout.qr_type_sms_layout , container , false) ;


        return v;
    }

}
