package com.vanespark.vertext.data.repository

import com.vanespark.vertext.data.dao.RuleDao
import com.vanespark.vertext.data.model.Rule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for automation rules
 */
@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao
) {

    /**
     * Get all rules as Flow
     */
    fun getAllRules(): Flow<List<Rule>> {
        return ruleDao.getAllRules()
    }

    /**
     * Get enabled rules as Flow
     */
    fun getEnabledRules(): Flow<List<Rule>> {
        return ruleDao.getEnabledRules()
    }

    /**
     * Get rule by ID
     */
    suspend fun getRuleById(ruleId: Long): Rule? {
        return ruleDao.getRuleById(ruleId)
    }

    /**
     * Get rule by ID as Flow
     */
    fun getRuleByIdFlow(ruleId: Long): Flow<Rule?> {
        return ruleDao.getRuleByIdFlow(ruleId)
    }

    /**
     * Create new rule
     */
    suspend fun createRule(rule: Rule): Long {
        return ruleDao.insertRule(rule)
    }

    /**
     * Update existing rule
     */
    suspend fun updateRule(rule: Rule) {
        ruleDao.updateRule(rule)
    }

    /**
     * Delete rule
     */
    suspend fun deleteRule(rule: Rule) {
        ruleDao.deleteRule(rule)
    }

    /**
     * Delete rule by ID
     */
    suspend fun deleteRuleById(ruleId: Long) {
        ruleDao.deleteRuleById(ruleId)
    }

    /**
     * Enable/disable rule
     */
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        ruleDao.setRuleEnabled(ruleId, enabled)
    }

    /**
     * Update rule trigger statistics
     */
    suspend fun updateRuleTriggerStats(ruleId: Long) {
        ruleDao.updateRuleTriggerStats(ruleId, System.currentTimeMillis())
    }

    /**
     * Get total rules count
     */
    suspend fun getRulesCount(): Int {
        return ruleDao.getRulesCount()
    }

    /**
     * Get enabled rules count
     */
    suspend fun getEnabledRulesCount(): Int {
        return ruleDao.getEnabledRulesCount()
    }

    /**
     * Delete all rules
     */
    suspend fun deleteAllRules() {
        ruleDao.deleteAllRules()
    }
}
