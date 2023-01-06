package com.flammky.musicplayer.domain.musiclib.service.provider

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.flammky.android.medialib.R
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemFactory.orEmpty
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemInfo
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.PlayerExtension.isOngoing
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.PlayerExtension.isStateBuffering
import com.flammky.musicplayer.dump.mediaplayer.helper.Preconditions.checkState
import timber.log.Timber

class MediaNotificationProvider(
    private val context: Context,
    private var itemInfoIntentConverter: MediaItemInfo.IntentConverter? = null,
) {
	private val actionBroadcastReceiver: BroadcastReceiver = ProviderBroadcastReceiver()
	private var actionReceiver: ActionReceiver? = null

	var isReleased: Boolean = false
		private set

	fun setActionReceiver(receiver: ActionReceiver) {
		if (isReleased) {
			return
		}

		if (actionReceiver == null) {
			context.registerReceiver(actionBroadcastReceiver, getIntentFilter())
		}

		actionReceiver = receiver
	}

	fun release() {
		if (isReleased) {
			checkState(actionReceiver == null)
			return
		}
		context.unregisterReceiver(actionBroadcastReceiver)
		actionReceiver = null
		isReleased = true
	}

	private fun getMediaNotificationBuilder(
		player: Player,
		largeIcon: Bitmap?,
		onGoing: Boolean,
		channelId: String
	): NotificationCompat.Builder {
		return NotificationCompat.Builder(context, channelId)
			.apply {

				val contentIntent = PendingIntent
					.getActivity(
						context, 445, context.packageManager.getLaunchIntentForPackage(context.packageName),
						PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
					)

				val mediaItem = player.currentMediaItem.orEmpty()
				Timber.d("provider ${mediaItem.mediaMetadata.albumArtist}")

				setChannelId(channelId)
				setContentIntent(contentIntent)

				setContentTitle(
					if (!mediaItem.mediaMetadata.title.isNullOrEmpty()) {
						mediaItem.mediaMetadata.title
					} else {
						mediaItem.mediaMetadata.displayTitle ?: "<Unknown>"
					}
				)
				setContentText(
					if (!mediaItem.mediaMetadata.albumArtist.isNullOrEmpty()) {
						mediaItem.mediaMetadata.albumArtist
					} else {
						mediaItem.mediaMetadata.artist ?: "<Unknown>"
					}
				)
				setSmallIcon(R.drawable.blu)

				setColorized(true)
				setOngoing(player.playbackState.isOngoing())
				setShowWhen(false)

				val subText = mediaItem.mediaMetadata.albumTitle
				if (!subText.isNullOrEmpty()) setSubText(subText)

				val repeatAction = when (player.repeatMode) {
					Player.REPEAT_MODE_OFF -> makeActionRepeatOffToOne(mediaItem)
					Player.REPEAT_MODE_ONE -> makeActionRepeatOneToAll(mediaItem)
					Player.REPEAT_MODE_ALL -> makeActionRepeatAllToOff(mediaItem)
					else -> throw IllegalStateException()
				}

				val prevAction =
					if (player.hasPreviousMediaItem()) {
						makeActionPrev(mediaItem)
					} else {
						makeActionPrevDisabled(mediaItem)
					}

				val showPlayAction =
					if (player.playbackState.isStateBuffering()) {
						!player.playWhenReady
					} else {
						!player.isPlaying
					}

				val playAction =
					if (showPlayAction) {
						makeActionPlay(mediaItem)
					} else {
						makeActionPause(mediaItem)
					}

				val nextAction =
					if (player.hasNextMediaItem()) {
						makeActionNext(mediaItem)
					} else {
						makeActionNextDisabled(mediaItem)
					}

				val cancelOrStopAction =
					if (onGoing) {
						makeActionStop(mediaItem)
					} else {
						makeActionCancel(mediaItem)
					}

				addAction(repeatAction)
				addAction(prevAction)
				addAction(playAction)
				addAction(nextAction)
				addAction(cancelOrStopAction)

				val fLargeIcon =
					if (largeIcon == null || largeIcon.width < 64 || largeIcon.height < 64) null else largeIcon

				setLargeIcon(fLargeIcon)

				val dismissIntent = makeDismissPendingIntent(mediaItem)
				setDeleteIntent(dismissIntent)
			}
	}

	private fun getFilledMediaNotificationBuilder(
		channelId: String,
		title: String?,
		subtitle: String?,
		subText: String?,
		onGoing: Boolean,
		repeatMode: Int,
		playerState: Int,
		hasPreviousMediaItem: Boolean,
		hasNextMediaItem: Boolean,
		playWhenReady: Boolean,
		isPlaying: Boolean,
		largeIcon: Bitmap?,
	): NotificationCompat.Builder {
		return NotificationCompat.Builder(context, channelId)
			.apply {
				val contentIntent = PendingIntent
					.getActivity(
						context, 445, context.packageManager.getLaunchIntentForPackage(context.packageName),
						PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
					)

				setChannelId(channelId)
				setContentIntent(contentIntent)

				setContentTitle(title ?: "")
				setContentText(subtitle ?: "")
				setSmallIcon(R.drawable.blu)

				setColorized(true)
				setOngoing(onGoing)
				setShowWhen(false)

				if (!subText.isNullOrEmpty()) setSubText(subText)

				val repeatAction = when (repeatMode) {
					Player.REPEAT_MODE_OFF -> makeActionRepeatOffToOne()
					Player.REPEAT_MODE_ONE -> makeActionRepeatOneToAll()
					Player.REPEAT_MODE_ALL -> makeActionRepeatAllToOff()
					else -> throw IllegalStateException()
				}

				val prevAction =
					if (hasPreviousMediaItem) {
						makeActionPrev()
					} else {
						makeActionPrevDisabled()
					}

				val showPlayAction =
					if (playerState.isStateBuffering()) {
						!playWhenReady
					} else {
						!isPlaying
					}

				val playAction =
					if (showPlayAction) {
						makeActionPlay()
					} else {
						makeActionPause()
					}

				val nextAction =
					if (hasNextMediaItem) {
						makeActionNext()
					} else {
						makeActionNextDisabled()
					}

				val cancelOrStopAction =
					if (onGoing) {
						makeActionStop()
					} else {
						makeActionCancel()
					}

				addAction(repeatAction)
				addAction(prevAction)
				addAction(playAction)
				addAction(nextAction)
				addAction(cancelOrStopAction)

				val fLargeIcon =
					if (largeIcon == null || largeIcon.width < 64 || largeIcon.height < 64) null else largeIcon

				setLargeIcon(fLargeIcon)

				val dismissIntent = makeDismissPendingIntent()
				setDeleteIntent(dismissIntent)
			}
	}

	fun buildMediaStyleNotification(
		mediaSession: MediaSession,
		largeIcon: Bitmap?,
		onGoing: Boolean,
		channelId: String
	): Notification {
		val player = mediaSession.player
		val item = player.currentMediaItem.orEmpty()
		return getMediaNotificationBuilder(player, largeIcon, onGoing, channelId)
			.apply {
				val style = MediaStyleNotificationHelper.MediaStyle(mediaSession)
					.setShowActionsInCompactView(1, 2, 3)
					.setCancelButtonIntent(makeDismissPendingIntent(item))
					.setShowCancelButton(true)
				setStyle(style)
			}
			.build()
	}

	fun buildMediaStyleNotification(
		player: Player,
		largeIcon: Bitmap?,
		onGoing: Boolean,
		channelId: String
	): Notification {
		return getMediaNotificationBuilder(player, largeIcon, onGoing, channelId)
			.apply {
				val style = androidx.media.app.NotificationCompat.MediaStyle()
					.setShowActionsInCompactView(1, 2, 3)
					.setCancelButtonIntent(makeDismissPendingIntent(player.currentMediaItem.orEmpty()))
					.setShowCancelButton(true)
				setStyle(style)
			}
			.build()
	}

	fun buildMediaStyleNotification(
		mediaSession: MediaSession,
		channelId: String,
		title: String?,
		subtitle: String?,
		subText: String?,
		onGoing: Boolean,
		repeatMode: Int,
		playerState: Int,
		hasPreviousMediaItem: Boolean,
		hasNextMediaItem: Boolean,
		playWhenReady: Boolean,
		isPlaying: Boolean,
		largeIcon: Bitmap?,
	): Notification {
		return getFilledMediaNotificationBuilder(channelId, title, subtitle, subText, onGoing,
			repeatMode, playerState, hasPreviousMediaItem, hasNextMediaItem, playWhenReady, isPlaying,
			largeIcon
		).apply {
			val style = MediaStyleNotificationHelper.MediaStyle(mediaSession)
				.setShowActionsInCompactView(1, 2, 3)
				.setCancelButtonIntent(makeDismissPendingIntent())
				.setShowCancelButton(true)
			setStyle(style)
		}.build()
	}

	private fun makeDismissPendingIntent(item: MediaItem? = null): PendingIntent {
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Dismiss_NAME)
				setPackage(context.packageName)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		return PendingIntent.getBroadcast(context, ACTION_Dismiss_CODE, intent, flag)
	}

	private fun makeActionStop(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_stop
		val title = "Stop"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Stop_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_Stop_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionCancel(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_close
		val title = "Cancel"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Cancel_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_Cancel_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionRepeatOneToAll(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_repeat_one
		val title = "RepeatAll"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_RepeatOneToAll_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_RepeatOneToAll_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionRepeatOffToOne(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_repeat_off
		val title = "RepeatOne"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_RepeatOffToOne_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_RepeatOffToOne_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionRepeatAllToOff(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_repeat_all
		val title = "RepeatOff"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_RepeatAllToOff_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_RepeatAllToOff_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionPlay(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_play
		val title = "Play"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Play_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_Play_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionPause(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_pause
		val title = "Pause"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Pause_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_Pause_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionNext(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_next
		val title = "Next"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Next_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_Next_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionNextDisabled(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_next_disabled
		val title = "NoNext"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_NoNext_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_NoNext_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionPrev(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_prev
		val title = "Previous"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_Previous_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_Previous_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun makeActionPrevDisabled(item: MediaItem? = null): NotificationCompat.Action {
		val resId = R.drawable.ic_notif_prev_disabled
		val title = "noPrevious"
		val intent = newActionButtonIntent()
			.apply {
				applyActionExtra(ACTION_NoPrevious_NAME)
				if (item != null) itemInfoIntentConverter?.applyFromMediaItem(this, item)
			}
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val pIntent = PendingIntent.getBroadcast(context, ACTION_NoPrevious_CODE, intent, flag)
		return buildNotificationCompatAction(resId, title, pIntent)
	}

	private fun newActionButtonIntent() = Intent(ACTION_BUTTON_INTENT_NAME)
		.setPackage(context.packageName)

	private fun buildNotificationCompatAction(
		iconId: Int,
		title: CharSequence?,
		pendingIntent: PendingIntent?
	): NotificationCompat.Action {
		return NotificationCompat.Action.Builder(iconId, title, pendingIntent).build()
	}

	private fun Intent.applyActionExtra(value: String) = putExtra(ACTION_KEY_NAME, value)

	interface ActionReceiver {
		fun actionAny(context: Context, intent: Intent): Int
		fun actionPlay(context: Context, intent: Intent)
		fun actionPause(context: Context, intent: Intent)
		fun actionNext(context: Context, intent: Intent)
		fun actionPrevious(context: Context, intent: Intent)
		fun actionRepeatOffToOne(context: Context, intent: Intent)
		fun actionRepeatOneToAll(context: Context, intent: Intent)
		fun actionRepeatAllToOff(context: Context, intent: Intent)
		fun actionNoNext(context: Context, intent: Intent)
		fun actionNoPrevious(context: Context, intent: Intent)
		fun actionStop(context: Context, intent: Intent)
		fun actionCancel(context: Context, intent: Intent)
		fun actionDismiss(context: Context, intent: Intent)
	}

	private inner class ProviderBroadcastReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (context == null
				|| intent == null
			) return

			onReceiveImpl(context, intent)
		}

		fun onReceiveImpl(context: Context, intent: Intent) {
			val receiver = actionReceiver!!

			val action = intent.getStringExtra(ACTION_KEY_NAME) ?: return
			if (receiver.actionAny(context, intent) < 0) return

			when (action) {
				ACTION_Play_NAME -> receiver.actionPlay(context, intent)
				ACTION_Pause_NAME -> receiver.actionPause(context, intent)
				ACTION_Next_NAME -> receiver.actionNext(context, intent)
				ACTION_Previous_NAME -> receiver.actionPrevious(context, intent)
				ACTION_RepeatOffToOne_NAME -> receiver.actionRepeatOffToOne(context, intent)
				ACTION_RepeatOneToAll_NAME -> receiver.actionRepeatOneToAll(context, intent)
				ACTION_RepeatAllToOff_NAME -> receiver.actionRepeatAllToOff(context, intent)
				ACTION_Stop_NAME -> receiver.actionStop(context, intent)
				ACTION_Cancel_NAME -> receiver.actionCancel(context, intent)
				ACTION_Dismiss_NAME -> receiver.actionDismiss(context, intent)
				ACTION_NoNext_NAME -> receiver.actionNoNext(context, intent)
				ACTION_NoPrevious_NAME -> receiver.actionNoPrevious(context, intent)
			}
		}
	}


	companion object {
		private const val ACTION_BUTTON_INTENT_NAME = "MediaNotificationBuilder.IntentAction"
		private const val ACTION_KEY_NAME = "MediaNotificationBuilder.IntentActionKeyName"

		const val ACTION_Play_NAME = "MediaNotificationBuilder.Play"
		const val ACTION_Play_CODE = 1

		const val ACTION_Pause_NAME = "MediaNotificationBuilder.Pause"
		const val ACTION_Pause_CODE = 2

		const val ACTION_Previous_NAME = "MediaNotificationBuilder.Previous"
		const val ACTION_Previous_CODE = 3

		const val ACTION_NoPrevious_NAME = "MediaNotificationBuilder.NoPrevious"
		const val ACTION_NoPrevious_CODE = 31

		const val ACTION_Next_NAME = "MediaNotificationBuilder.Next"
		const val ACTION_Next_CODE = 4

		const val ACTION_NoNext_NAME = "MediaNotificationBuilder.NoNext"
		const val ACTION_NoNext_CODE = 41

		const val ACTION_RepeatOffToOne_NAME = "MediaNotificationBuilder.RepeatOffToOne"
		const val ACTION_RepeatOffToOne_CODE = 5

		const val ACTION_RepeatOneToAll_NAME = "MediaNotificationBuilder.RepeatOneToAll"
		const val ACTION_RepeatOneToAll_CODE = 6

		const val ACTION_RepeatAllToOff_NAME = "MediaNotificationBuilder.RepeatAllToOff"
		const val ACTION_RepeatAllToOff_CODE = 7

		const val ACTION_Stop_NAME = "MediaNotificationBuilder.Stop"
		const val ACTION_Stop_CODE = 8

		const val ACTION_Cancel_NAME = "MediaNotificationBuilder.Cancel"
		const val ACTION_Cancel_CODE = 81

		const val ACTION_Dismiss_NAME = "MediaNotificationBuilder.Dismiss"
		const val ACTION_Dismiss_CODE = 90


		fun getIntentFilter(): IntentFilter = IntentFilter(ACTION_BUTTON_INTENT_NAME)
		fun getIntentActionKeyName(): String = ACTION_KEY_NAME
	}
}
