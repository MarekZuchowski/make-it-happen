package com.example.makeithappen.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.makeithappen.R;

public class EditTextAlertDialogHelper {

    private final Context context;

    public EditTextAlertDialogHelper(Context context) {
        this.context = context;
    }
    public AlertDialog showAlertDialog(String title) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(R.layout.input_dialog)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }
}
