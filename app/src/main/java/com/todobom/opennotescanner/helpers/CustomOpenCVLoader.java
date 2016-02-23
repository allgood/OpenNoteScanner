package com.todobom.opennotescanner.helpers;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.todobom.opennotescanner.R;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

/**
 * Created by allgood on 22/02/16.
 */
public class CustomOpenCVLoader extends OpenCVLoader {

    private static ServiceConnection dummyServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private static long myDownloadReference;


    public static boolean isGooglePlayInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try
        {
            PackageInfo info = pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES);
            String label = (String) info.applicationInfo.loadLabel(pm);
            app_installed = (label != null && !label.equals("Market"));
        }
        catch (PackageManager.NameNotFoundException e)
        {
            app_installed = false;
        }
        return app_installed;
    }

    public static boolean isOpenCVInstalled(String Version, Context AppContext) {
        Intent intent = new Intent("org.opencv.engine.BIND");
        intent.setPackage("org.opencv.engine");
        boolean result = AppContext.bindService(intent, dummyServiceConnection, Context.BIND_AUTO_CREATE);
        AppContext.unbindService(dummyServiceConnection);
        return result;
    };

    static MyBroadcastReceiver onComplete;

    private static class MyBroadcastReceiver extends BroadcastReceiver {

        private Context AppContext;

        public MyBroadcastReceiver(Context appContext) {
            AppContext = appContext;
        }

        public void onReceive(Context ctxt, Intent intent) {

            long id = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);

            if ( id == myDownloadReference ) {
                DownloadManager dm = (DownloadManager) AppContext.getSystemService(AppContext.DOWNLOAD_SERVICE);

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor cursor = dm.query(query);

                if (cursor.moveToFirst()) {
                    // get the status of the download
                    int columnIndex = cursor.getColumnIndex(DownloadManager
                            .COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);

                    int fileNameIndex = cursor.getColumnIndex(DownloadManager
                            .COLUMN_LOCAL_FILENAME);
                    String savedFilePath = cursor.getString(fileNameIndex);

                    // get the reason - more detail on the status
                    int columnReason = cursor.getColumnIndex(DownloadManager
                            .COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:

                            // start activity to display the downloaded image
                            Uri uri = dm.getUriForDownloadedFile(id);

                            intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setDataAndType(dm.getUriForDownloadedFile(id),
                                    dm.getMimeTypeForDownloadedFile(id));
                            AppContext.startActivity(intent);
                            AppContext.unregisterReceiver(onComplete);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            Toast.makeText(AppContext,
                                    "FAILED: " + reason,
                                    Toast.LENGTH_LONG).show();
                            AppContext.unregisterReceiver(onComplete);
                            break;
                        default:
                            Log.d("CustomOpenCVLoader", "Received download manager status: " + status);
                    }
                } else {
                    Log.d("CustomOpenCVLoader","missing download");
                    AppContext.unregisterReceiver(onComplete);
                }
                cursor.close();

            }
        }
    };

    static AlertDialog.Builder cancelInstallOpenCV;

    public static boolean initAsync(String Version, final Context AppContext, LoaderCallbackInterface Callback) {

        // if don't have google play, check for OpenCV before trying to init
        if (!isOpenCVInstalled(Version,AppContext) && !isGooglePlayInstalled(AppContext)) {

            AlertDialog.Builder askInstallOpenCV = new AlertDialog.Builder(AppContext);

            askInstallOpenCV.setTitle(AppContext.getString(R.string.install_opencv));
            askInstallOpenCV.setMessage(AppContext.getString(R.string.confirm_install_opencv));

            askInstallOpenCV.setPositiveButton(AppContext.getString(R.string.answer_yes), new DialogInterface.OnClickListener() {

                String arch = Build.SUPPORTED_ABIS[0];

                public void onClick(DialogInterface dialog, int which) {

                    String sAndroidUrl = "https://github.com/ctodobom/opencv/releases/download/3.1.0/OpenCV_3.1.0_Manager_3.10_"+arch+".apk";

                    onComplete = new MyBroadcastReceiver(AppContext);
                    AppContext.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    final DownloadManager dm = (DownloadManager) AppContext.getSystemService(AppContext.DOWNLOAD_SERVICE);
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sAndroidUrl));
                    String sDest = "file://" + android.os.Environment.getExternalStorageDirectory().toString() + "/Download/OpenCV_3.1.0_Manager_3.10_"+arch+".apk";
                    request.setDestinationUri(Uri.parse(sDest));
                    myDownloadReference = dm.enqueue(request);

                    dialog.dismiss();

                    cancelInstallOpenCV = new AlertDialog.Builder(AppContext);

                    cancelInstallOpenCV.setTitle(AppContext.getString(R.string.downloading));
                    cancelInstallOpenCV.setMessage(AppContext.getString(R.string.downloading_opencv));

                    cancelInstallOpenCV.setCancelable(false);
                    cancelInstallOpenCV.setOnCancelListener( new DialogInterface.OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dm.remove(myDownloadReference);
                            AppContext.unregisterReceiver(onComplete);
                            dialog.dismiss();
                        }
                    });

                    cancelInstallOpenCV.setNegativeButton(AppContext.getString(R.string.answer_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.remove(myDownloadReference);
                            AppContext.unregisterReceiver(onComplete);
                            dialog.dismiss();
                        }
                    });

                    cancelInstallOpenCV.create().show();

                }

            });

            askInstallOpenCV.setNegativeButton(AppContext.getString(R.string.answer_no), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            askInstallOpenCV.create().show();

        } else {
            // initialize opencv
            return OpenCVLoader.initAsync(Version, AppContext, Callback);
        };

        return false;

    }

}
