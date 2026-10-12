package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.dashboard.presentation.CourseUiModel

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "CourseCard – Senza immagine (Light)")
@Composable
private fun CourseCardCompactPreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 1,
                    shortname = "052537 - TECHNOLOGIES FOR INF... 889897",
                    fullname = "052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA) [2025-26]",
                    courseImage = null,
                    progress = 0.65f,
                    isFavourite = true
                ),
                onClick = {},
                onFavouriteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "CourseCard – Con immagine")
@Composable
private fun CourseCardImagePreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 2,
                    shortname = "097683 - MAT",
                    fullname = "097683 - Analisi Matematica 1 (Rossi Mario) [2025-26]",
                    courseImage = "https://example.invalid/cover.png",
                    progress = null
                ),
                onClick = {},
                onFavouriteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "CourseCard – Dark")
@Composable
private fun CourseCardDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 3,
                    shortname = "054309 - LOC",
                    fullname = "054309 - LOCALIZATION, NAVIGATION AND SMART MOBILITY (NICOLI MONICA) [2025-26]",
                    courseImage = null,
                    progress = 0.3f
                ),
                onClick = {}
            )
        }
    }
}
