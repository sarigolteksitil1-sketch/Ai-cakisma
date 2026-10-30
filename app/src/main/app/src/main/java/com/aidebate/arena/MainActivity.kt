
package com.aidebate.arena

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var geminiKey: EditText
    private lateinit var groqKey: EditText
    private lateinit var topic: EditText
    private lateinit var output: LinearLayout
    private lateinit var status: TextView
    private lateinit var startButton: Button

    @Volatile private var stopped = false
    @Volatile private var running = false

    private val history = mutableListOf<Pair<String, String>>()

    private val bg = Color.rgb(16, 19, 28)
    private val panel = Color.rgb(32, 38, 53)
    private val blue = Color.rgb(100, 165, 255)
    private val purple = Color.rgb(190, 145, 255)
    private val green = Color.rgb(100, 220, 165)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        val scroll = ScrollView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 28)
            setBackgroundColor(bg)
        }

        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("⚔️ AI DEBATE ARENA", 25, blue, true))
        root.addView(label("Gemini vs Groq • Sınırsız tartışma", 15, Color.LTGRAY))

        root.addView(label("API anahtarları", 18, Color.WHITE, true))

        geminiKey = edit("Gemini API anahtarı", true)
        groqKey = edit("Groq API anahtarı", true)

        root.addView(geminiKey)
        root.addView(groqKey)

        root.addView(label(
            "Anahtarları yalnızca güvendiğin uygulamaya gir. APK içindeki anahtarlar tamamen gizli değildir.",
            12, Color.LTGRAY
        ))

        root.addView(label("Tartışma konusu", 18, Color.WHITE, true))

        topic = edit("Örn. Yapay zekâ insanlığa faydalı mı?", false)
        topic.minLines = 3
        topic.gravity = Gravity.TOP
        root.addView(topic)

        startButton = button("▶ TARTIŞMAYI BAŞLAT", blue)
        startButton.setOnClickListener { startDebate() }
        root.addView(startButton)

        val stopButton = button("■ DURDUR", Color.rgb(145, 60, 75))
        stopButton.setOnClickListener {
            stopped = true
            status.text = "Durdurma isteği gönderildi..."
        }
        root.addView(stopButton)

        status = label("Hazır. API anahtarlarını ve konuyu gir.", 14, green)
        root.addView(status)

        root.addView(label("TARTIŞMA AKIŞI", 20, Color.WHITE, true))

        output = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(output)
    }

    private fun label(
        s: String,
        size: Int,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = s
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 9, 0, 9)
        }
    }

    private fun edit(hintText: String, password: Boolean): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(14, 8, 14, 8)
            setBackgroundColor(panel)

            if (password) {
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }
    }

    private fun button(textValue: String, color: Int): Button {
        return Button(this).apply {
            text = textValue
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
            isAllCaps = false
        }
    }

    private fun addCard(title: String, body: String, color: Int) {
        runOnUiThread {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(panel)
            }

            box.addView(label(title, 16, color, true))
            box.addView(label(body, 14, Color.WHITE))

            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 8, 0, 8)

            output.addView(box, lp)
        }
    }

    private fun setStatus(s: String) {
        runOnUiThread {
            status.text = s
        }
    }

    private fun startDebate() {

        if (running) return

        val gKey = geminiKey.text.toString().trim()
        val qKey = groqKey.text.toString().trim()
        val subject = topic.text.toString().trim()

        if (gKey.isBlank() || qKey.isBlank() || subject.isBlank()) {
            Toast.makeText(
                this,
                "İki API anahtarı ve konu gerekli.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!gKey.startsWith("AIza")) {
            Toast.makeText(
                this,
                "Gemini API anahtarını kontrol et.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        stopped = false
        running = true
        history.clear()
        output.removeAllViews()
        startButton.isEnabled = false

        executor.execute {

            var turn = 0

            try {

                while (!stopped) {

                    turn++

                    setStatus("$turn. konuşma: Gemini düşünüyor...")

                    val geminiPrompt = createPrompt(
                        subject,
                        "GEMINI",
                        "Sen tartışmanın ilk tarafısın. Konuyu değerlendir, görüşünü gerekçelendir ve Groq'un önceki cevaplarına doğrudan karşılık ver."
                    )

                    if (stopped) break

                    val a = askGemini(gKey, geminiPrompt)

                    history.add("Gemini" to a)
                    addCard("🔵 GEMINI • $turn", a, blue)

                    if (stopped) break

                    setStatus("$turn. konuşma: Groq düşünüyor...")

                    val groqPrompt = createPrompt(
                        subject,
                        "GROQ",
                        "Sen tartışmanın karşı tarafısın. Gemini'nin söylediklerini dikkatle değerlendir. Katılmadığın noktaları gerekçeleriyle eleştir ve kendi görüşünü savun."
                    )

                    val b = askGroq(qKey, groqPrompt)

                    history.add("Groq" to b)
                    addCard("🟣 GROQ • $turn", b, purple)

                }

                setStatus("Tartışma durduruldu. Toplam cevap: ${history.size}")

            } catch (e: Exception) {

                setStatus("HATA: ${e.message ?: "Bağlantı hatası"}")

            } finally {

                running = false

                runOnUiThread {
                    startButton.isEnabled = true
                }
            }
        }
    }

    private fun createPrompt(
        subject: String,
        speaker: String,
        role: String
    ): String {

        val previous = synchronized(history) {
            history.takeLast(12).joinToString("\n\n") {
                "${it.first}:\n${it.second}"
            }
        }

        return """
            Sen AI Debate Arena isimli tartışma sisteminin
            $speaker isimli katılımcısısın.

            TARTIŞMA KONUSU:
            $subject

            SENİN GÖREVİN:
            $role

            ÖNCEKİ KONUŞMALAR:
            ${if (previous.isBlank()) "Henüz konuşma yapılmadı." else previous}

            KURALLAR:
            - Türkçe konuş.
            - Diğer yapay zekânın söylediklerini dikkatle incele.
            - Sadece aynı şeyleri tekrar etme.
            - Somut gerekçeler ve örnekler sun.
            - Hatalı bulduğun argümanlara cevap ver.
            - Gerektiğinde kendi görüşünü değiştir.
            - Saygılı ve anlaşılır ol.
            - Tartışmayı doğal şekilde sürdür.
            - Cevaplarını gereksiz uzatma.
        """.trimIndent()
    }

    private fun postJson(
        urlString: String,
        apiKey: String,
        body: JSONObject,
        header: String
    ): JSONObject {

        val conn = URL(urlString).openConnection() as HttpURLConnection

        conn.requestMethod = "POST"
        conn.connectTimeout = 30000
        conn.readTimeout = 90000
        conn.doOutput = true

        conn.setRequestProperty("Content-Type", "application/json")

        conn.setRequestProperty(
            header,
            if (header == "Authorization") "Bearer $apiKey" else apiKey
        )

        OutputStreamWriter(
            conn.outputStream,
            Charsets.UTF_8
        ).use {
            it.write(body.toString())
        }

        val code = conn.responseCode

        val stream = if (code in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream
        }

        val response = stream.bufferedReader().use {
            it.readText()
        }

        conn.disconnect()

        if (code !in 200..299) {
            throw Exception("HTTP $code: ${response.take(350)}")
        }

        return JSONObject(response)
    }

    private fun askGemini(key: String, prompt: String): String {

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", prompt)
                            )
                        )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 700)
            )

        val result = postJson(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            key,
            body,
            "x-goog-api-key"
        )

        return result
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .let { arr ->
                (0 until arr.length()).joinToString("\n") {
                    arr.getJSONObject(it).optString("text")
                }
            }
    }

    private fun askGroq(key: String, prompt: String): String {

        val messages = JSONArray().put(
            JSONObject()
                .put("role", "user")
                .put("content", prompt)
        )

        val body = JSONObject()
            .put("model", "openai/gpt-oss-20b")
            .put("messages", messages)
            .put("temperature", 0.7)
            .put("max_tokens", 700)

        val result = postJson(
            "https://api.groq.com/openai/v1/chat/completions",
            key,
            body,
            "Authorization"
        )

        return result
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    override fun onDestroy() {
        stopped = true
        executor.shutdownNow()
        super.onDestroy()
    }
}
