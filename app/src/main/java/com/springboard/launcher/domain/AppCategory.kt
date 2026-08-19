package com.springboard.launcher.domain

/**
 * Heuristic auto-categorization for the App Library, based on label + package keywords.
 * Unknown apps fall into "Other".
 */
object AppCategory {

    private val keywordMap = listOf(
        Category("Social", setOf("instagram", "facebook", "whatsapp", "telegram", "messenger", "snapchat", "tiktok", "twitter", "tweet", "linkedin", "discord", "reddit", "threads", "pinterest", "xhamster", "viber", "imo", "snap", "clubhouse", "be real", "bereal")),
        Category("Entertainment", setOf("youtube", "netflix", "spotify", "music", "video", "prime video", "disney", "hulu", "hbo", "max", "audio", "podcast", "twitch", "plex", "apple tv", "vimeo", "soundcloud", "deezer", "tidal", "cast")),
        Category("Productivity", setOf("docs", "drive", "sheets", "slides", "word", "excel", "powerpoint", "notion", "gmail", "mail", "calendar", "notes", "files", "file", "calculator", "office", "slack", "zoom", "teams", "trello", "asana", "github", "outlook", "one note", "keep", "tasks", "todo")),
        Category("Games", setOf("game", "games", "angry", "candy", "clash", "roblox", "minecraft", "subway", "temple", "gacha", "cod", "fifa", "nba", "nfl", "pubg", "fortnite", "among", "mario", "pokemon", "solitaire", "chess", "puzzle", "word")),
        Category("Finance", setOf("bank", "banking", "paypal", "wallet", "finance", "cash", "money", "venmo", "zelle", "gpay", "google pay", "paytm", "upi", "credit", "stock", "crypto", "bitcoin", "coinbase", "revolut", "wise", "credit kar")),
        Category("Shopping", setOf("amazon", "shop", "shopping", "alibaba", "ebay", "walmart", "flipkart", "aliexpress", "etsy", "target", "best buy", "myntra", "shein", "temu", "zara")),
        Category("Travel", setOf("maps", "map", "uber", "lyft", "airbnb", "ola", "hotel", "flight", "oyo", "makemytrip", "goibibo", "gas", "navigation", "waze", "booking", "trivago", "expedia")),
        Category("Health & Fitness", setOf("health", "fit", "fitness", "workout", "sleep", "yoga", "med", "hospital", "myfitness", "steps", "runner", "gym", "meditation", "heart", "diet")),
        Category("Communication", setOf("phone", "dialer", "contact", "message", "sms", "duo", "imo", "calls", "recorder", "signal")),
        Category("Photo & Video", setOf("camera", "photo", "gallery", "photos", "video", "record", "lightroom", "vsco", "instagram stories", "cutter", "editor")),
        Category("Utilities", setOf("settings", "setings", "file", "explorer", "clock", "weather", "flashlight", "scanner", "launcher", "widget", "vpn", "browser", "chrome", "firefox", "brave", "edge", "safari", "download", "cleaning", "cleaner", "battery", "wifi")),
        Category("System", setOf("com.android.settings", "com.android.systemui", "systemui", "settings")),
    )

    private val DEFAULT = "Other"

    fun categoryFor(label: String, packageName: String): String {
        val hay = "$label $packageName".lowercase()
        return keywordMap.firstOrNull { cat -> cat.keywords.any { kw -> hay.contains(kw) } }?.name
            ?: DEFAULT
    }

    fun sortedCategoryNames(appLabels: List<Pair<String, String>>): List<String> {
        val used = appLabels.map { categoryFor(it.first, it.second) }.toSet()
        val ordered = keywordMap.map { it.name }.filter { it in used }
        return if (DEFAULT in used) ordered + DEFAULT else ordered
    }

    data class Category(val name: String, val keywords: Set<String>)
}