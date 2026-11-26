package app.furibudget.ui.appearance

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.furibudget.ui.theme.PhotoImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.R
import app.furibudget.ui.theme.AccentChoice
import app.furibudget.ui.theme.Accents
import app.furibudget.ui.theme.BackgroundKind
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import app.furibudget.ui.theme.ThemeMode
import app.furibudget.ui.theme.ThemePack
import app.furibudget.ui.theme.ThemePacks
import app.furibudget.ui.theme.ThemeSpec
import app.furibudget.ui.theme.resolveColors

@Composable
fun AppearanceScreen(spec: ThemeSpec, onChange: ((ThemeSpec) -> ThemeSpec) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge, color = Furi.colors.onSurface)
        }
        PreviewCard()
        PackSection(spec, onChange)
        AccentSection(spec, onChange)
        BrightnessSection(spec, onChange)
        BackgroundSection(spec, onChange)
        DetailsSection(spec, onChange)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Furi.colors.onSurface)
        content()
    }
}

/** What the app looks like with current choices. Sample figures, not real data. */
@Composable
private fun PreviewCard() {
    val c = Furi.colors
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val fill by animateFloatAsState(if (go) 0.57f else 0f, Furi.motion.spring(), label = "preview-bar")
    FuriCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.preview_left), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            Text("₱ 18,420.50", style = Furi.figures.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize), color = c.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("+ ₱ 32,000", style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.income)
                Text("− ₱ 13,579.50", style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.expense)
            }
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.onSurface.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(fill).height(10.dp).clip(CircleShape).background(c.accentBrush))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {}) { Text(stringResource(R.string.preview_add)) }
                Text(stringResource(R.string.preview_sample), style = MaterialTheme.typography.labelMedium, color = c.accentText)
            }
        }
    }
}

@Composable
private fun PackSection(spec: ThemeSpec, onChange: ((ThemeSpec) -> ThemeSpec) -> Unit) {
    Section(stringResource(R.string.theme_pack)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ThemePacks.all.forEach { pack ->
                PackTile(pack, spec, selected = spec.packId == pack.id) { onChange { it.copy(packId = pack.id) } }
            }
        }
    }
}

@Composable
internal fun PackTile(pack: ThemePack, spec: ThemeSpec, selected: Boolean, onClick: () -> Unit) {
    val preview = remember(pack, spec.accent, spec.mode, spec.gradientAccent) {
        resolveColors(spec.copy(packId = pack.id), pack, systemDark = true, dynamic = null)
    }
    val ring by animateFloatAsState(if (selected) 1f else 0f, Furi.motion.spring(), label = "pack-ring")
    val shape = RoundedCornerShape(pack.corner.coerceAtMost(22.dp))
    Column(
        Modifier
            .width(168.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(shape)
                .background(preview.background)
                .border(2.dp, Furi.colors.accent.copy(alpha = ring), shape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                fun blob(color: Color, x: Float, y: Float) = drawRect(
                    Brush.radialGradient(listOf(color, Color.Transparent), Offset(size.width * x, size.height * y), size.width * 0.7f),
                )
                blob(preview.meshA, 0.15f, 0.1f)
                blob(preview.meshB, 0.9f, 0.5f)
                blob(preview.meshC, 0.3f, 1f)
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(pack.corner / 2))
                    .background(preview.card)
                    .border(1.dp, preview.cardBorder, RoundedCornerShape(pack.corner / 2)),
            ) {
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 10.dp).width(54.dp).height(8.dp)
                        .clip(CircleShape).background(preview.accentBrush),
                )
            }
        }
        Text(pack.name, style = MaterialTheme.typography.titleSmall, color = Furi.colors.onSurface)
        Text(pack.tagline, style = MaterialTheme.typography.bodySmall, color = Furi.colors.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSection(spec: ThemeSpec, onChange: ((ThemeSpec) -> ThemeSpec) -> Unit) {
    val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val currentAccent = Furi.colors.accent.toArgb()
    Section(stringResource(R.string.accent)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Accents.swatches.forEach { sw ->
                val selected = (spec.accent as? AccentChoice.Swatch)?.id == sw.id
                Swatch(Color(sw.argb), sw.name, selected) { onChange { it.copy(accent = AccentChoice.Swatch(sw.id)) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dynamicOk) {
                FilterChip(
                    selected = spec.accent == AccentChoice.Dynamic,
                    onClick = { onChange { it.copy(accent = AccentChoice.Dynamic) } },
                    label = { Text(stringResource(R.string.accent_wallpaper)) },
                )
            }
            FilterChip(
                selected = spec.accent is AccentChoice.Custom,
                onClick = {
                    onChange { s ->
                        if (s.accent is AccentChoice.Custom) s else s.copy(accent = AccentChoice.Custom(currentAccent))
                    }
                },
                label = { Text(stringResource(R.string.accent_custom)) },
            )
        }
        (spec.accent as? AccentChoice.Custom)?.let { custom ->
            CustomPicker(custom.argb) { argb -> onChange { it.copy(accent = AccentChoice.Custom(argb)) } }
        }
    }
}

@Composable
internal fun Swatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, Furi.motion.spring(), label = "swatch-scale")
    val c = Furi.colors
    Box(
        Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) c.onSurface else c.cardBorder, CircleShape)
            .semantics { contentDescription = name }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = app.furibudget.core.color.onColor(color.toArgb()).let { Color(it) })
        }
    }
}

