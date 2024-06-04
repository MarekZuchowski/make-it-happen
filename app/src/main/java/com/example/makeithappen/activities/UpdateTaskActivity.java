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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.makeithappen.R;
import com.example.makeithappen.databinding.ActivityUpdateTaskBinding;
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

public class UpdateTaskActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private ActivityUpdateTaskBinding binding;
    private SharedPreferences sharedPreferences;
    private Task task;
    final Calendar myCalendar = Calendar.getInstance();
    private ActivityResultLauncher<String[]> mGetContent;
    private int notificationOption;
    private final int[] notificationOptions = new int[] {300000, 900000, 1800000, 3600000, 86400000};
    private NotificationScheduler notificationScheduler;
    private ArrayList<TaskCategory> categoryList;
    private ArrayAdapter<TaskCategory> categoryAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.updateTaskToolbar);

        notificationScheduler = new NotificationScheduler(getApplicationContext());
        sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        notificationOption = sharedPreferences.getInt("notification", 1);
        binding.taskNameUpdateInput.addTextChangedListener(getTaskNameWatcher());
        TimePickerDialog.OnTimeSetListener time = getOnTimeSetListener();
        DatePickerDialog.OnDateSetListener date = getOnDateSetListener(time);
        binding.completionDateUpdateInput.setOnClickListener(getCompletionDateOnClickListener(UpdateTaskActivity.this, myCalendar, date));
        mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(), getActivityResultCallback());
        binding.filenameTextUpdate.setOnClickListener(filenameOnClickListener);
        binding.deleteAttachmentUpdateButton.setOnClickListener(deleteAttachmentButtonOnClickListener);
        binding.selectedPhotoUpdate.setOnClickListener(selectedPhotoOnClickListener);
        binding.updateTaskButton.setOnClickListener(updateTaskButtonOnClickListener);
        binding.deleteTaskButton.setOnClickListener(deleteTaskButtonOnClickListener);
        binding.addCategoryUpdateButton.setOnClickListener(getAddCategoryButtonOnClickListener(UpdateTaskActivity.this));

        dbHelper = new DBHelper(UpdateTaskActivity.this);
        categoryList = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<TaskCategory>(this, android.R.layout.simple_spinner_dropdown_item, categoryList);
        getCategories();
        binding.categoryUpdateSpinner.setAdapter(categoryAdapter);

        getIntentData();
        myCalendar.setTime(task.getCompletionDate());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_update, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.attachFileUpdate) {
            mGetContent.launch(new String[]{"*/*"});
        }
        return super.onOptionsItemSelected(item);
    }

    void getIntentData() {
        if(getIntent().hasExtra("task")) {
            task = getIntent().getParcelableExtra("task");
            binding.taskNameUpdateInput.setText(task.getTitle());
            long categoryID = task.getCategory();
            int i;
            for(i = 0; i < categoryList.size(); ++i)
                if(categoryList.get(i).getId() == categoryID)
                    break;
            binding.categoryUpdateSpinner.setSelection(i);
            binding.descriptionUpdateInput.setText(task.getDescription());
            SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.displayedDateFormat), Locale.getDefault());
            binding.completionDateUpdateInput.setText(dateFormat.format(task.getCompletionDate()));
            binding.creationDateUpdateInput.setText(dateFormat.format(task.getCreationDate()));
            binding.notificationUpdateSwitch.setChecked(task.getNotification() != 0);
            if(task.getAttachment() != null) {
                Uri uri = Uri.parse(task.getAttachment());
                loadAttachment(uri);
            }
        }
    }

    @NonNull
    private ActivityResultCallback<Uri> getActivityResultCallback() {
        return new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                if (result != null) {
                    getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    task.setAttachment(result.toString());
                    loadAttachment(result);
                }
            }
        };
    }

    private void loadAttachment(Uri uri) {
        binding.attachmentUpdateLayout.setVisibility(View.VISIBLE);
        String filename = MediaHelper.getFileName(UpdateTaskActivity.this, uri);
        binding.filenameTextUpdate.setText(filename);
        String type = getContentResolver().getType(uri);
        if (type.startsWith("image")) {
            binding.selectedPhotoUpdate.setImageURI(uri);
            binding.selectedPhotoUpdate.setVisibility(View.VISIBLE);
        } else if (type.startsWith("video")) {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(getApplicationContext(), uri);
            Bitmap thumbnail = mediaMetadataRetriever.getFrameAtTime();
            binding.selectedPhotoUpdate.setImageBitmap(thumbnail);
            binding.selectedPhotoUpdate.setVisibility(View.VISIBLE);
        } else {
            binding.selectedPhotoUpdate.setImageDrawable(null);
            binding.selectedPhotoUpdate.setVisibility(View.GONE);
        }
    }

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
                                Toast.makeText(UpdateTaskActivity.this, R.string.categoryExists, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        long categoryID = dbHelper.insertCategory(categoryName);
                        if(categoryID != -1)
                            categoryAdapter.add(new TaskCategory(categoryID, categoryName));
                        binding.categoryUpdateSpinner.setSelection(categoryList.size() - 1);
                        alertDialog.dismiss();
                    }
                });
            }
        };
    }

    protected View.OnClickListener deleteTaskButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dbHelper.deleteTask(task.getId());
            if (task.getNotification() != 0)
                notificationScheduler.cancelNotification(task);
            finish();
        }
    };

    protected View.OnClickListener selectedPhotoOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String attachmentUri = task.getAttachment();
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
                        Toast.makeText(UpdateTaskActivity.this, "No suitable application!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    protected View.OnClickListener updateTaskButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int notificationID = task.getNotification();
            Date currentDate = new Date();
            if(binding.notificationUpdateSwitch.isChecked()) {
                if(notificationID == 0) {
                    do {
                        notificationID = new Random().nextInt();
                    } while(notificationID == 0);
                }
            }
            else {
                if(task.getNotification() != 0) {
                    notificationScheduler.cancelNotification(task);
                    notificationID = 0;
                }
            }
            task.update(
                    binding.taskNameUpdateInput.getText().toString().trim(),
                    categoryList.get(binding.categoryUpdateSpinner.getSelectedItemPosition()).getId(),
                    binding.descriptionUpdateInput.getText().toString().trim(),
                    myCalendar.getTime(),
                    notificationID,
                    task.getAttachment()
            );
            dbHelper.updateTask(task);
            if(binding.notificationUpdateSwitch.isChecked()) {
                if(myCalendar.getTime().compareTo(currentDate) > 0 && !task.isStatus())
                    notificationScheduler.scheduleNotification(task, notificationOptions[notificationOption]);
            }
            finish();
        }
    };

    protected View.OnClickListener deleteAttachmentButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            binding.attachmentUpdateLayout.setVisibility(View.GONE);
            task.setAttachment(null);
            binding.selectedPhotoUpdate.setImageDrawable(null);
        }
    };

    protected View.OnClickListener filenameOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String attachmentUri = task.getAttachment();
            if (attachmentUri != null) {
                Uri uri = Uri.parse(attachmentUri);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, getContentResolver().getType(uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(UpdateTaskActivity.this, "No suitable application!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @NonNull
    private static View.OnClickListener getCompletionDateOnClickListener(Context context, Calendar myCalendar, DatePickerDialog.OnDateSetListener date) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(context, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        };
    }

    @NonNull
    private DatePickerDialog.OnDateSetListener getOnDateSetListener(TimePickerDialog.OnTimeSetListener time) {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                updateCompletionDateLabel();
                new TimePickerDialog(UpdateTaskActivity.this, time, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(UpdateTaskActivity.this)).show();
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

    @NonNull
    private TextWatcher getTaskNameWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() == 0) {
                    binding.updateTaskButton.setEnabled(false);
                    binding.updateTaskButton.setTextColor(getColor(R.color._dark_gray));
                } else {
                    binding.updateTaskButton.setEnabled(true);
                    binding.updateTaskButton.setTextColor(getColor(R.color._blue));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
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

    private void updateCompletionDateLabel() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.displayedDateFormat), Locale.getDefault());
        binding.completionDateUpdateInput.setText(dateFormat.format(myCalendar.getTime()));
    }

}