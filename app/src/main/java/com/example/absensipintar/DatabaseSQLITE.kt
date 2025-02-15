package com.example.absensipintar.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.absensipintar.model.User

class DatabaseSQLITE(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_UID TEXT PRIMARY KEY,
                $COLUMN_NAMA TEXT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_ADMIN INTEGER
            )
        """.trimIndent()

        val createAbsenTable = """
            CREATE TABLE $TABLE_ABSENSI (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAMA TEXT,
                $COLUMN_TANGGAL TEXT,
                $COLUMN_WAKTU_MASUK TEXT,
                $COLUMN_STATUS TEXT
            )
        """.trimIndent()

        db.execSQL(createUserTable)
        db.execSQL(createAbsenTable)

        addAdminUser(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ABSENSI")
        onCreate(db)
    }

    private fun addAdminUser(db: SQLiteDatabase) {
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = 'admin@gmail.com'", null)
        if (!cursor.moveToFirst()) {
            val values = ContentValues().apply {
                put(COLUMN_UID, "1")
                put(COLUMN_NAMA, "Administrator")
                put(COLUMN_EMAIL, "admin@gmail.com")
                put(COLUMN_PASSWORD, "123456")
                put(COLUMN_ADMIN, 1)
            }
            db.insert(TABLE_USERS, null, values)
        }
        cursor.close()
    }

    fun saveAbsen(nama: String, tanggal: String, waktuMasuk: String, status: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAMA, nama)
            put(COLUMN_TANGGAL, tanggal)
            put(COLUMN_WAKTU_MASUK, waktuMasuk)
            put(COLUMN_STATUS, status)
        }

        val result = db.insert(TABLE_ABSENSI, null, values)
        db.close()
        return result != -1L
    }

    fun saveKeluar(nama: String, tanggal: String, waktuMasuk: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAMA, nama)
            put(COLUMN_TANGGAL, tanggal)
            put(COLUMN_WAKTU_MASUK, waktuMasuk)
        }

        val result = db.insert(TABLE_ABSENSI, null, values)
        db.close()
        return result != -1L
    }

    fun registerUser(uid: String, nama: String, email: String, password: String, isAdmin: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_UID, uid)
            put(COLUMN_NAMA, nama)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_ADMIN, if (isAdmin) 1 else 0)
        }

        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    fun loginUser(email: String, password: String): User? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(email, password))

        var user: User? = null
        if (cursor.moveToFirst()) {
            val uid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID))
            val nama = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAMA))
            val isAdmin = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ADMIN)) == 1

            user = User(uid, nama, email, password, isAdmin)
        }

        cursor.close()
        db.close()
        return user
    }

    fun isUserAdmin(email: String): Boolean {
        val db = readableDatabase
        val query = "SELECT $COLUMN_ADMIN FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))

        var isAdmin = false
        if (cursor.moveToFirst()) {
            isAdmin = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ADMIN)) == 1
        }

        cursor.close()
        db.close()
        return isAdmin
    }

    companion object {
        private const val DATABASE_NAME = "AbsensiDB"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USERS = "users"
        private const val COLUMN_UID = "uid"
        private const val COLUMN_NAMA = "nama"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_ADMIN = "admin"

        private const val TABLE_ABSENSI = "absensi"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TANGGAL = "tanggal"
        private const val COLUMN_WAKTU_MASUK = "waktu_masuk"
        private const val COLUMN_STATUS = "status"
    }
}
