package com.example.passmanager.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.passmanager.R;
import com.example.passmanager.model.Category;

/**
 * Dialog Fragment which prompts the user for a confirmation on a category deletion.
 */
public class DeleteCategoryDialogFragment extends DialogFragment {

    // Listener for clicking the positive button.
    private final DialogInterface.OnClickListener onPositiveClickListener;

    // Category to be deleted.
    private final Category category;

    public DeleteCategoryDialogFragment(Category category,
                                        DialogInterface.OnClickListener onPositiveClickListener) {
        this.category = category;
        this.onPositiveClickListener = onPositiveClickListener;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        TextView textView = new TextView(getContext());
        textView.setText(String.format(getString(R.string.delete_category_format), category.name));
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(10, 10, 10, 10);

        return builder
                .setCancelable(false)
                .setPositiveButton(R.string.yes, onPositiveClickListener)
                .setNegativeButton(R.string.no, null)
                .setCustomTitle(textView)
                .create();
    }
}
