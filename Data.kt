package com.oilchange.app

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class Customer(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val mobile: String = "", val note: String = "")

@Entity(
    indices = [Index("customerId"), Index("plateKey")],
    foreignKeys = [ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)]
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String,
    val brandModel: String,
    val color: String = "",
    val year: String = "",
    val plateKey: String,
    val plateText: String
)

@Entity(
    indices = [Index("vehicleId"), Index("serviceAt")],
    foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)]
)
data class Service(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val serviceAt: Long,
    val odometer: Int,
    val oilName: String = "",
    val intervalKm: Int = 0,
    val nextKm: Int = 0,
    val price: Long = 0,
    val note: String = "",
    val items: String = ""
)

@Entity data class Oil(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val intervalKm: Int)
@Entity data class ServiceType(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String)

data class VehicleRow(@Embedded val v: Vehicle, val customerName: String, val lastAt: Long?, val nextKm: Int?)
data class ServiceRow(@Embedded val s: Service, val plateText: String, val customerName: String)

@Dao
interface AppDao {
    @Query("SELECT * FROM Customer WHERE name LIKE '%'||:q||'%' OR mobile LIKE '%'||:q||'%' ORDER BY id DESC")
    fun customers(q: String): Flow<List<Customer>>
    @Query("SELECT * FROM Customer WHERE id=:id") fun customer(id: Long): Flow<Customer?>
    @Query("SELECT COUNT(*) FROM Customer") fun customerCount(): Flow<Int>
    @Insert suspend fun insert(c: Customer): Long
    @Update suspend fun update(c: Customer)
    @Delete suspend fun delete(c: Customer)

    @Query("SELECT COUNT(*) FROM Vehicle") fun vehicleCount(): Flow<Int>
    @Query("SELECT * FROM Vehicle WHERE id=:id") fun vehicle(id: Long): Flow<Vehicle?>
    @Query("SELECT * FROM Vehicle WHERE customerId=:cid ORDER BY id DESC") fun vehiclesOf(cid: Long): Flow<List<Vehicle>>
    @Query("""SELECT v.*, c.name AS customerName,
        (SELECT MAX(serviceAt) FROM Service s WHERE s.vehicleId=v.id) AS lastAt,
        (SELECT nextKm FROM Service s WHERE s.vehicleId=v.id ORDER BY serviceAt DESC, id DESC LIMIT 1) AS nextKm
        FROM Vehicle v JOIN Customer c ON c.id=v.customerId
        WHERE v.plateKey LIKE '%'||:q||'%' OR REPLACE(v.plateKey,'-','') LIKE '%'||:qd||'%'
        OR v.brandModel LIKE '%'||:q||'%' OR c.name LIKE '%'||:q||'%' OR c.mobile LIKE '%'||:q||'%'
        ORDER BY v.id DESC""")
    fun vehicles(q: String, qd: String): Flow<List<VehicleRow>>
    @Insert suspend fun insert(v: Vehicle): Long
    @Update suspend fun update(v: Vehicle)
    @Delete suspend fun delete(v: Vehicle)

    @Query("SELECT * FROM Service WHERE vehicleId=:vid ORDER BY serviceAt DESC, id DESC") fun services(vid: Long): Flow<List<Service>>
    @Query("SELECT * FROM Service WHERE vehicleId=:vid ORDER BY serviceAt DESC, id DESC LIMIT 1") fun lastService(vid: Long): Flow<Service?>
    @Query("SELECT COUNT(*) FROM Service WHERE serviceAt>=:ms") fun serviceCountSince(ms: Long): Flow<Int>
    @Query("SELECT COALESCE(SUM(price),0) FROM Service WHERE serviceAt>=:ms") fun revenueSince(ms: Long): Flow<Long>
    @Query("""SELECT s.*, v.plateText AS plateText, c.name AS customerName FROM Service s
        JOIN Vehicle v ON v.id=s.vehicleId JOIN Customer c ON c.id=v.customerId
        WHERE s.serviceAt>=:from ORDER BY s.serviceAt DESC""")
    fun servicesSince(from: Long): Flow<List<ServiceRow>>
    @Insert suspend fun insert(s: Service): Long
    @Delete suspend fun delete(s: Service)

    @Query("SELECT * FROM Oil ORDER BY name") fun oils(): Flow<List<Oil>>
    @Insert suspend fun insert(o: Oil): Long
    @Delete suspend fun delete(o: Oil)
    @Query("SELECT * FROM ServiceType ORDER BY id") fun types(): Flow<List<ServiceType>>
    @Insert suspend fun insert(t: ServiceType): Long
    @Delete suspend fun delete(t: ServiceType)
}

@Database(entities = [Customer::class, Vehicle::class, Service::class, Oil::class, ServiceType::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        const val NAME = "oilchange.db"
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        listOf("تعویض روغن موتور", "تعویض فیلتر روغن", "تعویض فیلتر هوا", "تعویض فیلتر کابین", "تعویض فیلتر بنزین",
                            "تعویض روغن گیربکس", "تعویض روغن ترمز", "تعویض روغن هیدرولیک فرمان", "تعویض ضدیخ", "چک سطح مایعات",
                            "چک ترمز", "چک باتری", "چک لاستیک و باد").forEach { db.execSQL("INSERT INTO ServiceType(title) VALUES ('$it')") }
                    }
                }).build().also { inst = it }
        }
        fun close() { inst?.close(); inst = null }
    }
}
