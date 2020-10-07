package ru.fulkin.musicapplication.service

import android.app.*
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import ru.fulkin.musicapplication.R
import ru.fulkin.musicapplication.Song
import ru.fulkin.musicapplication.activities.MainMusicActivity
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MusicService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private lateinit var player: MediaPlayer
    private lateinit var songs: ArrayList<Song>
    private var songPosition: Int = 0
    private val musicBind = MusicBinder() as IBinder
    private val NOTIFY_ID: Int = 1
    private var shuffle: Boolean = false
    private lateinit var random: Random

    override fun onCreate() {
        super.onCreate()
        songPosition = 0
        player = MediaPlayer()
        initMusicPlayer()
        random = Random()
    }

    private fun initMusicPlayer() {
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)
        player.setOnPreparedListener(this)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    fun setList(songs: ArrayList<Song>) {
        this.songs = songs
    }

    fun setSong(songIndex: Int) {
        songPosition = songIndex
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    fun playSong() {
        //need for reset because this code we use for every song
        player.reset()
        //get song
        val nowPlaySong: Song = songs[songPosition]
        //get ID
        val currentSong: Long = nowPlaySong.id
        //set Uri
        val trackUri: Uri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSong
        )
        try {
            player.setDataSource(applicationContext, trackUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "RError setting data source", e)
        }
        player.prepareAsync()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player.stop()
        player.release()
        return false
    }

    override fun onPrepared(mp: MediaPlayer?) {
        //start playback
        mp?.start()

        val notIntent = Intent(this, MainMusicActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = songs[songPosition].title
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("music_service", "background_service")
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        val builderNotification = Notification.Builder(this, channelId)
        builderNotification.setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_ctiom_play)
            .setTicker(title)
            .setOngoing(true)
            .setContentTitle("Playing")
            .setContentText(title)
        val notification = builderNotification.build() as Notification

        startForeground(NOTIFY_ID, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    fun setShuffle() {
        shuffle = !shuffle
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (player.currentPosition > 0) {
            mp?.reset()
            playNext()
        }
    }

    fun playNext() {
        if (shuffle) {
            var newSong = songPosition
            while (newSong == songPosition) {
                newSong = random.nextInt(songs.size)
            }
            songPosition = newSong
        } else {
            songPosition++
            if (songPosition > songs.size - 1) {
                songPosition = 0
            }
            playSong()
        }
    }

    fun playPrev() {
        songPosition--
        if (songPosition < 0) {
            songPosition = songs.size - 1
        }
        playSong()
    }

    fun getPosn(): Int {
        return player.currentPosition
    }

    fun getDur(): Int {
        return player.duration
    }

    fun isPng(): Boolean {
        return player.isPlaying
    }

    fun pausePlayer() {
        player.pause()
    }

    fun seek(posn: Int) {
        player.seekTo(posn)
    }

    fun go() {
        player.start()
    }
}