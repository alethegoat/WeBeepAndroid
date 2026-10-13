package it.polimi.webeep.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versioned Room migrations; they replace the destructive fallback in production.
 * Each one must stay as shipped: installed databases still run it, and `app/schemas` holds the matching schema.
 */
object DatabaseMigrations {

    /** Version bump only. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Unchanged schema between v1 and v2 (same identity hash); version bump only.
        }
    }

    /** Adds the module page `url`. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE course_modules ADD COLUMN url TEXT")
        }
    }

    /** Adds the file `filepath` (directory inside a folder module). */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE module_contents ADD COLUMN filepath TEXT")
        }
    }

    /** All migrations in order, as passed to the database builder. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
