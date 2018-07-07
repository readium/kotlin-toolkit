package org.readium.r2.navigator.pager

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.readium.r2.navigator.R
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File


class R2CbzPageFragment : Fragment() {

    private val TAG = this::class.java.simpleName

    val zipFile: String?
        get() = arguments!!.getString("zipFile")
    val zipEntry: String?
        get() = arguments!!.getString("zipEntry")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_page_cbz, container, false)
        val imageView = v.findViewById<ImageView>(R.id.imageView)

        val blob = ZipUtil.unpackEntry(File(zipFile), zipEntry)
        blob?.let {
            val arrayInputStream = ByteArrayInputStream(it)
            val bitmap = BitmapFactory.decodeStream(arrayInputStream)
            imageView.setImageBitmap(bitmap)
        }

        return v
    }

    companion object {

        fun newInstance(zipFile: String, zipEntry: String): R2CbzPageFragment {
            val args = Bundle()
            args.putString("zipFile", zipFile)
            args.putString("zipEntry", zipEntry)
            val fragment = R2CbzPageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


