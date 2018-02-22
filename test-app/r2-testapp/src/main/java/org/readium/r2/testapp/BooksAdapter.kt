package org.readium.r2.testapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.squareup.picasso.Picasso
import java.net.URL
import java.util.*

open class BooksAdapter(context: Context, list: ArrayList<Book>) : BaseAdapter() {

    private val TAG = this::class.java.simpleName

    var books: ArrayList<Book>
    var layoutInflater: LayoutInflater
    var context:Context = context

    override fun getItem(position: Int): Any {
        return books.get(position)
    }

    override fun getItemId(position: Int): Long {
        return books.get(position).id
    }

    override fun getCount(): Int {
        return books.size
    }


    init {
        this.books = list
        this.layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val holder: ViewHolder
        val retView: View
        if (convertView == null) {
            retView = layoutInflater.inflate(R.layout.linearlayout_book,  parent, false)
            holder = ViewHolder()

            holder.imageViewCoverArt = retView.findViewById<View>(R.id.imageview_cover_art) as ImageView?

            retView.tag = holder

        } else {
            holder = convertView.tag as ViewHolder
            retView = convertView
        }

        val book = books[position]
        holder.imageViewCoverArt!!.setImageResource(R.drawable.cover)

        book.coverLink.let {
            if (it != null) {
                Picasso.with(context).load(it.toString()).into(holder.imageViewCoverArt);
            }
        }

        return retView
    }

    internal class ViewHolder {
        var imageViewCoverArt: ImageView? = null
    }

}

class Book(val fileName: String, val title: String, val author: String, val fileUrl: String, val id: Long, val coverLink: URL?)
