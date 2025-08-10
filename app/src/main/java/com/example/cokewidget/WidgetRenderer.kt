package com.example.cokewidget

import android.content.Context
import android.graphics.*
import java.time.*
import java.time.format.DateTimeFormatter

object WidgetRenderer {

    private val tz: ZoneId = ZoneId.of("Europe/Berlin")

    private const val COKING_H = 18.0
    private const val WATER_H  = 1.0
    private const val DRILL_H  = 2.0
    private const val HEAT_H   = 2.0
    private const val CYCLE_H  = COKING_H + WATER_H + DRILL_H + HEAT_H // 23

    private val startA = ZonedDateTime.of(2025,8,7,20,0,0,0,tz)
    private val startC = ZonedDateTime.of(2025,8,7,12,0,0,0,tz)
    private val halfCycle = Duration.ofMinutes((CYCLE_H * 60 / 2).toLong())
    private val startB = startA.plus(halfCycle)
    private val startD = startC.plus(halfCycle)

    private val reactorsTop = listOf("Р-1","Р-2","Р-3","Р-4")
    private val reactorsBot = listOf(
        "DC-101A" to startA,
        "DC-101B" to startB,
        "DC-101C" to startC,
        "DC-101D" to startD
    )

    private val fmtH = DateTimeFormatter.ofPattern("HH:mm")

    fun render(ctx: Context, width: Int, height: Int): Bitmap {

        // --- Maintenance flags from SharedPreferences ---
        val sp = ctx.getSharedPreferences("com.example.cokewidget_preferences", 0)
        val m2110 = sp.getBoolean("maintenance_21_10", false)
        val m2120 = sp.getBoolean("maintenance_21_20", false)
        val mR = mapOf(
            "Р-1" to sp.getBoolean("maintenance_R1", false),
            "Р-2" to sp.getBoolean("maintenance_R2", false),
            "Р-3" to sp.getBoolean("maintenance_R3", false),
            "Р-4" to sp.getBoolean("maintenance_R4", false)
        )
        val mDC = mapOf(
            "DC-101A" to sp.getBoolean("maintenance_DC101A", false),
            "DC-101B" to sp.getBoolean("maintenance_DC101B", false),
            "DC-101C" to sp.getBoolean("maintenance_DC101C", false),
            "DC-101D" to sp.getBoolean("maintenance_DC101D", false)
        )

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // titles
        paint.color = Color.BLACK
        paint.textSize = 40f
        c.drawText("21-10", width*0.1f, 60f, paint)
        c.drawText("21-20", width*0.1f, height*0.5f - 20f, paint)

        val wCyl = (width / 8.5f).toInt()
        val hCyl = (height / 3.5f).toInt()
        val gap = (width - 4*wCyl) / 5
        val yTop = 80
        val yBot = (height*0.55f).toInt()

        // Top row: maintenance empty/open
        reactorsTop.forEachIndexed { i, name ->
            val x = gap + i*(wCyl+gap)
            val inRepair = m2110 || (mR[name] ?: false)
            if (inRepair) {
                drawCylinder(c, x, yTop, wCyl, hCyl, null, 0.0, name, "")
            } else {
                // show a light idle state
                drawCylinder(c, x, yTop, wCyl, hCyl, "Прогрев", 0.1, name, "")
            }
            paint.textSize = 28f
            c.drawText(name, x.toFloat(), (yTop-12).toFloat(), paint)
        }

        // Bottom row: live phases
        val now = ZonedDateTime.now(tz)
        reactorsBot.forEachIndexed { i, pair ->
            val (name, start) = pair
            val x = gap + i*(wCyl+gap)
            val inRepair = m2120 || (mDC[name] ?: false)
            if (inRepair) {
                drawCylinder(c, x, yBot, wCyl, hCyl, null, 0.0, name, "")
            } else {
                val (phase, progress) = currentPhase(start, now)
                val timeText = start.format(fmtH)
                drawCylinder(c, x, yBot, wCyl, hCyl, phase, progress, name, timeText)
            }
            paint.textSize = 28f
            c.drawText(name, x.toFloat(), (yBot-12).toFloat(), paint)
        }

        return bmp
    }

    private fun currentPhase(start: ZonedDateTime, now: ZonedDateTime): Pair<String, Double> {
        val elapsedH = java.time.Duration.between(start, now).toMinutes()/60.0
        var mod = ((elapsedH % CYCLE_H) + CYCLE_H) % CYCLE_H
        return when {
            mod < COKING_H -> "Коксование" to (mod / COKING_H)
            { mod -= COKING_H; mod < WATER_H }() -> "Вода" to (mod / WATER_H)
            { mod -= WATER_H; mod < DRILL_H }() -> "Бурение" to (mod / DRILL_H)
            else -> { mod -= DRILL_H; "Прогрев" to (mod / HEAT_H) }
        }
    }

