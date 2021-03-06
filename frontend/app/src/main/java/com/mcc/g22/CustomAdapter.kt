package com.mcc.g22

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import java.util.ArrayList

class CustomAdapter(private val context: Context, private val imageModelArrayList: ArrayList<ImageModel>) : BaseAdapter() {

    override fun getViewTypeCount(): Int {
        return count
    }

    override fun getItemViewType(position: Int): Int {

        return position
    }

    override fun getCount(): Int {
        return imageModelArrayList.size
    }

    override fun getItem(position: Int): Any {
        return imageModelArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder

        if (convertView == null) {
            holder = ViewHolder()
            val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.image, null, true)

            holder.tvname = convertView!!.findViewById(R.id.name) as TextView
            holder.iv = convertView.findViewById(R.id.imgView) as ImageView

            convertView.tag = holder
        } else {
            // the getTag returns the viewHolder object set as a tag to the view
            holder = convertView.tag as ViewHolder
        }

        holder.tvname!!.text = imageModelArrayList[position].getNames()
        if (imageModelArrayList[position].storagePath != null) {
            Glide.with(context).load(
                FirebaseStorage.getInstance().reference.child(imageModelArrayList[position].storagePath!!)
            ).into(holder.iv!!)
        }
        else if (imageModelArrayList[position].image_drawable != 0) {
            holder.iv!!.setImageResource(imageModelArrayList[position].getImage_drawables())
        }

        return convertView!!
    }

    private inner class ViewHolder {

        var tvname: TextView? = null
        internal var iv: ImageView? = null

    }

}