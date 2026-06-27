package com.skant.otpgateway.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "send_log")
data class SendLogEntry(
    @PrimaryKey val requestId: String,
    val phone: String,
    val status: String,        // pending | sent | failed | verified | expired
    val errorCode: Int?,
    val createdAt: Long
)

@Dao
interface SendLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: SendLogEntry)
    @Query("UPDATE send_log SET status = :status WHERE requestId = :requestId")
    suspend fun updateStatus(requestId: String, status: String)
    @Query("SELECT * FROM send_log ORDER BY createdAt DESC LIMIT 100")
    fun recent(): Flow<List<SendLogEntry>>
    @Query("SELECT COUNT(*) FROM send_log WHERE createdAt >= :sinceMs AND status = 'sent'")
    fun countSentSince(sinceMs: Long): Flow<Int>
    @Query("DELETE FROM send_log WHERE createdAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}

@Entity(tableName = "otp_pending")
data class OtpPending(
    @PrimaryKey val requestId: String,
    val phone: String,
    val otpHashBcrypt: String,
    val createdAt: Long,
    val expiresAt: Long,
    val attempts: Int
)

@Dao
interface OtpPendingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: OtpPending)
    @Query("SELECT * FROM otp_pending WHERE requestId = :requestId")
    suspend fun get(requestId: String): OtpPending?
    @Query("UPDATE otp_pending SET attempts = attempts + 1 WHERE requestId = :requestId")
    suspend fun bumpAttempts(requestId: String)
    @Query("DELETE FROM otp_pending WHERE requestId = :requestId")
    suspend fun delete(requestId: String)
    @Query("DELETE FROM otp_pending WHERE expiresAt < :nowMs")
    suspend fun purgeExpired(nowMs: Long)
    @Query("SELECT COUNT(*) FROM otp_pending WHERE phone = :phone AND createdAt >= :sinceMs")
    suspend fun countByPhoneSince(phone: String, sinceMs: Long): Int
}

@Database(entities = [SendLogEntry::class, OtpPending::class], version = 2, exportSchema = false)
abstract class GatewayDb : RoomDatabase() {
    abstract fun sendLogDao(): SendLogDao
    abstract fun otpPendingDao(): OtpPendingDao
}
