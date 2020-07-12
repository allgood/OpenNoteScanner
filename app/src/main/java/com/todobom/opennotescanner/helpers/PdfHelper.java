package com.todobom.opennotescanner.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.todobom.opennotescanner.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class PdfHelper {

    public static String mergeImagesToPdf(Context applicationContext, ArrayList<String> files) {
        //TODO move this to background thread
        if (files.isEmpty()) {
            Toast
                    .makeText(applicationContext, applicationContext.getString(R.string.no_files_selected), Toast.LENGTH_SHORT)
                    .show();
            return null;
        }
        String outputFile = "PDF-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".pdf";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        String pdfFilePath = new File(
                android.os.Environment.getExternalStorageDirectory()
                        + File.separator + sharedPreferences.getString("storage_folder", "OpenNoteScanner")
                , outputFile)
                .getAbsolutePath();

        PdfWriter pdfWriter = null;
        try {
            pdfWriter = new PdfWriter(pdfFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(pdfWriter == null){
            return null;
        }

        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        Document document = new Document(pdfDocument);

        Collections.sort(files);

        for (String file : files) {
            ImageData imageData = null;
            try {
                imageData = ImageDataFactory.create(file);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if(imageData == null){
                return null;
            }
            Image image = new Image(imageData);
            pdfDocument.addNewPage(new PageSize(image.getImageWidth(), image.getImageHeight()));
            document.add(image);
        }

        document.close();

        return pdfFilePath;
    }

}
