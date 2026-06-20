package com.nomad.android.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for the launch crash where [DatabaseModule.provideDatabase]
 * threw at `Room.Builder.build()`:
 *
 *   IllegalArgumentException: Inconsistency detected. A Migration was supplied to
 *   addMigration(...) that has a start or end version equal to a start version
 *   supplied to fallbackToDestructiveMigrationFrom(int... startVersions). Start version: 1
 *
 * Root cause: the builder listed `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)`
 * while also supplying MIGRATION_1_2/2_3/3_4/4_5 whose start versions (1..4)
 * overlap that destructive-fallback set. Room validates this at `build()`, so the
 * app crashed on every launch — including fresh installs — before any UI rendered.
 *
 * The full 1->6 migration chain makes destructive fallback unnecessary, so the
 * fix removes it (also honouring the project's never-drop/recreate rule).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DatabaseModuleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = DatabaseModule.provideDatabase(context)

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `provideDatabase builds without migration-fallback inconsistency`() {
        // Building alone reproduces the original crash; opening forces Room to
        // materialise the database and confirms it is actually usable.
        assertNotNull(db.settingsDao())
        db.openHelper.writableDatabase
    }
}
