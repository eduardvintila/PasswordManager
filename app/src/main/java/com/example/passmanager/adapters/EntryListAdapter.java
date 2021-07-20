package com.example.passmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.passmanager.R;
import com.example.passmanager.model.Entry;

import java.util.List;

/**
 *  Adapter for populating a RecyclerView with entries.
 */
public class EntryListAdapter extends RecyclerView.Adapter<EntryListAdapter.EntryViewHolder> {
    private final LayoutInflater inflater;
    private List<Entry> entries;

    public interface OnEntryClickListener {
        void onEntryClick(int position);
    }

    // Listener used for handling click events on the entries in the list.
    private final OnEntryClickListener onEntryClickListener;

    public EntryListAdapter(Context context, OnEntryClickListener onEntryClickListener) {
        inflater = LayoutInflater.from(context);
        this.onEntryClickListener = onEntryClickListener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.entry_row, parent, false);
        return new EntryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        if (entries != null) {
            Entry current = entries.get(position);
            holder.entryItemView.setText(current.name);
        } else {
            holder.entryItemView.setText("");
        }
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public int getItemCount() {
        if (entries != null) { return entries.size(); }
        return 0;
    }

    class EntryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView entryItemView;

        private EntryViewHolder(View itemView) {
            super(itemView);
            entryItemView = itemView.findViewById(R.id.textView);
            itemView.setOnClickListener(this);
        }

        /**
         * Called when an entry is clicked.
         */
        @Override
        public void onClick(View v) {
            if (onEntryClickListener != null) {
                onEntryClickListener.onEntryClick(getAdapterPosition());
            }
        }
    }
}
