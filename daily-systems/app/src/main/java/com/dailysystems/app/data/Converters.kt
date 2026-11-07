package com.dailysystems.app.data

import androidx.room.TypeConverter
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun dateToText(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun textToDate(s: String?): LocalDate? = s?.let(LocalDate::parse)

    @TypeConverter fun timeToText(t: LocalTime?): String? = t?.toString()
    @TypeConverter fun textToTime(s: String?): LocalTime? = s?.let(LocalTime::parse)

    @TypeConverter fun orderToText(v: ItemOrder): String = v.name
    @TypeConverter fun textToOrder(s: String): ItemOrder = ItemOrder.valueOf(s)

    @TypeConverter fun scheduleToText(v: ScheduleType): String = v.name
    @TypeConverter fun textToSchedule(s: String): ScheduleType = ScheduleType.valueOf(s)

    @TypeConverter fun kindToText(v: ItemKind): String = v.name
    @TypeConverter fun textToKind(s: String): ItemKind = ItemKind.valueOf(s)
}
