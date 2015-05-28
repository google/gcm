/*
Copyright 2015 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.google.android.gcm.demo.model;

import android.content.Context;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.android.gcm.demo.service.LoggingService;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Singleton class for storing information about tasks submitted to the
 * {@link com.google.android.gms.gcm.GcmNetworkManager}.
 * All data is stored in a file in the Internal Storage.
 */
public class TaskCollection {
    private static final String GCM_TASKS_FILE = "gcm-tasks.json";

    private static volatile TaskCollection mInstance;

    private final Context mContext;
    private final LoggingService.Logger mLogger;
    private SimpleArrayMap<String, TaskTracker> mTasks;

    private TaskCollection(Context context) {
        mContext = context;
        mLogger = new LoggingService.Logger(context);
        loadTasks();
    }

    public static TaskCollection getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TaskCollection.class) {
                if (mInstance == null) {
                    mInstance = new TaskCollection(context);
                }
            }
        }
        return mInstance;
    }

    private void loadTasks() {
        mTasks = new SimpleArrayMap<>();
        File file = new File(mContext.getFilesDir(), GCM_TASKS_FILE);
        if (!file.exists() || file.length() == 0) {
            return;
        }
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            int bytesRead = in.read(bytes);
            if (bytesRead == -1) {
                mLogger.log(Log.ERROR, "Failed to read tasks file.");
            } else {
                JSONArray tasksJson = new JSONArray(new String(bytes));
                for (int i = 0; i < tasksJson.length(); i++) {
                    TaskTracker task = TaskTracker.fromJson(tasksJson.getJSONObject(i));
                    mTasks.put(task.tag, task);
                }
            }
        } catch (IOException e) {
            mLogger.log(Log.ERROR, "Failed to read tasks file.", e);
        } catch (JSONException e) {
            mLogger.log(Log.ERROR, "Failed to deserialize tasks.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // silently ignore
                }
            }
        }
    }

    private void saveTasks() {
        File file = new File(mContext.getFilesDir(), GCM_TASKS_FILE);
        JSONArray jsonArray = new JSONArray();
        FileOutputStream out = null;
        try {
            for (int i = 0; i < mTasks.size(); i++) {
                TaskTracker task = mTasks.get(mTasks.keyAt(i));
                jsonArray.put(task.toJson());
            }
            out = new FileOutputStream(file);
            out.write(jsonArray.toString().getBytes());
        } catch (IOException e) {
            mLogger.log(Log.ERROR, "Failed to write tasks file.", e);
        } catch (JSONException e) {
            mLogger.log(Log.ERROR, "Failed to serialize tasks.", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // silently ignore
                }
            }
        }
    }

    public void clearTasks() {
        mTasks.clear();
        saveTasks();
    }

    public void updateTask(TaskTracker task) {
        mTasks.put(task.tag, task);
        saveTasks();
    }

    public void deleteTask(String tag) {
        TaskTracker task = mTasks.remove(tag);
        if (task != null) {
            saveTasks();
        }
    }

    public TaskTracker getTask(String tag) {
        return mTasks.get(tag);
    }

    public SimpleArrayMap<String, TaskTracker> getTasks() {
        return mTasks;
    }
}

