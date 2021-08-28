package eu.kanade.tachiyomi.extension.zh.xiangxiang1

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.lang.UnsupportedOperationException
import java.util.ArrayList

class Xiangxiang1 : HttpSource() {

    override val name = "香香"
    override val baseUrl = "https://boylove.house"
    override val lang = "zh"
    override val supportsLatest = true

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select("ul.stui-play__list > li > a").map { chapterFromElement(it) }
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("not used")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.body!!.string()
        val mangaList = JSONObject(result).getJSONObject("result").getJSONArray("list")
        return mangasFromJSONArray(mangaList)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/home/api/getpage/tp/1-newest-${page - 1}", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val des = document.select("div.stui-content__item")
        return SManga.create().apply {
            title = des.select("div.title > h1").text()
            author = des.select(".data")[1].select("a").text()
            description = des.select("span.detail-text").text()
            thumbnail_url = des.select("a.play").first().attr("data-original")
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val pages = ArrayList<Page>()
        val images = document.select("section.reader-cartoon-chapter > div.reader-cartoon-image > img")
        for ((i, image) in images.withIndex()) {
            pages.add(Page(i, "", image.attr("data-original")))
        }
        return pages
    }

    override fun popularMangaParse(response: Response): MangasPage = latestUpdatesParse(response)

    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val ret = ArrayList<SManga>()
        val mangas = document.select("div.stui-vodlist__box")
        for (manga in mangas) {
            ret.add(
                SManga.create().apply {
                    title = manga.select(".stui-vodlist__detail > a").first().ownText()
                    description = manga.select(".stui-vodlist__detail > p").first().ownText()
                    author = manga.select(".pic-text.text-left").text()
                    thumbnail_url = manga.select(".stui-vodlist__thumb").attr("data-original")
                }
            )
        }
        val last = document.select(".nowloadding > .reader-infinite-preloader").attr("false")

        return MangasPage(
            ret,
            when (last) {
                "true" -> false
                else -> true
            }
        )
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/home/index/search/?keyword=$query", headers)
    }

    private fun mangasFromJSONArray(arr: JSONArray): MangasPage {
        val ret = ArrayList<SManga>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getInt("id")
            ret.add(
                SManga.create().apply {
                    title = obj.getString("title")
                    thumbnail_url = obj.getString("cover")
                    author = obj.optString("auther")
                    status = when (obj.getInt("mhstatus")) {
                        1 -> SManga.COMPLETED
                        0 -> SManga.ONGOING
                        else -> SManga.UNKNOWN
                    }
                    genre = obj.optString("keyword").replace(",", ", ")
                    url = "/home/book/index/id/$id"
                }
            )
        }
        return MangasPage(ret, arr.length() != 0)
    }

    private fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = element.ownText()
            url = element.attr("href")
        }
    }
}