@Composable
private fun CustomPicker(argb: Int, onPick: (Int) -> Unit) {
    val hsv = remember(argb) { FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) } }
    var hex by remember(argb) { mutableStateOf("%06X".format(argb and 0xFFFFFF)) }
    fun emit(h: Float, s: Float, v: Float) = onPick(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    fun hsv(h: Float, sat: Float, v: Float) = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabeledSlider(
            stringResource(R.string.hue), hsv[0], 0f..360f,
            track = List(7) { hsv(it * 60f, 1f, 1f) },
        ) { emit(it, hsv[1].coerceAtLeast(0.15f), hsv[2].coerceAtLeast(0.3f)) }
        LabeledSlider(
            stringResource(R.string.saturation), hsv[1], 0f..1f,
            track = listOf(hsv(hsv[0], 0f, hsv[2]), hsv(hsv[0], 1f, hsv[2])),
        ) { emit(hsv[0], it, hsv[2]) }
        LabeledSlider(
            stringResource(R.string.brightness_value), hsv[2], 0f..1f,
            track = listOf(Color.Black, hsv(hsv[0], hsv[1], 1f)),
        ) { emit(hsv[0], hsv[1], it) }
        OutlinedTextField(
            value = hex,
            onValueChange = { v ->
                val clean = v.filter { it.isLetterOrDigit() }.take(6).uppercase()
                hex = clean
                if (clean.length == 6 && clean.all { it in "0123456789ABCDEF" }) onPick(0xFF000000.toInt() or clean.toInt(16))
            },
            label = { Text(stringResource(R.string.hex)) },
            prefix = { Text("#") },
            singleLine = true,
            modifier = Modifier.width(180.dp),
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    track: List<Color>,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, color = Furi.colors.onSurfaceVariant)
        FuriSlider(value, range, Modifier.weight(1f), onChange) {
            Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(Brush.horizontalGradient(track)))
        }
    }
}

