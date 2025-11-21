package com.example.chadmusic

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private val context: Context,
    private val titles: ArrayList<String>
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var currentIndex = -1

    fun setCurrentIndex(index: Int) {
        currentIndex = index
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_song, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {

        holder.txtTitle.text = titles[position]

        // highlight bài đang phát
        if (position == currentIndex) {
            holder.imgPlaying.visibility = View.VISIBLE
            holder.txtTitle.setTextColor(0xFFFFD700.toInt()) // vàng nổi bật
        } else {
            holder.imgPlaying.visibility = View.GONE
            holder.txtTitle.setTextColor(0xFF000000.toInt()) // trắng
        }

        // Click bài → gửi Service ACTION_PLAY_AT
        holder.itemView.setOnClickListener {
            val intent = Intent(context, MusicService::class.java).apply {
                action = "ACTION_PLAY_AT"
                putExtra("index", position)
            }
            context.startService(intent)
        }
    }

    override fun getItemCount(): Int = titles.size

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val imgPlaying: ImageView = itemView.findViewById(R.id.imgPlaying)
    }
}
