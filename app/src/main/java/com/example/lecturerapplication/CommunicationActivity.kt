package com.example.lecturerapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lecturerapplication.attendee.AttendeeAdapter
import com.example.lecturerapplication.chatlist.ChatListAdapter
import com.example.lecturerapplication.models.ContentModel
import com.example.lecturerapplication.models.Student
import com.example.lecturerapplication.network.Client
import com.example.lecturerapplication.network.NetworkMessageInterface
import com.example.lecturerapplication.network.Server
import com.example.lecturerapplication.peerlist.PeerListAdapter
import com.example.lecturerapplication.peerlist.PeerListAdapterInterface
import com.example.lecturerapplication.wifidirect.WifiDirectInterface
import com.example.lecturerapplication.wifidirect.WifiDirectManager

class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, PeerListAdapterInterface, NetworkMessageInterface {

    private var wfdManager: WifiDirectManager? = null
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var peerListAdapter: PeerListAdapter? = null
    private var chatListAdapter: ChatListAdapter? = null
    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var hasDevices = false
    private var server: Server? = null
    private var client: Client? = null
    private var deviceIp: String = ""
    private var attendeeAdapter: AttendeeAdapter? = null
    private var studentList: MutableList<Student> = mutableListOf()
    private var currentPeerIp: String? = null // Stores the IP address of the selected peer



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize WiFi Direct
        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)

        // Set up Peer List
        peerListAdapter = PeerListAdapter(this)
        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.adapter = peerListAdapter
        rvPeerList.layoutManager = LinearLayoutManager(this)

        // Set up Chat List
        chatListAdapter = ChatListAdapter()
        val rvChatList: RecyclerView = findViewById(R.id.rvChat)
        rvChatList.adapter = chatListAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)

        // Set up Attendee List
        attendeeAdapter = AttendeeAdapter(getStudentsData())
        val rvStudentList: RecyclerView = findViewById(R.id.rvStudents)
        rvStudentList.adapter = attendeeAdapter
        rvStudentList.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        wfdManager?.let {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.let {
            unregisterReceiver(it)
        }
    }

    // Called when starting the class (creating a group)
    fun startClass(view: View) {
        wfdManager?.createGroup()
        updateUI()
    }

    // Called when ending the class (disconnecting the group)
    fun endClass(view: View) {
        wfdManager?.disconnect()
    }

    private fun updateUI() {
        val wfdAdapterErrorView: ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView: ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val wfdConnectedView: ConstraintLayout = findViewById(R.id.clHasConnection)
        wfdConnectedView.visibility = if (wfdHasConnection) View.VISIBLE else View.GONE

        // Display SSID and password if connected
        if (wfdHasConnection) {
            val ssidTextView: TextView = findViewById(R.id.tvNetworkSSID)
            val passwordTextView: TextView = findViewById(R.id.tvNetworkPassword)

            val ssid = "WiFi Direct SSID: ${wfdManager?.groupInfo?.networkName}"
            val password = "Password: ${wfdManager?.groupInfo?.passphrase}"

            ssidTextView.text = ssid
            passwordTextView.text = password
        }
    }


    fun sendMessage(view: View) {
        val etMessage: EditText = findViewById(R.id.etMessage)
        val etString = etMessage.text.toString().trim()
        if (etString.isEmpty()) return

        val content = ContentModel(etString, deviceIp)
        etMessage.text.clear()

        if (wfdHasConnection && server != null) {
            // If a specific peer is selected, send the message only to that peer
            currentPeerIp?.let { peerIp ->
                server?.sendMessageToClient(peerIp, content)
            } ?: run {
                // Otherwise, broadcast the message to all clients
                server?.broadcastMessage(content)
            }
        } else if (client != null) {
            client?.sendMessage(content)
        }

        // Add the message to the local chat list
        chatListAdapter?.addItemToEnd(content)
    }






    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        hasDevices = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        Toast.makeText(this, "Nearby WiFi Direct devices updated.", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?) {
        wfdHasConnection = groupInfo != null
        if (groupInfo == null) {
            server?.close()
            client?.close()
        } else {
            if (groupInfo.isGroupOwner && server == null) {
                // Pass the current studentList when starting the server
                server = Server(this)
                deviceIp = "192.168.49.1"
            } else if (!groupInfo.isGroupOwner && client == null) {
                client = Client(this)
                deviceIp = client?.ip ?: ""
            }

            // Append new students to the attendee list
            if (groupInfo.clientList?.isNotEmpty() == true) {
                val newStudentsList = groupInfo.clientList.map { device ->
                    Student(device.deviceAddress, device.deviceName)
                }
                // Add new students to the studentList
                studentList.addAll(newStudentsList)

                attendeeAdapter?.updateList(studentList)  // Update with the full student list
            }
        }
        updateUI()
    }




    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        Toast.makeText(this, "Device status updated.", Toast.LENGTH_SHORT).show()
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        wfdManager?.connectToPeer(peer)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onContent(content: ContentModel) {
        runOnUiThread {
            // Add the chat message to the chat list
            chatListAdapter?.addItemToEnd(content)

            // Log the received message for debugging
            Log.d("ChatInterface", "Received message: ${content.message}")

            // Check for a message with Student ID
            val studentIdPart = content.message.takeIf { it.contains("Student ID:") }

            if (studentIdPart != null) {
                // Extract the student ID from the message
                val studentId = content.message.split(": ")[1].trim() // Added trim() to clean up the ID

                // Retrieve the student's name based on the ID (if needed)
                val student = studentList.find { it.studentID == studentId }
                val studentName = student?.name ?: "Unknown Student" // Use a default if not found

                // Create a new Student object using the student ID as the name
                val newStudent = Student(studentId, studentId) // Use studentId for both ID and name

                // Add the new student to the attendee list
                attendeeAdapter?.addStudent(newStudent)

                // Notify the adapter to update the list view
                attendeeAdapter?.notifyDataSetChanged()
            } else {
                // Handle normal chat messages (e.g., "hi")
                Log.d("ChatInterface", "Received normal message: ${content.message}")
                // You might want to handle/display these messages differently
            }

            // Ensure the UI is updated (if needed)
            updateUI()
        }
    }





    private fun getStudentsData(): List<Student> {
        return listOf(
            Student("123456789", "Bobby Bob"),
            Student("123412341", "Jon Ton")
        )
    }

}
