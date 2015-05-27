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

import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gcm.demo.R;
import com.google.android.gcm.demo.service.LoggingService.Logger;

import java.util.LinkedHashMap;

/**
 * Drawer menu for the app
 */
public class MainMenu {
    private final MainActivity mActivity;
    private LinkedHashMap<CharSequence, Class<? extends Fragment>> mMenu;

    public MainMenu(MainActivity a) {
        mActivity = a;
        mMenu = new LinkedHashMap<>();
        mMenu.put(a.getText(R.string.main_menu_instanceid), InstanceIdFragment.class);
        mMenu.put(a.getText(R.string.main_menu_downstream), DownstreamFragment.class);
        mMenu.put(a.getText(R.string.main_menu_upstream), UpstreamFragment.class);
        mMenu.put(a.getText(R.string.main_menu_notification), NotificationFragment.class);
        mMenu.put(a.getText(R.string.main_menu_groups), GroupsFragment.class);
        mMenu.put(a.getText(R.string.main_menu_topics), TopicsFragment.class);
        mMenu.put(a.getText(R.string.main_menu_network_scheduler), NetworkSchedulerFragment.class);
    }

    public CharSequence[] getEntries() {
        return mMenu.keySet().toArray(new CharSequence[mMenu.size()]);
    }

    public Fragment createFragment(int position)
            throws InstantiationException, IllegalAccessException {
        return mMenu.get(getEntries()[position]).newInstance();
    }

    public boolean onOverflowMenuItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toggle_logs: {
                CharSequence showLogs = mActivity.getString(R.string.show_logs);
                if (showLogs.equals(item.getTitle())) {
                    mActivity.toggleLogsView(true);
                    item.setTitle(R.string.hide_logs);
                    item.setIcon(R.drawable.visibility_off_white);
                } else {
                    mActivity.toggleLogsView(false);
                    item.setTitle(R.string.show_logs);
                    item.setIcon(R.drawable.visibility_white);
                }
                return true;
            }
            case R.id.clear_logs: {
                (new Logger(mActivity)).clearLogs();
                return true;
            }
            default:
                return false;
        }
    }
}
