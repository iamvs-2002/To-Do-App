package com.example.to_doapp.HomePage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.to_doapp.Adapter.TaskAdapter;
import com.example.to_doapp.Model.TaskModel;
import com.example.to_doapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static DocumentReference db;
    RecyclerView taskRecyclerView;
    public static TaskAdapter taskAdapter;
    public static List<TaskModel> taskList;
    ExtendedFloatingActionButton floatingActionButton;
    static FirebaseUser user;
    private FirebaseAuth mAuth;
    public static LottieAnimationView animationView;
    SwipeRefreshLayout swipeRefreshLayout;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        taskList = new ArrayList<>();
        db = FirebaseFirestore.getInstance().collection("users").document(user.getUid());

        // RecyclerView initialization
        taskRecyclerView = findViewById(R.id.task_recyclerView);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, MainActivity.this);
        taskRecyclerView.setAdapter(taskAdapter);

        floatingActionButton = findViewById(R.id.addTask_fab);
        animationView = findViewById(R.id.lottie_empty);

        taskList = addTasks();
        Collections.reverse(taskList);
        taskAdapter.setTasks(taskList);
        taskAdapter.notifyDataSetChanged();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Add the tasks again, so as to update
                // The "UPCOMING" status for each task
                taskList = addTasks();
                Collections.reverse(taskList);
                taskAdapter.setTasks(taskList);
                taskAdapter.notifyDataSetChanged();
                taskRecyclerView.setAdapter(taskAdapter);

                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // To add a new Task
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AddNewTask().show(getSupportFragmentManager(), AddNewTask.TAG);
            }
        });
    }

    // Channel to receieve alarm/updates for the tasks
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "taskReminder";
            String desc = "Channel to receive alarm for task";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("task", name, importance);
            channel.setDescription(desc);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Add the tasks to the List, to be shown in RecyclerView
    // By fetching the tasks from DataBase
    public static List<TaskModel> addTasks() {
        if(taskList!=null)
            taskList.clear();
        db.collection("tasks")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // task.getResult() returns a list of documents
                            // belonging to the collection = "tasks"
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = (String) document .get("id");
                                String name = (String) document .get("name");
                                String desc = (String) document .get("desc");
                                String date = (String) document .get("date");
                                String time = (String) document .get("time");
                                Boolean status = (Boolean) document .get("status");

                                TaskModel t = new TaskModel(id, name, desc, status, date, time);
                                taskList.add(t);
                                taskAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

        if (taskList.isEmpty())
            animationView.setVisibility(View.VISIBLE);
        else
            animationView.setVisibility(View.GONE);

        return taskList;
    }
}