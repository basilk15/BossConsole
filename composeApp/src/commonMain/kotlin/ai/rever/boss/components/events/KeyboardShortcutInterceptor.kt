package ai.rever.boss.components.events

import ai.rever.boss.keymap.KeymapSettingsManager
import ai.rever.boss.keymap.ShortcutReleaseTracker
import ai.rever.boss.keymap.handler.KeymapMatcher
import ai.rever.boss.keymap.model.ShortcutContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch

/**
 * Set of modifier-only keys that should not trigger shortcut matching.
 * These keys don't have a standalone action and are only used in combination.
 */
internal val MODIFIER_ONLY_KEYS =
    setOf(
        Key.CapsLock,
        Key.ShiftLeft,
        Key.ShiftRight,
        Key.CtrlLeft,
        Key.CtrlRight,
        Key.AltLeft,
        Key.AltRight,
        Key.MetaLeft,
        Key.MetaRight,
        Key.NumLock,
        Key.ScrollLock,
    )

/**
 * Creates a modifier that intercepts keyboard events and routes matched shortcuts
 * to KeyboardEventBus, preventing the wrapped component from receiving them.
 *
 * Use this to wrap components that consume all keyboard input (like terminals, browsers)
 * to ensure global/workspace shortcuts still work.
 *
 * @param windowId The current window ID for event routing
 * @param source The event source identifier (e.g., COMPONENT_TERMINAL, COMPONENT_BROWSER)
 * @param context The shortcut context for matching (e.g., TERMINAL, BROWSER)
 * @return Modifier that intercepts matched shortcuts
 */
@Composable
fun Modifier.interceptKeyboardShortcuts(
    windowId: String,
    source: KeyEventSource,
    context: ShortcutContext,
): Modifier {
    val settings by KeymapSettingsManager.currentSettings.collectAsState()
    val matcher = remember(settings) { KeymapMatcher(settings) }
    val coroutineScope = rememberCoroutineScope()
    val pendingShortcuts = remember { ShortcutReleaseTracker<Long, KeyboardEvent>() }

    return this.onPreviewKeyEvent { keyEvent ->
        when (keyEvent.type) {
            KeyEventType.KeyDown -> {
                // A non-modifier key that is not the repeated primary key cancels the current
                // gesture. This prevents a key-up delivered after an intervening gesture from
                // firing a stale shortcut.
                if (keyEvent.key !in MODIFIER_ONLY_KEYS && !pendingShortcuts.isPending(keyEvent.key.keyCode)) {
                    pendingShortcuts.clear()
                }

                // Skip modifier-only keys; they can cancel a pending gesture on release, but
                // never match a shortcut on their own.
                if (keyEvent.key in MODIFIER_ONLY_KEYS) return@onPreviewKeyEvent false

                val binding = matcher.match(keyEvent, context)
                if (binding == null) return@onPreviewKeyEvent false

                val pendingEvent =
                    KeyboardEvent(
                        // Keep the matching key-down event so handlers still see the original
                        // modifiers when the event is emitted after key-up.
                        keyEvent = keyEvent,
                        source = source,
                        context = context,
                        sourceWindowId = windowId,
                    )
                pendingShortcuts.arm(keyEvent.key.keyCode, pendingEvent)
                true // Consume the key-down, including auto-repeat, until the primary key-up.
            }

            KeyEventType.KeyUp -> {
                if (keyEvent.key in MODIFIER_ONLY_KEYS) {
                    // Releasing a modifier before the primary key cancels the chord. It must not
                    // accidentally invoke the pending action.
                    pendingShortcuts.clear()
                    return@onPreviewKeyEvent false
                }

                val pendingEvent = pendingShortcuts.release(keyEvent.key.keyCode) ?: return@onPreviewKeyEvent false
                coroutineScope.launch { KeyboardEventBus.emit(pendingEvent) }
                true // Consume the primary key-up after dispatching the pending shortcut.
            }

            else -> false
        }
    }
}

/**
 * Composable wrapper that intercepts keyboard shortcuts before they reach child content.
 * Convenience wrapper around [interceptKeyboardShortcuts] modifier.
 *
 * @param windowId The current window ID for event routing
 * @param source The event source identifier
 * @param context The shortcut context for matching
 * @param content The content to wrap
 */
@Composable
fun KeyboardShortcutInterceptor(
    windowId: String,
    source: KeyEventSource,
    context: ShortcutContext,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .interceptKeyboardShortcuts(windowId, source, context),
    ) {
        content()
    }
}
