/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jing.sakura.player

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.FastForwardAction
import androidx.leanback.widget.PlaybackControlsRow.RewindAction
import androidx.navigation.NavController
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Custom [PlaybackTransportControlGlue] that exposes a callback when the progress is updated.
 *
 * The callback is triggered based on a progress interval defined in several ways depending on the
 * [PlayerAdapter].
 *
 * [LeanbackPlayerAdapter] example:
 * ```
 *     private val updateMillis = 16
 *     LeanbackPlayerAdapter(context, exoplayer, updateMillis)
 * ```
 *
 * [MediaPlayerAdapter] example:
 * ```
 *     object : MediaPlayerAdapter(context) {
 *         private val updateMillis = 16
 *         override fun getProgressUpdatingInterval(): Int {
 *             return updateMillis
 *         }
 *     }
 * ```
 */
class ProgressTransportControlGlue<T : PlayerAdapter>(
    context: Context,
    private val lifeCycleScope: CoroutineScope,
    private val navController: NavController,
    impl: T,
    private val updateProgress: () -> Unit
) : PlaybackTransportControlGlue<T>(context, impl) {

    private var backPressed = false

    // Define actions for fast forward and rewind operations.
    @VisibleForTesting
    var skipForwardAction: FastForwardAction = FastForwardAction(context)

    @VisibleForTesting
    var skipBackwardAction: RewindAction = RewindAction(context)

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        // super.onCreatePrimaryActions() will create the play / pause action.
        super.onCreatePrimaryActions(primaryActionsAdapter)

        // Add the rewind and fast forward actions following the play / pause action.
        primaryActionsAdapter.apply {
            add(skipBackwardAction)
            add(skipForwardAction)
        }
    }

    override fun onUpdateProgress() {
        super.onUpdateProgress()
        updateProgress()
    }

    override fun onActionClicked(action: Action) {
        // Primary actions are handled manually. The superclass handles default play/pause action.
        when (action) {
            skipBackwardAction -> skipBackward()
            skipForwardAction -> skipForward()
            else -> super.onActionClicked(action)
        }
    }

    /** Skips backward 30 seconds.  */
    private fun skipBackward() {
        var newPosition: Long = currentPosition - THIRTY_SECONDS
        newPosition = newPosition.coerceAtLeast(0L)
        playerAdapter.seekTo(newPosition)
    }

    /** Skips forward 30 seconds.  */
    private fun skipForward() {
        var newPosition: Long = currentPosition + THIRTY_SECONDS
        newPosition = newPosition.coerceAtMost(duration)
        playerAdapter.seekTo(newPosition)
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        // 进度条隐藏时,直接按ok键开始播放或者暂停
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && !host.isControlsOverlayVisible) {
            if (playerAdapter.isPlaying) {
                playerAdapter.pause()
                host.showControlsOverlay(true)
            } else {
                playerAdapter.play()
            }
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 播放完成时,点返回键直接退出
            if (!playerAdapter.isPlaying && playerAdapter.duration - playerAdapter.currentPosition < 1000) {
                navController.popBackStack()
            } else if (playerAdapter.isPlaying) {
                if (backPressed) {
                    navController.popBackStack()
                    return true
                }
                if (host.isControlsOverlayVisible) {
                    host.hideControlsOverlay(true)
                }
                Toast.makeText(context, "再按一次退出播放", Toast.LENGTH_SHORT).show()
                backPressed = true
                lifeCycleScope.launch {
                    delay(2000L)
                    backPressed = false
                }
            }
            return true
        }

        // info键控制进度条显示或者隐藏
        if (keyCode == KeyEvent.KEYCODE_INFO) {
            if (host.isControlsOverlayVisible) {
                host.hideControlsOverlay(true)
            } else {
                host.showControlsOverlay(true)
            }
            return true
        }

        return super.onKey(v, keyCode, event)
    }

    companion object {
        private val THIRTY_SECONDS = TimeUnit.SECONDS.toMillis(30)
    }
}
