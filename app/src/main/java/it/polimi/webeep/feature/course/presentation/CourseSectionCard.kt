package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.core.util.MoodleText

private val LinkModuleTypes = setOf("url", "lti", "webex")

/** Start inset that lines text up with row labels (after the file-type icon). */
private val RowTextInset = WeBeepSpacing.md + FileIconContainerSize + WeBeepSpacing.sm

/** One course section: a single card whose rows sit directly on the card surface. */
@Composable
internal fun SectionCard(
    section: SectionUiModel,
    onHeaderClick: () -> Unit,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WeBeepSpacing.md)
    ) {
        SectionHeader(section = section, onClick = onHeaderClick)
        AnimatedVisibility(
            visible = section.isExpanded,
            enter = expandVertically(animationSpec = WeBeepMotion.enter(reduceMotion = reduceMotion)) +
                fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
            exit = shrinkVertically(animationSpec = WeBeepMotion.exit(reduceMotion = reduceMotion)) +
                fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WeBeepSpacing.xs)
            ) {
                if (section.modules.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_materials_section),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.xs)
                    )
                } else {
                    section.modules.forEachIndexed { index, module ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = RowTextInset, end = WeBeepSpacing.md)
                            )
                        }
                        ModuleRow(
                            module = module,
                            onPreviewClick = onPreviewClick,
                            onDownloadClick = onDownloadClick,
                            onOpenClick = onOpenClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    section: SectionUiModel,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
            .padding(
                start = WeBeepSpacing.md,
                end = WeBeepSpacing.xxs,
                top = WeBeepSpacing.sm,
                bottom = WeBeepSpacing.sm
            )
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = section.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!section.summary.isNullOrBlank()) {
                val cleanSummary = remember(section.summary) { MoodleText.cleanHtml(section.summary) }
                if (cleanSummary.isNotBlank()) {
                    Text(
                        text = cleanSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = WeBeepSpacing.xxs)
                    )
                }
            }
        }
        ExpandChevron(
            expanded = section.isExpanded,
            contentDescription = stringResource(
                if (section.isExpanded) R.string.cd_collapse_section else R.string.cd_expand_section
            )
        )
    }
}

@Composable
private fun ModuleRow(
    module: ModuleUiModel,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val isLink = module.type in LinkModuleTypes
    val isFolder = module.type == "folder"
    val isSingleFile = !isFolder && !isLink && module.files.size == 1
    val isMultiFile = !isFolder && !isLink && module.files.size > 1
    val hasLinkPreview = isLink && !module.url.isNullOrBlank()

    val fileColors = WeBeepTheme.colors
    val (moduleIcon, moduleColor) = when {
        isFolder -> Icons.Default.Folder to fileColors.fileFolder
        isLink -> Icons.Default.Link to fileColors.fileLink
        isSingleFile -> fileIconAndColor(module.files.first().filename, fileColors)
        else -> Icons.Default.Description to fileColors.fileGeneric
    }

    var isFolderExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .let { m ->
                    when {
                        isFolder -> m.bouncyClickable { isFolderExpanded = !isFolderExpanded }
                        isSingleFile -> m.bouncyClickable { onPreviewClick(module.files.first()) }
                        hasLinkPreview -> m.bouncyClickable { onPreviewClick(module.toLinkPreview()) }
                        else -> m
                    }
                }
                .padding(
                    start = WeBeepSpacing.md,
                    end = WeBeepSpacing.xxs,
                    top = WeBeepSpacing.xxs,
                    bottom = WeBeepSpacing.xxs
                )
        ) {
            FileTypeIcon(icon = moduleIcon, tint = moduleColor)
            Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
            RowLabels(
                title = module.name,
                meta = if (isSingleFile) module.files.first().formattedSize else null,
                modifier = Modifier.weight(1f)
            )
            ModuleTrailing(
                module = module,
                isFolder = isFolder,
                isFolderExpanded = isFolderExpanded,
                isSingleFile = isSingleFile,
                hasLinkPreview = hasLinkPreview,
                onDownloadClick = onDownloadClick,
                onOpenClick = onOpenClick
            )
        }

        if (!module.description.isNullOrBlank()) {
            val cleanDescription = remember(module.description) { MoodleText.cleanHtml(module.description) }
            if (cleanDescription.isNotBlank()) {
                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = RowTextInset, end = WeBeepSpacing.md, bottom = WeBeepSpacing.xs)
                )
            }
        }

        AnimatedVisibility(
            visible = (isFolder && isFolderExpanded) || isMultiFile,
            enter = expandVertically(animationSpec = WeBeepMotion.enter(reduceMotion = reduceMotion)) +
                fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
            exit = shrinkVertically(animationSpec = WeBeepMotion.exit(reduceMotion = reduceMotion)) +
                fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
        ) {
            val rootNode = remember(module.files) { buildFolderTree(module.files) }
            val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }
            Column(modifier = Modifier.fillMaxWidth()) {
                FolderTreeView(
                    node = rootNode,
                    expandedFolders = expandedFolders,
                    depth = 1,
                    onPreviewClick = onPreviewClick,
                    onDownloadClick = onDownloadClick,
                    onOpenClick = onOpenClick
                )
            }
        }
    }
}

@Composable
private fun ModuleTrailing(
    module: ModuleUiModel,
    isFolder: Boolean,
    isFolderExpanded: Boolean,
    isSingleFile: Boolean,
    hasLinkPreview: Boolean,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit
) {
    when {
        isFolder -> ExpandChevron(
            expanded = isFolderExpanded,
            contentDescription = stringResource(
                if (isFolderExpanded) R.string.cd_collapse_folder else R.string.cd_expand_folder
            )
        )
        isSingleFile -> {
            val file = module.files.first()
            FileActionButton(
                state = file.downloadState,
                onDownloadClick = { onDownloadClick(file) },
                onOpenClick = { onOpenClick(file) }
            )
        }
        hasLinkPreview -> Box(
            modifier = Modifier.size(WeBeepSpacing.touchTarget),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = stringResource(R.string.cd_preview),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(RowIconSize)
            )
        }
    }
}

private fun ModuleUiModel.toLinkPreview(): FileUiModel = FileUiModel(
    filename = name,
    filepath = "/",
    localFileName = "",
    fileUrl = url.orEmpty(),
    fileSize = 0L,
    mimeType = "text/html",
    formattedSize = ""
)
