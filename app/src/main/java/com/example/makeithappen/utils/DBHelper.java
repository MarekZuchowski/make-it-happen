package com.example.makeithappen.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.makeithappen.R;
import com.example.makeithappen.models.Task;
import com.example.makeithappen.models.TaskCategory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "ToDoList.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TASK_TABLE_NAME = "task";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CREATION_TIME = "creation_time";
    public static final String COLUMN_COMPLETION_TIME = "completion_time";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_NOTIFICATION = "notification";
    public static final String COLUMN_CATEGORY = "category_id";
    public static final String COLUMN_ATTACHMENT = "attachment";

    public static final String CATEGORY_TABLE_NAME = "category";
    public static final String COLUMN_NAME = "name";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String categoryTableQuery = "CREATE TABLE " + CATEGORY_TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT);";

        String defaultCategoryName = context.getString(R.string.defaultCategoryName);
        String insertDefaultCategory = "INSERT INTO " + CATEGORY_TABLE_NAME + "(" + COLUMN_NAME + ") VALUES(\"" + defaultCategoryName + "\");";

        String taskTableQuery = "CREATE TABLE " + TASK_TABLE_NAME + " (" +
                       COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                       COLUMN_TITLE + " TEXT, " + COLUMN_DESCRIPTION + " TEXT, " + COLUMN_CREATION_TIME + " DATE, " +
                       COLUMN_COMPLETION_TIME + " DATE, " + COLUMN_STATUS + " BOOLEAN, " + COLUMN_NOTIFICATION + " INTEGER, " +
                       COLUMN_CATEGORY + " TEXT REFERENCES " + CATEGORY_TABLE_NAME + "(" + COLUMN_ID + "), " +
                       COLUMN_ATTACHMENT + " TEXT);";

        db.execSQL(categoryTableQuery);
        db.execSQL(insertDefaultCategory);
        db.execSQL(taskTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CATEGORY_TABLE_NAME);
        onCreate(db);
    }

    public long insertTask(String title, long category, String description, Date creationDate, Date completionTime, int notification, String attachment) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CATEGORY, category);
        contentValues.put(COLUMN_DESCRIPTION, description);
        SimpleDateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.dateFormat), Locale.getDefault());
        contentValues.put(COLUMN_CREATION_TIME, dateFormat.format(creationDate));
        contentValues.put(COLUMN_COMPLETION_TIME, dateFormat.format(completionTime));
        contentValues.put(COLUMN_NOTIFICATION, notification);
        contentValues.put(COLUMN_STATUS, false);
        contentValues.put(COLUMN_ATTACHMENT, attachment);

        long taskID = db.insert(TASK_TABLE_NAME, null, contentValues);
        if(taskID == -1) {
            Toast.makeText(context, "Failed to add task!", Toast.LENGTH_SHORT).show();
        }

        return taskID;
    }


    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, name);

        long id = db.insert(CATEGORY_TABLE_NAME, null, contentValues);
        if(id == -1) {
            Toast.makeText(context, "Failed to add category!", Toast.LENGTH_SHORT).show();
        }

        return id;
    }

    public Cursor getAllTasks(boolean getCompletedTasks) {
        String query;
        if(getCompletedTasks)
            query = "SELECT * FROM " + TASK_TABLE_NAME + " ORDER BY " + COLUMN_COMPLETION_TIME;
        else
            query = "SELECT * FROM " + TASK_TABLE_NAME + " WHERE " + COLUMN_STATUS + " = 0 ORDER BY " + COLUMN_COMPLETION_TIME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if(db != null) {
            cursor =  db.rawQuery(query, null);
        }
        return cursor;
    }

    public Cursor getAllCategories() {
        String query = "SELECT " + COLUMN_ID + ", " + COLUMN_NAME + " FROM " + CATEGORY_TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if(db != null) {
            cursor =  db.rawQuery(query, null);
        }
        return cursor;
    }

    public Cursor getTasksByCategory(long category, boolean getCompletedTasks) {
        String query;
        if(getCompletedTasks)
            query = "SELECT * FROM " + TASK_TABLE_NAME + " WHERE " + COLUMN_CATEGORY  + " = \"" + category + "\" ORDER BY " + COLUMN_COMPLETION_TIME;
        else
            query = "SELECT * FROM " + TASK_TABLE_NAME + " WHERE " + COLUMN_CATEGORY  + " = \"" + category + "\" AND " + COLUMN_STATUS + " = 0 ORDER BY " + COLUMN_COMPLETION_TIME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if(db != null) {
            cursor =  db.rawQuery(query, null);
        }
        return cursor;
    }

    public void updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, task.getTitle());
        contentValues.put(COLUMN_CATEGORY, task.getCategory());
        contentValues.put(COLUMN_DESCRIPTION, task.getDescription());
        SimpleDateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.dateFormat), Locale.getDefault());
        contentValues.put(COLUMN_COMPLETION_TIME, dateFormat.format(task.getCompletionDate()));
        contentValues.put(COLUMN_NOTIFICATION, task.getNotification());
        contentValues.put(COLUMN_STATUS, task.isStatus());
        contentValues.put(COLUMN_ATTACHMENT, task.getAttachment());

        long result = db.update(TASK_TABLE_NAME, contentValues, "id = ?", new String[]{String.valueOf(task.getId())});
        if(result == -1)
            Toast.makeText(context, "Failed to update!", Toast.LENGTH_SHORT).show();
    }

    public void updateCategory(TaskCategory category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ID, category.getId());
        contentValues.put(COLUMN_NAME, category.getName());
        db.update(CATEGORY_TABLE_NAME, contentValues, " " + COLUMN_ID + " = ?", new String[]{String.valueOf(category.getId())});
    }

    public void deleteTask(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(TASK_TABLE_NAME, " id = ?", new String[]{String.valueOf(taskId)});
        if(result == -1) {
            Toast.makeText(context, "Failed to delete task!", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteAllTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(TASK_TABLE_NAME, null, null);
        if(result == -1) {
            Toast.makeText(context, "Failed to delete task!", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteCategory(long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TASK_TABLE_NAME, " " + COLUMN_CATEGORY + " = ?", new String[]{String.valueOf(categoryId)});
        db.delete(CATEGORY_TABLE_NAME, " " + COLUMN_ID + " = ?", new String[]{String.valueOf(categoryId)});
    }
}
