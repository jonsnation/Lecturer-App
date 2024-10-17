package com.example.lecturerapplication.models

/// The [ContentModel] class represents data that is transferred between devices when multiple
/// devices communicate with each other.

//Do i have to change this class to reflect attendees?
// do i neeed a reciever?
data class ContentModel(var message: String, var senderIp: String)
