package com.example.to_doapp.HomePage;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.to_doapp.Model.TaskModel;
import com.example.to_doapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AddNewTask extends BottomSheetDialogFragment {
    public static final String TAG = "ActionBottomDialog";
    private TextInputEditText taskName, taskDate, taskTime;
    private AppCompatButton saveBtn;
    DocumentReference db;
    FirebaseUser user;
    private FirebaseAuth mAuth;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.DialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_task_bottomsheet, container, false);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        taskName = view.findViewById(R.id.task_input_name);
        taskDate = view.findViewById(R.id.task_input_date);
        taskTime = view.findViewById(R.id.task_input_time);
        saveBtn = view.findViewById(R.id.task_input_save);

        final String[] utime = new String[1];
        final String[] udate = new String[1];

        // Date Picker
        taskDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {

                                udate[0] = (dayOfMonth + "." + (monthOfYear + 1) + "." + year);
                                taskDate.setText(udate[0]);
                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });
        // Time Picker
        taskTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        utime[0] = ( selectedHour + ":" + selectedMinute);
                        taskTime.setText(utime[0]);
                    }
                }, hour, minute, true);
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        });

        // Save the new task -> Set Alarm + Add task to DataBase
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = taskName.getText().toString().trim();
                String time = taskTime.getText().toString().trim();
                String date = taskDate.getText().toString().trim();
//                String time = utime[0];
//                String date = udate[0];

                if(name.equals("") || time.equals("") || date.equals(""))
                {
                    Toast.makeText(getContext(), "No field can be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.e("AlarmTime",date+" "+time);
                setAlarm(name, date, time);
                addToDB(name, time, date, false);
                dismiss();
            }
        });
        return view;
    }

    // Set alarm when a task is added to the list
    private void setAlarm(String name, String date, String time) {
        Calendar cal = Calendar.getInstance();
        String[] d = date.split("\\.");
        String[] t = time.split(":");

        int day = Integer.parseInt(d[0]);
        int month = Integer.parseInt(d[1]);
        int year = Integer.parseInt(d[2]);

        int hr = Integer.parseInt(t[0]);
        int min = Integer.parseInt(t[1]);

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, min);

        alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        intent.putExtra("name", name);

        pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
    }

    // Add Task to DataBase
    private void addToDB(String name, String time, String date, boolean status) {
        db = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        String id = db.collection("tasks").document().getId();

        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("date", date);
        task.put("time", time);
        task.put("status", status);

        db.collection("tasks") // name of the collection
                .document(id)
                .set(task);

        TaskModel t = new TaskModel(id, name, status, date, time);
        MainActivity.taskList.add(t);
        MainActivity.taskAdapter.notifyDataSetChanged();
        MainActivity.animationView.setVisibility(View.GONE);
    }
}
