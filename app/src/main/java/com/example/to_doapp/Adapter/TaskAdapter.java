package com.example.to_doapp.Adapter;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
        ImageButton taskDelete = holder.taskDelete;

        taskCheckBox.setText(taskModel.getName());
        taskDate.setText(taskModel.getDate());
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
                    boolean s = true;
                    status[0] = s;
                    taskModel.setStatus(s);
                    taskStatus.setText("");
                    updateDB(id,name,desc,time,date,s);
                }
                else{
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

    public boolean checkTime(String date, String time) throws ParseException {
        String currentDateTime = getCurrentDateTime();
        String[] s = currentDateTime.split("-");
        String cdate = s[0].trim();
        String ctime = s[1].trim();

        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("dd.mm.yyyy");
        Date d1 = simpleDateFormat.parse(cdate);
        Date d2 = simpleDateFormat.parse(date);

        if(d1.after(d2)) {
            return false;
        }
        else {
            SimpleDateFormat simpleDateFormathr
                    = new SimpleDateFormat("HH:mm");

            // Parsing the Time Period
            Date date1 = simpleDateFormathr.parse(ctime);
            Date date2 = simpleDateFormathr.parse(time);

            if(date1.after(date2))
                return false;

            // Calculating the difference in milliseconds
            long differenceInMilliSeconds
                    = Math.abs(date2.getTime() - date1.getTime());

            // Calculating the difference in Hours
            long differenceInHours
                    = (differenceInMilliSeconds / (60 * 60 * 1000))
                    % 24;

            return (differenceInHours <= 1);
        }
    }

    public static String getCurrentDateTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }

    @Override
    public int getItemCount() {
        int size = taskList.size();
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
        TextView taskDate, taskTime, taskStatus, taskDesc;
        ImageButton taskDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskDate = itemView.findViewById(R.id.taskDate_tv);
            taskDesc = itemView.findViewById(R.id.taskDesc_tv);
            taskTime = itemView.findViewById(R.id.taskTime_tv);
            taskStatus = itemView.findViewById(R.id.taskStatus_tv);
            taskDelete = itemView.findViewById(R.id.taskDeleteBtn);
        }
    }
}

