package com.jad.discordbot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.slf4j.LoggerFactory
import java.util.stream.Collectors

class AudioTrackScheduler(private val audioPlayer: AudioPlayer) : AudioEventAdapter() {
//    val service = Executors.newCachedThreadPool()

    val queue: ArrayList<AudioTrack> = ArrayList()
    var playing: Boolean = false
    var nextTrackPlaytop: Boolean = false
    var loopingTrack: Boolean = false
    var loopingPlaylist: Boolean = false

    fun play(track: AudioTrack, addToQueue: Boolean) {
        val started = audioPlayer.startTrack(track, addToQueue)
        playing = true
        if (!started) {
            if (nextTrackPlaytop) {
                logger.info("Track added first in queue: " + track.info.title)
                queue.add(0, track)
                nextTrackPlaytop = false
            } else {
                logger.info("Track added last in queue: " + track.info.title)
                queue.add(track)
            }
        }
    }

    fun play(playlist: AudioPlaylist, addToQueue: Boolean) {
        var playtop = false
        if (nextTrackPlaytop) playtop = true
        if (!playtop) {
            for (track in playlist.tracks) {
                this.play(track, addToQueue)
            }
        } else {
            for (i in playlist.tracks.indices) {
                nextTrackPlaytop = true
                this.play(playlist.tracks[playlist.tracks.size - 1 - i], addToQueue)
            }
        }
        nextTrackPlaytop = false
    }

    fun setPaused(paused: Boolean) {
        audioPlayer.isPaused = paused
    }

    fun nextTrack() {
        if (queue.isEmpty()) {
            audioPlayer.stopTrack()
            playing = false
//            service.submit { handleEndOfQueueWithLastActiveMessage(sendEndOfQueue) }
        } else {
            this.play(queue.removeAt(0), false)
        }
    }

    fun clear() {
        queue.clear()
    }

    fun shuffle() {
        queue.shuffle()
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        logger.info("Track Ended: $endReason")
        if (endReason.mayStartNext) {
            if (loopingTrack) {
                audioPlayer.startTrack(track.makeClone(), false)
                return
            }

            if (loopingPlaylist) {
                queue.add(track.makeClone())
                nextTrack()
                return
            }

            nextTrack()
        }
    }


    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException) {
        logger.error("Exception while playing audio track: ${exception.message}")
    }

//    fun handleEndOfQueue(@Nonnull context: Command, sendEndOfQueue: Boolean) {
//        if (sendEndOfQueue && AudioHandler.getDefaultAudioHandler().musicManagers.containsKey(
//                context.getGuild().getIdLong()
//            )
//        ) {
//            context.makeSuccess(context.i18nRaw("music.internal.queueHasEnded"))
//                .queue { queueMessage ->
//                    queueMessage.delete().queueAfter(45, TimeUnit.SECONDS, null, RestActionUtil.IGNORE)
//                }
//        }
//        LavalinkManager.LavalinkManagerHolder.LAVALINK.closeConnection(context.getGuild())
//        val manager: GuildMusicManager =
//            AudioHandler.getDefaultAudioHandler().musicManagers.get(context.getGuild().getIdLong())
//        manager.getPlayer().removeListener(this)
//        if (LavalinkManager.LavalinkManagerHolder.LAVALINK.isEnabled()) {
//            if (manager.getPlayer() is LavalinkPlayer) {
//                val player: LavalinkPlayer = manager.getPlayer() as LavalinkPlayer
//                if (player.getLink() != null && isNodeStateDestroyed(player.getLink().getState())) {
//                    player.getLink().destroy()
//                }
//            }
//        } else {
//            context.getGuild().getAudioManager().setSendingHandler(null)
//        }
//        AudioHandler.getDefaultAudioHandler().musicManagers.remove(
//            context.getGuild().getIdLong()
//        )
//    }

//    fun handleEndOfQueueWithLastActiveMessage(sendEndOfQueue: Boolean) {
//        handleEndOfQueue(manager.getLastActiveMessage(), sendEndOfQueue)
//    }

    fun describePlayList(): String {
        logger.info(queue.size.toString())
        val collectedQueueInfo =
            queue.stream().map { audioTrack: AudioTrack -> "${audioTrack.info.title} (${audioTrack.info.author})" }
                .collect(Collectors.toList())

        var result = "Currently not Playing any song\n"
        if (audioPlayer.playingTrack != null) {
            result =
                "Currently Playing: ${audioPlayer.playingTrack.info.title} (${audioPlayer.playingTrack.info.author})\n"
        }
        if (queue.isNotEmpty()) {
            result += "Next:\n${collectedQueueInfo.joinToString("\n")}"
        }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}