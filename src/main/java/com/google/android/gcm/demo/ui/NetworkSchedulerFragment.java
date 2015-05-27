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
package com.google.android.gcm.demo.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.CardView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.model.TaskCollection;
import com.google.android.gcm.demo.model.TaskTracker;
import com.google.android.gcm.demo.service.TaskSchedulerService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

/**
 * Fragment for scheduling tasks using {@link GcmNetworkManager}.
 * Scheduled tasks are displayed and can be cancelled and deleted.
 */
public class NetworkSchedulerFragment extends AbstractFragment
        implements View.OnClickListener, MainActivity.RefreshableFragment {

    private static final int TYPE_ONE_OFF = 0;
    private static final int TYPE_PERIODIC = 1;

    private GcmNetworkManager mScheduler;
    private Handler mHandler = new Handler();
    private volatile boolean mRunning;

    private TaskCollection mTasks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.fragment_network_scheduler, container, false);
        view.findViewById(R.id.scheduler_about_apis).setOnClickListener(this);
        view.findViewById(R.id.scheduler_add_oneoff).setOnClickListener(this);
        view.findViewById(R.id.scheduler_add_periodic).setOnClickListener(this);

        mScheduler = GcmNetworkManager.getInstance(getActivity());

        mTasks = TaskCollection.getInstance(getActivity());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mRunning = true;
        refresh();
        scheduleUiUpdate();
    }

    private void scheduleUiUpdate() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mRunning) {
                    refresh();
                    scheduleUiUpdate();
                }
            }
        }, 5000);
    }

    @Override
    public void onStop() {
        mRunning = false;
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scheduler_about_apis:
                toggleAboutApi();
                break;
            case R.id.scheduler_add_oneoff:
                showDialog(TYPE_ONE_OFF);
                break;
            case R.id.scheduler_add_periodic:
                showDialog(TYPE_PERIODIC);
                break;
        }

    }

    private void showDialog(final int type) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        @SuppressLint("InflateParams") // the layout is used inside a dialog so no parent is needed
                LinearLayout layout = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.widget_schedule_task_dialog, null);
        if (type == TYPE_ONE_OFF) {
            alertBuilder.setTitle(R.string.scheduler_add_oneoff);
            alertBuilder.setMessage(R.string.scheduler_add_oneoff_message);
            alertBuilder.setPositiveButton(R.string.scheduler_add_oneoff, null);
            ((TextView) layout.findViewById(R.id.task_param1_title))
                    .setText(R.string.scheduler_run_from);
            ((TextView) layout.findViewById(R.id.task_param2_title))
                    .setText(R.string.scheduler_run_by);
        } else {
            alertBuilder.setTitle(R.string.scheduler_add_periodic);
            alertBuilder.setMessage(R.string.scheduler_add_periodic_message);
            alertBuilder.setPositiveButton(R.string.scheduler_add_periodic, null);
            ((TextView) layout.findViewById(R.id.task_param1_title))
                    .setText(R.string.scheduler_period);
            ((TextView) layout.findViewById(R.id.task_param2_title))
                    .setText(R.string.scheduler_flex);
        }
        final EditText param1View = (EditText) layout.findViewById(R.id.task_param1);
        final EditText param2View = (EditText) layout.findViewById(R.id.task_param2);
        final RadioGroup connectivityView =
                (RadioGroup) layout.findViewById(R.id.task_connectivity_group);
        final CheckBox chargingView =
                (CheckBox) layout.findViewById(R.id.task_charging_checkbox);
        final CheckBox persistenceView =
                (CheckBox) layout.findViewById(R.id.task_persistence_checkbox);
        alertBuilder.setView(layout);
        alertBuilder.setNegativeButton(R.string.scheduler_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        // Show the dialog
        final AlertDialog dialog = alertBuilder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        // schedule the task
                        final long value1;
                        final long value2;
                        try {
                            value1 = Long.parseLong(param1View.getText().toString());
                            value2 = Long.parseLong(param2View.getText().toString());
                        } catch (NumberFormatException e) {
                            Toast.makeText(getActivity(),
                                    getString(R.string.scheduler_error_nan),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int connectivity = Task.NETWORK_STATE_CONNECTED;
                        switch (connectivityView.getCheckedRadioButtonId()) {
                            case R.id.task_connectivity_connected:
                                connectivity = Task.NETWORK_STATE_CONNECTED;
                                break;
                            case R.id.task_connectivity_unmetered:
                                connectivity = Task.NETWORK_STATE_UNMETERED;
                                break;
                            case R.id.task_connectivity_any:
                                connectivity = Task.NETWORK_STATE_ANY;
                                break;
                        }
                        boolean charging = chargingView.isChecked();
                        boolean persistence = persistenceView.isChecked();
                        if (type == TYPE_ONE_OFF) {
                            addOneOff(value1, value2, connectivity, charging);
                        } else {
                            addPeriodic(value1, value2, connectivity, charging, persistence);
                        }
                        refresh();
                        dialog.dismiss();
                    }
                });
    }

    private void addPeriodic(long periodSecs, long flexSecs, int connectivity,
                             boolean charging, boolean persistence) {
        if (flexSecs > periodSecs) {
            Toast.makeText(getActivity(),
                    getString(R.string.scheduler_error_flex),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String tag = Long.toString(SystemClock.currentThreadTimeMillis());
        final TaskTracker taskTracker = TaskTracker.createPeriodic(tag, periodSecs, flexSecs);
        PeriodicTask periodic = new PeriodicTask.Builder()
                .setService(TaskSchedulerService.class)
                .setPeriod(periodSecs)
                .setFlex(flexSecs)
                .setTag(tag)
                .setRequiredNetwork(connectivity)
                .setRequiresCharging(charging)
                .setPersisted(persistence)
                .build();
        mScheduler.schedule(periodic);
        mTasks.updateTask(taskTracker);
    }

    private void addOneOff(long winStartSecs, long winEndSecs, int connectivity, boolean charging) {
        if (winStartSecs > winEndSecs) {
            Toast.makeText(getActivity(),
                    getString(R.string.scheduler_error_window),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String tag = Long.toString(SystemClock.currentThreadTimeMillis());
        final long elapsedNowSeconds = SystemClock.elapsedRealtime() / 1000;
        final TaskTracker taskTracker = TaskTracker.createOneoff(tag,
                elapsedNowSeconds + winStartSecs, elapsedNowSeconds + winEndSecs);
        OneoffTask oneOff = new OneoffTask.Builder()
                .setService(TaskSchedulerService.class)
                .setTag(tag)
                .setExecutionWindow(winStartSecs, winEndSecs)
                .setRequiredNetwork(connectivity)
                        // Persistence not yet support for Oneoffs.
                .setRequiresCharging(charging)
                .build();
        mScheduler.schedule(oneOff);
        mTasks.updateTask(taskTracker);
    }

    @Override
    public void refresh() {
        FrameLayout tasksView = (FrameLayout) getActivity().findViewById(R.id.scheduler_tasks);
        // the view might have been destroyed, in which case we don't do anything
        if (tasksView != null) {
            float density = getActivity().getResources().getDisplayMetrics().density;
            SimpleArrayMap<String, TaskTracker> tasks = mTasks.getTasks();
            LinearLayout tasksList = new LinearLayout(getActivity());
            tasksList.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < tasks.size(); i++) {
                final TaskTracker task = tasks.valueAt(i);
                CardView taskCard = (CardView) getActivity().getLayoutInflater()
                        .inflate(R.layout.widget_task, tasksList, false);
                ImageView taskIcon = (ImageView) taskCard.findViewById(R.id.task_icon);
                taskIcon.setImageResource(R.drawable.check_circle_grey600);
                taskIcon.setPadding(0, 0, (int) (8 * density), 0);
                TextView taskLabel = (TextView) taskCard.findViewById(R.id.task_title);
                TextView taskParams = (TextView) taskCard.findViewById(R.id.task_params);
                if (task.period == 0) {
                    taskLabel.setText(getString(R.string.scheduler_oneoff, task.tag));
                    taskParams.setText(getString(R.string.scheduler_oneoff_params,
                            task.windowStartElapsedSecs, task.windowStopElapsedSecs));
                } else {
                    taskLabel.setText(getString(R.string.scheduler_periodic, task.tag));
                    taskParams.setText(getString(R.string.scheduler_periodic_params,
                            task.period, task.flex));
                }
                TextView taskCreatedAt = (TextView) taskCard.findViewById(R.id.task_created_at);
                taskCreatedAt.setText(getString(R.string.scheduler_secs_ago,
                        DateUtils.formatElapsedTime(
                                SystemClock.elapsedRealtime() / 1000 - task.createdAtElapsedSecs)));
                TextView lastExecuted = (TextView) taskCard.findViewById(R.id.task_last_exec);
                if (task.executionTimes.isEmpty()) {
                    lastExecuted.setText(getString(R.string.scheduler_na));
                } else {
                    long lastExecTime = task.executionTimes.get(task.executionTimes.size() - 1);
                    lastExecuted.setText(getString(R.string.scheduler_secs_ago,
                            DateUtils.formatElapsedTime(
                                    SystemClock.elapsedRealtime() / 1000 - lastExecTime)));
                }
                TextView state = (TextView) taskCard.findViewById(R.id.task_state);
                if (task.isCancelled()) {
                    state.setText(getString(R.string.scheduler_cancelled));
                } else if (task.isExecuted()) {
                    state.setText(getString(R.string.scheduler_executed));
                } else {
                    state.setText(getString(R.string.scheduler_pending));
                }
                Button cancel = (Button) taskCard.findViewById(R.id.task_cancel);
                cancel.setVisibility(View.VISIBLE);
                cancel.setText(R.string.scheduler_cancel);
                Button delete = (Button) taskCard.findViewById(R.id.task_delete);
                delete.setVisibility(View.VISIBLE);
                delete.setText(R.string.scheduler_delete);
                if (!task.isCancelled() && (!task.isExecuted() || task.period != 0)) {
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            cancelTask(task.tag);
                            refresh();
                        }
                    });
                    cancel.setEnabled(true);
                    delete.setEnabled(false);
                } else {
                    cancel.setEnabled(false);
                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mTasks.deleteTask(task.tag);
                            refresh();
                        }
                    });
                    delete.setEnabled(true);
                }
                tasksList.addView(taskCard);
            }
            tasksView.removeAllViews();
            tasksView.addView(tasksList);
        }
    }

    private void cancelTask(String tag) {
        mScheduler.cancelTask(tag, TaskSchedulerService.class);
        TaskTracker task = mTasks.getTask(tag);
        if (task != null) {
            task.cancel();
            mTasks.updateTask(task);
        }
        refresh();
    }

    private void toggleAboutApi() {
        toggleText((TextView) getActivity().findViewById(R.id.scheduler_about_apis),
                R.string.scheduler_about_apis, R.string.scheduler_about_apis_open);
        toggleVisibility(getActivity().findViewById(R.id.scheduler_about_apis_full_text));
    }

}
