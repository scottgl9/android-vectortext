package com.vanespark.vertext.ui.rules

import com.vanespark.vertext.data.model.Rule
import com.vanespark.vertext.data.model.RuleAction
import com.vanespark.vertext.data.model.RuleTrigger
import com.vanespark.vertext.data.repository.RuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RulesViewModel
 * Tests state management and user actions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RulesViewModelTest {

    private lateinit var viewModel: RulesViewModel
    private lateinit var ruleRepository: RuleRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ruleRepository = mock()

        // Default mock behavior
        whenever(ruleRepository.getAllRules())
            .thenReturn(flowOf(emptyList()))

        viewModel = RulesViewModel(ruleRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state should load rules from repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)  // Loading complete
        assertTrue(state.rules.isEmpty())  // Empty rules from mock
        assertNull(state.error)
        assertNull(state.successMessage)
        assertFalse(state.showEditor)
        assertNull(state.selectedRule)
    }

    @Test
    fun `should load rules on initialization`() = runTest {
        val testRules = listOf(
            createTestRule(id = 1, name = "Rule 1"),
            createTestRule(id = 2, name = "Rule 2")
        )

        whenever(ruleRepository.getAllRules())
            .thenReturn(flowOf(testRules))

        val viewModel = RulesViewModel(ruleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(2, state.rules.size)
        assertEquals("Rule 1", state.rules[0].name)
        assertEquals("Rule 2", state.rules[1].name)
    }

    // ========== Create Rule Tests ==========

    @Test
    fun `createRule should create rule and show success message`() = runTest {
        val newRule = createTestRule(name = "New Rule")

        whenever(ruleRepository.createRule(any()))
            .thenReturn(1L)

        viewModel.createRule(newRule)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("Rule created successfully", state.successMessage)
        assertFalse(state.showEditor)
        assertNull(state.selectedRule)
        verify(ruleRepository).createRule(newRule)
    }

    @Test
    fun `createRule should handle error`() = runTest {
        val newRule = createTestRule(name = "New Rule")
        val errorMessage = "Failed to create rule"

        whenever(ruleRepository.createRule(any()))
            .thenThrow(RuntimeException(errorMessage))

        viewModel.createRule(newRule)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(errorMessage, state.error)
        verify(ruleRepository).createRule(newRule)
    }

    // ========== Update Rule Tests ==========

    @Test
    fun `updateRule should update rule and show success message`() = runTest {
        val rule = createTestRule(id = 1, name = "Updated Rule")

        whenever(ruleRepository.updateRule(any()))
            .thenReturn(Unit)

        viewModel.updateRule(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("Rule updated successfully", state.successMessage)
        assertFalse(state.showEditor)
        assertNull(state.selectedRule)
        verify(ruleRepository).updateRule(rule)
    }

    @Test
    fun `updateRule should handle error`() = runTest {
        val rule = createTestRule(id = 1, name = "Updated Rule")
        val errorMessage = "Failed to update rule"

        whenever(ruleRepository.updateRule(any()))
            .thenThrow(RuntimeException(errorMessage))

        viewModel.updateRule(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(errorMessage, state.error)
        verify(ruleRepository).updateRule(rule)
    }

    // ========== Delete Rule Tests ==========

    @Test
    fun `deleteRule should delete rule and show success message`() = runTest {
        val ruleId = 1L

        whenever(ruleRepository.deleteRuleById(any()))
            .thenReturn(Unit)

        viewModel.deleteRule(ruleId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("Rule deleted successfully", state.successMessage)
        verify(ruleRepository).deleteRuleById(ruleId)
    }

    @Test
    fun `deleteRule should handle error`() = runTest {
        val ruleId = 1L
        val errorMessage = "Failed to delete rule"

        whenever(ruleRepository.deleteRuleById(any()))
            .thenThrow(RuntimeException(errorMessage))

        viewModel.deleteRule(ruleId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(errorMessage, state.error)
        verify(ruleRepository).deleteRuleById(ruleId)
    }

    // ========== Toggle Rule Tests ==========

    @Test
    fun `toggleRuleEnabled should enable rule`() = runTest {
        val ruleId = 1L

        whenever(ruleRepository.setRuleEnabled(any(), any()))
            .thenReturn(Unit)

        viewModel.toggleRuleEnabled(ruleId, true)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(ruleRepository).setRuleEnabled(ruleId, true)
    }

    @Test
    fun `toggleRuleEnabled should disable rule`() = runTest {
        val ruleId = 1L

        whenever(ruleRepository.setRuleEnabled(any(), any()))
            .thenReturn(Unit)

        viewModel.toggleRuleEnabled(ruleId, false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(ruleRepository).setRuleEnabled(ruleId, false)
    }

    @Test
    fun `toggleRuleEnabled should handle error`() = runTest {
        val ruleId = 1L
        val errorMessage = "Failed to toggle rule"

        whenever(ruleRepository.setRuleEnabled(any(), any()))
            .thenThrow(RuntimeException(errorMessage))

        viewModel.toggleRuleEnabled(ruleId, true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(errorMessage, state.error)
    }

    // ========== Editor State Tests ==========

    @Test
    fun `showNewRuleEditor should show editor with no selected rule`() = runTest {
        viewModel.showNewRuleEditor()

        val state = viewModel.uiState.value

        assertTrue(state.showEditor)
        assertNull(state.selectedRule)
    }

    @Test
    fun `showEditRuleEditor should show editor with selected rule`() = runTest {
        val rule = createTestRule(id = 1, name = "Edit Rule")

        viewModel.showEditRuleEditor(rule)

        val state = viewModel.uiState.value

        assertTrue(state.showEditor)
        assertEquals(rule, state.selectedRule)
    }

    @Test
    fun `hideEditor should hide editor and clear selected rule`() = runTest {
        val rule = createTestRule(id = 1, name = "Edit Rule")

        viewModel.showEditRuleEditor(rule)
        viewModel.hideEditor()

        val state = viewModel.uiState.value

        assertFalse(state.showEditor)
        assertNull(state.selectedRule)
    }

    // ========== Message Clearing Tests ==========

    @Test
    fun `clearSuccessMessage should clear success message`() = runTest {
        val newRule = createTestRule(name = "New Rule")

        whenever(ruleRepository.createRule(any()))
            .thenReturn(1L)

        viewModel.createRule(newRule)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify success message is set
        assertEquals("Rule created successfully", viewModel.uiState.value.successMessage)

        viewModel.clearSuccessMessage()

        // Verify success message is cleared
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        val newRule = createTestRule(name = "New Rule")

        whenever(ruleRepository.createRule(any()))
            .thenThrow(RuntimeException("Error"))

        viewModel.createRule(newRule)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        assertEquals("Error", viewModel.uiState.value.error)

        viewModel.clearError()

        // Verify error is cleared
        assertNull(viewModel.uiState.value.error)
    }

    // ========== Rule Statistics Tests ==========

    @Test
    fun `getRuleStats should return correct statistics`() = runTest {
        whenever(ruleRepository.getRulesCount())
            .thenReturn(10)
        whenever(ruleRepository.getEnabledRulesCount())
            .thenReturn(7)

        val stats = viewModel.getRuleStats()

        assertEquals(10, stats.totalRules)
        assertEquals(7, stats.enabledRules)
        assertEquals(3, stats.disabledRules)
        verify(ruleRepository).getRulesCount()
        verify(ruleRepository).getEnabledRulesCount()
    }

    @Test
    fun `getRuleStats should handle all enabled`() = runTest {
        whenever(ruleRepository.getRulesCount())
            .thenReturn(5)
        whenever(ruleRepository.getEnabledRulesCount())
            .thenReturn(5)

        val stats = viewModel.getRuleStats()

        assertEquals(5, stats.totalRules)
        assertEquals(5, stats.enabledRules)
        assertEquals(0, stats.disabledRules)
    }

    @Test
    fun `getRuleStats should handle all disabled`() = runTest {
        whenever(ruleRepository.getRulesCount())
            .thenReturn(3)
        whenever(ruleRepository.getEnabledRulesCount())
            .thenReturn(0)

        val stats = viewModel.getRuleStats()

        assertEquals(3, stats.totalRules)
        assertEquals(0, stats.enabledRules)
        assertEquals(3, stats.disabledRules)
    }

    @Test
    fun `getRuleStats should handle no rules`() = runTest {
        whenever(ruleRepository.getRulesCount())
            .thenReturn(0)
        whenever(ruleRepository.getEnabledRulesCount())
            .thenReturn(0)

        val stats = viewModel.getRuleStats()

        assertEquals(0, stats.totalRules)
        assertEquals(0, stats.enabledRules)
        assertEquals(0, stats.disabledRules)
    }

    // ========== Multiple Rules Tests ==========

    @Test
    fun `should handle multiple rules with different states`() = runTest {
        val testRules = listOf(
            createTestRule(id = 1, name = "Rule 1", isEnabled = true),
            createTestRule(id = 2, name = "Rule 2", isEnabled = false),
            createTestRule(id = 3, name = "Rule 3", isEnabled = true)
        )

        whenever(ruleRepository.getAllRules())
            .thenReturn(flowOf(testRules))

        val viewModel = RulesViewModel(ruleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(3, state.rules.size)
        assertTrue(state.rules[0].isEnabled)
        assertFalse(state.rules[1].isEnabled)
        assertTrue(state.rules[2].isEnabled)
    }

    // ========== Helper Methods ==========

    private fun createTestRule(
        id: Long = 1,
        name: String = "Test Rule",
        isEnabled: Boolean = true,
        triggers: List<RuleTrigger> = listOf(RuleTrigger.Always),
        actions: List<RuleAction> = listOf(RuleAction.MarkAsRead)
    ): Rule {
        return Rule(
            id = id,
            name = name,
            description = "Test rule description",
            isEnabled = isEnabled,
            triggers = triggers,
            conditions = emptyList(),
            actions = actions
        )
    }
}