    private fun drawCylinder(c: Canvas, x: Int, y: Int, w: Int, h: Int, phase: String?, progress: Double, name: String, timeText: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // metallic shell
        for (i in 0 until w) {
            val k = Math.abs((i - w/2f) / (w/2f))
            val shade = (190 + 45*k).toInt()
            p.color = Color.argb(220, shade, shade, shade)
            c.drawLine((x+i).toFloat(), y.toFloat(), (x+i).toFloat(), (y+h).toFloat(), p)
        }
        p.style = Paint.Style.FILL
        p.color = Color.argb(220,210,210,210)
        c.drawOval(RectF(x.toFloat(), (y-8).toFloat(), (x+w).toFloat(), (y+8).toFloat()), p)
        p.color = Color.argb(220,185,185,185)
        c.drawOval(RectF(x.toFloat(), (y+h-8).toFloat(), (x+w).toFloat(), (y+h+8).toFloat()), p)

        // inner content
        if (phase != null) drawInner(c, x, y, w, h, phase, progress)

        // slide gates
        drawGateTop(c, x, y, w, phase)
        drawGateBottom(c, x, y+h, w, phase)

        // center time text
        if (timeText.isNotEmpty()) {
            val tPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            tPaint.textSize = 36f
            tPaint.color = Color.WHITE
            tPaint.style = Paint.Style.FILL
            // outline
            val out = Paint(Paint.ANTI_ALIAS_FLAG)
            out.color = Color.BLACK
            out.textSize = 36f
            out.style = Paint.Style.STROKE
            out.strokeWidth = 4f
            val bounds = Rect()
            tPaint.getTextBounds(timeText, 0, timeText.length, bounds)
            val tx = x + (w - bounds.width())/2f
            val ty = y + (h + bounds.height())/2f
            c.drawText(timeText, tx, ty, out)
            c.drawText(timeText, tx, ty, tPaint)
        }
    }

    private fun drawInner(c: Canvas, x: Int, y: Int, w: Int, h: Int, phase: String, progress: Double) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (phase) {
            "Коксование" -> {
                val level = (progress * h).toInt()
                p.color = Color.argb(230, 0,0,0)
                c.drawRect(RectF(x.toFloat(), (y+h-level).toFloat(), (x+w).toFloat(), (y+h).toFloat()), p)
            }
            "Пропарка" -> {
                for (i in 0 until h) {
                    val r = (255 * (1 - i.toFloat()/h * 0.5f)).toInt()
                    p.color = Color.argb(180, r,0,0)
                    c.drawLine(x.toFloat(), (y+i).toFloat(), (x+w).toFloat(), (y+i).toFloat(), p)
                }
            }
            "Вода" -> {
                // background red-black
                for (i in 0 until h) {
                    val r = (255 * (1 - i.toFloat()/h * 0.5f)).toInt()
                    p.color = Color.argb(160, r,0,0)
                    c.drawLine(x.toFloat(), (y+i).toFloat(), (x+w).toFloat(), (y+i).toFloat(), p)
                }
                val water = (progress * h).toInt()
                for (i in 0 until water) {
                    val b = (255 - i.toFloat()/h * 100 - progress*80).toInt().coerceAtLeast(0)
                    p.color = Color.argb(200, 0,120,b)
                    c.drawLine(x.toFloat(), (y+h-i).toFloat(), (x+w).toFloat(), (y+h-i).toFloat(), p)
                }
            }
            "Прогрев" -> {
                val fill = (progress * h).toInt()
                for (i in 0 until fill) {
                    val r = (150 + i.toFloat()/h * 105).toInt()
                    p.color = Color.argb(180, r,0,0)
                    c.drawLine(x.toFloat(), (y+h-i).toFloat(), (x+w).toFloat(), (y+h-i).toFloat(), p)
                }
            }
            "Бурение" -> {
                val coke = ((1-progress) * h).toInt()
                p.color = Color.argb(220, 20,20,20)
                c.drawRect(RectF(x.toFloat(), (y+h-coke).toFloat(), (x+w).toFloat(), (y+h).toFloat()), p)
                // drill
                val drillLen = (progress * h).toInt()
                val dx = x + w/2 - 5
                val pMetal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(170,170,170) }
                c.drawRect(RectF(dx.toFloat(), y.toFloat(), (dx+10).toFloat(), (y+drillLen).toFloat()), pMetal)
                val bit = Path().apply {
                    moveTo(dx.toFloat(), (y+drillLen-5).toFloat())
                    lineTo((dx+10).toFloat(), (y+drillLen-5).toFloat())
                    lineTo((dx+5).toFloat(), (y+drillLen+8).toFloat())
                    close()
                }
                c.drawPath(bit, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(120,120,120) })
                // falling coke bottom (simple dots)
                val holeY = y + h + 6
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
                repeat(7) {
                    val fall = ((System.currentTimeMillis()/60 + it*7) % 50).toInt()
                    val px = x + w/2 + (-16..16).random()
                    val py = holeY + fall
                    if (py < y + h + 44) c.drawRect(RectF(px.toFloat(), py.toFloat(), (px+2).toFloat(), (py+2).toFloat()), dot)
                }
            }
        }
    }

    private fun drawGateTop(c: Canvas, x: Int, y: Int, w: Int, phase: String?) {
        val open = (phase == "Вода" || phase == "Пропарка")
        val gateH = if (open) 2 else 10
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80,80,80) }
        c.drawRect(RectF(x.toFloat(), (y-8).toFloat(), (x+w).toFloat(), (y-8+gateH).toFloat()), p)
    }
    private fun drawGateBottom(c: Canvas, x: Int, y: Int, w: Int, phase: String?) {
        val open = (phase == "Бурение")
        val gateH = if (open) 2 else 10
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80,80,80) }
        c.drawRect(RectF(x.toFloat(), (y+8).toFloat(), (x+w).toFloat(), (y+8+gateH).toFloat()), p)
    }
}
