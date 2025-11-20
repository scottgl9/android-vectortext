package com.vanespark.vertext.data.repository

import com.vanespark.vertext.data.dao.RuleDao
import com.vanespark.vertext.data.model.Rule
import com.vanespark.vertext.data.model.RuleAction
import com.vanespark.vertext.data.model.RuleCondition
import com.vanespark.vertext.data.model.RuleTrigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for RuleRepository
 * Tests data layer operations for automation rules
 */
class RuleRepositoryTest {

    private lateinit var repository: RuleRepository
    private lateinit var ruleDao: RuleDao

    @Before
    fun setup() {
        ruleDao = mock()
        repository = RuleRepository(ruleDao)
    }

    // ========== Get All Rules Tests ==========

    @Test
    fun `getAllRules should return all rules from DAO`() = runTest {
        val rules = listOf(
            createTestRule(id = 1, name = "Rule 1"),
            createTestRule(id = 2, name = "Rule 2"),
            createTestRule(id = 3, name = "Rule 3")
        )

        whenever(ruleDao.getAllRules())
            .thenReturn(flowOf(rules))

        val result = repository.getAllRules().first()

        assertEquals(3, result.size)
        assertEquals("Rule 1", result[0].name)
        assertEquals("Rule 2", result[1].name)
        assertEquals("Rule 3", result[2].name)
        verify(ruleDao).getAllRules()
    }

    @Test
    fun `getAllRules should return empty list when no rules exist`() = runTest {
        whenever(ruleDao.getAllRules())
            .thenReturn(flowOf(emptyList()))

        val result = repository.getAllRules().first()

        assertTrue(result.isEmpty())
        verify(ruleDao).getAllRules()
    }

    @Test
    fun `getAllRules should emit updates when rules change`() = runTest {
        val initialRules = listOf(createTestRule(id = 1, name = "Rule 1"))
        val updatedRules = listOf(
            createTestRule(id = 1, name = "Rule 1"),
            createTestRule(id = 2, name = "Rule 2")
        )

        whenever(ruleDao.getAllRules())
            .thenReturn(flowOf(initialRules, updatedRules))

        val emissions = mutableListOf<List<Rule>>()
        repository.getAllRules().collect { emissions.add(it) }

        assertEquals(2, emissions.size)
        assertEquals(1, emissions[0].size)
        assertEquals(2, emissions[1].size)
    }

    // ========== Get Enabled Rules Tests ==========

