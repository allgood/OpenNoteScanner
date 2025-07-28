package com.todobom.opennotescanner.helpers

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.todobom.opennotescanner.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfHelper {
    private const val TAG = "OpenNoteScanner-Pdf"

    @JvmStatic
    fun mergeImagesToPdf(applicationContext: Context, imageUris: ArrayList<Uri>): Uri? {
        if (imageUris.isEmpty()) {
            Toast.makeText(
                applicationContext,
                applicationContext.getString(R.string.no_files_selected),
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val customFolderName = preferences.getString("storage_folder", "OpenNoteScanner") ?: "OpenNoteScanner"

        val resolver = applicationContext.contentResolver

        val displayName = "PDF-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}.pdf"
        var outputStream: OutputStream? = null
        var pdfUri: Uri? = null
        var preQFile: File? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending until written
                    if (customFolderName.isNotBlank()) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + File.separator + "OpenNoteScanner")
                    } else {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                    }
                }

                pdfUri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
                pdfUri?.let {
                    outputStream = resolver.openOutputStream(it)
                }
            } else {
                // Fallback for pre-Android Q
                val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetDir = if (customFolderName.isNotBlank()) File(storageDir, customFolderName) else storageDir

                if (!targetDir.exists()) {
                    if (!targetDir.mkdirs()) {
                        Log.e(TAG, "Failed to create directory: ${targetDir.absolutePath}")
                        Toast.makeText(applicationContext, "Could not create directory", Toast.LENGTH_LONG).show()
                        return null
                    }
                }

                preQFile = File(targetDir, displayName)
                pdfUri = Uri.fromFile(preQFile)
                outputStream = FileOutputStream(preQFile)
            }

            if (outputStream == null) {
                Toast.makeText(applicationContext, "Failed to create PDF output stream", Toast.LENGTH_LONG).show()
                return null
            }

            val pdfWriter = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // sorts by date
            imageUris.sort()

            for (imageUri in imageUris) {
                try {
                    applicationContext.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        val imageData = ImageDataFactory.create(inputStream.readBytes())
                        val image = Image(imageData)

                        pdfDocument.addNewPage(PageSize(image.imageWidth, image.imageHeight))
                        document.add(image)
                    } ?: throw IOException("Failed to open stream for URI: $imageUri")
                } catch (e: Exception) { // Catch more general exceptions during image processing
                    e.printStackTrace()
                    // TODO: show error
                    // Decide if you want to skip this image or abort the PDF creation
                }
            }
            document.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (pdfUri != null) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(pdfUri, contentValues, null, null)
                }
            } else {
                // for good measures
                MediaScannerConnection.scanFile(applicationContext, arrayOf(preQFile?.absolutePath), arrayOf("application/pdf"), null)
            }

            Toast.makeText(applicationContext, "PDF saved to ${pdfUri?.path ?: displayName}", Toast.LENGTH_LONG).show()
            return pdfUri

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error writing PDF: ${e.message}", Toast.LENGTH_LONG).show()
            // Clean up if a MediaStore entry was created but writing failed
            pdfUri?.let { uri ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        applicationContext.contentResolver.delete(uri, null, null)
                    } catch (deleteEx: Exception) {
                        deleteEx.printStackTrace()
                    }
                }
            }
        }

        return null
    }
}