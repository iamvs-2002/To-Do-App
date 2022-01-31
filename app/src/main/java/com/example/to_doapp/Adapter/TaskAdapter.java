package com.example.to_doapp.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_doapp.HomePage.AddNewTask;
import com.example.to_doapp.HomePage.MainActivity;
import com.example.to_doapp.Model.TaskModel;
import com.example.to_doapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
        TextView taskTime = holder.taskTime;
        TextView taskStatus = holder.taskStatus;
        ImageButton taskDelete = holder.taskDelete;

        taskCheckBox.setText(taskModel.getName());
        taskDate.setText(taskModel.getDate());
        taskTime.setText(taskModel.getTime());
        Boolean status = taskModel.getStatus();
        taskCheckBox.setChecked(status);

        try {
            if (!status && checkTime(taskModel.getDate(), taskModel.getTime()))
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
                String date = taskModel.getDate();
                String time = taskModel.getTime();

                if(isChecked){
                    boolean status = true;
                    updateDB(id,name,time,date,status);
                }
                else{
                    boolean status = false;
                    updateDB(id,name,time,date,status);
                }
            }
        });
    }

    public boolean checkTime(String date, String time) throws ParseException {
        String currentDateTime = getCurrentDateTime();
        String[] s = currentDateTime.split("-");
        String cdate = s[0].trim();
        String ctime = s[1].trim();

        // Toast.makeText(context, ctime+" "+time, Toast.LENGTH_SHORT).show();

        if(date.equals(cdate)){
            SimpleDateFormat simpleDateFormat
                    = new SimpleDateFormat("HH:mm");

            // Parsing the Time Period
            Date date1 = simpleDateFormat.parse(ctime);
            Date date2 = simpleDateFormat.parse(time);

            if(date1.after(date2))
                return false;

            // Calculating the difference in milliseconds
            long differenceInMilliSeconds
                    = Math.abs(date2.getTime() - date1.getTime());

            // Calculating the difference in Hours
            long differenceInHours
                    = (differenceInMilliSeconds / (60 * 60 * 1000))
                    % 24;
            Toast.makeText(context, String.valueOf(differenceInHours), Toast.LENGTH_SHORT).show();

            return (differenceInHours <= 1);
        }

        return false;
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
    private void updateDB(String id, String name, String time, String date, boolean status) {
        db = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("date", date);
        task.put("time", time);
        task.put("status", status);

        db.collection("tasks") // name of the collection
                .document(id) // task id
                .update(task)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(context, "Task Added Successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox taskCheckBox;
        TextView taskDate, taskTime, taskStatus;
        ImageButton taskDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskDate = itemView.findViewById(R.id.taskDate_tv);
            taskTime = itemView.findViewById(R.id.taskTime_tv);
            taskStatus = itemView.findViewById(R.id.taskStatus_tv);
            taskDelete = itemView.findViewById(R.id.taskDeleteBtn);
        }
    }
}

