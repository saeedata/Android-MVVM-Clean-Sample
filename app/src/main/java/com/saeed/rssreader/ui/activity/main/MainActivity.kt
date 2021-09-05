package com.saeed.rssreader.ui.activity.main

import android.R.attr.mimeType
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.saeed.rssreader.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btn_create).setOnClickListener {
           val uri= createFileTextFileInDocuments() ?: return@setOnClickListener


            contentResolver.openOutputStream(uri).use {
                it?.write("saeed".toByteArray())
                it?.close()

            }
        }
    }



    fun createFileTextFileInDocuments():Uri?{
        val resolver: ContentResolver = contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "saeed")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)

        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun createFileTextFileInDownloads():Uri?{
        val resolver: ContentResolver = contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "saeed")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)

        return resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
    }
}