package com.example.makeithappen.activities;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.makeithappen.R;
import com.example.makeithappen.databinding.ActivityAddTaskBinding;
import com.example.makeithappen.models.Task;
import com.example.makeithappen.models.TaskCategory;
import com.example.makeithappen.utils.DBHelper;
import com.example.makeithappen.utils.EditTextAlertDialogHelper;
import com.example.makeithappen.utils.MediaHelper;
import com.example.makeithappen.utils.NotificationScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class AddTaskActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private ActivityAddTaskBinding binding;
    final Calendar myCalendar = Calendar.getInstance();
    private ActivityResultLauncher<String[]> mGetContent;
    private int notificationOption;
    private final int[] notificationOptions = new int[] {300000, 900000, 1800000, 3600000, 86400000};
    private NotificationScheduler notificationScheduler;
    private String attachmentUri = null;
    private ArrayList<TaskCategory> categoryList;
    private ArrayAdapter<TaskCategory> categoryAdapter;
    private long preselectedCategoryID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        notificationScheduler = new NotificationScheduler(getApplicationContext());
        if(getIntent().hasExtra("time"))
            notificationOption = getIntent().getIntExtra("time", 1);
        if(getIntent().hasExtra("categoryID")) {
            long id = getIntent().getLongExtra("categoryID", 1);
            if(id != 0)
                preselectedCategoryID = id;
        }

        setSupportActionBar(binding.toolbar);
        myCalendar.set(Calendar.HOUR_OF_DAY, myCalendar.get(Calendar.HOUR_OF_DAY) + 1);
        myCalendar.set(Calendar.MINUTE, 0);
        updateCompletionDateLabel();

        binding.taskNameInput.addTextChangedListener(getTaskNameWatcher());
        binding.filenameText.setOnClickListener(filenameOnClickListener);
        binding.selectedPhoto.setOnClickListener(selectedPhotoOnClickListener);
        binding.addTaskLayout.setOnClickListener(layoutOnClickListener);
        mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(), activityResultCallback);
        binding.deleteAttachmentButton.setOnClickListener(deleteAttachmentButtonOnClickListener);
        TimePickerDialog.OnTimeSetListener time = getOnTimeSetListener();
        DatePickerDialog.OnDateSetListener date = getOnDateSetListener(time);
        binding.completionDateInput.setOnClickListener(getCompletionDateOnClickListener(AddTaskActivity.this, myCalendar, date));
        binding.addTaskButton.setOnClickListener(addTaskButtonOnClickListener);
        binding.addCategoryButton.setOnClickListener(getAddCategoryButtonOnClickListener(AddTaskActivity.this));
        binding.cancelButton.setOnClickListener(cancelButtonOnClickListener);

        dbHelper = new DBHelper(AddTaskActivity.this);
        categoryList = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<TaskCategory>(this, android.R.layout.simple_spinner_dropdown_item, categoryList);
        getCategories();

        binding.categorySpinner.setAdapter(categoryAdapter);

        int i;
        for(i = 0; i < categoryList.size(); ++i)
            if(categoryList.get(i).getId() == preselectedCategoryID)
                break;
        binding.categorySpinner.setSelection(i);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("attachmentUri", attachmentUri);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        attachmentUri = savedInstanceState.getString("attachmentUri");
        if(attachmentUri != null) {
            loadAttachment(Uri.parse(attachmentUri));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_add, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.attachFile) {
            mGetContent.launch(new String[]{"*/*"});
        }

        return super.onOptionsItemSelected(item);
    }

    private void getCategories() {
        Cursor cursor = dbHelper.getAllCategories();
        if(cursor.getCount() > 0) {
            while(cursor.moveToNext()) {
                long categoryID = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                categoryAdapter.add(new TaskCategory(categoryID, categoryName));
            }
        }
    }

    @NonNull
    private static View.OnClickListener getCompletionDateOnClickListener(Context context, Calendar myCalendar, DatePickerDialog.OnDateSetListener date) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(context, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        };
    }

    protected View.OnClickListener cancelButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };

    protected View.OnClickListener deleteAttachmentButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            binding.attachmentLayout.setVisibility(View.GONE);
            attachmentUri = null;
            binding.selectedPhoto.setImageDrawable(null);
        }
    };

    protected ActivityResultCallback<Uri> activityResultCallback = new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            if(result != null) {
                getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                attachmentUri = result.toString();
                loadAttachment(result);
            }
        }
    };

    @NonNull
    private View.OnClickListener getAddCategoryButtonOnClickListener(Context context) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditTextAlertDialogHelper alertDialogHelper = new EditTextAlertDialogHelper(context);
                AlertDialog alertDialog = alertDialogHelper.showAlertDialog(getString(R.string.newCategory));
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText editText = (EditText) alertDialog.findViewById(R.id.inputEditText);
                        String categoryName = editText.getText().toString().trim();
                        for(TaskCategory taskCategory : categoryList) {
                            if(taskCategory.getName().equals(categoryName)) {
                                Toast.makeText(AddTaskActivity.this, R.string.categoryExists, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        long categoryID = dbHelper.insertCategory(categoryName);
                        if(categoryID != -1)
                            categoryAdapter.add(new TaskCategory(categoryID, categoryName));
                        binding.categorySpinner.setSelection(categoryList.size() - 1);
                        alertDialog.dismiss();
                    }
                });
            }
        };
    }

    protected View.OnClickListener addTaskButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            myCalendar.set(Calendar.SECOND, 0);
            int notificationID = 0;
            if(binding.notificationSwitch.isChecked()) {
                do {
                    notificationID = new Random().nextInt();
                } while(notificationID == 0);
            }
            long categoryID = categoryList.get(binding.categorySpinner.getSelectedItemPosition()).getId();
            Date creationDate = new Date();
            long taskID = dbHelper.insertTask(binding.taskNameInput.getText().toString().trim(), categoryID,
                    binding.descriptionInput.getText().toString().trim(), creationDate, myCalendar.getTime(),
                    notificationID, attachmentUri);
            Task task = new Task(taskID, binding.taskNameInput.getText().toString().trim(), categoryID,
                    binding.descriptionInput.getText().toString().trim(), creationDate, myCalendar.getTime(),
                    notificationID, false, attachmentUri);
            if(binding.notificationSwitch.isChecked())
                notificationScheduler.scheduleNotification(task, notificationOptions[notificationOption]);
            finish();
        }
    };

    @NonNull
    private DatePickerDialog.OnDateSetListener getOnDateSetListener(TimePickerDialog.OnTimeSetListener time) {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                updateCompletionDateLabel();
                new TimePickerDialog(AddTaskActivity.this, time, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(AddTaskActivity.this)).show();
            }
        };
    }

    @NonNull
    private TimePickerDialog.OnTimeSetListener getOnTimeSetListener() {
        return new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                myCalendar.set(Calendar.HOUR_OF_DAY, i);
                myCalendar.set(Calendar.MINUTE, i1);
                updateCompletionDateLabel();
            }
        };
    }

    protected View.OnClickListener layoutOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    };

    protected View.OnClickListener selectedPhotoOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (attachmentUri != null) {
                Uri uri = Uri.parse(attachmentUri);
                String type = getContentResolver().getType(uri);
                if (type.startsWith("image") || type.startsWith("video")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, getContentResolver().getType(uri));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(AddTaskActivity.this, "No suitable application!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    protected View.OnClickListener filenameOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (attachmentUri != null) {
                Uri uri = Uri.parse(attachmentUri);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, getContentResolver().getType(uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(intent);
                }
                catch(ActivityNotFoundException e) {
                    Toast.makeText(AddTaskActivity.this, "No suitable application!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @NonNull
    private TextWatcher getTaskNameWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() == 0) {
                    binding.addTaskButton.setEnabled(false);
                    binding.addTaskButton.setTextColor(getColor(R.color._dark_gray));
                } else {
                    binding.addTaskButton.setEnabled(true);
                    binding.addTaskButton.setTextColor(getColor(R.color._blue));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
    }

    private void updateCompletionDateLabel() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.displayedDateFormat), Locale.getDefault());
        binding.completionDateInput.setText(dateFormat.format(myCalendar.getTime()));
    }

    private void loadAttachment(Uri uri) {
        binding.attachmentLayout.setVisibility(View.VISIBLE);
        String filename = MediaHelper.getFileName(AddTaskActivity.this, uri);
        binding.filenameText.setText(filename);
        String type = getContentResolver().getType(uri);
        if(type.startsWith("image")) {
            binding.selectedPhoto.setImageURI(uri);
            binding.selectedPhoto.setVisibility(View.VISIBLE);
        }
        else if(type.startsWith("video")) {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(getApplicationContext(), uri);
            Bitmap thumbnail = mediaMetadataRetriever.getFrameAtTime();
            binding.selectedPhoto.setImageBitmap(thumbnail);
            binding.selectedPhoto.setVisibility(View.VISIBLE);
        }
        else {
            binding.selectedPhoto.setImageDrawable(null);
            binding.selectedPhoto.setVisibility(View.GONE);
        }
    }

}