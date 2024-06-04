package com.example.makeithappen.activities.settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.makeithappen.R;
import com.example.makeithappen.databinding.ActivityCategorySettingsBinding;
import com.example.makeithappen.models.TaskCategory;
import com.example.makeithappen.utils.DBHelper;
import com.example.makeithappen.utils.EditTextAlertDialogHelper;

import java.util.ArrayList;

public class CategorySettingsActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private ActivityCategorySettingsBinding binding;
    private ArrayList<TaskCategory> categories;
    private CategoryAdapter categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategorySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.categoryToolbar);

        dbHelper = new DBHelper(CategorySettingsActivity.this);
        categories = new ArrayList<>();
        getCategories();
        categoryAdapter = new CategoryAdapter(CategorySettingsActivity.this, categories);
        binding.categoryRecyclerView.setAdapter(categoryAdapter);
        binding.categoryRecyclerView.setLayoutManager(new LinearLayoutManager(CategorySettingsActivity.this));

    }

    private void getCategories() {
        Cursor cursor = dbHelper.getAllCategories();
        if(cursor.getCount() > 0) {
            while(cursor.moveToNext()) {
                long categoryID = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                categories.add(new TaskCategory(categoryID, categoryName));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.addCategoryButton) {
            showAddCategoryDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAddCategoryDialog() {
        EditTextAlertDialogHelper alertDialogHelper = new EditTextAlertDialogHelper(this);
        AlertDialog alertDialog = alertDialogHelper.showAlertDialog(getString(R.string.newCategory));
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = (EditText) alertDialog.findViewById(R.id.inputEditText);
                String categoryName = editText.getText().toString().trim();
                for(TaskCategory taskCategory : categories) {
                    if(taskCategory.getName().equals(categoryName)) {
                        Toast.makeText(CategorySettingsActivity.this, R.string.categoryExists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                long categoryID = dbHelper.insertCategory(categoryName);
                if(categoryID != -1) {
                    categories.add(new TaskCategory(categoryID, categoryName));
                    int count = categoryAdapter.getItemCount();
                    categoryAdapter.notifyItemInserted(count);
                }
                alertDialog.dismiss();
            }
        });

    }

}