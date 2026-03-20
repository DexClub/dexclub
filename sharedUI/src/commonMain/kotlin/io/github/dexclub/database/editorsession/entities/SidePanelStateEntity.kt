package io.github.dexclub.database.editorsession.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "side_panel_state")
data class SidePanelStateEntity(
    @PrimaryKey
    val stateId: Int = STATE_ID_SINGLETON,
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val horizontalScrollOffset: Int = 0,
    val selectedNodeType: String? = null,
    val selectedNodeKey: String? = null,
    val updatedAt: Long = 0L,
) {
    companion object {
        const val STATE_ID_SINGLETON = 0
    }
}
