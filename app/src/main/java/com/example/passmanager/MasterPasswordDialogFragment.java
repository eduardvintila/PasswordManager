package com.example.passmanager;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MasterPasswordDialogFragment extends DialogFragment {

    public interface DialogListener {
        void onClickPositiveButton(DialogInterface dialog);
    }

    DialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (DialogListener) context;
        } catch (ClassCastException e) {
            // Interface not implemented.
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View inflatedView = inflater.inflate(R.layout.dialog_masterpassword, null);

        final AlertDialog dialog = builder.setView(inflatedView)
                .setCancelable(false)
                .setPositiveButton("Ok", null)
                .setNegativeButton(R.string.dialogClose, null)
                .setTitle(R.string.enterPasswordTitleDialog)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button btn = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(v -> listener.onClickPositiveButton(dialogInterface));
            }
        });

        return dialog;
    }
}
