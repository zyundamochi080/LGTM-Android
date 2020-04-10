package vitaty14.kg.lgtm_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var prefs: SharedPreferences

    companion object {
        const val PERMISSION_WRITE_STORAGE = 600
        const val PERMISSION_READ_STORAGE  = 650
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val writePermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if ((writePermission != PackageManager.PERMISSION_GRANTED) || (readPermission != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_WRITE_STORAGE.requestStoragePermission(PERMISSION_READ_STORAGE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (SurfaceView.isAvailable) {
            openCamera()
        } else {
            SurfaceView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
                    openCamera()
                }
                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture?, p1: Int, p2: Int) {}
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture?) {}
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture?): Boolean {
                    if(cameraDevice != null) {
                        cameraDevice?.close()
                        cameraDevice = null
                    }
                    return false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_about_setting -> {
                cameraDevice?.close()
                cameraDevice = null
                val intent = Intent(this,SettingActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_about_app -> {
                val uri = Uri.parse("https://github.com/zyundamochi080/LGTM-Android")
                val intent = Intent(Intent.ACTION_VIEW,uri)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var cameraDevice: CameraDevice? = null

    private val cameraManager: CameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        cameraManager.openCamera("0", object: CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, p1: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
    }

    private var captureSession: CameraCaptureSession? = null

    private fun createCameraPreviewSession() {
        if (cameraDevice == null) {
            return
        }
        val texture = SurfaceView.surfaceTexture
        texture.setDefaultBufferSize(SurfaceView.width, SurfaceView.height)

        val surface = Surface(texture)
        val previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(surface)
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_MODE,
            CaptureRequest.CONTROL_MODE_AUTO
        )
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )

        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                shutterButton.setOnClickListener {
                    val appDir = File(getExternalStoragePublicDirectory(DIRECTORY_DCIM) ,"/LGTM" )
                    try {
                        val nowTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                        val filename = "$nowTime.jpg"
                        val file = (appDir.absolutePath) + "/" +filename

                        captureSession?.stopRepeating()
                        if (SurfaceView.isAvailable) {
                            if (!appDir.exists()) appDir.mkdir()
                            val fos = FileOutputStream(file,true)
                            val bitmap: Bitmap = SurfaceView.bitmap
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.close()
                        }

                        val dataStore : SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
                        val editor = dataStore.edit()
                        editor.putString("InputPath",file)
                        editor.putInt("widthSize",SurfaceView.width)
                        editor.putInt("heightSize",SurfaceView.height)
                        editor.commit()

                        val fragmentPopup = PopupDialogFragment()
                        fragmentPopup.show(supportFragmentManager,"PopupRequestDialog")
                    } catch (e: CameraAccessException) {
                        Log.d("debug", "CameraAccessException Error: $e")
                    } catch (e: FileNotFoundException) {
                        Log.d("debug", "FileNotFoundException Error: $e")
                    } catch (e: IOException) {
                        Log.d("debug", "IOException Error: $e")
                    } catch (e: Exception) {
                        Log.d("debug", "Other Error: $e")
                    }
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, null)
    }

    private fun Int.requestStoragePermission(READ_STORAGE_PERMISSION: Int) {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder(baseContext)
                .setMessage("Permission Here")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        this
                    )
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .create()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                this
            )
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            AlertDialog.Builder(baseContext)
                .setMessage("Permission Here")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_STORAGE_PERMISSION
                    )
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .create()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_STORAGE_PERMISSION
            )
        }
    }

}