package com.saeed.rssreader.utils.helper

/*
 * Copyright (C) 2018 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.saeed.rssreader.BuildConfig
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.text.DecimalFormat
import java.util.*


object FileUtils {

        private const val DOCUMENTS_DIR = "documents"

        // configured android:authorities in AndroidManifest (https://developer.android.com/reference/android/support/v4/content/FileProvider)
        private const val AUTHORITY = BuildConfig.APPLICATION_ID + ".fileProvider"
        private const val HIDDEN_PREFIX = "."

        /**
         * TAG for log messages.
         */
        private const val DEBUG = true // Set to true to enable logging

        /**
         * File and folder comparator. TODO Expose sorting option method
         */
        var sComparator: Comparator<File> = Comparator { f1, f2 ->
                // Sort alphabetically by lower case, which is much cleaner
                f1.name
                        .lowercase(Locale.getDefault())
                        .compareTo(
                                f2.name
                                        .lowercase(Locale.getDefault())
                        )
        }

        /**
         * File (not directories) filter.
         */
        var sFileFilter: FileFilter = FileFilter { file ->
                val fileName = file.name
                // Return files only (not directories) and skip hidden files
                file.isFile && !fileName.startsWith(HIDDEN_PREFIX)
        }

        /**
         * Folder (directories) filter.
         */
        var sDirFilter: FileFilter = FileFilter { file ->
                val fileName = file.name
                // Return directories only and skip hidden directories
                file.isDirectory && !fileName.startsWith(HIDDEN_PREFIX)
        }

        val downloadsDir: File
                get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val documentsDir: File
                get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        /**
         * Gets the extension of a file name, like ".png" or ".jpg".
         *
         * @param uri
         * @return Extension including the bg_dot("."); "" if there is no extension;
         * null if uri was null.
         */
        fun getExtension(uri: String?): String? {
                if (uri == null) {
                        return null
                }

                val dot = uri.lastIndexOf(".")
                return if (dot >= 0) {
                        uri.substring(dot)
                } else {
                        // No extension.
                        ""
                }
        }

        /**
         * @return Whether the URI is a local one.
         */
        private fun isLocal(url: String?): Boolean {
                return url != null && !url.startsWith("http://") && !url.startsWith("https://")
        }

        /**
         * @return True if Uri is a MediaStore Uri.
         * @author paulburke
         */
        fun isMediaUri(uri: Uri): Boolean {
                return "media".equals(uri.authority!!, ignoreCase = true)
        }

        /**
         * Convert File into Uri.
         *
         * @param file
         * @return uri
         */
        fun getUri(file: File?): Uri? {
                return if (file != null) {
                        Uri.fromFile(file)
                } else null
        }

        fun getURL(urlString: String?): URL? {
                try {
                        return URL(urlString)
                } catch (e: MalformedURLException) {
                        e.printStackTrace()
                }

                return null
        }

        /**
         * Returns the path only (without file name).
         *
         * @param file
         * @return
         */
        fun getPathWithoutFilename(file: File?): File? {
                if (file != null) {
                        if (file.isDirectory) {
                                // no file to be split off. Return everything
                                return file
                        } else {
                                val filename = file.name
                                val filepath = file.absolutePath

                                // Construct path without file name.
                                var pathwithoutname = filepath.substring(
                                        0,
                                        filepath.length - filename.length
                                )
                                if (pathwithoutname.endsWith("/")) {
                                        pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length - 1)
                                }
                                return File(pathwithoutname)
                        }
                }
                return null
        }

        /**
         * @return The MIME type for the given file.
         */
        fun getMimeType(file: File): String? {

                val extension = getExtension(file.name)

                return if (extension!!.isNotEmpty()) MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(extension.substring(1)) else "application/octet-stream"

        }

        /**
         * @return The MIME type for the give Uri.
         */
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun getMimeType(context: Context, uri: Uri): String? {
                val file = File(getPath(context, uri))
                return getMimeType(file)
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is local.
         */
        fun isLocalStorageDocument(uri: Uri): Boolean {
                return AUTHORITY == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        fun isExternalStorageDocument(uri: Uri): Boolean {
                return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
                return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
                return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        fun isGooglePhotosUri(uri: Uri): Boolean {
                return "com.google.android.apps.photos.content" == uri.authority
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        private fun getDataColumn(
                context: Context, uri: Uri?, selection: String?,
                selectionArgs: Array<String>?
        ): String? {
                var cursor: Cursor? = null
                val column = MediaStore.Files.FileColumns.DATA
                val projection = arrayOf(column)

                try {
                        cursor = context.contentResolver
                                .query(uri!!, projection, selection, selectionArgs, null)
                        if (cursor != null && cursor.moveToFirst()) {
                                if (DEBUG)
                                        DatabaseUtils.dumpCursor(cursor)

                                val column_index = cursor.getColumnIndexOrThrow(column)
                                return cursor.getString(column_index)
                        }
                } finally {
                        cursor?.close()
                }
                return null
        }

        fun getDownloadDirectoryFileList(): Array<String>? {
                return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath).list()
        }

        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.<br></br>
         * <br></br>
         * Callers should check whether the path is local before assuming it
         * represents a local file.
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @see .isLocal
         * @see .getFile
         */
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun getPath(context: Context, uri: Uri): String? {

                if (DEBUG)
                        Timber.d(
                                "Authority: " + uri.authority +
                                        ", Fragment: " + uri.fragment +
                                        ", Port: " + uri.port +
                                        ", Query: " + uri.query +
                                        ", Scheme: " + uri.scheme +
                                        ", Host: " + uri.host +
                                        ", Segments: " + uri.pathSegments.toString()
                        )

                val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

                // DocumentProvider
                if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                        // LocalStorageProvider
                        if (isLocalStorageDocument(uri)) {
                                // The path is the id
                                return DocumentsContract.getDocumentId(uri)
                        } else if (isExternalStorageDocument(uri)) {
                                val docId = DocumentsContract.getDocumentId(uri)
                                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                val type = split[0]

                                if ("primary".equals(type, ignoreCase = true)) {
                                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                                }
                        } else if (isDownloadsDocument(uri)) {

                                val id = DocumentsContract.getDocumentId(uri)

                                if (id != null && id.startsWith("raw:")) {
                                        return id.substring(4)
                                }

                                val contentUriPrefixesToTry =
                                        arrayOf(
                                                "content://downloads/public_downloads",
                                                "content://downloads/my_downloads"
                                        )

                                for (contentUriPrefix in contentUriPrefixesToTry) {
                                        val contentUri = ContentUris.withAppendedId(
                                                Uri.parse(contentUriPrefix),
                                                java.lang.Long.valueOf(id!!)
                                        )
                                        try {
                                                val path = getDataColumn(context, contentUri, null, null)
                                                if (path != null) {
                                                        return path
                                                }
                                        } catch (e: Exception) {
                                        }

                                }

                                // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                                val fileName = getFileName(context, uri)
                                val cacheDir = getDocumentCacheDir(context)
                                val file = generateFileName(fileName, cacheDir)
                                var destinationPath: String? = null
                                if (file != null) {
                                        destinationPath = file.absolutePath
                                        saveFileFromUri(context, uri, destinationPath)
                                }

                                return destinationPath
                        } else if (isMediaDocument(uri)) {
                                val docId = DocumentsContract.getDocumentId(uri)
                                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                val type = split[0]

                                var contentUri: Uri? = null
                                when (type) {
                                        "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                        "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                        "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                }

                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])

                                return getDataColumn(context, contentUri, selection, selectionArgs)
                        }// MediaProvider
                        // DownloadsProvider
                        // ExternalStorageProvider
                } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

                        // Return the remote address
                        return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                                context,
                                uri,
                                null,
                                null
                        )

                } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
                        return uri.path
                }// File
                // MediaStore (and general)

                return null
        }

        /**
         * Convert Uri into File, if possible.
         *
         * @return file A local file that the Uri was pointing to, or null if the
         * Uri is unsupported or pointed to a remote resource.
         * @author paulburke
         * @see .getPath
         */
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun getFile(context: Context, uri: Uri?): File? {
                if (uri != null) {
                        val path = getPath(context, uri)
                        if (path != null && isLocal(path)) {
                                return File(path)
                        }
                }
                return null
        }

        /**
         * Get the file size in a human-readable string.
         *
         * @param size
         * @return
         * @author paulburke
         */
        fun getReadableFileSize(size: Long): String {
                val BYTES_IN_KILOBYTES = 1024
                val dec = DecimalFormat("###.#")
                val KILOBYTES = "KB"
                val MEGABYTES = "MB"
                val GIGABYTES = "GB"
                var fileSize = 0f
                var suffix = KILOBYTES

                if (size > BYTES_IN_KILOBYTES) {
                        fileSize = (size / BYTES_IN_KILOBYTES).toFloat()
                        if (fileSize > BYTES_IN_KILOBYTES) {
                                fileSize /= BYTES_IN_KILOBYTES
                                if (fileSize > BYTES_IN_KILOBYTES) {
                                        fileSize /= BYTES_IN_KILOBYTES
                                        suffix = GIGABYTES
                                } else {
                                        suffix = MEGABYTES
                                }
                        }
                }
                return dec.format(fileSize.toDouble()) + suffix
        }

        /**
         * Get the Intent for selecting content to be used in an Intent Chooser.
         *
         * @return The intent for opening a file with Intent.createChooser()
         */
        fun createGetContentIntent(): Intent {
                // Implicitly allow the user to select a particular kind of data
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                // The MIME data type filter
                intent.type = "*/*"
                // Only return URIs that can be opened with ContentResolver
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                return intent
        }


        /**
         * Creates View intent for given file
         *
         * @param file
         * @return The intent for viewing file
         */
        fun getViewIntent(context: Context, file: File): Intent {
                //Uri uri = Uri.fromFile(file);

                val uri = FileProvider.getUriForFile(context, AUTHORITY, file)
                val intent = Intent(Intent.ACTION_VIEW)
                val url = file.toString()
                if (url.contains(".doc") || url.contains(".docx")) {
                        // Word document
                        intent.setDataAndType(uri, "application/msword")
                } else if (url.contains(".pdf")) {
                        // PDF file
                        intent.setDataAndType(uri, "application/pdf")
                } else if (url.contains(".ppt") || url.contains(".pptx")) {
                        // Powerpoint file
                        intent.setDataAndType(uri, "application/vnd.ms-powerpoint")
                } else if (url.contains(".xls") || url.contains(".xlsx")) {
                        // Excel file
                        intent.setDataAndType(uri, "application/vnd.ms-excel")
                } else if (url.contains(".zip") || url.contains(".rar")) {
                        // WAV audio file
                        intent.setDataAndType(uri, "application/x-wav")
                } else if (url.contains(".rtf")) {
                        // RTF file
                        intent.setDataAndType(uri, "application/rtf")
                } else if (url.contains(".wav") || url.contains(".mp3")) {
                        // WAV audio file
                        intent.setDataAndType(uri, "audio/x-wav")
                } else if (url.contains(".gif")) {
                        // GIF file
                        intent.setDataAndType(uri, "image/gif")
                } else if (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png")) {
                        // JPG file
                        intent.setDataAndType(uri, "image/jpeg")
                } else if (url.contains(".txt")) {
                        // Text file
                        intent.setDataAndType(uri, "text/plain")
                } else if (url.contains(".3gp") || url.contains(".mpg") || url.contains(
                                ".mpeg"
                        ) || url.contains(".mpe") || url.contains(".mp4") || url.contains(".avi")
                ) {
                        // Video files
                        intent.setDataAndType(uri, "video/*")
                } else {
                        intent.setDataAndType(uri, "*/*")
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                return intent
        }

        fun getDocumentCacheDir(context: Context): File {
                val dir = File(context.cacheDir, DOCUMENTS_DIR)
                if (!dir.exists()) {
                        dir.mkdirs()
                }

                logDir(dir)

                return dir
        }

        private fun logDir(dir: File) {
                if (!DEBUG) return
                Timber.d("Dir=$dir")
                val files = dir.listFiles()
                for (file in files) {
                        Timber.d("File=${file.path}")
                }
        }

        fun generateFileNameForce(name: String?, directory: File): File? {
                val newFileName = name ?: return null

                val file = File(directory, newFileName)

                if (file.exists()) {
                        return file
                }

                try {
                        if (!file.createNewFile()) {
                                return null
                        }
                } catch (e: IOException) {
                        Timber.e(e)
                        return null
                }

                logDir(directory)

                return file
        }

        fun generateFileName(name: String?, directory: File): File? {
                var newFileName = name ?: return null

                var file = File(directory, newFileName)

                if (file.exists()) {
                        var fileName = newFileName
                        var extension = ""
                        val dotIndex = newFileName.lastIndexOf('.')
                        if (dotIndex > 0) {
                                fileName = newFileName.substring(0, dotIndex)
                                extension = newFileName.substring(dotIndex)
                        }

                        var index = 0

                        while (file.exists()) {
                                index++
                                newFileName = "$fileName($index)$extension"
                                file = File(directory, newFileName)
                        }
                }

                try {
                        if (!file.createNewFile()) {
                                return null
                        }
                } catch (e: IOException) {
                        Timber.e(e)
                        return null
                }

                logDir(directory)

                return file
        }

        /**
         * Writes response body to disk
         *
         * @param body ResponseBody
         * @param path file path
         * @return File
         */
        fun writeResponseBodyToDisk(body: ResponseBody, path: String): File? {
                try {
                        val target = File(path)

                        var inputStream: InputStream? = null
                        var outputStream: OutputStream? = null

                        try {
                                val fileReader = ByteArray(4096)

                                inputStream = body.byteStream()
                                outputStream = FileOutputStream(target)

                                while (true) {
                                        val read = inputStream.read(fileReader)

                                        if (read == -1) {
                                                break
                                        }

                                        outputStream.write(fileReader, 0, read)
                                }

                                outputStream.flush()

                                return target
                        } catch (e: IOException) {
                                return null
                        } finally {
                                inputStream?.close()

                                outputStream?.close()
                        }
                } catch (e: IOException) {
                        return null
                }

        }

        private fun saveFileFromUri(context: Context, uri: Uri, destinationPath: String?) {
                var `is`: InputStream? = null
                var bos: BufferedOutputStream? = null
                try {
                        `is` = context.contentResolver
                                .openInputStream(uri)
                        bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
                        val buf = ByteArray(1024)
                        `is`?.read(buf)
                        do {
                                bos.write(buf)
                        } while (`is`?.read(buf) != -1)
                } catch (e: IOException) {
                        e.printStackTrace()
                } finally {
                        try {
                                `is`?.close()
                                bos?.close()
                        } catch (e: IOException) {
                                e.printStackTrace()
                        }

                }
        }

        fun readBytesFromFile(filePath: String): ByteArray? {

                var fileInputStream: FileInputStream? = null
                var bytesArray: ByteArray? = null

                try {

                        val file = File(filePath)
                        bytesArray = ByteArray(file.length().toInt())

                        //read file into bytes[]
                        fileInputStream = FileInputStream(file)
                        fileInputStream.read(bytesArray)

                } catch (e: IOException) {
                        e.printStackTrace()
                } finally {
                        if (fileInputStream != null) {
                                try {
                                        fileInputStream.close()
                                } catch (e: IOException) {
                                        e.printStackTrace()
                                }

                        }

                }

                return bytesArray

        }

        @Throws(IOException::class)
        fun createTempImageFile(context: Context, fileName: String): File {
                // Create an image file name
                val storageDir = File(context.cacheDir, DOCUMENTS_DIR)
                return File.createTempFile(fileName, ".jpg", storageDir)
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun getFileName(context: Context, uri: Uri): String? {
                val mimeType = context.contentResolver
                        .getType(uri)
                var filename: String? = null

                if (mimeType == null) {
                        val path = getPath(context, uri)
                        filename = if (path == null) {
                                getName(uri.toString())
                        } else {
                                val file = File(path)
                                file.name
                        }
                } else {
                        val returnCursor = context.contentResolver
                                .query(uri, null, null, null, null)
                        if (returnCursor != null) {
                                val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                returnCursor.moveToFirst()
                                filename = returnCursor.getString(nameIndex)
                                returnCursor.close()
                        }
                }

                return filename
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun getFileSize(context: Context, Uri: Uri): String {
                val format = DecimalFormat("#.##")
                val mb = (1024 * 1024).toLong()
                val kb: Long = 1024

                val file = getFile(context, Uri)

                val length = file!!.length()

                if (file.exists()) {

                        if (length > mb) {
                                return format.format(length / mb) + " Mb"
                        }

                        if (length > kb) {
                                return format.format(length / kb) + " Kb"
                        }
                }

                return format.format(length) + " B"
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun getFileDuration(context: Context, uri: Uri): String {
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(getFile(context, uri)!!.absolutePath)
                val durationStr =
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?: "0"
                return formatMilliSecond(durationStr.toLong())
        }

        fun getFileDuration(file: File): Int {
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(file.absolutePath)
                val durationStr =
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?: "0"
                return durationStr.toInt()
        }

        private fun formatMilliSecond(milliseconds: Long): String {
                var finalTimerString = ""

                // Convert total duration into time
                val hours = (milliseconds / (1000 * 60 * 60)).toInt()
                val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
                val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()

                // Add hours if there
                if (hours > 0) {
                        finalTimerString = "$hours:"
                }

                // Prepending 0 to seconds if it is one digit
                val secondsString = if (seconds < 10) {
                        "0$seconds"
                } else {
                        "" + seconds
                }

                finalTimerString = "$finalTimerString$minutes:$secondsString"

                //      return  String.format("%02d Min, %02d Sec",
                //                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                //                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                //                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));

                // return timer string
                return finalTimerString

        }

        private fun getName(filename: String?): String? {
                if (filename == null) {
                        return null
                }
                val index = filename.lastIndexOf('/')
                return filename.substring(index + 1)
        }
}//private constructor to enforce Singleton pattern
