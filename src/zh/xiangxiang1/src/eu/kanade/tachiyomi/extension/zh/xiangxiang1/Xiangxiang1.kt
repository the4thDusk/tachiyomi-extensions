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
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.lang.UnsupportedOperationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.math.abs

class Xiangxiang1 : HttpSource() {

    override val name = "香香"
    override val baseUrl = "https://boylove.house"
    override val lang = "zh"
    override val supportsLatest = true

    // console.log( '/home/api/chapter_list/tp/' + aid + "-" + order + "-" + pageNo + "-" + pageSize) ;
    override fun chapterListRequest(manga: SManga): Request {
        val id = manga.url.substring(20)
        return GET("$baseUrl/home/api/chapter_list/tp/$id-1-0-10", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
//        val document = response.asJsoup()
//        return document.select("ul.stui-play__list > li > a").map { chapterFromElement(it) }
        val result = response.body!!.string()
        val chapterList = JSONObject(result).getJSONObject("result").getJSONArray("list")
        val ret = ArrayList<SChapter>()
        for (i in 0 until chapterList.length()) {
            val obj = chapterList.getJSONObject(i)
            ret.add(
                SChapter.create().apply {
                    name = obj.optString("title")
                    val id = obj.optString("id")
                    url = "/home/book/capter/id/$id"
                    chapter_number = i.toFloat()
                    date_upload = stringToUnixTimestamp(obj.optString("create_time"))
                }
            )
        }
        return ret
    }

    private fun stringToUnixTimestamp(string: String, pattern: String = "yyyy-MM-dd HH:mm:ss", locale: Locale = Locale.CHINA): Long {
        return try {
            val time = SimpleDateFormat(pattern, locale).parse(string)?.time
            time ?: Date().time
        } catch (ex: Exception) {
            Date().time
        }
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("not used")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        return mangasFromJSONArray(response)
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
            thumbnail_url = baseUrl + des.select("a.play").first().attr("data-original")
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val pages = ArrayList<Page>()
        val images = document.select("section.reader-cartoon-chapter > div.reader-cartoon-image > img")
        val baseLength = images.first().attr("data-original").length
        for ((i, image) in images.withIndex()) {
            val url = image.attr("data-original")
            // the last few image of each chapter is blank, try to filter them
            val length = url.length
            if (abs(baseLength - length) > 4) {
                break
            }
            pages.add(Page(i, "", baseUrl + url))
        }
        return pages
    }

    override fun popularMangaParse(response: Response): MangasPage = latestUpdatesParse(response)

    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)

//    override fun searchMangaParse(response: Response): MangasPage {
//        val document = response.asJsoup()
//        val ret = ArrayList<SManga>()
//        val mangas = document.select("div.stui-vodlist__box")
//        for (manga in mangas) {
//            ret.add(
//                SManga.create().apply {
//                    title = manga.select(".stui-vodlist__detail > a").first().ownText()
//                    description = manga.select(".stui-vodlist__detail > p").first().ownText()
//                    author = manga.select(".pic-text.text-left").text()
//                    thumbnail_url = manga.select(".stui-vodlist__thumb").attr("data-original")
//                }
//            )
//        }
//        val last = document.select(".nowloadding > .reader-infinite-preloader").attr("false")
//
//        return MangasPage(
//            ret,
//            when (last) {
//                "true" -> false
//                else -> true
//            }
//        )
//    }

    override fun searchMangaParse(response: Response): MangasPage {
        return mangasFromJSONArray(response)
    }

    // https://boylove.house/home/api/searchk?key=%E6%97%A5%E6%BC%AB&type=1&pageNo=2
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/home/api/searchk?key=$query&type=1&pageNo=$page", headers)
    }

    private fun mangasFromJSONArray(response: Response): MangasPage {
        val result = response.body!!.string()
        val resultJson = JSONObject(result).getJSONObject("result")
        val mangaList = resultJson.getJSONArray("list")
        val ret = ArrayList<SManga>(mangaList.length())
        for (i in 0 until mangaList.length()) {
            val obj = mangaList.getJSONObject(i)
            val id = obj.getInt("id")
            ret.add(
                SManga.create().apply {
                    title = obj.optString("title")
                    thumbnail_url = baseUrl + obj.getString("image")
                    author = obj.optString("auther")
                    status = when (obj.getInt("mhstatus")) {
                        1 -> SManga.COMPLETED
                        0 -> SManga.ONGOING
                        else -> SManga.UNKNOWN
                    }
                    description = obj.optString("desc")
                    genre = obj.optString("keyword").replace(",", ", ")
                    url = "/home/book/index/id/$id"
                }
            )
        }
        return MangasPage(ret, !resultJson.optBoolean("lastPage", false))
    }

    private fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = element.ownText()
            url = element.attr("href")
        }
    }
}
