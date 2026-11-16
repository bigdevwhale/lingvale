package app.lingvale.ui.art

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.lingvale.BuildConfig
import app.lingvale.data.ArtStore
import app.lingvale.data.House
import app.lingvale.data.Status
import app.lingvale.domain.Chat
import app.lingvale.domain.Game
import app.lingvale.domain.hourFrac
import app.lingvale.domain.Style
import app.lingvale.domain.houseArt
import app.lingvale.Styling
import app.lingvale.container
import androidx.compose.runtime.collectAsState
import app.lingvale.domain.today
import app.lingvale.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pictures of the village itself, from res/drawable-nodpi, decoded once. */
object Art {
    private val cache = HashMap<String, ImageBitmap?>()
    @SuppressLint("DiscouragedApi")
    @Synchronized fun get(ctx: Context, name: String): ImageBitmap? = cache.getOrPut(name) {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) null else BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false })?.asImageBitmap()
    }
}

/** The map is the valley picture: 2 wide by 3 tall. */
const val MAP_RATIO = 2f / 3f

/**
 * The clearings for a village of n houses (the build plot counts as one), as fractions of the map: centre x, the line
 * the house stands on. Each count has its own layout spread over the whole valley, around the path and the stream,
 * so a small village doesn't huddle at the top.
 */
private val LAYOUTS = listOf(
    listOf(.72f to .40f),
    listOf(.78f to .27f, .26f to .80f),
    listOf(.27f to .25f, .78f to .60f, .20f to .90f),
    listOf(.28f to .24f, .74f to .40f, .21f to .67f, .74f to .92f),
    listOf(.28f to .23f, .74f to .37f, .21f to .66f, .78f to .79f, .21f to .94f),
    listOf(.29f to .22f, .75f to .30f, .18f to .45f, .74f to .64f, .30f to .79f, .72f to .93f),
    listOf(.29f to .22f, .75f to .32f, .18f to .45f, .75f to .63f, .22f to .71f, .77f to .83f, .21f to .94f),
    listOf(.20f to .22f, .79f to .25f, .20f to .435f, .80f to .445f, .20f to .68f, .79f to .68f, .21f to .905f, .79f to .89f),
    listOf(.20f to .22f, .79f to .25f, .20f to .435f, .80f to .445f, .20f to .68f, .79f to .68f, .21f to .905f, .79f to .89f, .50f to .995f),
)
/** Fewer houses stand a little bigger. */
private val SCALES = floatArrayOf(1.25f, 1.2f, 1.15f, 1.1f, 1.05f)
fun spots(n: Int) = LAYOUTS[n.coerceIn(1, LAYOUTS.size) - 1]
fun houseScale(n: Int) = SCALES.getOrElse(n.coerceAtLeast(1) - 1) { 1f }
const val HOUSE_W = .30f

/** Where each decoration likes to stand (x, foot line), along the path and the stream. */
val DECOR_SPOTS = mapOf(
    "board" to (.61f to .11f), "flowers" to (.35f to .15f), "bench" to (.61f to .35f), "lamp" to (.62f to .53f),
    "well" to (.36f to .60f), "postbox" to (.41f to .80f), "signpost" to (.70f to .99f), "appletree" to (.07f to .56f),
    "campfire" to (.62f to .75f), "beehives" to (.93f to .58f), "haycart" to (.08f to .32f), "fountain" to (.40f to .29f),
    "gazebo" to (.92f to .15f), "tent" to (.12f to .995f), "windmill" to (.85f to .10f), "balloon" to (.15f to .08f),
)
/** Spare places on the grass for a decoration whose own spot a house took. */
private val DECOR_SPARE = listOf(
    .30f to .30f, .58f to .27f, .60f to .42f, .30f to .38f, .33f to .66f, .53f to .65f, .40f to .72f, .33f to .75f,
    .62f to .86f, .30f to .86f, .60f to .95f, .07f to .75f, .93f to .72f, .08f to .45f, .93f to .36f, .64f to .13f,
    .36f to .18f, .93f to .90f, .07f to .90f, .62f to .62f,
)
private val DECOR_W = mapOf("windmill" to .17f, "balloon" to .15f, "gazebo" to .15f, "tent" to .14f, "appletree" to .14f, "fountain" to .11f, "haycart" to .12f, "lamp" to .05f, "postbox" to .06f, "signpost" to .08f, "well" to .09f, "bench" to .09f, "board" to .09f, "flowers" to .1f, "campfire" to .08f, "beehives" to .09f)

/**
 * Where the [owned] decorations stand: each on its own spot unless a house of [spots] (scaled by [scale]) is there,
 * else on the nearest free place. Shop order decides who gets a contested place first.
 */
