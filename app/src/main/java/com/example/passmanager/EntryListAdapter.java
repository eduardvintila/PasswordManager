package com.example.passmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 *  Adapter for populating the RecyclerView with entries.
 */
public class EntryListAdapter extends RecyclerView.Adapter<EntryListAdapter.EntryViewHolder> {
    private final LayoutInflater inflater;
    private List<Entry> entries;
    private OnEntryListener onEntryListener;

    EntryListAdapter(Context context, OnEntryListener onEntryListener) {
        inflater = LayoutInflater.from(context);
        this.onEntryListener = onEntryListener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.recyclerview_item, parent, false);
        return new EntryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        if (entries != null) {
            Entry current = entries.get(position);
            holder.entryItemView.setText(current.entryName);
        } else {
            holder.entryItemView.setText("");
        }
    }


    void setEntries(List<Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
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

        @Override
        public void onClick(View v) {
            onEntryListener.onEntryClick(getAdapterPosition());
        }
    }

    public interface OnEntryListener {
        void onEntryClick(int position);
    }
}
