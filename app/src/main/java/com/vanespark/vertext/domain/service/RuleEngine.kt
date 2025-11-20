package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.*
import com.vanespark.vertext.data.repository.BlockedContactRepository
import com.vanespark.vertext.data.repository.ContactRepository
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.RuleRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule engine for evaluating and executing automation rules
 */
@Singleton
class RuleEngine @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val blockedContactRepository: BlockedContactRepository,
    private val messagingService: MessagingService
) {

    /**
     * Process a message against all enabled rules
     */
    suspend fun processMessage(message: Message) {
        try {
            val enabledRules = ruleRepository.getEnabledRules().first()
            Timber.d("Processing message ${message.id} against ${enabledRules.size} rules")

            for (rule in enabledRules) {
                if (evaluateRule(rule, message)) {
                    Timber.d("Rule '${rule.name}' matched message ${message.id}")
                    executeRule(rule, message)
                    ruleRepository.updateRuleTriggerStats(rule.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing rules for message ${message.id}")
        }
    }

    /**
     * Evaluate if a rule matches a message
     */
    private suspend fun evaluateRule(rule: Rule, message: Message): Boolean {
        // Check all triggers (AND logic)
        val triggersMatch = rule.triggers.all { trigger ->
            evaluateTrigger(trigger, message)
        }

        if (!triggersMatch) {
            return false
        }

        // Check all conditions (AND logic)
        return rule.conditions.all { condition ->
            evaluateCondition(condition, message)
        }
    }

    /**
     * Evaluate a single trigger
     */
    private suspend fun evaluateTrigger(trigger: RuleTrigger, message: Message): Boolean {
        return when (trigger) {
            is RuleTrigger.Always -> true

            is RuleTrigger.FromSender -> {
                message.address.equals(trigger.phoneNumber, ignoreCase = true)
            }

            is RuleTrigger.ContainsKeyword -> {
                val text = if (trigger.caseSensitive) message.body else message.body.lowercase()
                trigger.keywords.any { keyword ->
                    val searchKeyword = if (trigger.caseSensitive) keyword else keyword.lowercase()
                    text.contains(searchKeyword)
                }
            }

            is RuleTrigger.TimeRange -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = message.date
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in trigger.startHour..trigger.endHour
            }

            is RuleTrigger.DaysOfWeek -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = message.date
                val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> DayOfWeek.SUNDAY
                    Calendar.MONDAY -> DayOfWeek.MONDAY
                    Calendar.TUESDAY -> DayOfWeek.TUESDAY
                    Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                    Calendar.THURSDAY -> DayOfWeek.THURSDAY
                    Calendar.FRIDAY -> DayOfWeek.FRIDAY
                    Calendar.SATURDAY -> DayOfWeek.SATURDAY
                    else -> DayOfWeek.SUNDAY
                }
                trigger.days.contains(dayOfWeek)
            }
        }
    }

    /**
     * Evaluate a single condition
     */
    private suspend fun evaluateCondition(condition: RuleCondition, message: Message): Boolean {
        return when (condition) {
            is RuleCondition.IsUnread -> {
                !message.isRead
            }

            is RuleCondition.MatchesPattern -> {
                try {
                    message.body.matches(Regex(condition.pattern))
                } catch (e: Exception) {
                    Timber.w(e, "Invalid regex pattern: ${condition.pattern}")
                    false
                }
            }

            is RuleCondition.SenderInContacts -> {
                contactRepository.getContactByPhone(message.address) != null
            }

            is RuleCondition.SenderNotInContacts -> {
                contactRepository.getContactByPhone(message.address) == null
            }

            is RuleCondition.ThreadCategory -> {
                val thread = threadRepository.getThreadById(message.threadId)
                thread?.category == condition.category
            }
        }
    }

    /**
     * Execute all actions for a rule
     */
    private suspend fun executeRule(rule: Rule, message: Message) {
        try {
            for (action in rule.actions) {
                executeAction(action, message)
            }
            Timber.d("Executed ${rule.actions.size} actions for rule '${rule.name}'")
        } catch (e: Exception) {
            Timber.e(e, "Error executing rule '${rule.name}'")
        }
    }

    /**
     * Execute a single action
     */
    private suspend fun executeAction(action: RuleAction, message: Message) {
        when (action) {
            is RuleAction.AutoReply -> {
                if (message.type == Message.TYPE_INBOX) {
                    Timber.d("Sending auto-reply to ${message.address}")
                    try {
                        messagingService.sendSmsMessage(
                            recipientAddress = message.address,
                            messageText = action.message
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to send auto-reply")
                    }
                }
            }

            is RuleAction.SetCategory -> {
                Timber.d("Setting thread category to ${action.category}")
                threadRepository.updateThreadCategory(message.threadId, action.category)
            }

            is RuleAction.MarkAsRead -> {
                Timber.d("Marking message and thread as read")
                messageRepository.markMessageAsRead(message.id)
                val thread = threadRepository.getThreadById(message.threadId)
                if (thread != null && thread.unreadCount > 0) {
                    threadRepository.updateThread(thread.copy(unreadCount = 0))
                }
            }

            is RuleAction.Archive -> {
                Timber.d("Archiving thread")
                val thread = threadRepository.getThreadById(message.threadId)
                if (thread != null) {
                    threadRepository.updateThread(thread.copy(isArchived = true))
                }
            }

            is RuleAction.MuteNotifications -> {
                Timber.d("Muting thread notifications")
                val thread = threadRepository.getThreadById(message.threadId)
                if (thread != null) {
                    threadRepository.updateThread(thread.copy(isMuted = true))
                }
            }

            is RuleAction.PinConversation -> {
                Timber.d("Pinning thread")
                val thread = threadRepository.getThreadById(message.threadId)
                if (thread != null) {
                    threadRepository.updateThread(thread.copy(isPinned = true))
                }
            }

            is RuleAction.BlockSender -> {
                Timber.d("Blocking sender ${message.address}")
                val contact = contactRepository.getContactByPhone(message.address)
                blockedContactRepository.blockContact(
                    phoneNumber = message.address,
                    contactName = contact?.name,
                    reason = "Blocked by automation rule"
                )
            }

            is RuleAction.CustomNotification -> {
                Timber.d("Custom notification action (not yet implemented)")
                // TODO: Implement custom notification handling
                // This would require notification service integration
            }
        }
    }

    /**
     * Test a rule against a message without executing actions
     */
    suspend fun testRule(rule: Rule, message: Message): RuleTestResult {
        val triggersMatch = rule.triggers.map { trigger ->
            TriggerEvaluation(
                trigger = trigger,
                matched = evaluateTrigger(trigger, message)
            )
        }

        val conditionsMatch = rule.conditions.map { condition ->
            ConditionEvaluation(
                condition = condition,
                matched = evaluateCondition(condition, message)
            )
        }

        val overallMatch = triggersMatch.all { it.matched } && conditionsMatch.all { it.matched }

        return RuleTestResult(
            rule = rule,
            message = message,
            matched = overallMatch,
            triggerEvaluations = triggersMatch,
            conditionEvaluations = conditionsMatch
        )
    }
}

/**
 * Result of rule testing
 */
data class RuleTestResult(
    val rule: Rule,
    val message: Message,
    val matched: Boolean,
    val triggerEvaluations: List<TriggerEvaluation>,
    val conditionEvaluations: List<ConditionEvaluation>
)

/**
 * Trigger evaluation result
 */
data class TriggerEvaluation(
    val trigger: RuleTrigger,
    val matched: Boolean
)

/**
 * Condition evaluation result
 */
data class ConditionEvaluation(
    val condition: RuleCondition,
    val matched: Boolean
)
