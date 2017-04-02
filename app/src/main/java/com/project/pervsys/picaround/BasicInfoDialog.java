package com.project.pervsys.picaround;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class BasicInfoDialog extends DialogFragment{
    private final static String TAG = "BasicInfoDialog";
    private Dialog d;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(R.layout.dialog_profile_info).setPositiveButton(R.string.continue_mex,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EditText ageField = (EditText) d.findViewById(R.id.dialog_age);
                validAge(ageField.getText().toString());
                if (true)
                    d.show();
            }
        });
        d = builder.create();
        d.setTitle(R.string.dialog_profile_info_title);
        d.setCanceledOnTouchOutside(false);

        return d;
    }


    private boolean validAge(String age){
        Log.e(TAG, age);
        if (age.equals("")) {
            Log.i(TAG, "Empty age");
            return false;
        }
        int a = Integer.parseInt(age);
        if (a < 3 || a > 100) {
            Log.i(TAG, "Invalid age range");
            return false;
        }
        return true;
    }

}
