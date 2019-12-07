package com.mcc.g22

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_create_task.*
import kotlinx.android.synthetic.main.activity_create_task.bottom_nav_view
import java.text.SimpleDateFormat
import java.util.*

class CreateTaskActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var addMembers: AutoCompleteTextView
    private var usernameToUid = HashMap<String, String>()

    private var membersArrayList = ArrayList<String>()
    private lateinit var membersAdapter: ArrayAdapter<String>

    private val cal = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        nav_view.setNavigationItemSelectedListener(this)
        bottom_nav_view.setOnNavigationItemSelectedListener(this)

        addMembers = findViewById(R.id.assigned_to)

        task_status.setOnClickListener {
            val popupMenu: PopupMenu = PopupMenu(this, task_status)
            popupMenu.menuInflater.inflate(R.menu.task_status, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.ongoing ->
                        task_status.text = "Ongoing"
                    R.id.pending ->
                        task_status.text = "Pending"
                    R.id.completed ->
                        task_status.text = "Completed"
                }
                true
            })
            popupMenu.show()
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        pick_date!!.setOnClickListener {
            DatePickerDialog(this@CreateTaskActivity,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        addMembers = findViewById(R.id.add_members_complete_text_view)
        val members = mutableListOf<String>()
        var adapter: ArrayAdapter<String> = ArrayAdapter(this,
                android.R.layout.select_dialog_item,
                members)
        adapter.setNotifyOnChange(true)
        addMembers.setAdapter(adapter)
        addMembers.threshold = 3
        addMembers.addTextChangedListener {
            User.searchForUsers(addMembers.text.toString(), {
                members.clear()
                for (m in it) {
                    members.add(m.second)
                    usernameToUid[m.second] = m.first
                }
                runOnUiThread {
                    adapter = ArrayAdapter(this,
                            android.R.layout.select_dialog_item,
                            members)
                    addMembers.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                    addMembers.showDropDown()
                }
            }, {

            })
        }
        addMembers.setOnItemClickListener { parent, view, position, id ->
            membersArrayList.add( members[position] )
            membersAdapter.notifyDataSetChanged()
        }

        create_task.setOnClickListener {
            val desc = findViewById<EditText>(R.id.task_description)
            val deadline = cal.toInstant()
            val members = null
        }
    }

    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.UK)
        due_date!!.setText(sdf.format(cal.getTime()))
    }

    fun toggleDrawer(view: View){
        if(drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.getItemId()) {
            R.id.nav_profile -> {
                showProfile()
                return true
            }
            R.id.nav_logOut -> {
                logOut()
                return true
            }
            R.id.nav_home -> {
                returnHome()
            }
            R.id.nav_fav -> {
                myFavorites()
            }
            R.id.nav_add -> {
                createProject()
            }
            R.id.nav_project -> {
                allProjects()
            }
            R.id.nav_tasks -> {
                myTasks()
            }
        }
        return true
    }

    fun showProfile() {
        intent = Intent(this, EditProfileActivity::class.java)
        startActivity(intent)
    }

    fun logOut() {

    }

    fun returnHome() {
        intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
    }

    fun createProject() {
        intent = Intent(this, CreateProjectActivity::class.java)
        startActivity(intent)
    }

    fun myTasks() {
        intent = Intent(this, MyTasksActivity::class.java)
        startActivity(intent)
    }

    fun myFavorites() {
        intent = Intent(this, FavoritesActivity::class.java)
        startActivity(intent)
    }

    fun allProjects() {
        intent = Intent(this, AllProjectsActivity::class.java)
        startActivity(intent)
    }
}
