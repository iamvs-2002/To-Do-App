package com.example.to_doapp.Adapter;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_doapp.HomePage.AlarmReceiver;
import com.example.to_doapp.HomePage.MainActivity;
import com.example.to_doapp.Model.TaskModel;
import com.example.to_doapp.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<TaskModel> taskList;
    private Context context;
    private MainActivity activity;
    DocumentReference db;
    FirebaseUser user;
    private FirebaseAuth mAuth;

    public TaskAdapter(Context context, MainActivity activity){
        this.context = context;
        this.activity = activity;
    }

    @NonNull
    @Override
    public TaskAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup root, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.task_item, root, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        TaskModel taskModel = taskList.get(position);

        CheckBox taskCheckBox = holder.taskCheckBox;
        TextView taskDate = holder.taskDate;
        TextView taskDesc = holder.taskDesc;
        TextView taskTime = holder.taskTime;
        TextView taskStatus = holder.taskStatus;
        TextView taskRemTime = holder.taskRemTime;
        ImageButton taskDelete = holder.taskDelete;
        ImageButton taskEdit = holder.taskEdit;

        taskCheckBox.setText(taskModel.getName());
        taskDate.setText(taskModel.getDate());
        try {
            taskRemTime.setText(getRemTime(taskModel.getDate(), taskModel.getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        taskDesc.setText(taskModel.getDesc());
        taskTime.setText(taskModel.getTime());
        final Boolean[] status = {taskModel.getStatus()};
        taskCheckBox.setChecked(status[0]);

        try {
            if (!status[0] && checkTime(taskModel.getDate(), taskModel.getTime()))
                taskStatus.setText(R.string.upcomingTask);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        taskEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTask(position);
            }
        });

        taskDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteTask(position);
            }
        });

        taskCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                String id = taskModel.getId();
                String name = taskModel.getName();
                String desc = taskModel.getDesc();
                String date = taskModel.getDate();
                String time = taskModel.getTime();

                if(isChecked){
                    taskRemTime.setText("0");
                    boolean s = true;
                    status[0] = s;
                    taskModel.setStatus(s);
                    taskStatus.setText("");
                    updateDB(id,name,desc,time,date,s);
                }
                else{
                    String remTime = "0";
                    try {
                        remTime = getRemTime(date, time);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    taskRemTime.setText(remTime);
                    boolean s = false;
                    status[0] = s;
                    taskModel.setStatus(s);
                    try {
                        if (!status[0] && checkTime(taskModel.getDate(), taskModel.getTime())) {
                            taskStatus.setText(R.string.upcomingTask);
                            MainActivity.taskAdapter.notifyDataSetChanged();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    updateDB(id,name,desc,time,date,s);
                }
            }
        });
    }

    private String getRemTime(String date, String time) throws ParseException{
        String currentDateTime = getCurrentDateTime();

        Log.e("Diff", currentDateTime);
        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");

        String t = date+"-"+time;
        Date d1 = simpleDateFormat.parse(t);
        Date d2 = simpleDateFormat.parse(currentDateTime);

        Log.e("Diff", d1+" "+d2);

        if (d1.before(d2))
            return "0";

        long differenceInMilliSeconds
                = Math.abs(d2.getTime() - d1.getTime());

        Log.e("Diff",d1.getTime()+" "+d2.getTime());

        // Calculating the difference in Hours
        long differenceInHours = (long) Math.ceil(differenceInMilliSeconds / (60 * 60 * 1000));

        Log.e("Diff", String.valueOf(differenceInHours));

        if (differenceInHours <= 5)
            return String.valueOf(differenceInHours);
        else
            return "5+";
    }
    public boolean checkTime(String date, String time) throws ParseException {
        String currentDateTime = getCurrentDateTime();

        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");

        String t = date+"-"+time;

        Log.e("Diff", t+" "+currentDateTime);

        Date d1 = simpleDateFormat.parse(t);
        Date d2 = simpleDateFormat.parse(currentDateTime);

        if (d1.before(d2))
            return false;

        long differenceInMilliSeconds
                = Math.abs(d1.getTime() - d2.getTime());

        // Calculating the difference in Hours
        long differenceInHours = (long) Math.ceil(differenceInMilliSeconds / (60 * 60 * 1000));

        return (differenceInHours <= 5);
    }
    public static String getCurrentDateTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }

    @Override
    public int getItemCount() {
        int size = taskList.size();

        MainActivity.taskCount_tv.setText((String.valueOf(size)));

        if (size==0)
            MainActivity.animationView.setVisibility(View.VISIBLE);
        else
            MainActivity.animationView.setVisibility(View.GONE);
        return size;
    }

    public void setTasks(List<TaskModel> taskList){
        this.taskList = taskList;
        notifyDataSetChanged();
    }
    public void deleteTask(int position) {
        TaskModel taskModel = taskList.get(position);
        String id = taskModel.getId();

        cancelAlarm(taskModel.getDate(), taskModel.getTime());

        deleteItemFromDB(id);
        taskList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, taskList.size());
        notifyDataSetChanged();
    }
    private void editTask(int position) {
        TaskModel taskModel = taskList.get(position);
        String id = taskModel.getId();
        showAlertDialogForEditText(id, position);
    }
    private void showAlertDialogForEditText(String id, int position) {
        AlertDialog.Builder builder
                = new AlertDialog.Builder(context);
        View customLayout = LayoutInflater.from(context).inflate(R.layout.edittaskalertdialog, null, false);
        builder.setView(customLayout);

        TextInputEditText name,desc,date,time;
        AppCompatButton ok,cancel;
        name = customLayout.findViewById(R.id.task_edit_name);
        desc = customLayout.findViewById(R.id.task_edit_desc);
        date = customLayout.findViewById(R.id.task_edit_date);
        time = customLayout.findViewById(R.id.task_edit_time);
        ok = customLayout.findViewById(R.id.task_edit_ok);
        cancel = customLayout.findViewById(R.id.task_edit_cancel);

        name.setText(taskList.get(position).getName());
        desc.setText(taskList.get(position).getDesc());
        date.setText(taskList.get(position).getDate());
        time.setText(taskList.get(position).getTime());

        // date
        final String[] utime = new String[1];
        final String[] udate = new String[1];
        final long today = MaterialDatePicker.todayInUtcMilliseconds();
        MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText("SELECT A DATE");
        materialDateBuilder.setSelection(today);
        final MaterialDatePicker materialDatePicker = materialDateBuilder.build();
        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis((long)selection);
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                String formattedDate  = format.format(calendar.getTime());

                udate[0] = formattedDate;
                date.setText(udate[0]);
            }
        });

        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDatePicker.show(activity.getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
            }
        });

        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        if (selectedHour==0)
                            selectedHour = 24;
                        utime[0] = String.format("%02d:%02d", selectedHour, selectedMinute);
                        time.setText(utime[0]);
                    }
                }, hour, minute, true);
                // mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String t = time.getText().toString().trim();
                String n = name.getText().toString().trim();
                String des = desc.getText().toString().trim();
                String dt = date.getText().toString().trim();

                if(name.equals("") || desc.equals("") || time.equals("") || date.equals(""))
                {
                    Toast.makeText(context, "No field can be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    taskList.get(position).setName(n);
                    taskList.get(position).setDesc(des);
                    taskList.get(position).setTime(t);
                    taskList.get(position).setDate(dt);
                    updateDB(id,n,des,t,dt,taskList.get(position).getStatus());
                    notifyItemChanged(position);
                    notifyDataSetChanged();
                    dialog.dismiss();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void deleteItemFromDB(String id) {
        db = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        db.collection("tasks")
                .document(id)
                .delete();
    }
    private void cancelAlarm(String date, String time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);

        if(pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
    private void updateDB(String id, String name, String desc, String time, String date, boolean status) {
        db = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("desc", desc);
        task.put("date", date);
        task.put("time", time);
        task.put("status", status);

        db.collection("tasks") // name of the collection
                .document(id) // task id
                .update(task);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox taskCheckBox;
        TextView taskDate, taskTime, taskStatus, taskDesc, taskRemTime;
        ImageButton taskDelete, taskEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskDate = itemView.findViewById(R.id.taskDate_tv);
            taskDesc = itemView.findViewById(R.id.taskDesc_tv);
            taskRemTime = itemView.findViewById(R.id.remainingTaskTime_tv);
            taskTime = itemView.findViewById(R.id.taskTime_tv);
            taskStatus = itemView.findViewById(R.id.taskStatus_tv);
            taskDelete = itemView.findViewById(R.id.taskDeleteBtn);
            taskEdit = itemView.findViewById(R.id.taskEditBtn);
        }
    }
}