fun decorPlaces(owned: Set<String>, spots: List<Pair<Float, Float>>, scale: Float): Map<String, Pair<Float, Float>> {
    val taken = mutableListOf<Triple<Float, Float, Float>>()
    val out = LinkedHashMap<String, Pair<Float, Float>>()
    val hw = HOUSE_W * scale / 2
    fun clear(x: Float, y: Float, w: Float): Boolean {
        val top = y - w * .9f   // a decoration is roughly as tall as it is wide
        if (spots.any { (hx, hy) -> abs(x - hx) < hw + w / 2 && top < hy + .07f && y > hy - .17f * scale }) return false
        return taken.none { (tx, ty, tw) -> abs(x - tx) < (w + tw) / 2 && abs(y - ty) < .06f }
    }
    val pool = DECOR_SPOTS.values + DECOR_SPARE
    for (d in Game.decor) {
        if (d.id !in owned) continue
        val pref = DECOR_SPOTS[d.id] ?: continue
        val w = DECOR_W[d.id] ?: .1f
        val at = if (clear(pref.first, pref.second, w)) pref
            else pool.filter { clear(it.first, it.second, w) }.minByOrNull { (it.first - pref.first).let { a -> a * a } + (it.second - pref.second).let { b -> b * b } } ?: continue
        taken += Triple(at.first, at.second, w); out[d.id] = at
    }
    return out
}

/** Houses take the clearings from the top: the ones that live here first, then the ones moving in, then empty plots. */
fun layout(houses: List<House>): List<House> = houses.sortedBy { when (it.status) { Status.LIVE -> 0; Status.OLD, Status.LOCKED -> 1; Status.MISSING -> 2 } }

/** The light through the day, by the phone's clock: an hour and the colour the picture is multiplied by then. */
private val LIGHT = listOf(
    0f to Color(0xFF5A6496), 4.5f to Color(0xFF5A6496), 6f to Color(0xFFFFD2B0), 7.5f to Color(0xFFFFEEDD), 9f to Color.White,
    17f to Color.White, 18.5f to Color(0xFFFFDDB8), 20f to Color(0xFFFFB98E), 21f to Color(0xFF8A90C0), 22.5f to Color(0xFF5A6496), 24f to Color(0xFF5A6496),
)

/** What the time of day [h] (with minutes as a fraction) does to the picture, shading smoothly from one hour to the next. */
private fun tint(h: Float): Color? {
    val k = LIGHT.indexOfLast { it.first <= h }.coerceIn(0, LIGHT.size - 2)
    val (h0, c0) = LIGHT[k]; val (h1, c1) = LIGHT[k + 1]
    val c = androidx.compose.ui.graphics.lerp(c0, c1, ((h - h0) / (h1 - h0)).coerceIn(0f, 1f))
    return if (c == Color.White) null else c
}

/** The picture of this house in the design chosen in the shop: by day (0) or night (1). */
@Composable
fun House.art(n: Int = 0): String {
    val st by LocalContext.current.container.prefs.style.collectAsState()
    return houseArt(home.look, st.skins[home.id] ?: 0, n)
}

private fun House.lit() = live && snap!!.days.any { it.d > today() - 7 }

@Composable
fun rememberTime(): State<Float> = produceState(4f) {
    if (BuildConfig.SHOTS) return@produceState
    val start = withFrameMillis { it }
    while (true) withFrameMillis { value = 4f + (it - start) / 1000f }
}

private fun DrawScope.img(b: ImageBitmap, cx: Float, foot: Float, w: Float, filter: ColorFilter? = null, flip: Boolean = false) {
    val h = w * b.height / b.width
    val dst = IntOffset((cx - w / 2).roundToInt(), (foot - h).roundToInt())
    val sz = IntSize(w.roundToInt(), h.roundToInt())
    if (flip) withTransform({ scale(-1f, 1f, Offset(cx, foot)) }) { drawImage(b, IntOffset.Zero, IntSize(b.width, b.height), dst, sz, colorFilter = filter, filterQuality = FilterQuality.Low) }
    else drawImage(b, IntOffset.Zero, IntSize(b.width, b.height), dst, sz, colorFilter = filter, filterQuality = FilterQuality.Low)
}


/**
 * The village map. [owned] decorations stand where they belong (out of the houses' way); [fair] hangs flags; [chats]
 * float over the houses one at a time. Tapping a house calls [onHouse].
 */
