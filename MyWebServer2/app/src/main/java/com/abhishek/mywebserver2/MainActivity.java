package com.abhishek.mywebserver2;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.abhishek.mywebserver2.services.ReceiveFilesService;
import com.abhishek.mywebserver2.services.ScreenStreamService;
import com.abhishek.mywebserver2.services.SendDataService;
import com.abhishek.mywebserver2.services.WebServerService;
import com.codekidlabs.storagechooser.StorageChooser;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    int REQUEST_SCREENSHOT = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        setUp();
        ini();
    }

    void setUp(){
        ((SeekBar)findViewById(R.id.screen_stream_quality)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {ScreenStreamService.quality = i;setTitle(""+i);}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {setTitle("MyServer");}
        });
        ((SeekBar)findViewById(R.id.screen_stream_size)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {ScreenStreamService.resize = i;setTitle(""+i);}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {setTitle("MyServer");}
        });

    }

    void ini(){
        if(isMyServiceRunning(WebServerService.class)){
            ((ToggleButton)findViewById(R.id.start_server_service_toggle_button)).setText("Stop Server");
        }else {
            ((ToggleButton)findViewById(R.id.start_server_service_toggle_button)).setText("Start Server");
        }

        if(isMyServiceRunning(ScreenStreamService.class)){
            ((ToggleButton)findViewById(R.id.screen_stream_toggle_button)).setText("Stop Screen Stream");

            findViewById(R.id.screen_stream_quality).setVisibility(View.VISIBLE);
            ((SeekBar)findViewById(R.id.screen_stream_quality)).setProgress(ScreenStreamService.quality);

            findViewById(R.id.screen_stream_quality_text).setVisibility(View.VISIBLE);

            findViewById(R.id.screen_stream_size).setVisibility(View.VISIBLE);
            ((SeekBar)findViewById(R.id.screen_stream_size)).setProgress(ScreenStreamService.resize);
            findViewById(R.id.screen_stream_size_text).setVisibility(View.VISIBLE);
        }else{
            ((ToggleButton)findViewById(R.id.screen_stream_toggle_button)).setText("Start Screen Stream");

            findViewById(R.id.screen_stream_quality).setVisibility(View.GONE);
            findViewById(R.id.screen_stream_quality_text).setVisibility(View.GONE);

            findViewById(R.id.screen_stream_size).setVisibility(View.GONE);
            findViewById(R.id.screen_stream_size_text).setVisibility(View.GONE);
        }

        if(isMyServiceRunning(SendDataService.class)){
            ((ToggleButton)findViewById(R.id.send_data_service_button)).setText("Stop Sending Data");
        }else{
            ((ToggleButton)findViewById(R.id.send_data_service_button)).setText("Send Data");
        }

        if(isMyServiceRunning(ReceiveFilesService.class)){
            ((ToggleButton)findViewById(R.id.receive_data_toggle_button)).setText("Stop Receiving Data");
        }else{
            ((ToggleButton)findViewById(R.id.receive_data_toggle_button)).setText("Receive Data");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_SCREENSHOT){
            if(resultCode == RESULT_OK){

                Intent i = new Intent(this,ScreenStreamService.class)
                        .putExtra(ScreenStreamService.RESULT_CODE,resultCode)
                        .putExtra(ScreenStreamService.INTENT_DATA,data) ;
                startService(i) ;
            }
        }
        ini();
        super.onActivityResult(requestCode, resultCode, data);
    }




    public void toggleServerActivity(View view) {
        if(((ToggleButton)view).isChecked() && (!isMyServiceRunning(WebServerService.class))) {
            startService(new Intent(this, WebServerService.class));
        }else if(isMyServiceRunning(WebServerService.class)){
            stopService(new Intent(this, WebServerService.class)) ;
        }
        ini();
    }

    public void toggleScreenStreamActivity(View view) {
        if(((ToggleButton)view).isChecked() && (!isMyServiceRunning(ScreenStreamService.class))) {

            MediaProjectionManager mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE) ;
            startActivityForResult(mgr.createScreenCaptureIntent(),REQUEST_SCREENSHOT);

        }else if(isMyServiceRunning(ScreenStreamService.class)){
            stopService(new Intent(this,ScreenStreamService.class)) ;
        }
        ini();
    }

    public void toggleSendDataService(View view) {

        if( ((ToggleButton)view).isChecked() ){
            if(!isMyServiceRunning(SendDataService.class)){
                startService(new Intent(this,SendDataService.class));
            }

        }else {
            if(isMyServiceRunning(SendDataService.class)){
                stopService(new Intent(this,SendDataService.class)) ;
                SendDataService.uris = new ArrayList<String>();
            }
        }
        ini();
    }

    public void toggleReceiveFilesService(View view) {

        if(((ToggleButton)findViewById(R.id.receive_data_toggle_button)).isChecked()){
            if(isMyServiceRunning(ReceiveFilesService.class))return;
            else startService(new Intent(this,ReceiveFilesService.class));

        }else{
            if(isMyServiceRunning(ReceiveFilesService.class)){
                stopService(new Intent(this,ReceiveFilesService.class));
            }
        }

        ini();
    }




    public void showServerUrls(View view) {
        if(isMyServiceRunning(WebServerService.class)){
            showAlertDialog("Enter any url into a Browser ..." , Utility.getUrls(WebServerService.port,WebServerService.password));
        }else{
            showAlertDialog("First Start the Server and then Enter any url into a Browser ..." , Utility.getUrls(WebServerService.port,WebServerService.password));
        }
    }

    public void showStreamUrls(View view) {
        if(isMyServiceRunning(ScreenStreamService.class)){
            showAlertDialog("Enter Any Url into a Browser...",Utility.getUrls(ScreenStreamService.port,ScreenStreamService.password));
        }else {
            showAlertDialog("First Start Screen Stream and then Enter Any Url into a Browser...", Utility.getUrls(ScreenStreamService.port, ScreenStreamService.password));
        }
    }

    public void showSendFileUrls(View view) {
        if(isMyServiceRunning(SendDataService.class)){
            showAlertDialog("Enter Any Url into a Browser...",Utility.getUrls(SendDataService.port,SendDataService.password));
        }else {
            showAlertDialog("First Start Screen Stream and then Enter Any Url into a Browser...", Utility.getUrls(SendDataService.port, SendDataService.password));
        }
    }

    public void showReceiveFileUrls(View view) {
    }


    void showAlertDialog(String title , final String[]data ){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton("Got IT !!!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, data), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, data[i], Toast.LENGTH_SHORT).show();
                builder.show();
            }
        }) ;
        builder.show();
    }

    void showAlertDialog(String title , final String[]data  , DialogInterface.OnClickListener listener ){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton("Got IT !!!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, data), listener) ;
        builder.show();
    }

    void showFileChooser(String type, StorageChooser.OnSelectListener selectListener , StorageChooser.OnMultipleSelectListener multipleSelectListener){
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme(theme.getDefaultScheme());

        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(MainActivity.this)
                .withFragmentManager(getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
//              .setType(StorageChooser.FILE_PICKER)
                .setType(type)
//                .shouldResumeSession(true)
                .allowAddFolder(true)
                .setTheme(theme)
                .build() ;
        chooser.show();

        chooser.setOnSelectListener(selectListener);
        chooser.setOnMultipleSelectListener(multipleSelectListener);
    }

    void showFileChooser(StorageChooser.FileType type, StorageChooser.OnSelectListener selectListener , StorageChooser.OnMultipleSelectListener multipleSelectListener){
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme(theme.getDefaultScheme());

        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(MainActivity.this)
                .withFragmentManager(getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .filter(type)
//                .shouldResumeSession(true)
                .setTheme(theme)
                .build() ;
        chooser.show();

        chooser.setOnSelectListener(selectListener);
        chooser.setOnMultipleSelectListener(multipleSelectListener);
    }

    public void showAddFilesDialog(View view) {

        showAlertDialog("...", new String[]{"Select Files", "Select Directory", "Audios", "Videos", "Images", "Documents","Archives"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String[] type = new String[]{StorageChooser.FILE_PICKER,StorageChooser.DIRECTORY_CHOOSER};
                StorageChooser.FileType[] fileTypes = new StorageChooser.FileType[]{StorageChooser.FileType.AUDIO, StorageChooser.FileType.VIDEO, StorageChooser.FileType.IMAGES, StorageChooser.FileType.DOCS, StorageChooser.FileType.ARCHIVE};

                if(i < 2){
                    showFileChooser(type[i], new StorageChooser.OnSelectListener() {
                                @Override
                                public void onSelect(String s) {
                                    SendDataService.uris.add(s);
                                }
                            } ,
                            new StorageChooser.OnMultipleSelectListener() {
                                @Override
                                public void onDone(ArrayList<String> arrayList) {
                                    SendDataService.uris.addAll(arrayList);
                                }
                            });

                }else {
                    showFileChooser(fileTypes[i - 2], new StorageChooser.OnSelectListener() {
                                @Override
                                public void onSelect(String s) {
                                    SendDataService.uris.add(s);
                                }
                            },
                            new StorageChooser.OnMultipleSelectListener() {
                                @Override
                                public void onDone(ArrayList<String> arrayList) {
                                    SendDataService.uris.addAll(arrayList);
                                }
                            });
                }
            }
        });

    }



    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Settings").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getTitle().toString().equalsIgnoreCase("Settings")){
            startActivity(new Intent(this,PreferenceActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }



}
