package com.nomad.android.data.repository

import app.cash.turbine.test
import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.SearchHistoryDao
import com.nomad.android.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllHistory returns history from DAO`() = runTest {
        val history = listOf(
            SearchHistoryEntity(query = "cpr", source = "knowledge", timestamp = 1000),
            SearchHistoryEntity(query = "water", source = "knowledge", timestamp = 2000)
        )
        val dao = FakeSearchHistoryDao(history)
        val repo = SearchRepository(dao)

        repo.getAllHistory().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @Test
    fun `insertHistoryEntry succeeds`() = runTest {
        val dao = FakeSearchHistoryDao()
        val repo = SearchRepository(dao)
        val entry = SearchHistoryEntity(query = "fire", source = "knowledge", timestamp = 1000)

        val result = repo.insertHistoryEntry(entry)
        assertTrue(result.isSuccess)
        assertTrue(dao.insertedEntries.contains(entry))
    }

    @Test
    fun `deleteOlderThan succeeds`() = runTest {
        val dao = FakeSearchHistoryDao()
        val repo = SearchRepository(dao)

        val result = repo.deleteOlderThan(5000)
        assertTrue(result.isSuccess)
        assertEquals(5000L, dao.deletedBeforeTimestamp)
    }
}

class FakeSearchHistoryDao(
    private val history: List<SearchHistoryEntity> = emptyList(),
    private val shouldFail: Boolean = false
) : SearchHistoryDao {

    val insertedEntries = mutableListOf<SearchHistoryEntity>()
    var deletedBeforeTimestamp: Long? = null

    override fun getAll() =
        if (shouldFail) throw RuntimeException("DB error")
        else flowOf(history)

    override suspend fun insert(entry: SearchHistoryEntity) {
        if (shouldFail) throw RuntimeException("DB error")
        insertedEntries.add(entry)
    }

    override suspend fun deleteOlderThan(cutoffTimestamp: Long) {
        if (shouldFail) throw RuntimeException("DB error")
        deletedBeforeTimestamp = cutoffTimestamp
    }
}
