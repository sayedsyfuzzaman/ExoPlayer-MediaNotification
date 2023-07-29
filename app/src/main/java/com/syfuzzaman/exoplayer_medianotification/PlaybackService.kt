package com.syfuzzaman.exoplayer_medianotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.collect.ImmutableList

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var callback = CustomMediaSessionCallback()
    private var notification_id = "1"
    private lateinit var nBuilder: NotificationCompat.Builder


    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri("https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")
            .setMimeType(MimeTypes.APPLICATION_MP4)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        this.setMediaNotificationProvider(object : MediaNotification.Provider{
            @RequiresApi(Build.VERSION_CODES.O)
            override fun createNotification(
                mediaSession: MediaSession,// this is the session we pass to style
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                createNotification(mediaSession)
                // notification should be created before you return here
                return MediaNotification(notification_id.toInt(),nBuilder.build())
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean {
                TODO("Not yet implemented")
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun  createNotification(session: MediaSession) {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel(notification_id,"Channel", NotificationManager.IMPORTANCE_LOW))

        nBuilder = NotificationCompat.Builder(this, notification_id)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle("FM Radio")
            .setContentText("Now Playing")
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.thumb))
            .setOngoing(true)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
//            .addAction(R.drawable.ic_stop, "Stop", null)
//            .addAction(R.drawable.ic_play, "Play", null)
//            .addAction(R.drawable.ic_pause, "Pause", null)

    }


    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        Log.d("PlaybackService", "Destroyed")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        stopForeground(true) // Stop the foreground service and remove the notification
        super.onDestroy()
    }


    // This example always accepts the connection request
    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    private inner class CustomMediaSessionCallback: MediaSession.Callback {
        // Configure commands available to the controller in onConnect()
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands =
                connectionResult.availableSessionCommands
                    .buildUpon()
                    // Add custom commands
                    .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands, connectionResult.availablePlayerCommands)
        }
    }
}