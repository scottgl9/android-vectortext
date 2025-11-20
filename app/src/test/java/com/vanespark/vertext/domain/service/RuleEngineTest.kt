package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.*
import com.vanespark.vertext.data.repository.BlockedContactRepository
import com.vanespark.vertext.data.repository.ContactRepository
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.RuleRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for RuleEngine
 * Tests trigger evaluation, condition evaluation, and action execution
 */
class RuleEngineTest {

    private lateinit var ruleEngine: RuleEngine
    private lateinit var ruleRepository: RuleRepository
    private lateinit var threadRepository: ThreadRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var blockedContactRepository: BlockedContactRepository
    private lateinit var messagingService: MessagingService

    @Before
    fun setup() {
        ruleRepository = mock()
        threadRepository = mock()
        messageRepository = mock()
        contactRepository = mock()
        blockedContactRepository = mock()
        messagingService = mock()

        ruleEngine = RuleEngine(
            ruleRepository = ruleRepository,
            threadRepository = threadRepository,
            messageRepository = messageRepository,
            contactRepository = contactRepository,
            blockedContactRepository = blockedContactRepository,
            messagingService = messagingService
        )
    }

    // ========== Trigger Evaluation Tests ==========

    @Test
    fun `Always trigger should always match`() = runTest {
        val message = createTestMessage(body = "Any message")
        val rule = createTestRule(triggers = listOf(RuleTrigger.Always))

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        assertTrue(result.triggerEvaluations.all { it.matched })
    }

