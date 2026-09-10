package com.good4.community
import androidx.activity.compose.setContent

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.good4.MainActivity
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.data.repository.FirebaseAuthRepository
import com.good4.auth.domain.AuthUser
import com.good4.core.data.repository.*
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.KClass

class CommunityFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun open(manager: Boolean) {
        val auth = object : AuthRepository by FirebaseAuthRepository() {
            override val currentUser = AuthUser("test-manager", "manager@example.com", "Yönetici", true)
        }
        val repository = CommunityRepository(FixtureStore(manager), auth)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContentForCommunityTest(CommunityViewModel(repository))
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Test Topluluğu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Test Topluluğu").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tanışma Buluşması").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun studentSeesEventsAndCouponsWithoutManagement() {
        open(false)
        compose.onNodeWithText("Topluluğunu Yönet").assertDoesNotExist()
        compose.onNodeWithText("Tanışma Buluşması").performClick()
        compose.onNodeWithText("Birlikte tanışıyoruz.").assertIsDisplayed()
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithText("Topluluğa Özel Kuponlar").performClick()
        compose.onNodeWithText("Kahve İndirimi").assertIsDisplayed()
    }

    @Test fun managerCanOpenSimpleEditorAndPreview() {
        open(true)
        compose.onNodeWithText("Topluluğunu Yönet").performClick()
        compose.onNodeWithText("Etkinlik ekle").assertIsDisplayed()
        compose.onNodeWithText("Kupon ekle").assertIsDisplayed()
        compose.onNodeWithText("Topluluk bilgilerini düzenle").assertIsDisplayed()
        compose.onNodeWithText("Yönetimi kapat").performClick()
        compose.onNodeWithText("Tanışma Buluşması").performClick()
        compose.onNodeWithText("Düzenle").performClick()
        compose.onNodeWithText("Önizle").performScrollTo().performClick()
        compose.onNodeWithText("Önizleme").assertIsDisplayed()
        compose.onNodeWithText("Yayınla").assertIsDisplayed()
    }
}

private fun MainActivity.setContentForCommunityTest(vm: CommunityViewModel) {
    setContent { MaterialTheme { CommunitiesScreen(onBack = {}, viewModel = vm) } }
}

private class FixtureStore(private val manager: Boolean) : FirestoreRepository by FirestoreRepositoryImpl() {
    private val entries = listOf(
        DocumentWithId("event", CommunityEntryDto(title="Tanışma Buluşması",description="Birlikte tanışıyoruz.",date="2026-10-15",time="14:30",location="Kampüs")),
        DocumentWithId("coupon", CommunityEntryDto(kind="coupon",title="Kahve İndirimi",description="Bir kahvede indirim.",date="2026-10-30",location="Test Kafe",code="TEST"))
    )
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> getCollectionWithIds(collectionPath: String, clazz: KClass<T>): Result<List<DocumentWithId<T>>, Error> = Result.Success(
        (if (collectionPath == "communities") listOf(DocumentWithId("test", CommunityDto("Test Topluluğu", "Kampüste bir aradayız."))) else entries) as List<DocumentWithId<T>>
    )
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> getDocument(collectionPath: String, documentId: String, clazz: KClass<T>): Result<T, Error> = Result.Success(CommunityAccessDto(listOf("test"), manager) as T)
    override suspend fun <T : Any> queryCollectionWithIds(collectionPath: String, field: String, value: Any, clazz: KClass<T>) = getCollectionWithIds(collectionPath, clazz)
}
