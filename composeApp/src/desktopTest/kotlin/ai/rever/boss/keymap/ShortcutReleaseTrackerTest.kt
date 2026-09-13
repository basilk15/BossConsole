package ai.rever.boss.keymap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShortcutReleaseTrackerTest {
    @Test
    fun `key-down arms without completing the action`() {
        val tracker = ShortcutReleaseTracker<String, String>()

        assertTrue(tracker.arm("N", "window.new"))
        assertTrue(tracker.isPending("N"))
        assertNull(tracker.release("other"))
        assertEquals("window.new", tracker.release("N"))
    }

    @Test
    fun `auto-repeat does not replace or duplicate the pending action`() {
        val tracker = ShortcutReleaseTracker<String, String>()

        assertTrue(tracker.arm("N", "first"))
        assertFalse(tracker.arm("N", "repeat"))
        assertEquals("first", tracker.release("N"))
        assertNull(tracker.release("N"))
    }

    @Test
    fun `cancellation prevents a later key-up from completing the action`() {
        val tracker = ShortcutReleaseTracker<String, String>()

        tracker.arm("N", "window.new")
        tracker.cancel("N")

        assertFalse(tracker.isPending("N"))
        assertNull(tracker.release("N"))
    }

    @Test
    fun `clear cancels every pending shortcut`() {
        val tracker = ShortcutReleaseTracker<String, String>()

        tracker.arm("N", "window.new")
        tracker.arm("W", "window.close")
        tracker.clear()

        assertNull(tracker.release("N"))
        assertNull(tracker.release("W"))
    }

    @Test
    fun `cancelWhere only removes matching modifier chords`() {
        val tracker = ShortcutReleaseTracker<String, Set<String>>()

        tracker.arm("N", setOf("ctrl"))
        tracker.arm("P", setOf("alt"))
        tracker.cancelWhere { "ctrl" in it }

        assertNull(tracker.release("N"))
        assertEquals(setOf("alt"), tracker.release("P"))
    }
}
