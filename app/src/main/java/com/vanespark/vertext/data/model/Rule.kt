package com.vanespark.vertext.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Automation rule entity
 */
@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isEnabled: Boolean = true,
    val triggers: List<RuleTrigger>,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null,
    val triggerCount: Int = 0
)

/**
 * Rule trigger types
 */
sealed class RuleTrigger {
    /**
     * Trigger on message from specific sender
     */
    data class FromSender(val phoneNumber: String) : RuleTrigger()

    /**
     * Trigger on message containing keywords
     */
    data class ContainsKeyword(val keywords: List<String>, val caseSensitive: Boolean = false) : RuleTrigger()

    /**
     * Trigger at specific time
     */
    data class TimeRange(val startHour: Int, val endHour: Int) : RuleTrigger()

    /**
     * Trigger on specific days of week
     */
    data class DaysOfWeek(val days: Set<DayOfWeek>) : RuleTrigger()

    /**
     * Trigger on any message (always)
     */
    data object Always : RuleTrigger()
}

/**
 * Additional rule conditions
 */
sealed class RuleCondition {
    /**
     * Message must be unread
     */
    data object IsUnread : RuleCondition()

    /**
     * Message must match regex pattern
     */
    data class MatchesPattern(val pattern: String) : RuleCondition()

    /**
     * Sender must be in contacts
     */
    data object SenderInContacts : RuleCondition()

    /**
     * Sender must not be in contacts
     */
    data object SenderNotInContacts : RuleCondition()

    /**
     * Thread must have specific category
     */
    data class ThreadCategory(val category: String) : RuleCondition()
}

/**
 * Rule actions
 */
sealed class RuleAction {
    /**
     * Send auto-reply
     */
    data class AutoReply(val message: String) : RuleAction()

    /**
     * Set thread category
     */
    data class SetCategory(val category: String) : RuleAction()

    /**
     * Mark as read
     */
    data object MarkAsRead : RuleAction()

    /**
     * Archive conversation
     */
    data object Archive : RuleAction()

    /**
     * Mute notifications
     */
    data object MuteNotifications : RuleAction()

    /**
     * Pin conversation
     */
    data object PinConversation : RuleAction()

    /**
     * Block sender
     */
    data object BlockSender : RuleAction()

    /**
     * Custom notification sound
     */
    data class CustomNotification(val soundUri: String?, val priority: NotificationPriority) : RuleAction()
}

/**
 * Days of week enum
 */
enum class DayOfWeek {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW, DEFAULT, HIGH
}

/**
 * Type converters for Rule entity
 */
class RuleTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromTriggerList(triggers: List<RuleTrigger>): String {
        return gson.toJson(triggers.map { TriggerDto.fromTrigger(it) })
    }

    @TypeConverter
    fun toTriggerList(data: String): List<RuleTrigger> {
        val type = object : TypeToken<List<TriggerDto>>() {}.type
        val dtos: List<TriggerDto> = gson.fromJson(data, type)
        return dtos.map { it.toTrigger() }
    }

    @TypeConverter
    fun fromConditionList(conditions: List<RuleCondition>): String {
        return gson.toJson(conditions.map { ConditionDto.fromCondition(it) })
    }

    @TypeConverter
    fun toConditionList(data: String): List<RuleCondition> {
        val type = object : TypeToken<List<ConditionDto>>() {}.type
        val dtos: List<ConditionDto> = gson.fromJson(data, type)
        return dtos.map { it.toCondition() }
    }

    @TypeConverter
    fun fromActionList(actions: List<RuleAction>): String {
        return gson.toJson(actions.map { ActionDto.fromAction(it) })
    }

    @TypeConverter
    fun toActionList(data: String): List<RuleAction> {
        val type = object : TypeToken<List<ActionDto>>() {}.type
        val dtos: List<ActionDto> = gson.fromJson(data, type)
        return dtos.map { it.toAction() }
    }
}

/**
 * DTOs for serialization
 */
