package com.project.pervsys.picaround;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by Riccardo on 18/03/2017.
 */

public class BasicInfoDialog extends DialogFragment{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(R.layout.dialog_profile_info).setPositiveButton(R.string.continue_mex,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        Dialog d = builder.create();
        d.setTitle(R.string.dialog_profile_info_title);
        d.setCanceledOnTouchOutside(false);
        return d;
    }

}
