package com.example.passmanager.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.passmanager.R;

/**
 * Dialog Fragment which displays a loading animation.
 */
public class LoadingDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View inflatedView = inflater.inflate(R.layout.dialog_loading, null);

        final AlertDialog alertDialog = builder.setView(inflatedView)
                .setCancelable(false)
                .create();

        return alertDialog;
    }
}
