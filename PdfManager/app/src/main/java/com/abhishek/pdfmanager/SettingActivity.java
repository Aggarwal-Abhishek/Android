package com.abhishek.pdfmanager;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import java.io.File;

import io.objectbox.Box;

public class SettingActivity extends AppCompatActivity
        implements FolderChooserDialog.FolderCallback{

    CheckBox c1, c2, c3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        myBox = App.boxStore.boxFor(SettingDB.class);

        c1 = findViewById(R.id.setting_thumb_checkbox_good);
        c2 = findViewById(R.id.setting_thumb_checkbox_low);
        c3 = findViewById(R.id.setting_thumb_checkbox_none);


        try{
            ((TextView)findViewById(R.id.setting_pdf_location_txt)).setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        }catch (Exception e){e.printStackTrace();}

        c1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    c1.setChecked(true);
                    setCheck(c1, c1.getId());
                }
            }
        });

        c2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    c2.setChecked(true);
                    setCheck(c2, c2.getId());
                }
            }
        });

        c3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    c3.setChecked(true);
                    setCheck(c3, c3.getId());
                }
            }
        });



        try{
            if(myBox.count() == 2){

                ((TextView)findViewById(R.id.setting_pdf_location_txt)).setText(myBox.get(1).str);
                clear_check_box();
                ((CheckBox)findViewById(Integer.parseInt(myBox.get(2).str))).setChecked(true);
            }
            else {
                myBox.removeAll();
                myBox.put(new SettingDB(0, ((TextView)findViewById(R.id.setting_pdf_location_txt)).getText().toString()));
                myBox.put(new SettingDB(0, R.id.setting_thumb_checkbox_low+""));
            }
        }catch (Exception e){
            e.printStackTrace();
            myBox.removeAll();
            myBox.put(new SettingDB(0, ((TextView)findViewById(R.id.setting_pdf_location_txt)).getText().toString()));
            myBox.put(new SettingDB(0, R.id.setting_thumb_checkbox_low+""));
        }



    }

    void clear_check_box(){
        ((CheckBox)findViewById(R.id.setting_thumb_checkbox_good)).setChecked(false);
        ((CheckBox)findViewById(R.id.setting_thumb_checkbox_low)).setChecked(false);
        ((CheckBox)findViewById(R.id.setting_thumb_checkbox_none)).setChecked(false);
    }

    void setCheck(CheckBox c, int id){
        ((CheckBox)findViewById(R.id.setting_thumb_checkbox_good)).setChecked(false);
        ((CheckBox)findViewById(R.id.setting_thumb_checkbox_low)).setChecked(false);
        ((CheckBox)findViewById(R.id.setting_thumb_checkbox_none)).setChecked(false);

        c.setChecked(true);
        myBox.put(new SettingDB(2, id+""));
    }



    Box<SettingDB> myBox;

    public void AddDirectory(View view) {

        new FolderChooserDialog.Builder(this)
                .goUpLabel("Parent Directory.....")
                .allowNewFolder(true, R.string.m_new_folder)
                .show(this);
    }


    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        ((TextView)findViewById(R.id.setting_pdf_location_txt)).setText(folder.getAbsolutePath());

        myBox.put(new SettingDB(1, folder.getAbsolutePath()));
//        Toast.makeText(this, "Folder : "+folder.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFolderChooserDismissed(@NonNull FolderChooserDialog dialog) {}
}
