package com.mcc.g22

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.icu.text.CaseMap
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.*
import kotlin.random.Random

class NotificationsService : Service() {
    private lateinit var assignChangedListener: ChildEventListener
    private lateinit var projectsChangedListener: ChildEventListener
    private lateinit var binder: IBinder
    private lateinit var database: DatabaseReference
    private lateinit var notificationHelper: NotificationHelper

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Get name of the user to watch
        val usernameToWatch = intent!!.extras!!.get(EXTRA_USERNAME_TO_WATCH) as String

        // Get reference to the database
        database = FirebaseDatabase.getInstance().reference

        // Prepare to show notifications
        var importance = 3 // Importance default
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importance = NotificationManager.IMPORTANCE_DEFAULT
        }
        notificationHelper = NotificationHelper(applicationContext,
            getString(R.string.channel_name),
            getString(R.string.channel_description),
            importance)

        // Listen for adding to a new project
        projectsChangedListener = object : ChildEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "projectsChanged:onCancelled", databaseError.toException())
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, p1: String?) {
                // Left empty intentionally
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, p1: String?) {
                // Left empty intentionally
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, p1: String?) {

                // Value must be true
                if (!(dataSnapshot.value as Boolean)) return
                showAddedToProjectNotification(usernameToWatch, dataSnapshot.key as String)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // Left empty intentionally
            }
        }
        database.child("users")
                .child(usernameToWatch)
                .child("projects")
                .addChildEventListener(projectsChangedListener)

        // Listen to be assign to a new task
        assignChangedListener = object : ChildEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "assignChanged:onCancelled", databaseError.toException())
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, p1: String?) {
                // Left empty intentionally
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, p1: String?) {
                // Left empty intentionally
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, p1: String?) {

                // Value must be true
                if (!(dataSnapshot.value as Boolean)) return
                showAssignedToTaskNotification(usernameToWatch, dataSnapshot.key as String)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // Left empty intentionally
            }
        }
        database.child("users")
            .child(usernameToWatch)
            .child("tasks")
            .addChildEventListener(assignChangedListener)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        database.removeEventListener(projectsChangedListener)
        database.removeEventListener(assignChangedListener)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    /**
     * Show notification to inform the user that they are added to the new project
     * @param username
     * @param projectId ID of the project in Firebase Realtime Database
     */
    private fun showAddedToProjectNotification(username: String, projectId: String) {
        database.child("projects").child(projectId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "gettingProjectName:onCancelled", databaseError.toException())
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val notificationTitle = getString(R.string.added_to_project_notif_title)
                    val projectName = dataSnapshot.value as String
                    val notificationText = username + ", " +
                                getString(R.string.added_to_project_notif_text) + " " + projectName

                    // TODO add proper icon
                    // TODO add proper onTapAction
                    notificationHelper.showNotification(notificationTitle, notificationText)
                }
            })
    }

    /**
     * Show notification to inform the user that they are assigned to the task
     * @param username
     * @param taskId ID of assigned task in Firebase Realtime Database
     */
    private fun showAssignedToTaskNotification(username: String, taskId: String) {
        database.child("tasks").child(taskId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "gettingTaskName:onCancelled", databaseError.toException())
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dp in dataSnapshot.children) {
                        val notificationTitle = getString(R.string.assigned_to_task_notif_title)
                        val taskName = dp.key as String
                        val notificationText = username + ", " +
                                getString(R.string.assigned_to_task_notif_text) + " " + taskName

                        // TODO add proper icon
                        // TODO add proper onTapAction
                        notificationHelper.showNotification(notificationTitle, notificationText)
                    }
                }
            }
        )
    }

    companion object {
        private const val EXTRA_USERNAME_TO_WATCH: String = "USERNAME_TO_WATCH"
        private const val TAG: String = "MCC"

        /**
         * Start service so it could show notifications to the user
         * @param ctx application context
         * @param username
         */
        fun startNotificationService(ctx: Context, username: String) {
            val i = Intent(ctx, NotificationsService::class.java)
            i.putExtra(EXTRA_USERNAME_TO_WATCH, username)
            ctx.startService(i)
        }
    }
}