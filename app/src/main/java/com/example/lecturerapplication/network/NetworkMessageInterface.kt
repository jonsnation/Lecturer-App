package com.example.lecturerapplication.network

import com.example.lecturerapplication.models.ContentModel

/// This [NetworkMessageInterface] acts as an interface.
interface NetworkMessageInterface {
    fun onContent(content: ContentModel)
}
