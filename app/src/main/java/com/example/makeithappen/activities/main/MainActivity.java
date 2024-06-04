package com.example.makeithappen.activities.main;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.View;

import com.example.makeithappen.R;
import com.example.makeithappen.activities.AddTaskActivity;
import com.example.makeithappen.activities.settings.CategorySettingsActivity;
import com.example.makeithappen.databinding.ActivityMainBinding;
import com.example.makeithappen.models.Task;
import com.example.makeithappen.models.TaskCategory;
import com.example.makeithappen.utils.DBHelper;
import com.example.makeithappen.utils.NotificationScheduler;
import com.example.makeithappen.utils.TaskNotification;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private DBHelper dbHelper;
    private ArrayList<Task> tasks;
    private TaskAdapter taskAdapter;
    private ArrayList<TaskCategory> categoryList;
    private ArrayAdapter<TaskCategory> categoryAdapter;
    private long selectedCategory;
    private MenuItem showCompletedTasksMenuItem;
    private boolean showCompletedTasks;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private int notificationOption;
    private int newNotificationOption;
    private NotificationScheduler notificationScheduler;
    private final int[] notificationOptions = new int[] {300000, 900000, 1800000, 3600000, 86400000};
    public final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    public final int POST_NOTIFICATIONS_REQUEST_CODE = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        createNotificationChannel();
        notificationScheduler = new NotificationScheduler(getApplicationContext());

        sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        showCompletedTasks = sharedPreferences.getBoolean("showCompletedTasks", true);
        notificationOption = sharedPreferences.getInt("notification", 1);

        binding.searchView.clearFocus();
        binding.searchView.setOnQueryTextListener(getOnQueryTextListener());
        binding.fab.setOnClickListener(fabOnClickListener);

        dbHelper = new DBHelper(MainActivity.this);
        tasks = new ArrayList<>();
        selectedCategory = 0;

        taskAdapter = new TaskAdapter(MainActivity.this, tasks);
        taskAdapter.setShowCompletedTask(showCompletedTasks);
        taskAdapter.setNotificationOption(notificationOption);
        binding.taskRecyclerView.setAdapter(taskAdapter);
        binding.taskRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        categoryList = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<TaskCategory>(this, android.R.layout.simple_spinner_dropdown_item, categoryList);
        getCategories();

        binding.toolbarSpinner.setAdapter(categoryAdapter);
        binding.toolbarSpinner.setOnItemSelectedListener(categoryOnClickListener);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("selectedCategory", selectedCategory);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedCategory = savedInstanceState.getLong("selectedCategory");
        if(selectedCategory != 0)
            getTasks(showCompletedTasks);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCategories();
        getTasks(showCompletedTasks);
        String text = binding.searchView.getQuery().toString();
        if(text.length() > 0)
            filterTasks(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        showCompletedTasksMenuItem = menu.getItem(0);
        if(showCompletedTasks)
            showCompletedTasksMenuItem.setTitle(R.string.hideCompletedTasks);
        else
            showCompletedTasksMenuItem.setTitle(R.string.showCompletedTasks);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(showCompletedTasks)
            showCompletedTasksMenuItem.setTitle(R.string.hideCompletedTasks);
        else
            showCompletedTasksMenuItem.setTitle(R.string.showCompletedTasks);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.showCompletedTasksAction) {
            showCompletedTasks = !showCompletedTasks;
            taskAdapter.setShowCompletedTask(showCompletedTasks);
            getTasks(showCompletedTasks);
            editor.putBoolean("showCompletedTasks", showCompletedTasks);
            editor.apply();
            String text = binding.searchView.getQuery().toString();
            if(text.length() > 0)
                filterTasks(text);
        }
        else if(id == R.id.deleteAllTasks) {
            for(Task task : tasks) {
                if(task.getNotification() != 0)
                    notificationScheduler.cancelNotification(task);
            }
            taskAdapter.deleteAllTasks();
            dbHelper.deleteAllTasks();
            binding.noTasksText.setVisibility(View.VISIBLE);
        }
        else if(id == R.id.categorySettings) {
            Intent intent = new Intent(MainActivity.this, CategorySettingsActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.notificationAction) {
            showNotificationDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    protected View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            intent.putExtra("time", notificationOption);
            intent.putExtra("categoryID", selectedCategory);
            startActivity(intent);
        }
    };

    @NonNull
    private SearchView.OnQueryTextListener getOnQueryTextListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTasks(newText);
                return true;
            }
        };
    }

    private void getTasks(boolean getDoneTasks) {
        Cursor cursor;
        if(selectedCategory == 0)
            cursor = dbHelper.getAllTasks(getDoneTasks);
        else
            cursor = dbHelper.getTasksByCategory(selectedCategory, getDoneTasks);

        tasks.clear();
        if(cursor.getCount() == 0) {
            binding.noTasksText.setVisibility(View.VISIBLE);
        }
        else {
            while(cursor.moveToNext()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.dateFormat), Locale.getDefault());
                Date taskCreationDate;
                Date taskCompletionDate;

                try {
                    taskCreationDate = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CREATION_TIME)));
                    taskCompletionDate = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_COMPLETION_TIME)));
                }
                catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                tasks.add(new Task(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID)),
                                   cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE)),
                                   cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CATEGORY)),
                                   cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DESCRIPTION)),
                                   taskCreationDate,
                                   taskCompletionDate,
                                   cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NOTIFICATION)),
                                   cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_STATUS)) > 0,
                                   cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ATTACHMENT))));
            }
            binding.noTasksText.setVisibility(View.GONE);
        }
        taskAdapter.setFilteredTasks(tasks);
        taskAdapter.setNoTasksTextView(binding.noTasksText);
    }

    private void getCategories() {
        categoryAdapter.clear();
        categoryAdapter.add(new TaskCategory(0L, getString(R.string.allTasks)));
        Cursor cursor = dbHelper.getAllCategories();
        if(cursor.getCount() > 0) {
            while(cursor.moveToNext()) {
                long categoryID = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                categoryAdapter.add(new TaskCategory(categoryID, categoryName));
            }
        }
    }

    protected AdapterView.OnItemSelectedListener categoryOnClickListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            selectedCategory = categoryList.get(binding.toolbarSpinner.getSelectedItemPosition()).getId();;
            getTasks(showCompletedTasks);
            String text = binding.searchView.getQuery().toString();
            if(text.length() > 0)
                filterTasks(text);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    private void filterTasks(String text) {
        ArrayList<Task> filteredTasks = new ArrayList<>();
        for(Task task : tasks)
            if(task.getTitle().toLowerCase().contains(text.toLowerCase()))
                filteredTasks.add(task);

        taskAdapter.setFilteredTasks(filteredTasks);
        if(filteredTasks.isEmpty())
            binding.noTasksText.setVisibility(View.VISIBLE);
        else {
            binding.noTasksText.setVisibility(View.GONE);
        }
    }

    private void showNotificationDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        String[] notificationSettings = getResources().getStringArray(R.array.notificationOptions);
        alertDialog.setSingleChoiceItems(notificationSettings, notificationOption, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                newNotificationOption = i;
            }
        });
        alertDialog.setTitle(R.string.notifications);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(notificationOption != newNotificationOption) {
                    notificationOption = newNotificationOption;
                    taskAdapter.setNotificationOption(notificationOption);
                    editor.putInt("notification", notificationOption);
                    editor.apply();
                    updateNotifications();
                }
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    private void updateNotifications() {
        Cursor cursor;
        cursor = dbHelper.getAllTasks(true);
        if(cursor.getCount() > 0) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.dateFormat), Locale.getDefault());
            while(cursor.moveToNext()) {
                int notificationID = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NOTIFICATION));
                boolean status = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_STATUS)) > 0;
                Date taskCreationDate;
                Date taskCompletionDate;
                try {
                    taskCreationDate = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CREATION_TIME)));
                    taskCompletionDate = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_COMPLETION_TIME)));
                }
                catch(ParseException e) {
                    throw new RuntimeException(e);
                }

                if(notificationID != 0 && !status && Objects.requireNonNull(taskCompletionDate).compareTo(currentDate) > 0) {
                    Task task = new Task(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID)),
                                         cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE)),
                                         cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CATEGORY)),
                                         cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DESCRIPTION)),
                                         taskCreationDate, taskCompletionDate,
                                         cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NOTIFICATION)),
                                    false, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ATTACHMENT)));
                    notificationScheduler.scheduleNotification(task, notificationOptions[notificationOption]);
                }
            }
        }
    }

    private void createNotificationChannel() {
        String channelName = "Task Notification Channel";
        String channelDescription = "Notification about upcoming tasks";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel notificationChannel = new NotificationChannel(TaskNotification.channelID, channelName, importance);
        notificationChannel.setDescription(channelDescription);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        if(!notificationManager.areNotificationsEnabled()) {
            requestPermissions(new String[]{POST_NOTIFICATIONS}, POST_NOTIFICATIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, R.string.notificationsWillNotBeSent, Toast.LENGTH_LONG).show();
            }
        }
    }

}