    @Test
    fun `getEnabledRules should return only enabled rules`() = runTest {
        val rules = listOf(
            createTestRule(id = 1, name = "Enabled 1", isEnabled = true),
            createTestRule(id = 2, name = "Enabled 2", isEnabled = true)
        )

        whenever(ruleDao.getEnabledRules())
            .thenReturn(flowOf(rules))

        val result = repository.getEnabledRules().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isEnabled })
        verify(ruleDao).getEnabledRules()
    }

    @Test
    fun `getEnabledRules should return empty list when no enabled rules`() = runTest {
        whenever(ruleDao.getEnabledRules())
            .thenReturn(flowOf(emptyList()))

        val result = repository.getEnabledRules().first()

        assertTrue(result.isEmpty())
        verify(ruleDao).getEnabledRules()
    }

    // ========== Get Rule by ID Tests ==========

    @Test
    fun `getRuleById should return rule when it exists`() = runTest {
        val rule = createTestRule(id = 1, name = "Test Rule")

        whenever(ruleDao.getRuleById(1))
            .thenReturn(rule)

        val result = repository.getRuleById(1)

        assertEquals(rule, result)
        assertEquals("Test Rule", result?.name)
        verify(ruleDao).getRuleById(1)
    }

    @Test
    fun `getRuleById should return null when rule does not exist`() = runTest {
        whenever(ruleDao.getRuleById(999))
            .thenReturn(null)

        val result = repository.getRuleById(999)

        assertEquals(null, result)
        verify(ruleDao).getRuleById(999)
    }

    // ========== Create Rule Tests ==========

    @Test
    fun `createRule should insert rule and return ID`() = runTest {
        val rule = createTestRule(name = "New Rule")

        whenever(ruleDao.insertRule(rule))
            .thenReturn(5L)

        val result = repository.createRule(rule)

        assertEquals(5L, result)
        verify(ruleDao).insertRule(rule)
    }

    @Test
    fun `createRule should handle rule with multiple triggers`() = runTest {
        val rule = createTestRule(
            triggers = listOf(
                RuleTrigger.FromSender("+1234567890"),
                RuleTrigger.ContainsKeyword(listOf("urgent"), false),
                RuleTrigger.TimeRange(9, 17)
            )
        )

        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val result = repository.createRule(rule)

        assertEquals(1L, result)
        verify(ruleDao).insertRule(rule)
    }

    @Test
    fun `createRule should handle rule with multiple actions`() = runTest {
        val rule = createTestRule(
            actions = listOf(
                RuleAction.MarkAsRead,
                RuleAction.SetCategory("Work"),
                RuleAction.AutoReply("Thanks!")
            )
        )

        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val result = repository.createRule(rule)

        assertEquals(1L, result)
        verify(ruleDao).insertRule(rule)
    }

    // ========== Update Rule Tests ==========

    @Test
    fun `updateRule should call DAO update`() = runTest {
        val rule = createTestRule(id = 1, name = "Updated Rule")

        whenever(ruleDao.updateRule(rule))
            .thenReturn(Unit)

        repository.updateRule(rule)

        verify(ruleDao).updateRule(rule)
    }

    @Test
    fun `updateRule should update rule name`() = runTest {
        val originalRule = createTestRule(id = 1, name = "Original")
        val updatedRule = originalRule.copy(name = "Updated")

        whenever(ruleDao.updateRule(updatedRule))
            .thenReturn(Unit)

        repository.updateRule(updatedRule)

        verify(ruleDao).updateRule(updatedRule)
    }

    @Test
    fun `updateRule should update enabled state`() = runTest {
        val rule = createTestRule(id = 1, isEnabled = false)

        whenever(ruleDao.updateRule(rule))
            .thenReturn(Unit)

        repository.updateRule(rule)

        verify(ruleDao).updateRule(rule)
    }

    // ========== Delete Rule Tests ==========

    @Test
    fun `deleteRule should call DAO delete`() = runTest {
        val rule = createTestRule(id = 1)

        whenever(ruleDao.deleteRule(rule))
            .thenReturn(Unit)

        repository.deleteRule(rule)

        verify(ruleDao).deleteRule(rule)
    }

    @Test
    fun `deleteRuleById should call DAO delete by ID`() = runTest {
        whenever(ruleDao.deleteRuleById(1))
            .thenReturn(Unit)

        repository.deleteRuleById(1)

        verify(ruleDao).deleteRuleById(1)
    }

    @Test
    fun `deleteRuleById should handle non-existent rule`() = runTest {
        whenever(ruleDao.deleteRuleById(999))
            .thenReturn(Unit)

        repository.deleteRuleById(999)

        verify(ruleDao).deleteRuleById(999)
    }

    // ========== Set Rule Enabled Tests ==========

    @Test
    fun `setRuleEnabled should enable rule`() = runTest {
        whenever(ruleDao.setRuleEnabled(1, true))
            .thenReturn(Unit)

        repository.setRuleEnabled(1, true)

        verify(ruleDao).setRuleEnabled(1, true)
    }

    @Test
    fun `setRuleEnabled should disable rule`() = runTest {
        whenever(ruleDao.setRuleEnabled(1, false))
            .thenReturn(Unit)

        repository.setRuleEnabled(1, false)

        verify(ruleDao).setRuleEnabled(1, false)
    }

    @Test
    fun `setRuleEnabled should handle multiple rules`() = runTest {
        whenever(ruleDao.setRuleEnabled(any(), any()))
            .thenReturn(Unit)

        repository.setRuleEnabled(1, true)
        repository.setRuleEnabled(2, false)
        repository.setRuleEnabled(3, true)

        verify(ruleDao).setRuleEnabled(1, true)
        verify(ruleDao).setRuleEnabled(2, false)
        verify(ruleDao).setRuleEnabled(3, true)
    }

    // ========== Update Trigger Stats Tests ==========

    @Test
    fun `updateRuleTriggerStats should update stats with current timestamp`() = runTest {
        whenever(ruleDao.updateRuleTriggerStats(eq(1), any()))
            .thenReturn(Unit)

        repository.updateRuleTriggerStats(1)

        verify(ruleDao).updateRuleTriggerStats(eq(1), any())
    }

    @Test
    fun `updateRuleTriggerStats should be called for each rule trigger`() = runTest {
        whenever(ruleDao.updateRuleTriggerStats(any(), any()))
            .thenReturn(Unit)

        repository.updateRuleTriggerStats(1)
        repository.updateRuleTriggerStats(2)
        repository.updateRuleTriggerStats(3)

        verify(ruleDao, times(3)).updateRuleTriggerStats(any(), any())
    }

    // ========== Get Rules Count Tests ==========

    @Test
    fun `getRulesCount should return total count`() = runTest {
        whenever(ruleDao.getRulesCount())
            .thenReturn(10)

        val result = repository.getRulesCount()

        assertEquals(10, result)
        verify(ruleDao).getRulesCount()
    }

    @Test
    fun `getRulesCount should return zero when no rules`() = runTest {
        whenever(ruleDao.getRulesCount())
            .thenReturn(0)

        val result = repository.getRulesCount()

        assertEquals(0, result)
        verify(ruleDao).getRulesCount()
    }

    // ========== Get Enabled Rules Count Tests ==========

    @Test
    fun `getEnabledRulesCount should return enabled count`() = runTest {
        whenever(ruleDao.getEnabledRulesCount())
            .thenReturn(7)

        val result = repository.getEnabledRulesCount()

        assertEquals(7, result)
        verify(ruleDao).getEnabledRulesCount()
    }

    @Test
    fun `getEnabledRulesCount should return zero when no enabled rules`() = runTest {
        whenever(ruleDao.getEnabledRulesCount())
            .thenReturn(0)

        val result = repository.getEnabledRulesCount()

        assertEquals(0, result)
        verify(ruleDao).getEnabledRulesCount()
    }

    @Test
    fun `getEnabledRulesCount should be less than or equal to total count`() = runTest {
        whenever(ruleDao.getRulesCount())
            .thenReturn(10)
        whenever(ruleDao.getEnabledRulesCount())
            .thenReturn(7)

        val total = repository.getRulesCount()
        val enabled = repository.getEnabledRulesCount()

        assertTrue(enabled <= total)
    }

    // ========== Integration Tests ==========

    @Test
    fun `should create, update, and delete rule lifecycle`() = runTest {
        val rule = createTestRule(id = 0, name = "Test Rule")
        val createdRule = rule.copy(id = 1)
        val updatedRule = createdRule.copy(name = "Updated Rule")

        // Create
        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val ruleId = repository.createRule(rule)
        assertEquals(1L, ruleId)

        // Update
        whenever(ruleDao.updateRule(updatedRule))
            .thenReturn(Unit)

        repository.updateRule(updatedRule)

        // Delete
        whenever(ruleDao.deleteRuleById(1))
            .thenReturn(Unit)

        repository.deleteRuleById(1)

        verify(ruleDao).insertRule(rule)
        verify(ruleDao).updateRule(updatedRule)
        verify(ruleDao).deleteRuleById(1)
    }

    @Test
    fun `should handle concurrent rule operations`() = runTest {
        val rule1 = createTestRule(id = 1, name = "Rule 1")
        val rule2 = createTestRule(id = 2, name = "Rule 2")

        whenever(ruleDao.insertRule(any()))
            .thenReturn(1L, 2L)
        whenever(ruleDao.setRuleEnabled(any(), any()))
            .thenReturn(Unit)

        // Create rules
        repository.createRule(rule1)
        repository.createRule(rule2)

        // Enable/disable concurrently
        repository.setRuleEnabled(1, true)
        repository.setRuleEnabled(2, false)

        verify(ruleDao, times(2)).insertRule(any())
        verify(ruleDao).setRuleEnabled(1, true)
        verify(ruleDao).setRuleEnabled(2, false)
    }

    // ========== Edge Cases Tests ==========

    @Test
    fun `should handle rule with empty triggers list`() = runTest {
        val rule = createTestRule(triggers = emptyList())

        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val result = repository.createRule(rule)

        assertEquals(1L, result)
        verify(ruleDao).insertRule(rule)
    }

    @Test
    fun `should handle rule with empty conditions list`() = runTest {
        val rule = createTestRule(conditions = emptyList())

        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val result = repository.createRule(rule)

        assertEquals(1L, result)
        verify(ruleDao).insertRule(rule)
    }

    @Test
    fun `should handle rule with empty actions list`() = runTest {
        val rule = createTestRule(actions = emptyList())

        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val result = repository.createRule(rule)

        assertEquals(1L, result)
        verify(ruleDao).insertRule(rule)
    }

    @Test
    fun `should handle rule with very long description`() = runTest {
        val longDescription = "x".repeat(1000)
        val rule = createTestRule(description = longDescription)

        whenever(ruleDao.insertRule(rule))
            .thenReturn(1L)

        val result = repository.createRule(rule)

        assertEquals(1L, result)
        verify(ruleDao).insertRule(rule)
    }

    // ========== Helper Methods ==========

    private fun createTestRule(
        id: Long = 1,
        name: String = "Test Rule",
        description: String = "Test description",
        isEnabled: Boolean = true,
        triggers: List<RuleTrigger> = listOf(RuleTrigger.Always),
        conditions: List<RuleCondition> = emptyList(),
        actions: List<RuleAction> = listOf(RuleAction.MarkAsRead),
        triggerCount: Int = 0,
        lastTriggeredAt: Long? = null
    ): Rule {
        return Rule(
            id = id,
            name = name,
            description = description,
            isEnabled = isEnabled,
            triggers = triggers,
            conditions = conditions,
            actions = actions,
            triggerCount = triggerCount,
            lastTriggeredAt = lastTriggeredAt
        )
    }
}
