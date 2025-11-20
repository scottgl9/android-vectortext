package com.vanespark.vertext.data.dao

import androidx.room.*
import com.vanespark.vertext.data.model.Rule
import kotlinx.coroutines.flow.Flow

/**
 * DAO for automation rules
 */
@Dao
interface RuleDao {

    /**
     * Get all rules
     */
    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<Rule>>

    /**
     * Get all enabled rules
     */
    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledRules(): Flow<List<Rule>>

    /**
     * Get rule by ID
     */
    @Query("SELECT * FROM rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: Long): Rule?

    /**
     * Get rule by ID as Flow
     */
    @Query("SELECT * FROM rules WHERE id = :ruleId")
    fun getRuleByIdFlow(ruleId: Long): Flow<Rule?>

    /**
     * Insert new rule
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: Rule): Long

    /**
     * Update existing rule
     */
    @Update
    suspend fun updateRule(rule: Rule)

    /**
     * Delete rule
     */
    @Delete
    suspend fun deleteRule(rule: Rule)

    /**
     * Delete rule by ID
     */
    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)

    /**
     * Enable/disable rule
     */
    @Query("UPDATE rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)

    /**
     * Update last triggered time and count
     */
    @Query("UPDATE rules SET lastTriggeredAt = :timestamp, triggerCount = triggerCount + 1 WHERE id = :ruleId")
    suspend fun updateRuleTriggerStats(ruleId: Long, timestamp: Long)

    /**
     * Get rules count
     */
    @Query("SELECT COUNT(*) FROM rules")
    suspend fun getRulesCount(): Int

    /**
     * Get enabled rules count
     */
    @Query("SELECT COUNT(*) FROM rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesCount(): Int

    /**
     * Delete all rules
     */
    @Query("DELETE FROM rules")
    suspend fun deleteAllRules()
}
