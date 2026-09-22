package com.abrarshakhi.dourdiary.common.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abrarshakhi.dourdiary.common.data.database.DourDiaryDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DourDiaryDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DourDiaryDatabase::class.java,
    )

    @Test
    fun migrating_from_1_to_2_keeps_the_runs_that_were_already_recorded() {
        helper.createDatabase(TestDatabaseName, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO runs (
                    id, startedAtEpochMillis, finishedAtEpochMillis, distanceMeters,
                    movingDurationMillis, elapsedDurationMillis, accumulatedPausedMillis, isComplete
                ) VALUES (1, 1700000000000, 1700000600000, 5000.0, 600000, 640000, 40000, 1)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO run_points (
                    id, runId, latitude, longitude, altitudeMeters,
                    epochMillis, elapsedRealtimeMillis, cumulativeDistanceMeters
                ) VALUES (1, 1, 52.52, 13.405, 34.0, 1700000000000, 0, 0.0)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TestDatabaseName,
            2,
            true,
            DourDiaryDatabase.Migration1To2,
        )

        migrated.query("SELECT distanceMeters, simplifiedRoute FROM runs WHERE id = 1").use { row ->
            assertTrue("The existing run did not survive the migration", row.moveToFirst())
            assertEquals(5000.0, row.getDouble(0), 1e-9)
            assertTrue("A pre-existing run must have no thumbnail, not a fake one", row.isNull(1))
        }

        migrated.query("SELECT COUNT(*) FROM run_points WHERE runId = 1").use { row ->
            row.moveToFirst()
            assertEquals("Route points were lost by the migration", 1, row.getInt(0))
        }
    }

    @Test
    fun the_new_column_accepts_a_route_summary() {
        helper.createDatabase(TestDatabaseName, 1).close()
        val migrated = helper.runMigrationsAndValidate(
            TestDatabaseName,
            2,
            true,
            DourDiaryDatabase.Migration1To2,
        )

        migrated.execSQL(
            """
            INSERT INTO runs (
                id, startedAtEpochMillis, finishedAtEpochMillis, distanceMeters,
                movingDurationMillis, elapsedDurationMillis, accumulatedPausedMillis,
                isComplete, simplifiedRoute
            ) VALUES (2, 1700000000000, NULL, 0.0, 0, 0, 0, 0, '_p~iF~ps|U')
            """.trimIndent(),
        )

        migrated.query("SELECT simplifiedRoute FROM runs WHERE id = 2").use { row ->
            row.moveToFirst()
            assertEquals("_p~iF~ps|U", row.getString(0))
        }
    }

    private companion object {
        const val TestDatabaseName = "migration-test.db"
    }
}
