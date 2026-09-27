package com.good4.community
import androidx.activity.compose.setContent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.good4.MainActivity
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.data.repository.FirebaseAuthRepository
import com.good4.auth.domain.AuthUser
import com.good4.core.data.repository.*
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.flowOf

class CommunityFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun open(manager: Boolean) {
        val auth = object : AuthRepository by FirebaseAuthRepository() {
            override val currentUser = AuthUser("test-manager", "manager@example.com", "Yönetici", true)
        }
        val repository = CommunityRepository(FixtureStore(manager), auth)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContentForCommunityTest(CommunityViewModel(repository, object : EventAdmissionGateway {
                override fun followers(communityId: String) = flowOf(42)
                override fun observe(communityId: String, eventId: String) = flowOf(EventAdmissionSnapshot())
                override suspend fun record(communityId: String, eventId: String, userId: String, token: String?, undo: Boolean) = true
            }))
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Test Topluluğu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Test Topluluğu").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(if (manager) "Topluluğumu Yönet" else "Tanışma Buluşması")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun studentSeesEventsAndCouponsWithoutManagement() {
        open(false)
        compose.onNodeWithText("Topluluğumu Yönet").assertDoesNotExist()
        compose.onNodeWithText("Tanışma Buluşması").performClick()
        compose.onNodeWithText("Birlikte tanışıyoruz.").assertIsDisplayed()
        compose.onNodeWithText("Etkinliğe kayıt ol").assertIsDisplayed()
        compose.onNodeWithContentDescription("Kapat").performClick()
        compose.onNodeWithText("Kuponlar").performClick()
        compose.onNodeWithText("Kahve İndirimi").assertIsDisplayed()
        compose.onNodeWithText("Kahve İndirimi").performClick()
        compose.onNodeWithText("TEST").assertDoesNotExist()
    }

    @Test fun studentCanOpenFeaturedEventFromCommunityList() {
        val auth = object : AuthRepository by FirebaseAuthRepository() {
            override val currentUser = AuthUser("test-student", "student@example.com", "Öğrenci", true)
        }
        val repository = CommunityRepository(FixtureStore(manager = false), auth)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContentForCommunityTest(CommunityViewModel(repository, object : EventAdmissionGateway {
                override fun followers(communityId: String) = flowOf(42)
                override fun observe(communityId: String, eventId: String) = flowOf(EventAdmissionSnapshot())
                override suspend fun record(communityId: String, eventId: String, userId: String, token: String?, undo: Boolean) = true
            }))
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Tanışma Buluşması").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Yaklaşan etkinlikler").assertIsDisplayed()
        compose.onNodeWithText("Tanışma Buluşması").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Etkinliğe kayıt ol").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Birlikte tanışıyoruz.").assertIsDisplayed()
    }

    @Test fun managerCanOpenSimpleEditorAndPreview() {
        open(true)
        compose.onNodeWithText("Takip et").assertDoesNotExist()
        compose.onNodeWithText("Topluluğumu Yönet").assertIsDisplayed()
        compose.onNodeWithText("YÖNETİCİ PANELİ").assertIsDisplayed()
        compose.onNodeWithText("Takip ediliyor").assertDoesNotExist()
        compose.onNodeWithText("42").assertIsDisplayed()
        compose.onNodeWithText("Yaklaşanlar").assertIsDisplayed()
        compose.onNodeWithText("Toplam kayıt").assertIsDisplayed()
        compose.onNodeWithText("Etkinlik ekle").assertIsDisplayed()
        compose.onNodeWithText("Kupon ekle").assertIsDisplayed()
        compose.onNodeWithText("Öğrenci görünümünü aç").assertIsDisplayed()
        compose.onNodeWithText("Topluluk bilgilerini düzenle").assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Tanışma Buluşması"))
        compose.onNodeWithText("Katılımcılar · QR").assertIsDisplayed()
        compose.onNodeWithText("Tanışma Buluşması").performClick()
        compose.onAllNodesWithText("0 kişi kayıtlı").onFirst().assertIsDisplayed()
        compose.onNode(hasText("Düzenle") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Önizle").performScrollTo().performClick()
        compose.onNodeWithText("Önizleme").assertIsDisplayed()
        compose.onNodeWithText("Yayınla").assertIsDisplayed()
    }

    @Test fun managerCanOpenQrAdmissionAndReturnToEvent() {
        open(true)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Katılımcılar · QR"))
        compose.onNodeWithText("Katılımcılar · QR").performClick()
        compose.onNodeWithText("Katılımcıları Yönet").assertIsDisplayed()
        compose.onNodeWithText("QR okut").assertIsDisplayed()
        compose.onNode(hasText("0 kayıtlı · 0 giriş yaptı") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("Gelenler").performClick()
        compose.onNodeWithText("Henüz giriş yapan yok.").assertIsDisplayed()
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithText("Katılımcıları Yönet").assertDoesNotExist()
    }

    @Test fun managerCanUnpublishAnExistingEvent() {
        open(true)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Yayından kaldır"))
        compose.onNodeWithText("Yayından kaldır").performClick()
        compose.onNodeWithText("Etkinlik yayından kaldırılsın mı?").assertIsDisplayed()
        compose.onNodeWithText("Mevcut kayıt ve katılım geçmişi korunacak.", substring = true).assertIsDisplayed()
        compose.onNode(hasText("Kaldır") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tanışma Buluşması").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Geçmiş").performClick()
        compose.onNodeWithText("Tanışma Buluşması").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Kaldırıldı").assertIsDisplayed()
    }

    @Test fun studentRegistersAndReopensQrFromTheirRegistrations() {
        open(false)
        compose.onNodeWithText("Tanışma Buluşması").performClick()
        compose.onNodeWithText("Etkinliğe kayıt ol").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("QR giriş biletin").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Etkinlik giriş QR kodu").assertIsDisplayed()
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithContentDescription("Kapat").performClick()
        compose.onNodeWithText("Etkinlik kayıtlarım").performScrollTo().performClick()
        compose.onNodeWithText("Tanışma Buluşması").performScrollTo().performClick()
        compose.onNodeWithText("QR biletimi göster").performClick()
        compose.onNodeWithContentDescription("Etkinlik giriş QR kodu").assertIsDisplayed()
    }

    @Test fun manualAdmissionRequiresConfirmationAndCanBeUndone() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                var attendance by remember { mutableStateOf(emptyList<EventAttendanceDto>()) }
                MaterialTheme {
                    EventAdmissionScreen(
                        CommunityEntry("event", CommunityEntryDto(title = "Giriş testi")),
                        listOf(CommunityEventRegistrationDto("ada", "Ada Yılmaz", 1)), attendance,
                        false, null, null, {}, {}, {},
                        onAdmit = { uid, undo -> attendance = if (undo) emptyList() else listOf(EventAttendanceDto(uid, "manager", 1)) }
                    )
                }
            }
        }
        compose.onNodeWithText("1 kayıtlı · 0 giriş yaptı").assertIsDisplayed()
        compose.onNodeWithText("Geldi").performClick()
        compose.onNodeWithText("Vazgeç").performClick()
        compose.onNodeWithText("1 kayıtlı · 0 giriş yaptı").assertIsDisplayed()
        compose.onNodeWithText("Geldi").performClick()
        compose.onNodeWithText("Onayla").performClick()
        compose.onNodeWithText("1 kayıtlı · 1 giriş yaptı").assertIsDisplayed()
        compose.onNodeWithText("Gelenler").performClick()
        compose.onNodeWithText("Ada Yılmaz").assertIsDisplayed()
        compose.onNodeWithText("Geri al").performClick()
        compose.onNodeWithText("Onayla").performClick()
        compose.onNodeWithText("1 kayıtlı · 0 giriş yaptı").assertIsDisplayed()
        compose.onNodeWithText("Henüz giriş yapan yok.").assertIsDisplayed()
    }
}

