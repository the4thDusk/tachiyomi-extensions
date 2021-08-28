package eu.kanade.tachiyomi.extension.zh.yandan

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.ArrayList
import java.util.regex.Pattern

class Yandan : ParsedHttpSource() {
    override val lang = "zh"
    override val name = "言耽社"
    override val baseUrl = "https://yandanshe.com"
    override val supportsLatest: Boolean = false

    private var mangeid = ""

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        var _name = element.select("a").text()
        var _url = element.select("a").attr("href").replace(baseUrl, "")
        if (_name.isEmpty()) {
            _name = "1"
            _url = mangeid + "1/"
        }
        name = _name
        url = _url
    }

    override fun chapterListSelector(): String = "div.post-navigation.font-theme ul li"

    override fun imageUrlParse(document: Document): String {
        throw UnsupportedOperationException("This method should not be called!")
    }

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = popularMangaNextPageSelector()

    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)

    override fun latestUpdatesSelector(): String = popularMangaSelector()

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        mangeid = document.selectFirst("link[rel=canonical]").attr("href").replace(baseUrl, "")
        title = document.select("h1.post-title").text()
        author = document.select("div.post-meta").select("a").first().text()
        artist = author
        description = document.select("blockquote").first().text()
        thumbnail_url = document.select(".post-content").select("img").first().attr("data-src")
    }

    override fun pageListParse(document: Document): List<Page> {
        val node = document.select("div.post-content").select("p")
        val ret = ArrayList<Page>()
        for ((i, p) in node.withIndex()) {
            ret.add(Page(i, "", p.select("img").attr("data-src")))
        }
        return ret
    }

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        url = element.select(".media-content").attr("href").replace(baseUrl, "")
        title = element.select("a.list-title.h-1x").text()
        val thumbStyle = element.select(".media-content").attr("style")
        val matcher = Pattern.compile("url\\(\'([^']*)\'\\)").matcher(thumbStyle)
        matcher.find()
        thumbnail_url = matcher.group(1)
    }

    override fun popularMangaNextPageSelector(): String = "a.next.page-numbers"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/page/$page/", headers)
    }

    override fun popularMangaSelector(): String = "div.list-item"

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector(): String = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val myFilter = filters[0]
        var param = ""
        if (myFilter is MangaFilter) {
            param = myFilter.getParam()
        }
        return GET("$baseUrl$param/page/$page/", headers)
    }

    override fun searchMangaSelector(): String = popularMangaSelector()

    override fun getFilterList() = FilterList(
        MangaFilter(
            "分区",
            arrayOf(
                Pair("首頁", "/"),
                Pair("耽美·完結", "/blwj"),
                Pair("耽美·連載", "/bllz"),
                Pair("女性向·完結", "/bgwj"),
                Pair("女性向·連載", "/bglz"),
                Pair("會員專區", "/tag/會員專區"),
            )
        )
    )

    private class MangaFilter(
        displayName: String,
        val vals: Array<Pair<String, String>>,
        defaultValue: Int = 0
    ) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray(), defaultValue) {
        fun getParam(): String {
            return vals[state].second
        }
    }
}
