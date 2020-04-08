package vitaty14.kg.lgtm_android

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PopupDialogFragment : DialogFragment() {

    private val appDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) ,"/LGTM" )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(context!!)
        val dialogWindow = dialog.window

        val inflater = requireActivity().layoutInflater
        val settingView = inflater.inflate(R.layout.popup_layout,null)

        val dataStore : SharedPreferences = this.activity!!.getSharedPreferences("DataStore",
            Context.MODE_PRIVATE)

        val getPath = dataStore.getString("InputPath","")
        val getWidth = dataStore.getInt("widthSize",600)
        val getHeight = dataStore.getInt("heightSize",480)
        val paint = Paint()

        val baseBitmap = Bitmap.createBitmap(getWidth, getHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(baseBitmap)

        val ios = FileInputStream(getPath)
        var photoBitmap =  BitmapFactory.decodeStream(ios)
        photoBitmap = Bitmap.createScaledBitmap(photoBitmap,getWidth,getHeight,false)
        canvas.drawBitmap(photoBitmap,0F,0F,paint)

        var frameBitmap = BitmapFactory.decodeResource(resources, R.drawable.lgtm)
        frameBitmap = Bitmap.createScaledBitmap(frameBitmap,500,500,false)
        canvas.drawBitmap(frameBitmap,540F,600F,paint)
//        canvas.drawBitmap(frameBitmap,280F,300F,paint) 大体真ん中

        val tmpPopupView : ImageView = settingView.findViewById(R.id.popupView)
        tmpPopupView.setImageBitmap(baseBitmap)

        return dialogWindow.let {
            val builder = AlertDialog.Builder(requireContext())

            builder.setView(settingView)
            builder.setPositiveButton("ok") { _, _ ->
                if(!appDir.exists()) {
                    appDir.mkdir()
                }
                val fos = FileOutputStream(getPath,false)
                val bitmap: Bitmap = baseBitmap
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
            }
            builder.setNegativeButton("cancel") {_,_ ->
                val deleteFile = File(getPath)
                deleteFile.delete()
            }
            builder.create()
        } ?: throw IllegalStateException("error")
    }

}