private fun MainActivity.setContentForCommunityTest(vm: CommunityViewModel) {
    setContent { MaterialTheme { CommunitiesScreen(onBack = {}, viewModel = vm) } }
}

private class FixtureStore(private val manager: Boolean) : FirestoreRepository by FirestoreRepositoryImpl() {
    private val registrations = mutableMapOf<String, CommunityEventRegistrationDto>()
    override suspend fun <T : Any> updateDocument(collectionPath: String, documentId: String, data: T): Result<Unit, Error> {
        if (data is CommunityEventRegistrationDto) registrations[documentId] = data
        return Result.Success(Unit)
    }
    private val entries = mutableListOf(
        DocumentWithId("event", CommunityEntryDto(title="Tanışma Buluşması",description="Birlikte tanışıyoruz.",date="2099-10-15",time="14:30",location="Kampüs",imageUrl="https://example.invalid/event.jpg")),
        DocumentWithId("coupon", CommunityEntryDto(kind="coupon",title="Kahve İndirimi",description="Bir kahvede indirim.",date="2026-10-30",location="Test Kafe",code="TEST"))
    )
    override suspend fun updateFields(
        collectionPath: String,
        documentId: String,
        fields: Map<String, Any?>
    ): Result<Unit, Error> {
        if (collectionPath.endsWith("/entries")) {
            val index = entries.indexOfFirst { it.id == documentId }
            val status = fields["status"] as? String
            if (index >= 0 && status != null) {
                val current = entries[index]
                entries[index] = current.copy(data = current.data.copy(status = status))
            }
        }
        return Result.Success(Unit)
    }
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> getCollectionWithIds(collectionPath: String, clazz: KClass<T>): Result<List<DocumentWithId<T>>, Error> = Result.Success(
        (when {
            collectionPath == "communities" -> listOf(DocumentWithId("test", CommunityDto("Test Topluluğu", "Kampüste bir aradayız.")))
            collectionPath.endsWith("/registrations") -> emptyList()
            else -> entries
        }) as List<DocumentWithId<T>>
    )
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> getDocument(collectionPath: String, documentId: String, clazz: KClass<T>): Result<T, Error> =
        if (collectionPath == "community_access") Result.Success(CommunityAccessDto(listOf("test"), manager) as T)
        else if (collectionPath.endsWith("/registrations") && registrations[documentId] != null) Result.Success(registrations.getValue(documentId) as T)
        else Result.Error(NetworkError("not found"))
    override suspend fun <T : Any> queryCollectionWithIds(collectionPath: String, field: String, value: Any, clazz: KClass<T>) = getCollectionWithIds(collectionPath, clazz)
}
