package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.costumi.app.data.local.entity.PedidoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {

    /** La UI observa este Flow; se conserva el orden del servidor con [PedidoEntity.orden]. */
    @Query("SELECT * FROM pedido ORDER BY orden")
    fun observarTodos(): Flow<List<PedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(pedidos: List<PedidoEntity>)

    @Query("DELETE FROM pedido")
    suspend fun limpiar()

    /** Reemplaza todo el historial del cliente en una transacción (es la lista completa del usuario). */
    @Transaction
    suspend fun reemplazar(pedidos: List<PedidoEntity>) {
        limpiar()
        guardar(pedidos)
    }
}
