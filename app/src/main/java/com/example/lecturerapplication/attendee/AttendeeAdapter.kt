package com.example.lecturerapplication.attendee

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.lecturerapplication.CommunicationActivity
import com.example.lecturerapplication.R
import com.example.lecturerapplication.models.Student

class AttendeeAdapter(private val students: List<Student> = listOf()) : RecyclerView.Adapter<AttendeeAdapter.ViewHolder>() {

    // Mutable list to track the attendee list dynamically
    private val attendeeList: MutableList<Student> = students.toMutableList()

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val studentIDTextView: TextView = itemView.findViewById(R.id.studentIDTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = attendeeList[position]

        // Bind the student information to the view holder
        holder.studentIDTextView.text = student.studentID
        holder.nameTextView.text = student.name
    }

    override fun getItemCount(): Int {
        return attendeeList.size
    }


    // Method to add a single new student and notify the adapter
    fun addStudent(newStudent: Student) {
        attendeeList.add(newStudent)
        notifyItemInserted(attendeeList.size - 1) // Notify that an item is inserted at the last position
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newStudentsList: Collection<Student>) {
        attendeeList.clear()
        attendeeList.addAll(newStudentsList)
        notifyDataSetChanged()
    }
}
