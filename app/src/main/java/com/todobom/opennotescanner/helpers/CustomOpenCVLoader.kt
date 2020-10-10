package com.todobom.opennotescanner.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.todobom.opennotescanner.R
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.io.File

/**
 * Created by allgood on 22/02/16.
 */
object CustomOpenCVLoader {
    private val dummyServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {}
    }
    private var myDownloadReference: Long = 0
    private var Callback: LoaderCallbackInterface? = null
    private var Version: String? = null
    private var mAskInstallDialog: AlertDialog? = null
    fun isGooglePlayInstalled(context: Context): Boolean {
        val pm = context.packageManager

        // DISABLED installation from Google Play since OpenCV Manager is removed from there
        /*
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
        */return false
    }

    fun isOpenCVInstalled(Version: String?, AppContext: Context): Boolean {
        val intent = Intent("org.opencv.engine.BIND")
        intent.setPackage("org.opencv.engine")
        val result = AppContext.bindService(intent, dummyServiceConnection, Context.BIND_AUTO_CREATE)
        AppContext.unbindService(dummyServiceConnection)
        return result
    }

    var onComplete: MyBroadcastReceiver? = null
    var waitOpenCVDialog: Dialog? = null
    fun initAsync(version: String?, AppContext: Context, callback: LoaderCallbackInterface?): Boolean {
        Version = version
        Callback = callback

        // if dialog is showing, remove
        mAskInstallDialog?.dismiss()
        mAskInstallDialog = null

        // if don't have google play, check for OpenCV before trying to init
        if (!isOpenCVInstalled(Version, AppContext)) {
            var isNonPlayAppAllowed = false
            try {
                isNonPlayAppAllowed = Settings.Secure.getInt(AppContext.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }
            val askInstallOpenCV = AlertDialog
                    .Builder(AppContext)
                    .setTitle(R.string.install_opencv)
                    .setMessage(R.string.ask_install_opencv)
                    .setCancelable(false)

            if (isNonPlayAppAllowed) {
                askInstallOpenCV.setNeutralButton(R.string.githubdownload, object : DialogInterface.OnClickListener {
                    var arch = Build.SUPPORTED_ABIS[0]
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        val sAndroidUrl = "https://github.com/ctodobom/opencv/releases/download/3.1.0/OpenCV_3.1.0_Manager_3.10_$arch.apk"
                        onComplete = MyBroadcastReceiver(AppContext)
                        AppContext.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                        val dm = AppContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val request = DownloadManager.Request(Uri.parse(sAndroidUrl))
                        val sDest = "file://" + Environment.getExternalStorageDirectory().toString() + "/Download/OpenCV_3.1.0_Manager_3.10_" + arch + ".apk"
                        request.setDestinationUri(Uri.parse(sDest))
                        myDownloadReference = dm.enqueue(request)
                        dialog.dismiss()
                        waitOpenCVDialog = AlertDialog
                                .Builder(AppContext)
                                .setTitle(R.string.downloading)
                                .setMessage(R.string.downloading_opencv)
                                .setCancelable(false)
                                .setOnCancelListener { dialog1: DialogInterface ->
                                    dm.remove(myDownloadReference)
                                    AppContext.unregisterReceiver(onComplete)
                                    dialog1.dismiss()
                                    mAskInstallDialog = null
                                }
                                .setNegativeButton(R.string.answer_cancel) { dialog12: DialogInterface, which1: Int ->
                                    dm.remove(myDownloadReference)
                                    AppContext.unregisterReceiver(onComplete)
                                    dialog12.dismiss()
                                    mAskInstallDialog = null
                                }
                                .create()
                                .also {
                                    it.show()
                                }
                    }
                })
            }
            if (isGooglePlayInstalled(AppContext)) {
                askInstallOpenCV.setPositiveButton(R.string.googleplay) { dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                    AppContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.opencv.engine")))
                }
            }
            if (!isNonPlayAppAllowed && !isGooglePlayInstalled(AppContext)) {
                askInstallOpenCV.setMessage("""
    ${AppContext.getString(R.string.ask_install_opencv)}

    ${AppContext.getString(R.string.messageactivateunknown)}
    """.trimIndent()
                )
                askInstallOpenCV.setNeutralButton(R.string.activateunknown) { dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                    AppContext.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
            }
            mAskInstallDialog = askInstallOpenCV.create().also {
                it.show()
            }
        } else {
            // initialize opencv
            return OpenCVLoader.initAsync(Version, AppContext, Callback)
        }
        return false
    }

    class MyBroadcastReceiver(private val AppContext: Context) : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val id = intent.extras!!.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
            if (id == myDownloadReference) {
                val dm = AppContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query()
                query.setFilterById(id)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    // get the status of the download
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    when (val status = cursor.getInt(columnIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val downloadFileLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            val apkFile: File
                            apkFile = File(Uri.parse(downloadFileLocalUri).path)
                            waitOpenCVDialog?.dismiss()
                            AppContext.unregisterReceiver(onComplete)
                            val uri: Uri
                            val installIntent: Intent
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                                uri = FileProvider.getUriForFile(ctxt, ctxt.applicationContext.packageName + ".fileprovider", apkFile)
                            } else {
                                installIntent = Intent(Intent.ACTION_VIEW)
                                uri = Uri.fromFile(apkFile)
                            }
                            installIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
                            AppContext.startActivity(installIntent)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            // get the reason - more detail on the status
                            val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(columnReason)
                            Toast.makeText(AppContext,
                                    "FAILED: $reason",
                                    Toast.LENGTH_LONG).show()
                            AppContext.unregisterReceiver(onComplete)
                        }
                        else -> Log.d("CustomOpenCVLoader", "Received download manager status: $status")
                    }
                } else {
                    Log.d("CustomOpenCVLoader", "missing download")
                    AppContext.unregisterReceiver(onComplete)
                }
                cursor.close()
            }
        }

        companion object {
            private const val TAG = "CustomOpenCVLoader"
        }
    }
}