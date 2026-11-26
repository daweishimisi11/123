package com.av123.video.data.repository

import com.av123.video.data.db.FollowedActressDao
import com.av123.video.data.db.FollowedActressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedActressRepositoryTest {

    private class FakeFollowedActressDao : FollowedActressDao {
        private val state = MutableStateFlow<List<FollowedActressEntity>>(emptyList())

        override fun observeAll(): Flow<List<FollowedActressEntity>> = state
        override fun observeByPath(path: String): Flow<FollowedActressEntity?> =
            state.map { list -> list.firstOrNull { it.path == path } }
        override suspend fun get(path: String): FollowedActressEntity? =
            state.value.firstOrNull { it.path == path }
        override suspend fun getAll(): List<FollowedActressEntity> = state.value
        override suspend fun upsert(entity: FollowedActressEntity) {
            state.value = listOf(entity) + state.value.filterNot { it.path == entity.path }
        }
        override suspend fun upsertAll(entities: List<FollowedActressEntity>) {
            val paths = entities.map { it.path }.toSet()
            state.value = entities + state.value.filterNot { it.path in paths }
        }
        override suspend fun delete(path: String) {
            state.value = state.value.filterNot { it.path == path }
        }
        override suspend fun clear() {
            state.value = emptyList()
        }
    }

    @Test
    fun `unfollow removes actress and restore re-follows cleanly`() = runBlocking {
        val repo = FollowedActressRepository(FakeFollowedActressDao())
        val actress = FollowedActress(path = "/actress/1", name = "测试演员")

        repo.follow(actress)
        assertTrue(repo.isFollowed(actress.path).first())

        repo.unfollow(actress.path)
        assertFalse(repo.isFollowed(actress.path).first())
        assertTrue(repo.actresses.first().isEmpty())

        repo.follow(actress)
        assertTrue(repo.isFollowed(actress.path).first())
    }
}
