package com.unpad.padukampus.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.unpad.padukampus.data.local.entity.*
import com.unpad.padukampus.data.local.relation.TugasDetail
import com.unpad.padukampus.data.repository.*
import com.unpad.padukampus.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun cleanup() {
        store.clear()
    }

    private class FakeRepository : TugasRepository {
        var saved: TugasInput? = null
        var detail: TugasDetail? = null

        override fun observeTugas(filter: TugasFilter) = flowOf(emptyList<TugasDetail>())

        override suspend fun getTugas(id: String) = detail

        override suspend fun simpan(input: TugasInput): TugasEntity {
            saved = input
            return TugasEntity(
                mataKuliahId = "course-test",
                judul = input.judul,
                deadline = input.deadline,
            )
        }

        override suspend fun setSelesai(id: String, selesai: Boolean) = Unit

        override suspend fun hapus(id: String) = Unit

        override suspend fun reconcileReminders() = Unit
    }

    private fun model(
        repository: FakeRepository,
        saved: SavedStateHandle = SavedStateHandle(),
        id: String = "",
    ): TaskViewModel = TaskViewModel(repository, saved, id).also { store.put("test", it) }

    @Test
    fun validFormIsConvertedToRepositoryInput() = runTest {
        val repository = FakeRepository()
        val model = model(repository)
        model.updateDraft {
            TaskFormDraft(
                "Mata kuliah uji",
                "Tugas uji",
                deadline = "2026-11-23 13:00",
                sumber = "LMS",
                pengingatMenit = "60",
            )
        }
        model.save()
        runCurrent()
        assertEquals(60, repository.saved!!.pengingatMenit)
        assertEquals("LMS", repository.saved!!.sumber)
        assertTrue(model.formState.value.saved)
    }

    @Test
    fun impossibleDateDoesNotReachRepository() = runTest {
        val repository = FakeRepository()
        val model = model(repository)
        model.updateDraft {
            it.copy(mataKuliah = "Uji", judul = "Uji", deadline = "2026-02-31 13:00")
        }
        model.save()
        runCurrent()
        assertNull(repository.saved)
        assertNotNull(model.formState.value.error)
    }

    @Test
    fun nonNumericReminderDoesNotReachRepository() = runTest {
        val repository = FakeRepository()
        val model = model(repository)
        model.updateDraft { it.copy(deadline = "2026-11-23 13:00", pengingatMenit = "abc") }
        model.save()
        runCurrent()
        assertNull(repository.saved)
        assertFalse(model.formState.value.saving)
    }

    @Test
    fun missingEditTargetCannotBeRecreatedAsNewTask() = runTest {
        val repository = FakeRepository()
        val model = model(repository, id = "deleted-test")
        runCurrent()
        model.save()
        runCurrent()
        assertTrue(model.formState.value.unavailable)
        assertNull(repository.saved)
    }

    @Test
    fun unsavedDraftSurvivesViewModelRecreation() = runTest {
        val saved = SavedStateHandle()
        val repository = FakeRepository()
        model(repository, saved).updateDraft {
            it.copy(judul = "Draf belum disimpan", sumber = "WA")
        }
        val recreated = model(repository, saved)
        assertEquals("Draf belum disimpan", recreated.formState.value.draft.judul)
        assertEquals("WA", recreated.formState.value.draft.sumber)
    }
}
