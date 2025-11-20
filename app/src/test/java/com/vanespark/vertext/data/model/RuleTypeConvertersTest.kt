package com.vanespark.vertext.data.model

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for RuleTypeConverters
 * Tests JSON serialization and deserialization of rule components
 */
class RuleTypeConvertersTest {

    private lateinit var converters: RuleTypeConverters

    @Before
    fun setup() {
        converters = RuleTypeConverters()
    }

    // ========== Trigger Conversion Tests ==========

    @Test
    fun `should convert Always trigger to JSON and back`() {
        val triggers = listOf(RuleTrigger.Always)

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleTrigger.Always)
    }

    @Test
    fun `should convert FromSender trigger to JSON and back`() {
        val triggers = listOf(RuleTrigger.FromSender("+1234567890"))

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleTrigger.FromSender)
        assertEquals("+1234567890", (result[0] as RuleTrigger.FromSender).phoneNumber)
    }

    @Test
    fun `should convert ContainsKeyword trigger to JSON and back`() {
        val triggers = listOf(
            RuleTrigger.ContainsKeyword(
                keywords = listOf("urgent", "important"),
                caseSensitive = true
            )
        )

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleTrigger.ContainsKeyword)
        val trigger = result[0] as RuleTrigger.ContainsKeyword
        assertEquals(2, trigger.keywords.size)
        assertTrue(trigger.keywords.contains("urgent"))
        assertTrue(trigger.keywords.contains("important"))
        assertTrue(trigger.caseSensitive)
    }

    @Test
    fun `should convert ContainsKeyword trigger with case insensitive to JSON and back`() {
        val triggers = listOf(
            RuleTrigger.ContainsKeyword(
                keywords = listOf("test"),
                caseSensitive = false
            )
        )

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleTrigger.ContainsKeyword)
        val trigger = result[0] as RuleTrigger.ContainsKeyword
        assertEquals(listOf("test"), trigger.keywords)
        assertEquals(false, trigger.caseSensitive)
    }

    @Test
    fun `should convert TimeRange trigger to JSON and back`() {
        val triggers = listOf(RuleTrigger.TimeRange(startHour = 9, endHour = 17))

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleTrigger.TimeRange)
        val trigger = result[0] as RuleTrigger.TimeRange
        assertEquals(9, trigger.startHour)
        assertEquals(17, trigger.endHour)
    }

    @Test
    fun `should convert DaysOfWeek trigger to JSON and back`() {
        val triggers = listOf(
            RuleTrigger.DaysOfWeek(
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            )
        )

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleTrigger.DaysOfWeek)
        val trigger = result[0] as RuleTrigger.DaysOfWeek
        assertEquals(3, trigger.days.size)
        assertTrue(trigger.days.contains(DayOfWeek.MONDAY))
        assertTrue(trigger.days.contains(DayOfWeek.WEDNESDAY))
        assertTrue(trigger.days.contains(DayOfWeek.FRIDAY))
    }

    @Test
    fun `should convert multiple triggers to JSON and back`() {
        val triggers = listOf(
            RuleTrigger.FromSender("+1234567890"),
            RuleTrigger.ContainsKeyword(listOf("urgent"), false),
            RuleTrigger.TimeRange(9, 17)
        )

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(3, result.size)
        assertTrue(result[0] is RuleTrigger.FromSender)
        assertTrue(result[1] is RuleTrigger.ContainsKeyword)
        assertTrue(result[2] is RuleTrigger.TimeRange)
    }

    // ========== Condition Conversion Tests ==========

    @Test
    fun `should convert IsUnread condition to JSON and back`() {
        val conditions = listOf(RuleCondition.IsUnread)

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleCondition.IsUnread)
    }

    @Test
    fun `should convert MatchesPattern condition to JSON and back`() {
        val conditions = listOf(RuleCondition.MatchesPattern("\\d{6}"))

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleCondition.MatchesPattern)
        assertEquals("\\d{6}", (result[0] as RuleCondition.MatchesPattern).pattern)
    }

    @Test
    fun `should convert SenderInContacts condition to JSON and back`() {
        val conditions = listOf(RuleCondition.SenderInContacts)

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleCondition.SenderInContacts)
    }

    @Test
    fun `should convert SenderNotInContacts condition to JSON and back`() {
        val conditions = listOf(RuleCondition.SenderNotInContacts)

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleCondition.SenderNotInContacts)
    }

    @Test
    fun `should convert ThreadCategory condition to JSON and back`() {
        val conditions = listOf(RuleCondition.ThreadCategory("Work"))

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleCondition.ThreadCategory)
        assertEquals("Work", (result[0] as RuleCondition.ThreadCategory).category)
    }

    @Test
    fun `should convert multiple conditions to JSON and back`() {
        val conditions = listOf(
            RuleCondition.IsUnread,
            RuleCondition.SenderInContacts,
            RuleCondition.ThreadCategory("Work")
        )

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(3, result.size)
        assertTrue(result[0] is RuleCondition.IsUnread)
        assertTrue(result[1] is RuleCondition.SenderInContacts)
        assertTrue(result[2] is RuleCondition.ThreadCategory)
    }

    // ========== Action Conversion Tests ==========

    @Test
    fun `should convert AutoReply action to JSON and back`() {
        val actions = listOf(RuleAction.AutoReply("Thanks for your message"))

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.AutoReply)
        assertEquals("Thanks for your message", (result[0] as RuleAction.AutoReply).message)
    }

    @Test
    fun `should convert SetCategory action to JSON and back`() {
        val actions = listOf(RuleAction.SetCategory("Work"))

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.SetCategory)
        assertEquals("Work", (result[0] as RuleAction.SetCategory).category)
    }

    @Test
    fun `should convert MarkAsRead action to JSON and back`() {
        val actions = listOf(RuleAction.MarkAsRead)

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.MarkAsRead)
    }

    @Test
    fun `should convert Archive action to JSON and back`() {
        val actions = listOf(RuleAction.Archive)

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.Archive)
    }

    @Test
    fun `should convert MuteNotifications action to JSON and back`() {
        val actions = listOf(RuleAction.MuteNotifications)

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.MuteNotifications)
    }

    @Test
    fun `should convert PinConversation action to JSON and back`() {
        val actions = listOf(RuleAction.PinConversation)

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.PinConversation)
    }

    @Test
    fun `should convert BlockSender action to JSON and back`() {
        val actions = listOf(RuleAction.BlockSender)

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.BlockSender)
    }

    @Test
    fun `should convert CustomNotification action to JSON and back`() {
        val actions = listOf(
            RuleAction.CustomNotification(
                soundUri = "content://media/notification.mp3",
                priority = NotificationPriority.HIGH
            )
        )

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is RuleAction.CustomNotification)
        val action = result[0] as RuleAction.CustomNotification
        assertEquals("content://media/notification.mp3", action.soundUri)
        assertEquals(NotificationPriority.HIGH, action.priority)
    }

    @Test
    fun `should convert multiple actions to JSON and back`() {
        val actions = listOf(
            RuleAction.MarkAsRead,
            RuleAction.SetCategory("Work"),
            RuleAction.Archive,
            RuleAction.AutoReply("Auto-reply message")
        )

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(4, result.size)
        assertTrue(result[0] is RuleAction.MarkAsRead)
        assertTrue(result[1] is RuleAction.SetCategory)
        assertTrue(result[2] is RuleAction.Archive)
        assertTrue(result[3] is RuleAction.AutoReply)
    }

    // ========== Empty List Tests ==========

    @Test
    fun `should handle empty trigger list`() {
        val triggers = emptyList<RuleTrigger>()

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle empty condition list`() {
        val conditions = emptyList<RuleCondition>()

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle empty action list`() {
        val actions = emptyList<RuleAction>()

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertTrue(result.isEmpty())
    }

    // ========== Complex Rule Tests ==========

    @Test
    fun `should convert complex rule with all components`() {
        val triggers = listOf(
            RuleTrigger.FromSender("+1234567890"),
            RuleTrigger.ContainsKeyword(listOf("urgent", "asap"), false),
            RuleTrigger.TimeRange(9, 17),
            RuleTrigger.DaysOfWeek(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        )

        val conditions = listOf(
            RuleCondition.IsUnread,
            RuleCondition.SenderInContacts,
            RuleCondition.ThreadCategory("Work")
        )

        val actions = listOf(
            RuleAction.MarkAsRead,
            RuleAction.SetCategory("Important"),
            RuleAction.AutoReply("I'll get back to you soon")
        )

        val triggersJson = converters.fromTriggerList(triggers)
        val conditionsJson = converters.fromConditionList(conditions)
        val actionsJson = converters.fromActionList(actions)

        val triggersResult = converters.toTriggerList(triggersJson)
        val conditionsResult = converters.toConditionList(conditionsJson)
        val actionsResult = converters.toActionList(actionsJson)

        assertEquals(triggers.size, triggersResult.size)
        assertEquals(conditions.size, conditionsResult.size)
        assertEquals(actions.size, actionsResult.size)
    }

    // ========== JSON Format Tests ==========

    @Test
    fun `trigger JSON should be valid JSON array`() {
        val triggers = listOf(RuleTrigger.Always)
        val json = converters.fromTriggerList(triggers)

        assertNotNull(json)
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    @Test
    fun `condition JSON should be valid JSON array`() {
        val conditions = listOf(RuleCondition.IsUnread)
        val json = converters.fromConditionList(conditions)

        assertNotNull(json)
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    @Test
    fun `action JSON should be valid JSON array`() {
        val actions = listOf(RuleAction.MarkAsRead)
        val json = converters.fromActionList(actions)

        assertNotNull(json)
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    // ========== Special Characters Tests ==========

    @Test
    fun `should handle special characters in phone number`() {
        val triggers = listOf(RuleTrigger.FromSender("+1 (234) 567-8900"))

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        assertEquals("+1 (234) 567-8900", (result[0] as RuleTrigger.FromSender).phoneNumber)
    }

    @Test
    fun `should handle special characters in keywords`() {
        val triggers = listOf(
            RuleTrigger.ContainsKeyword(
                keywords = listOf("test@example.com", "price: $100"),
                caseSensitive = false
            )
        )

        val json = converters.fromTriggerList(triggers)
        val result = converters.toTriggerList(json)

        assertEquals(1, result.size)
        val trigger = result[0] as RuleTrigger.ContainsKeyword
        assertTrue(trigger.keywords.contains("test@example.com"))
        assertTrue(trigger.keywords.contains("price: $100"))
    }

    @Test
    fun `should handle special characters in auto-reply message`() {
        val actions = listOf(
            RuleAction.AutoReply("Thanks! I'll reply soon 😊\nBest regards,\nJohn")
        )

        val json = converters.fromActionList(actions)
        val result = converters.toActionList(json)

        assertEquals(1, result.size)
        assertEquals(
            "Thanks! I'll reply soon 😊\nBest regards,\nJohn",
            (result[0] as RuleAction.AutoReply).message
        )
    }

    @Test
    fun `should handle regex pattern with backslashes`() {
        val conditions = listOf(RuleCondition.MatchesPattern("\\d{3}-\\d{4}"))

        val json = converters.fromConditionList(conditions)
        val result = converters.toConditionList(json)

        assertEquals(1, result.size)
        assertEquals("\\d{3}-\\d{4}", (result[0] as RuleCondition.MatchesPattern).pattern)
    }
}
