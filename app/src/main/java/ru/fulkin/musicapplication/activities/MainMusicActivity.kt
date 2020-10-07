package ru.fulkin.musicapplication.activities

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.fulkin.musicapplication.adapter.SongAdapter
import ru.fulkin.musicapplication.R
import ru.fulkin.musicapplication.Song
import ru.fulkin.musicapplication.controller.MusicController
import ru.fulkin.musicapplication.service.MusicService
import ru.fulkin.musicapplication.service.MusicService.MusicBinder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class MainMusicActivity : AppCompatActivity(), MediaController.MediaPlayerControl {

    private val listSong: ArrayList<Song> = ArrayList()
    private lateinit var musicRecyclerView: RecyclerView
    private var musicService: MusicService = MusicService()
    private lateinit var playIntent: Intent
    private var musicBound: Boolean = false
    private lateinit var controller: MusicController

    //Соеденить с Service
    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicBinder
            //get service
            musicService = binder.service
            //pass list
            musicService.setList(listSong)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        playIntent = Intent(this, MusicService::class.java)
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
        startService(playIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        musicRecyclerView = findViewById(R.id.recycler_view_for_song)
        musicRecyclerView.layoutManager = LinearLayoutManager(this)
        getSongList()
        listSong.sortWith(Comparator { a, b -> a?.title!!.compareTo(b!!.title) })

        val songAdapter = SongAdapter(this, listSong)
        songAdapter.notifyDataSetChanged()
        musicRecyclerView.adapter = songAdapter
        setController()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_shuffle -> {
                musicService.setShuffle()
            }
            R.id.action_end -> {
                stopService(playIntent)
                exitProcess(0)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        stopService(playIntent)
        super.onDestroy()
    }

    fun songPicked(view: View) {
        musicService.setSong(view.tag.toString().toInt())
        musicService.playSong()

    }

    private fun getSongList() {
        val musicResolver: ContentResolver = contentResolver
        val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var musicCursor: Cursor? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            musicCursor = musicResolver.query(musicUri, null, null, null, null)
        }

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val titleColumn: Int =
                musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn: Int =
                musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val artistColumn: Int =
                musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            //add song to list
            do {
                val songId: Long = musicCursor.getLong(idColumn)
                val songTitle: String = musicCursor.getString(titleColumn)
                val songArtist: String = musicCursor.getString(artistColumn)
                listSong.add(Song(songId, songTitle, songArtist))
            } while (musicCursor.moveToNext())
        }
    }

    private fun setController() {
        controller = MusicController(this)
        controller.setPrevNextListeners({ playNext() }, { playPrev() })
        controller.setMediaPlayer(this)
        controller.setAnchorView(findViewById(R.id.recycler_view_for_song))
    }

    private fun playNext() {
        musicService.playNext()
        controller.show(0)
    }

    private fun playPrev() {
        musicService.playPrev()
        controller.show(0)
    }

    override fun start() {
        musicService.go()
    }

    override fun pause() {
        musicService.pausePlayer()
    }

    override fun seekTo(pos: Int) {
        musicService.seek(pos)
    }

    override fun isPlaying(): Boolean {
        return if (musicBound) {
            musicService.isPng()
        } else false
    }

    override fun getDuration(): Int {
        return if (musicBound && musicService.isPng()) {
            musicService.getDur()
        } else 0
    }

    override fun getCurrentPosition(): Int {
        return if (musicBound && musicService.isPng()) {
            musicService.getPosn()
        } else 0
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getBufferPercentage(): Int {
        TODO("Not yet implemented")
    }

    override fun getAudioSessionId(): Int {
        TODO("Not yet implemented")
    }

}