/** Slider whose value lives with the caller. Material's current Slider is state-based, so mirror the value in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuriSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit,
    track: (@Composable (SliderState) -> Unit)? = null,
) {
    val state = remember { SliderState(value = value, steps = 0, trackRange = range) }
    if (state.value != value) state.value = value
    if (track != null) {
        Slider(state = state, onValueChange = onChange, modifier = modifier, track = track)
    } else {
        Slider(state = state, onValueChange = onChange, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrightnessSection(spec: ThemeSpec, onChange: ((ThemeSpec) -> ThemeSpec) -> Unit) {
    val pack = Furi.pack
    val locked = pack.dark == null || pack.light == null
    Section(stringResource(R.string.brightness)) {
        val modes = listOf(ThemeMode.SYSTEM to R.string.mode_system, ThemeMode.LIGHT to R.string.mode_light, ThemeMode.DARK to R.string.mode_dark)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = spec.mode == mode,
                    onClick = { onChange { it.copy(mode = mode) } },
                    enabled = !locked,
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(stringResource(label)) }
            }
        }
        if (locked) {
            Text(
                stringResource(R.string.pack_locked, pack.name, stringResource(if (pack.dark == null) R.string.mode_light else R.string.mode_dark).lowercase()),
                style = MaterialTheme.typography.bodySmall, color = Furi.colors.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundSection(spec: ThemeSpec, onChange: ((ThemeSpec) -> ThemeSpec) -> Unit) {
    val current = spec.background ?: Furi.pack.defaultBackground
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val photo = withContext(Dispatchers.IO) { PhotoImport.import(context, uri, spec.photoPath) }
            if (photo != null) onChange { it.copy(background = BackgroundKind.PHOTO, photoPath = photo.path, photoColors = photo.colors, photoLuma = photo.luma) }
        }
    }
    Section(stringResource(R.string.background)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                BackgroundKind.MESH to R.string.bg_mesh,
                BackgroundKind.AURORA to R.string.bg_aurora,
                BackgroundKind.PARTICLES to R.string.bg_particles,
                BackgroundKind.GRADIENT to R.string.bg_gradient,
                BackgroundKind.SOLID to R.string.bg_solid,
            ).forEach { (kind, label) ->
                FilterChip(selected = current == kind, onClick = { onChange { it.copy(background = kind) } }, label = { Text(stringResource(label)) })
            }
            FilterChip(
                selected = current == BackgroundKind.PHOTO,
                onClick = {
                    if (spec.photoPath != null) onChange { it.copy(background = BackgroundKind.PHOTO) }
                    else pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                label = { Text(stringResource(R.string.bg_photo)) },
            )
        }
        if (current == BackgroundKind.PHOTO) {
            FilledTonalButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(stringResource(R.string.choose_photo))
            }
            LabeledSlider(stringResource(R.string.photo_blur), spec.photoBlur, 0f..1f, listOf(Color.Transparent, Furi.colors.accent)) { v -> onChange { it.copy(photoBlur = v) } }
            LabeledSlider(stringResource(R.string.photo_dim), spec.photoDim, 0f..0.9f, listOf(Color.Transparent, Color.Black)) { v -> onChange { it.copy(photoDim = v) } }
            if (spec.photoColors.isNotEmpty()) {
                Text(stringResource(R.string.accent_from_photo), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    spec.photoColors.forEach { argb ->
                        Swatch(Color(argb), stringResource(R.string.accent_from_photo), (spec.accent as? AccentChoice.Photo)?.argb == argb) {
                            onChange { it.copy(accent = AccentChoice.Photo(argb)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsSection(spec: ThemeSpec, onChange: ((ThemeSpec) -> ThemeSpec) -> Unit) {
    Section(stringResource(R.string.details)) {
        SwitchRow(stringResource(R.string.gradient_accent), spec.gradientAccent) { v -> onChange { it.copy(gradientAccent = v) } }
        SwitchRow(stringResource(R.string.parallax), spec.parallax) { v -> onChange { it.copy(parallax = v) } }
        SwitchRow(stringResource(R.string.colorblind), spec.colorblindSafe) { v -> onChange { it.copy(colorblindSafe = v) } }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.animation), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Furi.colors.onSurface)
                Text("${(spec.motion * 100).toInt()}%", style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = Furi.colors.onSurfaceVariant)
            }
            FuriSlider(spec.motion, 0f..1f, Modifier.fillMaxWidth(), { v -> onChange { it.copy(motion = v) } })
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Furi.colors.onSurface)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