private data class TriggerDto(
    val type: String,
    val data: Map<String, Any>
) {
    fun toTrigger(): RuleTrigger = when (type) {
        "from_sender" -> RuleTrigger.FromSender(data["phoneNumber"] as String)
        "contains_keyword" -> RuleTrigger.ContainsKeyword(
            keywords = (data["keywords"] as List<*>).map { it as String },
            caseSensitive = data["caseSensitive"] as? Boolean ?: false
        )
        "time_range" -> RuleTrigger.TimeRange(
            startHour = (data["startHour"] as Double).toInt(),
            endHour = (data["endHour"] as Double).toInt()
        )
        "days_of_week" -> RuleTrigger.DaysOfWeek(
            days = (data["days"] as List<*>).map { DayOfWeek.valueOf(it as String) }.toSet()
        )
        "always" -> RuleTrigger.Always
        else -> RuleTrigger.Always
    }

    companion object {
        fun fromTrigger(trigger: RuleTrigger): TriggerDto = when (trigger) {
            is RuleTrigger.FromSender -> TriggerDto("from_sender", mapOf("phoneNumber" to trigger.phoneNumber))
            is RuleTrigger.ContainsKeyword -> TriggerDto("contains_keyword", mapOf(
                "keywords" to trigger.keywords,
                "caseSensitive" to trigger.caseSensitive
            ))
            is RuleTrigger.TimeRange -> TriggerDto("time_range", mapOf(
                "startHour" to trigger.startHour,
                "endHour" to trigger.endHour
            ))
            is RuleTrigger.DaysOfWeek -> TriggerDto("days_of_week", mapOf(
                "days" to trigger.days.map { it.name }
            ))
            is RuleTrigger.Always -> TriggerDto("always", emptyMap())
        }
    }
}

private data class ConditionDto(
    val type: String,
    val data: Map<String, Any>
) {
    fun toCondition(): RuleCondition = when (type) {
        "is_unread" -> RuleCondition.IsUnread
        "matches_pattern" -> RuleCondition.MatchesPattern(data["pattern"] as String)
        "sender_in_contacts" -> RuleCondition.SenderInContacts
        "sender_not_in_contacts" -> RuleCondition.SenderNotInContacts
        "thread_category" -> RuleCondition.ThreadCategory(data["category"] as String)
        else -> RuleCondition.IsUnread
    }

    companion object {
        fun fromCondition(condition: RuleCondition): ConditionDto = when (condition) {
            is RuleCondition.IsUnread -> ConditionDto("is_unread", emptyMap())
            is RuleCondition.MatchesPattern -> ConditionDto("matches_pattern", mapOf("pattern" to condition.pattern))
            is RuleCondition.SenderInContacts -> ConditionDto("sender_in_contacts", emptyMap())
            is RuleCondition.SenderNotInContacts -> ConditionDto("sender_not_in_contacts", emptyMap())
            is RuleCondition.ThreadCategory -> ConditionDto("thread_category", mapOf("category" to condition.category))
        }
    }
}

private data class ActionDto(
    val type: String,
    val data: Map<String, Any>
) {
    fun toAction(): RuleAction = when (type) {
        "auto_reply" -> RuleAction.AutoReply(data["message"] as String)
        "set_category" -> RuleAction.SetCategory(data["category"] as String)
        "mark_as_read" -> RuleAction.MarkAsRead
        "archive" -> RuleAction.Archive
        "mute_notifications" -> RuleAction.MuteNotifications
        "pin_conversation" -> RuleAction.PinConversation
        "block_sender" -> RuleAction.BlockSender
        "custom_notification" -> RuleAction.CustomNotification(
            soundUri = data["soundUri"] as? String,
            priority = NotificationPriority.valueOf(data["priority"] as String)
        )
        else -> RuleAction.MarkAsRead
    }

    companion object {
        fun fromAction(action: RuleAction): ActionDto = when (action) {
            is RuleAction.AutoReply -> ActionDto("auto_reply", mapOf("message" to action.message))
            is RuleAction.SetCategory -> ActionDto("set_category", mapOf("category" to action.category))
            is RuleAction.MarkAsRead -> ActionDto("mark_as_read", emptyMap())
            is RuleAction.Archive -> ActionDto("archive", emptyMap())
            is RuleAction.MuteNotifications -> ActionDto("mute_notifications", emptyMap())
            is RuleAction.PinConversation -> ActionDto("pin_conversation", emptyMap())
            is RuleAction.BlockSender -> ActionDto("block_sender", emptyMap())
            is RuleAction.CustomNotification -> ActionDto("custom_notification", mapOf(
                "soundUri" to (action.soundUri ?: ""),
                "priority" to action.priority.name
            ))
        }
    }
}
