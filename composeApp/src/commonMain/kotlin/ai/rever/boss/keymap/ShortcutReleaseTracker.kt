package ai.rever.boss.keymap

/**
 * Tracks shortcuts that have been recognized on key-down and are waiting for their primary key
 * to be released.
 *
 * A key repeat must keep the original pending value rather than arm a second invocation. Callers
 * clear the tracker when the surrounding focus or shortcut gesture is cancelled.
 */
internal class ShortcutReleaseTracker<K, V> {
    private val pending = mutableMapOf<K, V>()

    /**
     * Arms [value] for [key]. Returns true only for the first key-down in a physical key press.
     */
    fun arm(
        key: K,
        value: V,
    ): Boolean {
        if (pending.containsKey(key)) return false
        pending[key] = value
        return true
    }

    /**
     * Completes and removes the pending value for [key].
     */
    fun release(key: K): V? = pending.remove(key)

    /**
     * Whether [key] currently has a pending shortcut.
     */
    fun isPending(key: K): Boolean = key in pending

    /**
     * Cancels pending shortcuts whose values match [predicate].
     */
    fun cancelWhere(predicate: (V) -> Boolean) {
        pending.entries.removeAll { predicate(it.value) }
    }

    /**
     * Cancels a pending shortcut for [key].
     */
    fun cancel(key: K) {
        pending.remove(key)
    }

    /**
     * Cancels every pending shortcut, for example after focus loss.
     */
    fun clear() {
        pending.clear()
    }
}
