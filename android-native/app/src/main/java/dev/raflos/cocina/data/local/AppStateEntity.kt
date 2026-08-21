package dev.raflos.cocina.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
    /** `sha` del blob de GitHub sobre el que se basa cada archivo local (null si nunca se leyó). */
    val despensaSha: String?,
    val recetasSha: String?,
    val listaSha: String?,
    /** true si ese archivo tiene cambios locales que todavía no se commitearon en GitHub. */
    val despensaDirty: Boolean,
    val recetasDirty: Boolean,
    val listaDirty: Boolean,
    val updatedAt: Long,
)
