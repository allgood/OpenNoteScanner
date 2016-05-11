package com.todobom.opennotescanner.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.provider.Settings;
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
    private static LoaderCallbackInterface Callback;
    private static String Version;
    private static AlertDialog mAskInstallDialog;

    public static boolean isGooglePlayInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try
        {
            PackageInfo info = pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES);
            String label = (String) info.applicationInfo.loadLabel(pm);
            app_installed = (label != null && label.equals("Google Play Store"));
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

        private static final String TAG = "CustomOpenCVLoader";
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

                            waitOpenCVDialog.dismiss();
                            AppContext.unregisterReceiver(onComplete);

                            String path = "file://" + savedFilePath;
                            Uri uri = Uri.parse(path);
                            Log.d(TAG,"dm query: " + path );
                            intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setDataAndType(uri, dm.getMimeTypeForDownloadedFile(id));

                            AppContext.startActivity(intent);
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
    }

    static AlertDialog.Builder waitInstallOpenCV;
    static Dialog waitOpenCVDialog;


    public static boolean initAsync(String version, final Context AppContext, LoaderCallbackInterface callback) {

        Version = version;
        Callback = callback;

        // if dialog is showing, remove
        if (mAskInstallDialog != null) {
            mAskInstallDialog.dismiss();
            mAskInstallDialog = null;
        }

        // if don't have google play, check for OpenCV before trying to init
        if (!isOpenCVInstalled(Version,AppContext)) {

            boolean isNonPlayAppAllowed = false;
            try {
                isNonPlayAppAllowed = Settings.Secure.getInt(AppContext.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            AlertDialog.Builder askInstallOpenCV = new AlertDialog.Builder(AppContext);

            askInstallOpenCV.setTitle(R.string.install_opencv);
            askInstallOpenCV.setMessage(R.string.ask_install_opencv);
            askInstallOpenCV.setCancelable(false);

            if (isNonPlayAppAllowed) {
                askInstallOpenCV.setNeutralButton(R.string.githubdownload, new DialogInterface.OnClickListener() {

                    String arch = Build.SUPPORTED_ABIS[0];

                    public void onClick(DialogInterface dialog, int which) {

                        String sAndroidUrl = "https://github.com/ctodobom/opencv/releases/download/3.1.0/OpenCV_3.1.0_Manager_3.10_" + arch + ".apk";

                        onComplete = new MyBroadcastReceiver(AppContext);
                        AppContext.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                        final DownloadManager dm = (DownloadManager) AppContext.getSystemService(AppContext.DOWNLOAD_SERVICE);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sAndroidUrl));
                        String sDest = "file://" + android.os.Environment.getExternalStorageDirectory().toString() + "/Download/OpenCV_3.1.0_Manager_3.10_" + arch + ".apk";
                        request.setDestinationUri(Uri.parse(sDest));
                        myDownloadReference = dm.enqueue(request);

                        dialog.dismiss();

                        waitInstallOpenCV = new AlertDialog.Builder(AppContext);

                        waitInstallOpenCV.setTitle(R.string.downloading);
                        waitInstallOpenCV.setMessage(R.string.downloading_opencv);

                        waitInstallOpenCV.setCancelable(false);
                        waitInstallOpenCV.setOnCancelListener(new DialogInterface.OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dm.remove(myDownloadReference);
                                AppContext.unregisterReceiver(onComplete);
                                dialog.dismiss();
                                mAskInstallDialog = null;
                            }
                        });

                        waitInstallOpenCV.setNegativeButton(R.string.answer_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dm.remove(myDownloadReference);
                                AppContext.unregisterReceiver(onComplete);
                                dialog.dismiss();
                                mAskInstallDialog = null;
                            }
                        });

                        waitOpenCVDialog = waitInstallOpenCV.create();
                        waitOpenCVDialog.show();

                    }

                });
            }

            if (isGooglePlayInstalled(AppContext)) {
                askInstallOpenCV.setPositiveButton(R.string.googleplay, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        AppContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.opencv.engine")));
                    }
                });
            }

            if (!isNonPlayAppAllowed && !isGooglePlayInstalled(AppContext)) {
                askInstallOpenCV.setMessage( AppContext.getString(R.string.ask_install_opencv)
                        + "\n\n" + AppContext.getString(R.string.messageactivateunknown)
                );

                askInstallOpenCV.setNeutralButton(R.string.activateunknown , new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        AppContext.startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
                    }
                });
            }

            mAskInstallDialog = askInstallOpenCV.create();

            mAskInstallDialog.show();

        } else {
            // initialize opencv
            return OpenCVLoader.initAsync(Version, AppContext, Callback);
        }

        return false;

    }

}
