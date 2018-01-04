package org.readium.r2.testapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.*

open class BooksAdapter(context: Context, list: ArrayList<Book>) : BaseAdapter() {

    private val TAG = this::class.java.simpleName

    var books: ArrayList<Book>
    var layoutInflater: LayoutInflater

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
            retView = layoutInflater.inflate(R.layout.linearlayout_book, null)
            holder = ViewHolder()

            holder.imageViewCoverArt = retView.findViewById<View>(R.id.imageview_cover_art) as ImageView?
            holder.nameTextView = retView.findViewById<View>(R.id.textview_book_name) as TextView
            holder.authorTextView = retView.findViewById<View>(R.id.textview_book_author) as TextView

            retView.tag = holder//error in this line

        } else {
            holder = convertView.tag as ViewHolder
            retView = convertView
        }

        val book = books[position]
        holder.imageViewCoverArt!!.setImageResource(R.drawable.cover)

        //        Picasso.with(mContext).load(book.getImageUrl()).into(viewHolder.imageViewCoverArt);

        holder.nameTextView!!.text = book.title
        holder.authorTextView!!.text = book.author


        return retView
    }

    internal class ViewHolder {
        var nameTextView: TextView? = null
        var authorTextView: TextView? = null
        var imageViewCoverArt: ImageView? = null
    }

}

class Book(val fileName: String, val title: String, val author: String, val fileUrl: String, val id: Long)
