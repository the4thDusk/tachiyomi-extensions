package eu.kanade.tachiyomi.extension.en.mangahz

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class MangaHZ : Madara("MangaHZ", "https://www.mangahz.com", "en", dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US))
