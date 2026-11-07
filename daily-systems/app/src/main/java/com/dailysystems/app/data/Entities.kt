package com.dailysystems.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "priority_label")
data class PriorityLabelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Int,
    val rank: Int,
)

@Entity(
    tableName = "task",
    foreignKeys = [
        ForeignKey(PriorityLabelEntity::class, ["id"], ["labelId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(SystemEntity::class, ["id"], ["runSystemId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("labelId"), Index("runSystemId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val labelId: String?,
    val dueDate: LocalDate?,
    val runSystemId: String?,
    val notes: String = "",
    val notesUpdatedAt: Long?,
    val done: Boolean,
    val doneOn: LocalDate?,
    val doneAt: Long?,
    val createdAt: Long,
    val isExample: Boolean = false,
    // v2
    val reminderTime: LocalTime? = null,
    @ColumnInfo(defaultValue = "NONE") val repeatUnit: String = "NONE",
    @ColumnInfo(defaultValue = "1") val repeatInterval: Int = 1,
    /** bit 0 = Monday … bit 6 = Sunday */
    @ColumnInfo(defaultValue = "0") val repeatWeekdays: Int = 0,
    val repeatAnchor: LocalDate? = null,
    val seriesId: String? = null,
)

@Entity(
    tableName = "system",
    foreignKeys = [ForeignKey(PriorityLabelEntity::class, ["id"], ["labelId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("labelId")],
)
data class SystemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val labelId: String?,
    val active: Boolean,
    val itemOrder: ItemOrder,
    val scheduleType: ScheduleType,
    /** bit 0 = Monday … bit 6 = Sunday */
    val weekdaysMask: Int,
    /** CSV of 1..31, plus 0 meaning "last day of month" */
    val monthDays: String,
    val createdAt: Long,
    val isExample: Boolean = false,
    // v2
    val reminderTime: LocalTime? = null,
)

@Entity(
    tableName = "system_date",
    primaryKeys = ["systemId", "date"],
    foreignKeys = [ForeignKey(SystemEntity::class, ["id"], ["systemId"], onDelete = ForeignKey.CASCADE)],
)
data class SystemDateEntity(
    val systemId: String,
    val date: LocalDate,
)

@Entity(
    tableName = "system_item",
    foreignKeys = [
        ForeignKey(SystemEntity::class, ["id"], ["systemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PriorityLabelEntity::class, ["id"], ["labelId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("systemId"), Index("labelId")],
)
data class SystemItemEntity(
    @PrimaryKey val id: String,
    val systemId: String,
    val kind: ItemKind,
    val title: String,
    val labelId: String?,
    val position: Int,
    val slotCount: Int = 1,
)

@Entity(
    tableName = "slot_accepted_label",
    primaryKeys = ["itemId", "labelId"],
    foreignKeys = [
        ForeignKey(SystemItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PriorityLabelEntity::class, ["id"], ["labelId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("labelId")],
)
data class SlotAcceptedLabelEntity(
    val itemId: String,
    val labelId: String,
)

@Entity(
    tableName = "step_completion",
    primaryKeys = ["systemId", "itemId", "date"],
    foreignKeys = [
        ForeignKey(SystemEntity::class, ["id"], ["systemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SystemItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("itemId"), Index("date")],
)
data class StepCompletionEntity(
    val systemId: String,
    val itemId: String,
    val date: LocalDate,
)

@Entity(
    tableName = "note",
    foreignKeys = [ForeignKey(PriorityLabelEntity::class, ["id"], ["labelId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("labelId")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val labelId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isExample: Boolean = false,
)
