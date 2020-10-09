package com.todobom.opennotescanner.helpers

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import android.widget.Toast
import com.itextpdf.io.image.ImageData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.todobom.opennotescanner.R
import java.io.File
import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.text.SimpleDateFormat
import java.util.*

object PdfHelper {
    @JvmStatic
    fun mergeImagesToPdf(applicationContext: Context, files: ArrayList<String>): String? {
        //TODO move this to background thread
        if (files.isEmpty()) {
            Toast
                    .makeText(applicationContext, applicationContext.getString(R.string.no_files_selected), Toast.LENGTH_SHORT)
                    .show()
            return null
        }
        val outputFile = ("PDF-"
                + SimpleDateFormat("yyyyMMdd-HHmmss").format(Date()) + ".pdf")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val pdfFilePath = File(
                Environment.getExternalStorageDirectory()
                        .toString() + File.separator + sharedPreferences.getString("storage_folder", "OpenNoteScanner"), outputFile)
                .absolutePath
        var pdfWriter: PdfWriter? = null
        try {
            pdfWriter = PdfWriter(pdfFilePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        if (pdfWriter == null) {
            return null
        }
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)
        files.sort()
        for (file in files) {
            var imageData: ImageData? = null
            try {
                imageData = ImageDataFactory.create(file)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
            if (imageData == null) {
                return null
            }
            val image = Image(imageData)
            pdfDocument.addNewPage(PageSize(image.imageWidth, image.imageHeight))
            document.add(image)
        }
        document.close()
        return pdfFilePath
    }
}