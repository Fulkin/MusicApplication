package ru.fulkin.musicapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.fulkin.musicapplication.R
import ru.fulkin.musicapplication.Song

class SongAdapter(context: Context, listSong: ArrayList<Song>): RecyclerView.Adapter<SongAdapter.MusicHolder>() {

    private val songList: ArrayList<Song> = listSong

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_song, parent, false)
        return MusicHolder(view)
    }

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(holder: MusicHolder, position: Int) {
        val song = songList[position]
        holder.bind(song)
        holder.itemView.tag = position
    }

    inner class MusicHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        private lateinit var song: Song
        private val titleTitle: TextView = itemView.findViewById(R.id.song_title)
        private val artistTitle: TextView = itemView.findViewById(R.id.song_artist)

        fun bind(song: Song) {
            this.song = song
            titleTitle.text = song.title
            artistTitle.text = song.artist
        }
    }

}
