package com.dailysystems.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.ScheduleType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM priority_label ORDER BY rank, name")
    fun observeAll(): Flow<List<PriorityLabelEntity>>

    @Query("SELECT * FROM priority_label ORDER BY rank, name")
    suspend fun getAll(): List<PriorityLabelEntity>

    @Query("SELECT COUNT(*) FROM priority_label")
    suspend fun count(): Int

    @Upsert suspend fun upsert(label: PriorityLabelEntity)
    @Upsert suspend fun upsertAll(labels: List<PriorityLabelEntity>)

    @Query("UPDATE priority_label SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE priority_label SET colorArgb = :color WHERE id = :id")
    suspend fun setColor(id: String, color: Int)

    @Query("UPDATE priority_label SET rank = :rank WHERE id = :id")
    suspend fun setRank(id: String, rank: Int)

    @Query("DELETE FROM priority_label WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE id = :id")
    fun observe(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: TaskEntity)
    @Update suspend fun update(task: TaskEntity)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE task SET runSystemId = NULL WHERE runSystemId = :systemId")
    suspend fun clearRunSystem(systemId: String)

    @Query("SELECT * FROM task WHERE seriesId = :seriesId AND done = 0")
    suspend fun openInSeries(seriesId: String): List<TaskEntity>
}

@Dao
interface SystemDao {
    @Query("SELECT * FROM system") fun observeSystems(): Flow<List<SystemEntity>>
    @Query("SELECT * FROM system_item") fun observeItems(): Flow<List<SystemItemEntity>>
    @Query("SELECT * FROM slot_accepted_label") fun observeAccepted(): Flow<List<SlotAcceptedLabelEntity>>
    @Query("SELECT * FROM system_date") fun observeDates(): Flow<List<SystemDateEntity>>

    @Query("SELECT * FROM system WHERE id = :id") suspend fun get(id: String): SystemEntity?
    @Query("SELECT * FROM system_item WHERE systemId = :systemId ORDER BY position")
    suspend fun itemsOf(systemId: String): List<SystemItemEntity>
    @Query("SELECT * FROM system_item WHERE id = :id") suspend fun item(id: String): SystemItemEntity?

    @Insert suspend fun insert(system: SystemEntity)
    @Update suspend fun update(system: SystemEntity)
    @Query("DELETE FROM system WHERE id = :id") suspend fun delete(id: String)

    @Query("UPDATE system SET name = :name WHERE id = :id") suspend fun setName(id: String, name: String)
    @Query("UPDATE system SET labelId = :labelId WHERE id = :id") suspend fun setLabel(id: String, labelId: String?)
    @Query("UPDATE system SET active = :active WHERE id = :id") suspend fun setActive(id: String, active: Boolean)
    @Query("UPDATE system SET itemOrder = :order WHERE id = :id") suspend fun setOrder(id: String, order: ItemOrder)
    @Query("UPDATE system SET scheduleType = :type WHERE id = :id") suspend fun setScheduleType(id: String, type: ScheduleType)
    @Query("UPDATE system SET weekdaysMask = :mask WHERE id = :id") suspend fun setWeekdays(id: String, mask: Int)
    @Query("UPDATE system SET monthDays = :csv WHERE id = :id") suspend fun setMonthDays(id: String, csv: String)
    @Query("UPDATE system SET reminderTime = :time WHERE id = :id") suspend fun setReminderTime(id: String, time: java.time.LocalTime?)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertDate(date: SystemDateEntity)
    @Delete suspend fun deleteDate(date: SystemDateEntity)

    @Insert suspend fun insertItem(item: SystemItemEntity)
    @Insert suspend fun insertItems(items: List<SystemItemEntity>)
    @Query("DELETE FROM system_item WHERE id = :id") suspend fun deleteItem(id: String)
    @Query("UPDATE system_item SET title = :title WHERE id = :id") suspend fun setItemTitle(id: String, title: String)
    @Query("UPDATE system_item SET labelId = :labelId WHERE id = :id") suspend fun setItemLabel(id: String, labelId: String?)
    @Query("UPDATE system_item SET kind = :kind WHERE id = :id") suspend fun setItemKind(id: String, kind: ItemKind)
    @Query("UPDATE system_item SET slotCount = :count WHERE id = :id") suspend fun setSlotCount(id: String, count: Int)
    @Query("UPDATE system_item SET position = :position WHERE id = :id") suspend fun setItemPosition(id: String, position: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAccepted(rows: List<SlotAcceptedLabelEntity>)
    @Query("DELETE FROM slot_accepted_label WHERE itemId = :itemId AND labelId = :labelId")
    suspend fun deleteAccepted(itemId: String, labelId: String)
    @Query("SELECT COUNT(*) FROM slot_accepted_label WHERE itemId = :itemId AND labelId = :labelId")
    suspend fun isAccepted(itemId: String, labelId: String): Int

    @Transaction
    suspend fun moveItem(systemId: String, itemId: String, delta: Int) {
        val i = itemsOf(systemId).indexOfFirst { it.id == itemId }
        if (i >= 0) moveItemTo(systemId, itemId, i + delta)
    }

    /** Drag-and-drop: put [itemId] at [toIndex] and renumber positions 0..n-1. */
    @Transaction
    suspend fun moveItemTo(systemId: String, itemId: String, toIndex: Int) {
        val items = itemsOf(systemId).toMutableList()
        val i = items.indexOfFirst { it.id == itemId }
        if (i < 0 || toIndex !in items.indices || toIndex == i) return
        items.add(toIndex, items.removeAt(i))
        items.forEachIndexed { pos, it -> if (it.position != pos) setItemPosition(it.id, pos) }
    }

    @Transaction
    suspend fun deleteItemAndCompact(systemId: String, itemId: String) {
        deleteItem(itemId)
        itemsOf(systemId).forEachIndexed { pos, it -> if (it.position != pos) setItemPosition(it.id, pos) }
    }
}

@Dao
interface StepCompletionDao {
    @Query("SELECT * FROM step_completion WHERE date = :date")
    fun observeOn(date: LocalDate): Flow<List<StepCompletionEntity>>

    /** ISO date text sorts chronologically, so BETWEEN works on the stored strings. */
    @Query("SELECT * FROM step_completion WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<StepCompletionEntity>>

    @Query("SELECT COUNT(*) FROM step_completion WHERE systemId = :systemId AND itemId = :itemId AND date = :date")
    suspend fun count(systemId: String, itemId: String, date: LocalDate): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(row: StepCompletionEntity)

    @Query("DELETE FROM step_completion WHERE systemId = :systemId AND itemId = :itemId AND date = :date")
    suspend fun delete(systemId: String, itemId: String, date: LocalDate)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note") fun observeAll(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM note WHERE id = :id") fun observe(id: String): Flow<NoteEntity?>
    @Query("SELECT * FROM note WHERE id = :id") suspend fun get(id: String): NoteEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(note: NoteEntity)
    @Update suspend fun update(note: NoteEntity)
    @Query("DELETE FROM note WHERE id = :id") suspend fun delete(id: String)
}
