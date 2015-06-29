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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.google.android.gcm.demo.R;

import java.util.ArrayList;

/**
 * This class implements a custom view for displaying and editing a list of key-value pairs.
 * It allows adding, removing and editing the the key-value pairs. Duplicate and empty keys are
 * permitted.
 */
public class EditableMapView extends LinearLayout implements View.OnClickListener {

    private TableLayout mTable;

    public EditableMapView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_editable_map, this, true);

        mTable = (TableLayout) getChildAt(2);
        getChildAt(3).setOnClickListener(this);

    }

    public EditableMapView(Context context) {
        this(context, null);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.entries = getMapEntries();

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.entries != null) {
            for (MapEntry entry : ss.entries) {
                String key = entry.key;
                String value = entry.value;
                addDataRow(key, value);
            }
        }
    }

    private void addDataRow(String key, String value) {
        Context context = getContext();
        TableRow tableRow = new TableRow(context);
        EditText keyText = new EditText(context);
        EditText valueText = new EditText(context);
        Button button = new Button(context);
        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs);
        int selectableBackgroundResource = typedArray.getResourceId(0, 0);
        button.setBackgroundResource(selectableBackgroundResource);

        keyText.setHint(context.getString(R.string.data_hint_key));
        if (key != null) {
            keyText.setText(key);
        }
        valueText.setHint(context.getString(R.string.data_hint_value));
        if (value != null) {
            valueText.setText(value);
        }
        button.setText(context.getString(R.string.data_delete));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TableRow tableRow = (TableRow) view.getParent();
                mTable.removeView(tableRow);
            }
        });
        tableRow.addView(keyText, new TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.45f));
        tableRow.addView(valueText, new TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.45f));
        tableRow.addView(button, new TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.1f));

        mTable.addView(tableRow, new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1.0f));
    }

    public ArrayList<MapEntry> getMapEntries() {
        ArrayList<MapEntry> mapEntries = new ArrayList<>();
        int numRows = mTable.getChildCount();
        for (int index = 0; index < numRows; index++) {
            TableRow row = (TableRow) mTable.getChildAt(index);
            EditText keyText = (EditText) row.getChildAt(0);
            EditText valueText = (EditText) row.getChildAt(1);
            mapEntries.add(new MapEntry(keyText.getText().toString(), valueText.getText()
                    .toString()));
        }
        return mapEntries;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.data_add:
                addDataRow(null, null);
                break;
        }
    }

    static class SavedState extends BaseSavedState {
        ArrayList<MapEntry> entries;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            int size = in.readInt();
            entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                entries.add((MapEntry) in.readParcelable(MapEntry.class.getClassLoader()));
            }

        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(entries.size());
            for (MapEntry entry : entries) {
                out.writeParcelable(entry, flags);
            }
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public static class MapEntry implements Parcelable {
        public String key;
        public String value;

        public static final Parcelable.Creator<MapEntry> CREATOR
                = new Parcelable.Creator<MapEntry>() {
            public MapEntry createFromParcel(Parcel in) {
                return new MapEntry(in);
            }

            public MapEntry[] newArray(int size) {
                return new MapEntry[size];
            }
        };

        public MapEntry(Parcel in) {
            this.key = in.readString();
            this.value = in.readString();
        }

        public MapEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(key);
            dest.writeString(value);
        }
    }
}
