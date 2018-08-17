package com.abhishek.pdfmanager;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.abhishek.pdfmanager.viewpdf.PDFViewActivity;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by Abhishek on 6/25/2018.
 */

public class OperationPerformedDialog {

    public static void showPDFConvertedDialog(final AppCompatActivity context, final String path){

        final Dialog builder = new Dialog(context);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setCanceledOnTouchOutside(true);

        View view = context.getLayoutInflater().inflate(R.layout.operation_performed_dialog, null);


        ((TextView)view.findViewById(R.id.operation_performed_dialog_title)).setText("PDF File Saved to : \n\n"+path);
        view.findViewById(R.id.operation_performed_dialog_button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.cancel();
            }
        });
        view.findViewById(R.id.operation_performed_dialog_button_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.cancel();
                context.finish();
            }
        });
        view.findViewById(R.id.operation_performed_dialog_button_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PDFViewActivity.uri = path;
                context.startActivity(new Intent(context, PDFViewActivity.class));
                builder.cancel();
            }
        });

        builder.setContentView(view);
        builder.show();
    }


    public static String error = "";
    public static MaterialDialog dialog;
    public static void showErrorDialog(final AppCompatActivity context){

        if( dialog != null && dialog.isShowing()){
            dialog.dismiss();
        }

          dialog = new MaterialDialog.Builder(context)
                .positiveText("report")
                .negativeText("ok")
                .neutralText("copy")
                .content(error)
                .title("Error Occurred")

                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                        Intent sendIntent = new Intent();
//                        sendIntent.setAction(Intent.ACTION_SEND);
//                        sendIntent.putExtra(Intent.EXTRA_TEXT, error);
//                        sendIntent.setType("text/plain");
//                        sendIntent.setPackage("com.whatsapp");
//                        context.startActivity(sendIntent);


                        try{
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=918267827798&text="+error));
                            intent.setPackage("com.whatsapp");
                            context.startActivity(intent);

                        }catch (Exception e){
                            Toast.makeText(context, "Failed...", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("error msg", error);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })

                .show();

    }

}
