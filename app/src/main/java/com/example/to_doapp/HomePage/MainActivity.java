package com.example.to_doapp.HomePage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static DocumentReference db;
    RecyclerView taskRecyclerView;
    public static TaskAdapter taskAdapter;
    public static List<TaskModel> taskList;
    AppCompatButton addnewTaskBtn;
    static FirebaseUser user;
    private FirebaseAuth mAuth;
    public static LottieAnimationView animationView;
    SwipeRefreshLayout swipeRefreshLayout;

    public static TextView taskCount_tv;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Window window = this.getWindow();
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.scheme));


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

        addnewTaskBtn = findViewById(R.id.addTask_btn);
        animationView = findViewById(R.id.lottie_empty);
        taskCount_tv = findViewById(R.id.taskCount_tv);

        taskList = addTasks();
        taskCount_tv.setText(String.valueOf(taskList.size()));
        //Collections.reverse(taskList);
        Collections.sort(taskList, new TimeComparator());
        taskAdapter.setTasks(taskList);
        taskAdapter.notifyDataSetChanged();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Add the tasks again, so as to update
                // The "UPCOMING" status for each task
                taskList = addTasks();
                //Collections.reverse(taskList);
                Collections.sort(taskList, new TimeComparator());
                taskAdapter.setTasks(taskList);
                taskAdapter.notifyDataSetChanged();
                taskRecyclerView.setAdapter(taskAdapter);

                swipeRefreshLayout.setRefreshing(false);
            }
        });
        // To add a new Task
        addnewTaskBtn.setOnClickListener(new View.OnClickListener() {
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
class TimeComparator implements Comparator {
    public int compare(Object o1, Object o2){
        TaskModel t1 = (TaskModel) o1;
        TaskModel t2 = (TaskModel) o2;

        String date1 = t1.getDate()+"-"+t1.getTime();
        String date2 = t2.getDate()+"-"+t2.getTime();

        String currentDateTime = getCurrentDateTime();

        Log.e("Diff", currentDateTime);
        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");

        try {
            Date d1 = simpleDateFormat.parse(date1);
            Date d2 = simpleDateFormat.parse(date2);

            if (d1.equals(d2))
                return 0;
            else if(d1.before(d2))
                return -1;
            else
                return 1;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return 0;
    }
    public static String getCurrentDateTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }
}