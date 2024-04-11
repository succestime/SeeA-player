package com.jaidev.seeaplayer
//
//import android.annotation.SuppressLint
//import android.graphics.Color
//import android.os.Bundle
//import android.widget.Button
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.downloader.Error
//import com.downloader.OnDownloadListener
//import com.downloader.OnPauseListener
//import com.downloader.OnProgressListener
//import com.downloader.OnStartOrResumeListener
//import com.downloader.PRDownloader
//import com.downloader.Status
//
//class DownloadWithPauseResumeNew : AppCompatActivity() {
//
//    private lateinit var buttonOne: Button
//    private lateinit var buttonCancelOne: Button
//    private lateinit var textViewProgressOne: TextView
//    private lateinit var textViewFilenameSet: TextView
//    private lateinit var progressBarOne: ProgressBar
//    private var downloadIdOne: Int = 0
//    private var dirPath: String = ""
//    private var urlsss: String = ""
//    private var filename: String = ""
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_download_with_pause_resume_new)
//
//        dirPath = Utils.getRootPath(applicationContext)
//      urlsss = intent.getStringExtra("urlss").toString()
//     filename = intent.getStringExtra("filenames").toString()
//
//        initViews()
//        textViewFilenameSet.text = filename
//        setOnClickListenerOne()
//    }
//
//    private fun initViews() {
//        buttonOne = findViewById(R.id.buttonOne)
//        buttonCancelOne = findViewById(R.id.buttonCancelOne)
//        textViewProgressOne = findViewById(R.id.textViewProgressOne)
//        textViewFilenameSet = findViewById(R.id.textViewfilenameset)
//        progressBarOne = findViewById(R.id.progressBarOne)
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun setOnClickListenerOne() {
//        buttonOne.setOnClickListener {
//            if (Status.RUNNING == PRDownloader.getStatus(downloadIdOne)) {
//                PRDownloader.pause(downloadIdOne)
//                return@setOnClickListener
//            }
//
//            buttonOne.isEnabled = false
//            progressBarOne.isIndeterminate = true
//            progressBarOne.indeterminateDrawable.setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN)
//
//            if (Status.PAUSED == PRDownloader.getStatus(downloadIdOne)) {
//                PRDownloader.resume(downloadIdOne)
//                return@setOnClickListener
//            }
//
//            downloadIdOne = PRDownloader.download(urlsss, dirPath, filename)
//                .build()
//                .setOnStartOrResumeListener(object : OnStartOrResumeListener {
//                    override fun onStartOrResume() {
//                        progressBarOne.isIndeterminate = false
//                        buttonOne.isEnabled = true
//                        buttonOne.text = "Pause"
//                        buttonCancelOne.isEnabled = true
//                    }
//                })
//                .setOnPauseListener(object : OnPauseListener {
//                    override fun onPause() {
//                        buttonOne.text = "Resume"
//                    }
//                })
//                .setOnCancelListener {
//                    buttonOne.text = "Start"
//                    buttonCancelOne.isEnabled = false
//                    progressBarOne.progress = 0
//                    downloadIdOne = 0
//                    progressBarOne.isIndeterminate = false
//                }
//                .setOnProgressListener(object : OnProgressListener {
//                    override fun onProgress(progress: com.downloader.Progress) {
//                        val progressPercent = progress.currentBytes * 100 / progress.totalBytes
//                        progressBarOne.progress = progressPercent.toInt()
//                        textViewProgressOne.text = Utils.getProgressDisplayLine(progress.currentBytes, progress.totalBytes)
//                        progressBarOne.isIndeterminate = false
//                    }
//                })
//                .start(object : OnDownloadListener {
//                    @SuppressLint("SetTextI18n")
//                    override fun onDownloadComplete() {
//                        buttonOne.isEnabled = false
//                        buttonCancelOne.isEnabled = false
//                        buttonOne.text = "Completed"
//                    }
//
//                    @SuppressLint("SetTextI18n")
//                    override fun onError(error: Error) {
//                        buttonOne.text = "Start"
//                        Toast.makeText(applicationContext, getString(R.string.some_Error_Occurred) + " 1", Toast.LENGTH_SHORT).show()
//                        textViewProgressOne.text = ""
//                        progressBarOne.progress = 0
//                        downloadIdOne = 0
//                        buttonCancelOne.isEnabled = false
//                        progressBarOne.isIndeterminate = false
//                        buttonOne.isEnabled = true
//                    }
//                })
//        }
//
//        buttonCancelOne.setOnClickListener {
//            PRDownloader.cancel(downloadIdOne)
//        }
//    }
//}