    @Test
    fun `FromSender trigger should match sender phone number`() = runTest {
        val message = createTestMessage(address = "+1234567890")
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.FromSender("+1234567890"))
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        assertTrue(result.triggerEvaluations.first().matched)
    }

    @Test
    fun `FromSender trigger should not match different sender`() = runTest {
        val message = createTestMessage(address = "+1111111111")
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.FromSender("+1234567890"))
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
        assertFalse(result.triggerEvaluations.first().matched)
    }

    @Test
    fun `FromSender trigger should be case insensitive`() = runTest {
        val message = createTestMessage(address = "+1234567890")
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.FromSender("+1234567890"))
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    @Test
    fun `ContainsKeyword trigger should match keyword in message`() = runTest {
        val message = createTestMessage(body = "This is a test message")
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.ContainsKeyword(
                    keywords = listOf("test"),
                    caseSensitive = false
                )
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        assertTrue(result.triggerEvaluations.first().matched)
    }

    @Test
    fun `ContainsKeyword trigger should match multiple keywords`() = runTest {
        val message = createTestMessage(body = "urgent meeting tomorrow")
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.ContainsKeyword(
                    keywords = listOf("urgent", "important", "asap"),
                    caseSensitive = false
                )
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    @Test
    fun `ContainsKeyword trigger case sensitive should match case`() = runTest {
        val message = createTestMessage(body = "Test message")
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.ContainsKeyword(
                    keywords = listOf("Test"),
                    caseSensitive = true
                )
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    @Test
    fun `ContainsKeyword trigger case sensitive should not match different case`() = runTest {
        val message = createTestMessage(body = "test message")
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.ContainsKeyword(
                    keywords = listOf("Test"),
                    caseSensitive = true
                )
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
    }

    @Test
    fun `TimeRange trigger should match time within range`() = runTest {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val message = createTestMessage(date = System.currentTimeMillis())
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.TimeRange(
                    startHour = if (currentHour > 0) currentHour - 1 else 0,
                    endHour = if (currentHour < 23) currentHour + 1 else 23
                )
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    @Test
    fun `TimeRange trigger should not match time outside range`() = runTest {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val message = createTestMessage(date = System.currentTimeMillis())
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.TimeRange(
                    startHour = (currentHour + 5) % 24,
                    endHour = (currentHour + 6) % 24
                )
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
    }

    @Test
    fun `DaysOfWeek trigger should match current day`() = runTest {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }

        val message = createTestMessage(date = System.currentTimeMillis())
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.DaysOfWeek(setOf(currentDayOfWeek))
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    // ========== Condition Evaluation Tests ==========

    @Test
    fun `IsUnread condition should match unread message`() = runTest {
        val message = createTestMessage(isRead = false)
        val rule = createTestRule(
            conditions = listOf(RuleCondition.IsUnread)
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        assertTrue(result.conditionEvaluations.first().matched)
    }

    @Test
    fun `IsUnread condition should not match read message`() = runTest {
        val message = createTestMessage(isRead = true)
        val rule = createTestRule(
            conditions = listOf(RuleCondition.IsUnread)
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
        assertFalse(result.conditionEvaluations.first().matched)
    }

    @Test
    fun `MatchesPattern condition should match regex pattern`() = runTest {
        val message = createTestMessage(body = "Code: 123456")
        val rule = createTestRule(
            conditions = listOf(
                RuleCondition.MatchesPattern("Code: \\d{6}")
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        assertTrue(result.conditionEvaluations.first().matched)
    }

    @Test
    fun `MatchesPattern condition should not match invalid pattern`() = runTest {
        val message = createTestMessage(body = "Code: ABC")
        val rule = createTestRule(
            conditions = listOf(
                RuleCondition.MatchesPattern("Code: \\d{6}")
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
    }

    @Test
    fun `SenderInContacts condition should match contact`() = runTest {
        val message = createTestMessage(address = "+1234567890")
        val contact = Contact(
            id = 1,
            name = "John Doe",
            phone = "+1234567890"
        )

        whenever(contactRepository.getContactByPhone("+1234567890"))
            .thenReturn(contact)

        val rule = createTestRule(
            conditions = listOf(RuleCondition.SenderInContacts)
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        verify(contactRepository).getContactByPhone("+1234567890")
    }

    @Test
    fun `SenderInContacts condition should not match non-contact`() = runTest {
        val message = createTestMessage(address = "+1234567890")

        whenever(contactRepository.getContactByPhone("+1234567890"))
            .thenReturn(null)

        val rule = createTestRule(
            conditions = listOf(RuleCondition.SenderInContacts)
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
    }

    @Test
    fun `SenderNotInContacts condition should match non-contact`() = runTest {
        val message = createTestMessage(address = "+1234567890")

        whenever(contactRepository.getContactByPhone("+1234567890"))
            .thenReturn(null)

        val rule = createTestRule(
            conditions = listOf(RuleCondition.SenderNotInContacts)
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    @Test
    fun `ThreadCategory condition should match thread category`() = runTest {
        val message = createTestMessage(threadId = 1)
        val thread = Thread(
            id = 1,
            recipient = "+1234567890",
            category = "Work"
        )

        whenever(threadRepository.getThreadById(1))
            .thenReturn(thread)

        val rule = createTestRule(
            conditions = listOf(RuleCondition.ThreadCategory("Work"))
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
    }

    // ========== Combined Rules Tests ==========

    @Test
    fun `Rule should match when all triggers and conditions match`() = runTest {
        val message = createTestMessage(
            address = "+1234567890",
            body = "urgent meeting",
            isRead = false
        )

        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.FromSender("+1234567890"),
                RuleTrigger.ContainsKeyword(listOf("urgent"), false)
            ),
            conditions = listOf(
                RuleCondition.IsUnread
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertTrue(result.matched)
        assertTrue(result.triggerEvaluations.all { it.matched })
        assertTrue(result.conditionEvaluations.all { it.matched })
    }

    @Test
    fun `Rule should not match when one trigger fails`() = runTest {
        val message = createTestMessage(
            address = "+1111111111",  // Different sender
            body = "urgent meeting",
            isRead = false
        )

        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.FromSender("+1234567890"),
                RuleTrigger.ContainsKeyword(listOf("urgent"), false)
            )
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
    }

    @Test
    fun `Rule should not match when one condition fails`() = runTest {
        val message = createTestMessage(
            body = "test message",
            isRead = true  // Read message
        )

        val rule = createTestRule(
            triggers = listOf(RuleTrigger.Always),
            conditions = listOf(RuleCondition.IsUnread)
        )

        val result = ruleEngine.testRule(rule, message)

        assertFalse(result.matched)
    }

    // ========== Message Processing Tests ==========

    @Test
    fun `processMessage should execute enabled rules`() = runTest {
        val message = createTestMessage(body = "test")
        val enabledRule = createTestRule(
            id = 1,
            isEnabled = true,
            triggers = listOf(RuleTrigger.Always),
            actions = listOf(RuleAction.MarkAsRead)
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(enabledRule)))
        whenever(messageRepository.markMessageAsRead(any()))
            .thenReturn(Unit)
        whenever(threadRepository.getThreadById(any()))
            .thenReturn(Thread(id = 1, recipient = "+1234567890", unreadCount = 1))
        whenever(threadRepository.updateThread(any()))
            .thenReturn(Unit)

        ruleEngine.processMessage(message)

        verify(ruleRepository).updateRuleTriggerStats(1)
        verify(messageRepository).markMessageAsRead(message.id)
    }

    @Test
    fun `processMessage should not execute non-matching rules`() = runTest {
        val message = createTestMessage(body = "test")
        val nonMatchingRule = createTestRule(
            id = 1,
            triggers = listOf(RuleTrigger.FromSender("+9999999999")),
            actions = listOf(RuleAction.MarkAsRead)
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(nonMatchingRule)))

        ruleEngine.processMessage(message)

        verify(ruleRepository, never()).updateRuleTriggerStats(any())
        verify(messageRepository, never()).markMessageAsRead(any())
    }

    // ========== Action Execution Tests ==========

    @Test
    fun `AutoReply action should send reply for inbox message`() = runTest {
        val message = createTestMessage(
            type = Message.TYPE_INBOX,
            address = "+1234567890"
        )
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.Always),
            actions = listOf(RuleAction.AutoReply("Thanks for your message"))
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(rule)))
        whenever(messagingService.sendSmsMessage(any(), any()))
            .thenReturn(Result.success(1L))

        ruleEngine.processMessage(message)

        verify(messagingService).sendSmsMessage(
            recipientAddress = "+1234567890",
            messageText = "Thanks for your message"
        )
    }

    @Test
    fun `AutoReply action should not send reply for sent message`() = runTest {
        val message = createTestMessage(
            type = Message.TYPE_SENT,
            address = "+1234567890"
        )
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.Always),
            actions = listOf(RuleAction.AutoReply("Thanks for your message"))
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(rule)))

        ruleEngine.processMessage(message)

        verify(messagingService, never()).sendSmsMessage(any(), any())
    }

    @Test
    fun `SetCategory action should update thread category`() = runTest {
        val message = createTestMessage(threadId = 1)
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.Always),
            actions = listOf(RuleAction.SetCategory("Work"))
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(rule)))

        ruleEngine.processMessage(message)

        verify(threadRepository).updateThreadCategory(1, "Work")
    }

    @Test
    fun `Archive action should archive thread`() = runTest {
        val message = createTestMessage(threadId = 1)
        val thread = Thread(id = 1, recipient = "+1234567890")
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.Always),
            actions = listOf(RuleAction.Archive)
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(rule)))
        whenever(threadRepository.getThreadById(1))
            .thenReturn(thread)
        whenever(threadRepository.updateThread(any()))
            .thenReturn(Unit)

        ruleEngine.processMessage(message)

        verify(threadRepository).updateThread(thread.copy(isArchived = true))
    }

    @Test
    fun `BlockSender action should block sender`() = runTest {
        val message = createTestMessage(address = "+1234567890")
        val contact = Contact(id = 1, name = "John Doe", phone = "+1234567890")
        val rule = createTestRule(
            triggers = listOf(RuleTrigger.Always),
            actions = listOf(RuleAction.BlockSender)
        )

        whenever(ruleRepository.getEnabledRules())
            .thenReturn(flowOf(listOf(rule)))
        whenever(contactRepository.getContactByPhone("+1234567890"))
            .thenReturn(contact)
        whenever(blockedContactRepository.blockContact(any(), any(), any()))
            .thenReturn(1L)

        ruleEngine.processMessage(message)

        verify(blockedContactRepository).blockContact(
            phoneNumber = "+1234567890",
            contactName = "John Doe",
            reason = "Blocked by automation rule"
        )
    }

    // ========== Helper Methods ==========

    private fun createTestMessage(
        id: Long = 1,
        threadId: Long = 1,
        address: String = "+1234567890",
        body: String = "Test message",
        date: Long = System.currentTimeMillis(),
        type: Int = Message.TYPE_INBOX,
        isRead: Boolean = false
    ): Message {
        return Message(
            id = id,
            threadId = threadId,
            address = address,
            body = body,
            date = date,
            type = type,
            isRead = isRead
        )
    }

    private fun createTestRule(
        id: Long = 1,
        name: String = "Test Rule",
        isEnabled: Boolean = true,
        triggers: List<RuleTrigger> = listOf(RuleTrigger.Always),
        conditions: List<RuleCondition> = emptyList(),
        actions: List<RuleAction> = emptyList()
    ): Rule {
        return Rule(
            id = id,
            name = name,
            description = "Test rule description",
            isEnabled = isEnabled,
            triggers = triggers,
            conditions = conditions,
            actions = actions
        )
    }
}
