package dev.raflos.cocina.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
    /** Última revisión del servidor sobre la que se basa este estado local. */
    val revision: Int,
    /** true si hay cambios locales que todavía no se confirmaron en el servidor. */
    val pendingSync: Boolean,
    val updatedAt: Long,
)
