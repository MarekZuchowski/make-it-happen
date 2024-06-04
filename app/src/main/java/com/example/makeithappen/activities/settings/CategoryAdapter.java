package com.example.makeithappen.activities.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.makeithappen.R;
import com.example.makeithappen.databinding.CategoryRowBinding;
import com.example.makeithappen.models.TaskCategory;
import com.example.makeithappen.utils.DBHelper;
import com.example.makeithappen.utils.EditTextAlertDialogHelper;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private Context context;
    private ArrayList<TaskCategory> categories;

    public CategoryAdapter(Context context, ArrayList<TaskCategory> categories) {
        this.context = context;
        this.categories = categories;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CategoryRowBinding binding = CategoryRowBinding.inflate(LayoutInflater.from(context), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(position == 0) {
            holder.binding.editCategoryNameButton.setVisibility(View.GONE);
            holder.binding.deleteCategoryButton.setVisibility(View.GONE);
        }
        else {
            holder.binding.editCategoryNameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showEditCategoryNameDialog(holder);
                }
            });
            holder.binding.deleteCategoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteCategoryDialog(holder);
                }
            });
        }

        holder.binding.categoryNameTextView.setText(categories.get(position).getName());

    }

    private void showEditCategoryNameDialog(ViewHolder holder) {
        EditTextAlertDialogHelper alertDialogHelper = new EditTextAlertDialogHelper(context);
        AlertDialog alertDialog = alertDialogHelper.showAlertDialog(context.getString(R.string.editCategory));
        EditText input = alertDialog.findViewById(R.id.inputEditText);
        input.setText(categories.get(holder.getBindingAdapterPosition()).getName());

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TaskCategory category = categories.get(holder.getBindingAdapterPosition());
                String newCategoryName = input.getText().toString().trim();
                if(category.getName().equals(newCategoryName)) {
                    alertDialog.dismiss();
                    return;
                }
                for(TaskCategory taskCategory : categories) {
                    if(taskCategory.getName().equals(newCategoryName)) {
                        Toast.makeText(context, R.string.categoryExists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                DBHelper dbHelper = new DBHelper(context);
                category.setName(newCategoryName);
                dbHelper.updateCategory(category);
                notifyItemChanged(holder.getBindingAdapterPosition());
                alertDialog.dismiss();
            }
        });
    }

    private void showDeleteCategoryDialog(ViewHolder holder) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.areYouSure)
                .setMessage(R.string.deleteCategoryDescription)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DBHelper dbHelper = new DBHelper(context);
                        long categoryID = categories.get(holder.getBindingAdapterPosition()).getId();
                        dbHelper.deleteCategory(categoryID);
                        categories.remove(holder.getBindingAdapterPosition());
                        notifyItemRemoved(holder.getBindingAdapterPosition());
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private CategoryRowBinding binding;

        public ViewHolder(CategoryRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }

}