@Composable
fun VillageMap(houses: List<House>, art: ArtStore, owned: Set<String>, fair: Boolean, chats: List<Chat>, onHouse: (House) -> Unit, modifier: Modifier = Modifier, onBuild: (() -> Unit)? = null, style: Styling = Styling()) {
    val ctx = LocalContext.current
    // The time of day follows the phone's clock, checked every half minute.
    val clock by produceState(hourFrac()) { if (!BuildConfig.SHOTS) while (true) { delay(30_000); value = hourFrac() } }
    val hour = clock.toInt()
    val night = hour >= 21 || hour < 5
    val sleep = hour >= 23 || hour < 6
    val dimFilter = remember(clock) { tint(clock)?.let { ColorFilter.lighting(it, Color.Black) } }
    val placed = houses.take(LAYOUTS.size)
    val free = if (onBuild != null && placed.size < LAYOUTS.size) placed.size else -1
    val count = placed.size + if (free >= 0) 1 else 0
    val spots = spots(count)
    val sc = houseScale(count)
    val houseW = HOUSE_W * sc
    val decor = remember(owned, spots, sc) { decorPlaces(owned, spots, sc) }
    val time by rememberTime()
    val campfire = "campfire" in owned
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val plaqueStyle = T.tiny.copy(color = Color(0xFF3A2E22))
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(MAP_RATIO)) {
        Canvas(Modifier.fillMaxSize().clipToBounds().pointerInput(placed, spots) {
            detectTapGestures { o ->
                val fx = o.x / size.width; val fy = o.y / size.height
                if (free >= 0) { val (x, y) = spots[free]; if (abs(fx - x) < houseW / 2 && fy in (y - .12f * sc)..(y + .03f)) { onBuild?.invoke(); return@detectTapGestures } }
                placed.forEachIndexed { i, h ->
                    val (x, y) = spots[i]
                    if (abs(fx - x) < houseW / 2 && fy in (y - .16f * sc)..(y + .03f)) { onHouse(h); return@detectTapGestures }
                }
            }
        }) {
            val w0 = size.width; val h0 = size.height
            Art.get(ctx, Style.valley(style.bg))?.let { drawImage(it, IntOffset.Zero, IntSize(it.width, it.height), IntOffset.Zero, IntSize(w0.roundToInt(), h0.roundToInt()), colorFilter = dimFilter, filterQuality = FilterQuality.Low) }
            val t = time
            Style.paths.firstOrNull { it.id == style.path && it.art.isNotEmpty() }?.let { p ->
                Art.get(ctx, p.art)?.let { drawImage(it, IntOffset.Zero, IntSize(it.width, it.height), IntOffset.Zero, IntSize(w0.roundToInt(), h0.roundToInt()), colorFilter = dimFilter, filterQuality = FilterQuality.Low) }
            }

            // Everything that stands, drawn from the back (top) to the front (bottom).
            class Thing(val foot: Float, val draw: DrawScope.() -> Unit)
            val things = mutableListOf<Thing>()
            val roofs = HashMap<Int, Float>()
            placed.forEachIndexed { i, h ->
                val (x, y) = spots[i]
                val lit = h.lit()
                val name = when {
                    h.live -> houseArt(h.home.look, style.skins[h.home.id] ?: 0, if (night && lit) 1 else 0)
                    h.status == Status.MISSING -> "plot_0"
                    else -> "plot_1"
                }
                val b = Art.get(ctx, name) ?: Art.get(ctx, houseArt(h.home.look, 0, 0)) ?: return@forEachIndexed
                // tall, narrow buildings (a tower, a spire) stand as high as the others, not wider
                val w = ((if (!h.live) houseW * .8f else houseW * minOf(1f, 1.05f * b.width / b.height)) * w0)
                    // a tall house near the top edge (a weathervane, a spire) shrinks to stay inside the map
                    .coerceAtMost((y - .015f) * h0 * b.width / b.height)
                roofs[i] = y * h0 - w * b.height / b.width
                things += Thing(y) {
                    img(b, x * w0, y * h0, w, if (night && lit) null else dimFilter)
                    if (night && lit) drawCircle(Brush.radialGradient(listOf(Color(0x44FFC870), Color.Transparent), Offset(x * w0, (y - .05f) * h0), w * .6f), w * .6f, Offset(x * w0, (y - .05f) * h0))
                }
            }
            for ((id, at) in decor) {
                val (x, y) = at
                val b = Art.get(ctx, "decor_$id") ?: continue
                val w = (DECOR_W[id] ?: .1f) * w0
                things += Thing(y) {
                    img(b, x * w0, y * h0, w, dimFilter)
                    if (night && (id == "lamp" || id == "campfire")) {
                        val r = w * 1.3f * (.85f + .15f * sin(t * 9f + x * 10))
                        drawCircle(Brush.radialGradient(listOf(Color(0x77FFC060), Color.Transparent), Offset(x * w0, (y - .03f) * h0), r), r, Offset(x * w0, (y - .03f) * h0))
                    }
                }
            }
            // The residents: each team walks on the grass in front of its house; after dark by the campfire, if there is one.
            placed.forEachIndexed { i, h ->
                if (!h.live || h.pkg == null) return@forEachIndexed
                val (x, y) = spots[i]
                h.snap!!.walkers.take(3).forEachIndexed { k, p ->
                    val seed = (p.id * 31 + i * 7 + k).toInt()
                    val ph = (seed % 11) / 11f * 6.28f
                    val sp = .25f + (seed % 5) * .05f
                    val atFire = campfire && night && !sleep
                    val edge = y > .94f   // the house at the bottom edge: its residents stand beside it, not below
                    val baseX = if (atFire) .50f + (i * 3 + k - 4) * .03f else if (edge) x + (if (k % 2 == 0) -1 else 1) * (.2f + (k / 2) * .07f) else x + (k - 1) * .075f
                    val footY = if (atFire) .63f + ((i + k) % 2) * .02f else if (edge) y - .01f else y + .035f + (k % 2) * .018f
                    val amp = if (sleep || atFire) 0f else .04f + (seed % 3) * .012f
                    val px = baseX + amp * sin(t * sp + ph)
                    val goingLeft = amp > 0 && cos(t * sp + ph) < 0
                    val frame = when { sleep && p.stage >= 3 -> 5; p.stage <= 1 -> 0; p.stage == 2 -> 1; else -> 2 }
                    val b = art.get(h.pkg, "pet_${p.species}_$frame") ?: return@forEachIndexed
                    val ph2 = when (p.stage) { 1 -> .042f; 2 -> .052f; else -> .062f } * h0
                    val pw = ph2 * b.width / b.height
                    val bob = if (amp > 0) abs(sin(t * 5f + ph)) * h0 * .003f else 0f
                    things += Thing(footY) {
                        drawOval(Color(0x33000000), Offset(px * w0 - pw * .3f, footY * h0 - h0 * .004f), Size(pw * .6f, h0 * .008f))
                        img(b, px * w0, footY * h0 - bob, pw, if (atFire) null else dimFilter, flip = goingLeft)
                    }
                }
            }
            if (free >= 0) Art.get(ctx, "plot_0")?.let { b ->
                val (x, y) = spots[free]
                val w = houseW * .75f * w0
                things += Thing(y) { img(b, x * w0, y * h0, w, dimFilter) }
                roofs[-1] = y * h0 - w * b.height / b.width
            }
            things.sortedBy { it.foot }.forEach { it.draw(this) }
            clipRect { weather(style.weather, t, night) }
            if (free >= 0) roofs[-1]?.let { roof ->
                val (x, _) = spots[free]
                val label = measurer.measure(if (app.lingvale.domain.Lang.ru) "+ Построить" else "+ Build", plaqueStyle.copy(color = Color(0xFFFFF1D6)), maxLines = 1)
                val pw = label.size.width + 14.dp.toPx(); val phh = label.size.height + 4.dp.toPx()
                val top = roof + phh * .2f; val left = (x * w0 - pw / 2).coerceIn(2f, w0 - pw - 2f)
                drawRoundRect(Color(0xFF6B4423), Offset(left, top), Size(pw, phh), androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()))
                drawText(label, topLeft = Offset(left + 7.dp.toPx(), top + 2.dp.toPx()))
            }
            // Name plaques on the roofs, above everything: residents walk below the houses, so nobody hides behind a name.
            placed.forEachIndexed { i, h ->
                if (h.status == Status.MISSING) return@forEachIndexed
                val (x, y) = spots[i]
                val roof = roofs[i] ?: return@forEachIndexed
                val label = measurer.measure(h.home.house, plaqueStyle, maxLines = 1)
                val pw = label.size.width + 12.dp.toPx(); val phh = label.size.height + 2.dp.toPx()
                val top = maxOf(roof, (y - .15f * sc) * h0) + phh * .35f
                val left = (x * w0 - pw / 2).coerceIn(2f, w0 - pw - 2f)
                drawRoundRect(Color(0xE6FFF6E2), Offset(left, top), Size(pw, phh), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                drawText(label, topLeft = Offset(left + 6.dp.toPx(), top + 1.dp.toPx()))
            }

            // A fair: strings of flags across the valley.
            if (fair) Art.get(ctx, "icon_flags")?.let { f ->
                for (row in listOf(.07f)) for (k in 0 until 5) {
                    val sway = sin(t * 1.3f + k + row * 9) * h0 * .002f
                    img(f, w0 * (.1f + k * .2f), h0 * row + sway, w0 * .2f, dimFilter)
                }
            }
            // Fireflies after dark.
            if (night) for (k in 0 until 22) {
                val fx = (k * 0.137f + .05f * sin(t * .4f + k) + 1f) % 1f
                val fy = .12f + ((k * 0.311f) % .82f) + .01f * cos(t * .7f + k * 2)
                val a = (.5f + .5f * sin(t * 2.2f + k * 1.7f)).coerceIn(0f, 1f)
                drawCircle(Color(1f, .9f, .5f, a * .35f), w0 * .012f, Offset(fx * w0, fy * h0))
                drawCircle(Color(1f, .95f, .6f, a), w0 * .004f, Offset(fx * w0, fy * h0))
            }
        }
        // A resident speaks: one line at a time over its house.
        if (chats.isNotEmpty()) {
            var n by remember(chats) { mutableIntStateOf(0) }
            LaunchedEffect(chats) { if (!BuildConfig.SHOTS) while (true) { delay(5200); n++ } }
            val c = chats[n % chats.size]
            val i = placed.indexOfFirst { it.home.id == c.house }
            if (i >= 0) {
                val (x, y) = spots[i]
                AnimatedContent(c, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "chat",
                    modifier = Modifier.offset(x = maxWidth * (x - .26f).coerceIn(.02f, .46f), y = (maxHeight * (y - .22f * sc)).coerceAtLeast(6.dp))) { cc ->
                    Text(cc.text, style = T.smallB.copy(color = Color(0xFF2B2A25)), textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = maxWidth * .52f).clip(RoundedCornerShape(12.dp)).background(Color(0xF2FFFBEF)).padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}

/** What falls from the sky (or flutters over the meadow), bought in the shop. */
private fun DrawScope.weather(kind: String, t: Float, night: Boolean) {
    val w0 = size.width; val h0 = size.height
    fun fall(k: Int, speed: Float) = ((k * .0713f + t * speed) % 1.08f) - .04f
    when (kind) {
        "snow" -> for (k in 0 until 70) {
            val x = (k * .137f + .015f * sin(t * .6f + k)) % 1f
            drawCircle(Color(1f, 1f, 1f, .88f), w0 * (.0035f + .0018f * (k % 3)), Offset(x * w0, fall(k, .025f + .008f * (k % 3)) * h0))
        }
        "leaves", "petals" -> {
            val colors = if (kind == "leaves") listOf(Color(0xFFD9822B), Color(0xFFB8452C), Color(0xFFE3B341), Color(0xFF9C5B2E))
                else listOf(Color(0xFFF7C6D9), Color(0xFFF2A7C3), Color(0xFFFFE1EC))
            val n = if (kind == "leaves") 22 else 34
            for (k in 0 until n) {
                val x = (k * .173f + .05f * sin(t * .5f + k * 1.3f) + t * .006f) % 1f
                val y = fall(k, if (kind == "leaves") .03f else .02f)
                val sz = w0 * (if (kind == "leaves") .016f else .011f)
                withTransform({ rotate((t * 60f + k * 47f) % 360f, Offset(x * w0, y * h0)); scale(1f, .45f + .4f * abs(sin(t * 2f + k)), Offset(x * w0, y * h0)) }) {
                    drawOval(colors[k % colors.size], Offset(x * w0 - sz / 2, y * h0 - sz / 4), Size(sz, sz / 2))
                }
            }
        }
        "butterflies" -> if (!night) for (k in 0 until 7) {
            val cx = (.5f + .42f * sin(t * (.07f + k * .011f) + k * 2.1f)) * w0
            val cy = (.15f + (k * .13f) % .8f + .03f * sin(t * .5f + k)) * h0
            val flap = .25f + .75f * abs(sin(t * 11f + k))
            val wing = w0 * .012f
            val c = listOf(Color(0xFFF4D03F), Color(0xFFFFFFFF), Color(0xFF8EC9F0), Color(0xFFF5A3C7))[k % 4]
            drawOval(c, Offset(cx - wing * flap, cy - wing / 2), Size(wing * flap, wing))
            drawOval(c, Offset(cx, cy - wing / 2), Size(wing * flap, wing))
            drawOval(Color(0xFF3B2A1E), Offset(cx - wing * .12f, cy - wing * .45f), Size(wing * .24f, wing * .9f))
        }
    }
}
