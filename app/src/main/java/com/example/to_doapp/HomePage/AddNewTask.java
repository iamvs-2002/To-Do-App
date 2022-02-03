package com.example.to_doapp.HomePage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AddNewTask extends BottomSheetDialogFragment {
    public static final String TAG = "ActionBottomDialog";
    private TextInputEditText taskName, taskDate, taskTime, taskDesc;
    private AppCompatButton saveBtn;
    DocumentReference db;
    FirebaseUser user;
    private FirebaseAuth mAuth;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    String format;

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
        taskDesc = view.findViewById(R.id.task_input_desc);
        taskDate = view.findViewById(R.id.task_input_date);
        taskTime = view.findViewById(R.id.task_input_time);
        saveBtn = view.findViewById(R.id.task_input_save);

        final String[] utime = new String[1];
        final String[] udate = new String[1];

        final long today = MaterialDatePicker.todayInUtcMilliseconds();

        MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText("SELECT A DATE");
        materialDateBuilder.setSelection(today);
        final MaterialDatePicker materialDatePicker = materialDateBuilder.build();


        // Date Picker
        taskDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDatePicker.show(getActivity().getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
            }
        });
        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis((long)selection);
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                String formattedDate  = format.format(calendar.getTime());

                udate[0] = formattedDate;
                taskDate.setText(udate[0]);
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
                        if (selectedHour==0)
                            selectedHour = 24;
                        utime[0] = String.format("%02d:%02d", selectedHour, selectedMinute);
                        taskTime.setText(utime[0]);
                    }
                }, hour, minute, true);
                // mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        });

        // Save the new task -> Set Alarm + Add task to DataBase
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = taskName.getText().toString().trim();
                String desc = taskDesc.getText().toString().trim();
                String time = taskTime.getText().toString().trim();
                String date = taskDate.getText().toString().trim();
//                String time = utime[0];
//                String date = udate[0];

                if(name.equals("") || desc.equals("") || time.equals("") || date.equals(""))
                {
                    Toast.makeText(getContext(), "No field can be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.e("AlarmTime",date+" "+time);

                // setAlarm(name, desc, date, time);
                addToDB(name, desc, time, date, false);
                dismiss();
            }
        });
        return view;
    }

    // Set alarm when a task is added to the list
    private void setAlarm(String name, String desc, String date, String time) {
        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");

        String t = date+"-"+time;

        String currentDateTime = getCurrentDateTime();


        try{
            Date d1 = simpleDateFormat.parse(t);
            Date d2 = simpleDateFormat.parse(currentDateTime);

            if (d1.before(d2))
                return;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, d1.getYear());
            cal.set(Calendar.MONTH, d1.getMonth());
            cal.set(Calendar.DAY_OF_MONTH, d1.getDate());
            cal.set(Calendar.HOUR_OF_DAY, d1.getHours());
            cal.set(Calendar.MINUTE, d1.getMinutes());

        /*
        Calendar cal = Calendar.getInstance();
        //String[] d = date.split("\\.");
        //String[] t = time.split(":");

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
        */

            alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getContext(), AlarmReceiver.class);
            intent.putExtra("name", name);
            intent.putExtra("desc", desc);

            pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);

            // Toast.makeText(getContext(), "Created alarm", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
    public static String getCurrentDateTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'-'kk:mm a");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }

    // Add Task to DataBase
    private void addToDB(String name, String desc, String time, String date, boolean status) {
        db = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        String id = db.collection("tasks").document().getId();

        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("date", date);
        task.put("desc", desc);
        task.put("time", time);
        task.put("status", status);

        db.collection("tasks") // name of the collection
                .document(id)
                .set(task);

        TaskModel t = new TaskModel(id, name, desc, status, date, time);
        MainActivity.taskList.add(t);
        MainActivity.taskAdapter.notifyDataSetChanged();
        MainActivity.animationView.setVisibility(View.GONE);
    }
}
