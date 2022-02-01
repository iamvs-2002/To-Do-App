package com.example.to_doapp.Adapter;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_doapp.HomePage.AlarmReceiver;
import com.example.to_doapp.HomePage.MainActivity;
import com.example.to_doapp.Model.TaskModel;
import com.example.to_doapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        taskDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteItem(position);
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


        long differenceInMilliSeconds
                = Math.abs(d2.getTime() - d1.getTime());

        Log.e("Diff",d1.getTime()+" "+d2.getTime());

        // Calculating the difference in Hours
        long differenceInHours = (differenceInMilliSeconds / (60 * 60 * 1000));

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
        Date d1 = simpleDateFormat.parse(t);
        Date d2 = simpleDateFormat.parse(currentDateTime);

        long differenceInMilliSeconds
                = Math.abs(d1.getTime() - d2.getTime());

        // Calculating the difference in Hours
        long differenceInHours
                = (differenceInMilliSeconds / (60 * 60 * 1000));

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

    public void deleteItem(int position) {
        TaskModel taskModel = taskList.get(position);
        String id = taskModel.getId();

        cancelAlarm(taskModel.getDate(), taskModel.getTime());

        deleteItemFromDB(id);
        taskList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, taskList.size());
        notifyDataSetChanged();
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
        ImageButton taskDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskDate = itemView.findViewById(R.id.taskDate_tv);
            taskDesc = itemView.findViewById(R.id.taskDesc_tv);
            taskRemTime = itemView.findViewById(R.id.remainingTaskTime_tv);
            taskTime = itemView.findViewById(R.id.taskTime_tv);
            taskStatus = itemView.findViewById(R.id.taskStatus_tv);
            taskDelete = itemView.findViewById(R.id.taskDeleteBtn);
        }
    }
}

