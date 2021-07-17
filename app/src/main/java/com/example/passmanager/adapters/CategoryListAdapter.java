package com.example.passmanager.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.passmanager.R;
import com.example.passmanager.model.Category;
import com.example.passmanager.model.Entry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Adapter for populating a RecyclerView with categories, along with their entries.
 */
public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.CategoryViewHolder> {
    private final LayoutInflater inflater;

    // List of categories.
    List<Category> categories;

    // Lists of entries for each category.
    List<List<Entry>> entriesLists;

    public interface OnEntryClickListener {
        void onEntryClick(int categoryIndex, int entryIndex);
    }

    // Listener used for handling click events on the entries in the list.
    private final OnEntryClickListener onEntryClickListener;

    public CategoryListAdapter(Context context,
                        OnEntryClickListener onEntryClickListener) {
        inflater = LayoutInflater.from(context);
        this.onEntryClickListener = onEntryClickListener;
    }

    @NonNull
    @Override
    public CategoryListAdapter.CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.category_row, parent, false);
        return new CategoryListAdapter.CategoryViewHolder(itemView);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull CategoryListAdapter.CategoryViewHolder holder, int position) {
        if (categories != null) {
            Category current = categories.get(position);
            holder.categoryName.setText(current.name);
            try {
                // Check that the icon is still accessible.
                InputStream inputStream =
                        holder.itemView.
                                getContext().getContentResolver().openInputStream(current.icon);
                inputStream.close();
            } catch (IOException e) {
                current.icon = null; // TODO: Set icon to null in the database, too.
            }
            holder.categoryIcon.setImageURI(current.icon);

            if (entriesLists != null) {
                holder.numberOfEntries.setText(String.format("(%d)",
                        entriesLists.get(position).size()));
                // Setup the recycler view and it's adapter for entries in this category.
                RecyclerView entriesRv = holder.itemView.findViewById(R.id.entriesRecyclerView);
                Context context = entriesRv.getContext();
                entriesRv.setLayoutManager(new LinearLayoutManager(context));
                EntryListAdapter adapter = new EntryListAdapter(context, holder);
                adapter.setEntries(entriesLists.get(position));
                entriesRv.setAdapter(adapter);
            }
        } else {
            holder.numberOfEntries.setText("(0)");
        }
    }

    public void setEntriesLists(List<List<Entry>> entriesLists) {
        this.entriesLists = entriesLists;
        notifyDataSetChanged();
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public List<Category> getEntries() {
        return categories;
    }

    @Override
    public int getItemCount() {
        if (categories != null) { return categories.size(); }
        return 0;
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            EntryListAdapter.OnEntryClickListener {
        private final TextView categoryName;
        private final ImageView categoryIcon;
        private final ImageView categoryArrow;
        private final TextView numberOfEntries;

        private CategoryViewHolder(View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryNameTextView);
            categoryIcon = itemView.findViewById(R.id.categoryIconImageView);
            categoryArrow = itemView.findViewById(R.id.categoryArrow);
            numberOfEntries = itemView.findViewById(R.id.numberOfEntriesTextView);
            itemView.setOnClickListener(this);
        }

        /**
         * Called when a category is clicked. Expands/closes the layout which contains the
         * RecyclerView with entries from that category.
         */
        @Override
        public void onClick(View v) {
            View layout = v.findViewById(R.id.expandableLayout);
            boolean visible = layout.getVisibility() == View.VISIBLE;
            if (visible) {
                layout.setVisibility(View.GONE);
                categoryArrow.setImageResource(R.drawable.ic_baseline_arrow_down_24);
            } else {
                layout.setVisibility(View.VISIBLE);
                categoryArrow.setImageResource(R.drawable.ic_baseline_arrow_up_24);
            }
        }

        /**
         * Called when an entry from this category has been clicked.
         * @param entryIndex index in the list of entries for this category.
         */
        @Override
        public void onEntryClick(int entryIndex) {
            if (onEntryClickListener != null) {
                onEntryClickListener.onEntryClick(getAdapterPosition(), entryIndex);
            }
        }
    }
}
