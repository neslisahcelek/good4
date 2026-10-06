package com.good4.social

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Kayaking
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Skateboarding
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.SportsVolleyball
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** One activity type; [id] matches SOCIAL_ACTIVITY_TYPES in functions/src/social.ts. */
data class SocialType(val id: String, val kind: String, val label: StringResource, val icon: ImageVector)

private fun sport(id: String, label: StringResource, icon: ImageVector) = SocialType(id, "sport", label, icon)
private fun social(id: String, label: StringResource, icon: ImageVector) = SocialType(id, "social", label, icon)

val SOCIAL_TYPES: List<SocialType> = listOf(
    sport("football", Res.string.social_type_football, Icons.Outlined.SportsSoccer),
    sport("basketball", Res.string.social_type_basketball, Icons.Outlined.SportsBasketball),
    sport("volleyball", Res.string.social_type_volleyball, Icons.Outlined.SportsVolleyball),
    sport("tennis", Res.string.social_type_tennis, Icons.Outlined.SportsTennis),
    sport("table-tennis", Res.string.social_type_table_tennis, Icons.Outlined.SportsTennis),
    sport("badminton", Res.string.social_type_badminton, Icons.Outlined.SportsTennis),
    sport("running", Res.string.social_type_running, Icons.AutoMirrored.Outlined.DirectionsRun),
    sport("hiking", Res.string.social_type_hiking, Icons.Outlined.Hiking),
    sport("cycling", Res.string.social_type_cycling, Icons.AutoMirrored.Outlined.DirectionsBike),
    sport("swimming", Res.string.social_type_swimming, Icons.Outlined.Pool),
    sport("sup-kayak", Res.string.social_type_sup_kayak, Icons.Outlined.Kayaking),
    sport("skating", Res.string.social_type_skating, Icons.Outlined.Skateboarding),
    sport("fitness", Res.string.social_type_fitness, Icons.Outlined.FitnessCenter),
    sport("yoga-pilates", Res.string.social_type_yoga_pilates, Icons.Outlined.SelfImprovement),
    sport("other-sport", Res.string.social_type_other, Icons.Outlined.Category),
    social("board-games", Res.string.social_type_board_games, Icons.Outlined.Casino),
    social("coffee", Res.string.social_type_coffee, Icons.Outlined.LocalCafe),
    social("food", Res.string.social_type_food, Icons.Outlined.Restaurant),
    social("meetup", Res.string.social_type_meetup, Icons.Outlined.Groups),
    social("movie", Res.string.social_type_movie, Icons.Outlined.Movie),
    social("concert", Res.string.social_type_concert, Icons.Outlined.Celebration),
    social("video-games", Res.string.social_type_video_games, Icons.Outlined.SportsEsports),
    social("music", Res.string.social_type_music, Icons.Outlined.MusicNote),
    social("trip", Res.string.social_type_trip, Icons.Outlined.Explore),
    social("language-exchange", Res.string.social_type_language_exchange, Icons.Outlined.Translate),
    social("study", Res.string.social_type_study, Icons.Outlined.AutoStories),
    social("other-social", Res.string.social_type_other, Icons.Outlined.Category)
)

fun socialType(id: String): SocialType? = SOCIAL_TYPES.firstOrNull { it.id == id }

/** Games offered for "Masa oyunları"; matches SOCIAL_BOARD_GAMES on the server. */
val SOCIAL_BOARD_GAMES: List<Pair<String, StringResource>> = listOf(
    "okey" to Res.string.social_game_okey,
    "backgammon" to Res.string.social_game_backgammon,
    "chess" to Res.string.social_game_chess,
    "uno" to Res.string.social_game_uno,
    "taboo" to Res.string.social_game_taboo,
    "cards" to Res.string.social_game_cards,
    "other" to Res.string.social_game_other
)

val SOCIAL_LEVELS: List<Pair<String, StringResource>> = listOf(
    "any" to Res.string.social_level_any,
    "beginner" to Res.string.social_level_beginner,
    "intermediate" to Res.string.social_level_intermediate,
    "advanced" to Res.string.social_level_advanced
)

/** Optional choices for digital game activities. */
val SOCIAL_VIDEO_GAMES: List<Pair<String, StringResource>> = listOf(
    "fifa" to Res.string.social_game_fifa,
    "pes" to Res.string.social_game_pes
)

val SOCIAL_REPORT_REASONS: List<Pair<String, StringResource>> = listOf(
    "harassment" to Res.string.social_report_harassment,
    "safety" to Res.string.social_report_safety,
    "inappropriate" to Res.string.social_report_inappropriate,
    "spam" to Res.string.social_report_spam,
    "other" to Res.string.social_report_other
)
