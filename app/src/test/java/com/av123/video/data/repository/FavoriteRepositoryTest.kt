package com.av123.video.data.repository

import com.av123.video.data.db.FavoriteDao
import com.av123.video.data.db.FavoriteEntity
import com.av123.video.data.db.FavoriteGroupDao
import com.av123.video.data.db.FavoriteGroupEntity
import com.av123.video.data.db.FavoriteGroupItemEntity
import com.av123.video.data.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteRepositoryTest {

    private class FakeFavoriteDao : FavoriteDao {
        private val state = MutableStateFlow<List<FavoriteEntity>>(emptyList())

        override fun observeAll(): Flow<List<FavoriteEntity>> = state
        override fun observeIsFavorite(videoId: String): Flow<Int?> =
            state.map { list -> if (list.any { it.videoId == videoId }) 1 else null }
        override fun observeCount(): Flow<Int> = state.map { it.size }
        override suspend fun insertIfAbsent(entity: FavoriteEntity) {
            if (state.value.none { it.videoId == entity.videoId }) {
                state.value = listOf(entity) + state.value
            }
        }
        override suspend fun delete(videoId: String) {
            state.value = state.value.filterNot { it.videoId == videoId }
        }
        override suspend fun delete(videoIds: Collection<String>) {
            state.value = state.value.filterNot { it.videoId in videoIds }
        }
        override suspend fun upsertAll(entities: List<FavoriteEntity>) {
            val incoming = entities.associateBy { it.videoId }
            state.value = entities + state.value.filterNot { it.videoId in incoming }
        }
        override suspend fun getAll(): List<FavoriteEntity> = state.value
        override suspend fun clear() {
            state.value = emptyList()
        }
    }

    private class FakeFavoriteGroupDao : FavoriteGroupDao {
        private var nextId = 1L
        private val groupsState = MutableStateFlow<List<FavoriteGroupEntity>>(emptyList())
        private val itemsState = MutableStateFlow<List<FavoriteGroupItemEntity>>(emptyList())

        override fun observeGroups(): Flow<List<FavoriteGroupEntity>> = groupsState
        override fun observeItems(): Flow<List<FavoriteGroupItemEntity>> = itemsState
        override suspend fun getAll(): List<FavoriteGroupEntity> = groupsState.value
        override suspend fun getAllItems(): List<FavoriteGroupItemEntity> = itemsState.value
        override suspend fun count(): Int = groupsState.value.size
        override suspend fun findByName(name: String): FavoriteGroupEntity? =
            groupsState.value.firstOrNull { it.name == name }
        override suspend fun get(groupId: Long): FavoriteGroupEntity? =
            groupsState.value.firstOrNull { it.groupId == groupId }
        override suspend fun insert(entity: FavoriteGroupEntity): Long {
            if (findByName(entity.name) != null) return -1L
            val id = nextId++
            groupsState.value = listOf(entity.copy(groupId = id)) + groupsState.value
            return id
        }
        override suspend fun rename(groupId: Long, name: String) {
            groupsState.value = groupsState.value.map {
                if (it.groupId == groupId) it.copy(name = name) else it
            }
        }
        override suspend fun delete(groupId: Long) {
            groupsState.value = groupsState.value.filterNot { it.groupId == groupId }
            itemsState.value = itemsState.value.filterNot { it.groupId == groupId }
        }
        override suspend fun upsertItems(items: List<FavoriteGroupItemEntity>) {
            val keys = items.map { it.groupId to it.videoId }.toSet()
            itemsState.value = itemsState.value.filterNot { (it.groupId to it.videoId) in keys } + items
        }
        override suspend fun removeItems(groupId: Long, videoIds: Collection<String>) {
            itemsState.value = itemsState.value.filterNot {
                it.groupId == groupId && it.videoId in videoIds
            }
        }
    }

    @Test
    fun `removing from one group preserves global favorite and other group memberships`() = runBlocking {
        val favoriteDao = FakeFavoriteDao()
        val groupDao = FakeFavoriteGroupDao()
        val repo = FavoriteRepository(favoriteDao, groupDao)

        val video = Video(id = "v1", title = "测试视频", category = "")
        repo.add(video)
        val groupA = (repo.createGroup("收藏夹A") as GroupEditResult.Success).group
        val groupB = (repo.createGroup("收藏夹B") as GroupEditResult.Success).group
        repo.setGroupMembership(groupA.id, listOf("v1"), member = true)
        repo.setGroupMembership(groupB.id, listOf("v1"), member = true)

        repo.setGroupMembership(groupA.id, listOf("v1"), member = false)

        assertTrue(repo.isFavorite("v1"))
        val remainingItems = repo.groupItems.first()
        assertEquals(listOf(groupB.id), remainingItems.map { it.groupId })
    }

    @Test
    fun `replaceVideoGroups switches memberships without losing favorite record`() = runBlocking {
        val favoriteDao = FakeFavoriteDao()
        val groupDao = FakeFavoriteGroupDao()
        val repo = FavoriteRepository(favoriteDao, groupDao)

        val video = Video(id = "v1", title = "测试视频", category = "")
        repo.add(video)
        val groupA = (repo.createGroup("分组A") as GroupEditResult.Success).group
        val groupB = (repo.createGroup("分组B") as GroupEditResult.Success).group
        val groupC = (repo.createGroup("分组C") as GroupEditResult.Success).group

        repo.replaceVideoGroups("v1", setOf(groupA.id, groupB.id))
        assertEquals(setOf(groupA.id, groupB.id), repo.groupItems.first().map { it.groupId }.toSet())

        repo.replaceVideoGroups("v1", setOf(groupB.id, groupC.id))
        assertTrue(repo.isFavorite("v1"))
        assertEquals(setOf(groupB.id, groupC.id), repo.groupItems.first().map { it.groupId }.toSet())

        repo.replaceVideoGroups("v1", emptySet())
        assertTrue(repo.isFavorite("v1"))
        assertTrue(repo.groupItems.first().isEmpty())
    }
}
