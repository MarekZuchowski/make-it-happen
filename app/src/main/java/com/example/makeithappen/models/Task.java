package com.example.makeithappen.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;

public class Task implements Parcelable {
    private long id;
    private String title;
    private long category;
    private String description;
    private Date creationDate;
    private Date completionDate;
    private int notification;
    private boolean status;
    private String attachment;

    public Task(long id, String title, long category, String description, Date creationDate, Date completionDate, int notification, boolean status, String attachment) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.creationDate = creationDate;
        this.completionDate = completionDate;
        this.notification = notification;
        this.status = status;
        this.attachment = attachment;
    }

    protected Task(Parcel in) {
        id = in.readLong();
        title = in.readString();
        category = in.readLong();
        description = in.readString();
        creationDate = new Date(in.readLong());
        completionDate = new Date(in.readLong());
        notification = in.readInt();
        status = in.readByte() != 0;
        attachment = in.readString();
    }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) {
            return new Task(in);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    public void update(String title, long category, String description, Date completionDate, int notification, String attachment) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.completionDate = completionDate;
        this.notification = notification;
        this.attachment = attachment;
    }

    public void updateStatus(boolean status) {
        this.status = status;
    }

    public void setNotification(int notification) {
        this.notification = notification;
    }

    public long getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }

    public long getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public int getNotification() {
        return notification;
    }

    public boolean isStatus() {
        return status;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(title);
        parcel.writeLong(category);
        parcel.writeString(description);
        parcel.writeLong(creationDate.getTime());
        parcel.writeLong(completionDate.getTime());
        parcel.writeInt(notification);
        parcel.writeByte((byte) (status ? 1 : 0));
        parcel.writeString(attachment);
    }
}
