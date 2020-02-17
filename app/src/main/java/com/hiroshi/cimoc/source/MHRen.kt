package com.hiroshi.cimoc.source

import android.util.Log
import android.util.Pair
import com.hiroshi.cimoc.model.Chapter
import com.hiroshi.cimoc.model.Comic
import com.hiroshi.cimoc.model.ImageUrl
import com.hiroshi.cimoc.model.Source
import com.hiroshi.cimoc.parser.JsonIterator
import com.hiroshi.cimoc.parser.MangaCategory
import com.hiroshi.cimoc.parser.MangaParser
import com.hiroshi.cimoc.parser.SearchIterator
import com.hiroshi.cimoc.soup.Node
import com.hiroshi.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*

class MHRen(source: Source) : MangaParser() {

    private fun myGet(url: HttpUrl): Request {
        val now = StringUtils.getFormatTime("yyyy-MM-dd+HH:mm:ss", System.currentTimeMillis());
        val real_url = url.newBuilder()
                .setQueryParameter("gsm", "md5")
                .setQueryParameter("gft", "json")
                .setQueryParameter("gts", now)
                .setQueryParameter("gak", "android_manhuaren2")
                .setQueryParameter("gat", "")
                .setQueryParameter("gaui", "191909801")
                .setQueryParameter("gui", "191909801")
                .setQueryParameter("gut", "0")
        return Request.Builder()
                .url(real_url.setQueryParameter("gsn", generateGSNHash(real_url.build())).build())
                .addHeader("X-Yq-Yqci", "{\"le\": \"zh\"}")
                .addHeader("User-Agent", "okhttp/3.11.0")
                .addHeader("Referer", "http://www.dm5.com/dm5api/")
                .addHeader("clubReferer", "http://mangaapi.manhuaren.com/")
                .build()
    }

    private fun generateGSNHash(url: HttpUrl): String {
        var s = c + "GET"
        url.queryParameterNames().toSortedSet().forEach {
            if (it != "gsn") {
                s += it
                s += urlEncode(url.queryParameterValues(it).get(0))
            }
        }
        s += c
        return hashString("MD5", s)
    }

    private fun urlEncode(str: String): String {
        return URLEncoder.encode(str, "UTF-8")
                .replace("+", "%20")
                .replace("%7E", "~")
                .replace("*", "%2A")
    }
    private fun hashString(type: String,input: String): String {
        val HEX_CHARS = "0123456789abcdef"
        val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        try {
            val url = baseHttpUrl.newBuilder()
                    .addQueryParameter("start", (pageSize * (page - 1)).toString())
                    .addQueryParameter("limit", pageSize.toString())
                    .addQueryParameter("keywords", keyword)
                    .addPathSegments("/v1/search/getSearchManga")
                    .build()
            return myGet(url)
        } catch (e: Exception) {
            return null
        }
    }

    override fun getSearchIterator(html: String, page: Int, keyword: String): SearchIterator? {
        try {
            val re = StringUtils.match("\"result\": (\\[[\\s\\S]*\\])",html,1);
            val re2 = StringUtils.match("\"mangas\": (\\[[\\s\\S]*\\])",html,1);
            if (re!=null) {
                return object : JsonIterator(JSONArray(re)) {
                    override fun parse(`object`: JSONObject): Comic? {
                        try {
                            val cid = `object`.getString("mangaId")
                            val title = `object`.getString("mangaName")
                            val cover = `object`.getString("mangaCoverimageUrl")
                            val author = `object`.optString("mangaAuthor")
                            val update: String? = null
                            return Comic(TYPE, cid, title, cover, update, author)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                        return null
                    }
                }
            }
            else{
                return object : JsonIterator(JSONArray(re2)) {
                    override fun parse(`object`: JSONObject): Comic? {
                        try {
                            val cid = `object`.getString("mangaId")
                            val title = `object`.getString("mangaName")
                            val cover = `object`.getString("mangaCoverimageUrl")
                            val author = `object`.optString("mangaAuthor")
                            val update: String? = null
                            return Comic(TYPE, cid, title, cover, update, author)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                        return null
                    }
                }
            }
        } catch (e: JSONException) {
            return null
        }

    }

    override fun getInfoRequest(cid: String): Request {
        val url = HttpUrl.parse(baseUrl+"/v1/manga/getDetail?mangaId=$cid")!!
        return myGet(url);
    }

    override fun parseInfo(html: String, comic: Comic) {
        val obj = JSONObject(html).getJSONObject("response")
        val title = obj.getString("mangaName")
        var cover = ""
        obj.optString("mangaPicimageUrl").let {
                        if (it != "") { cover = it }
        }
        if (cover == "") {
            obj.optString("shareIcon").let {
                if (it != "") { cover = it }
            }
        }
        if (cover == "") {
            obj.optString("mangaCoverimageUrl").let {
                if (it != "") { cover = it }
            }
        }

        val arr = obj.getJSONArray("mangaAuthors")
        val tmparr = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            tmparr.add(arr.getString(i))
        }
        val author = tmparr.joinToString(", ")
        val intro = obj.getString("mangaIntro")
        val status = obj.getInt("mangaIsOver")
        var finish = false
            if (status == 1) {finish = true}
        var update = obj.getString("mangaNewestTime")
        update = StringUtils.match("(\\d+-\\d+-\\d+)",update,1)
        comic.setInfo(title, cover, update, intro, author, finish)
    }

    override fun parseChapter(html: String): List<Chapter> {
        val list = ArrayList<Chapter>()
        try {
            val obj = JSONObject(html).getJSONObject("response")
            val array = obj.getJSONArray("mangaWords")
            val array2 = obj.getJSONArray("mangaRolls")
            val array3 = obj.getJSONArray("mangaEpisode")
            for (i in 0 until array.length()) {
                val obj1 = array.getJSONObject(i)
                var title = obj1.getString("sectionName")
                var subtitle = obj1.getString("sectionTitle")
                if (subtitle!=null){
                    title = title+" "+subtitle
                }
                if(obj1.getString("isMustPay")=="1") title = "🔒"+title;
                val path = "/v1/manga/getRead?mangaSectionId=${obj1.getInt("sectionId")}"
                list.add(Chapter(title, path))
                }
            for (i in 0 until array2.length()) {
                val obj1 = array2.getJSONObject(i)
                val title = obj1.getString("sectionName")
                val path = "/v1/manga/getRead?mangaSectionId=${obj1.getInt("sectionId")}"
                list.add(Chapter(title, path))
            }
            for (i in 0 until array3.length()) {
                val obj1 = array3.getJSONObject(i)
                val title = "[番外]"+obj1.getString("sectionName")
                val path = "/v1/manga/getRead?mangaSectionId=${obj1.getInt("sectionId")}"
                list.add(Chapter(title, path))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = HttpUrl.parse(baseUrl + path)!!.newBuilder()
                .addQueryParameter("netType", "4")
                .addQueryParameter("loadreal", "1")
                .addQueryParameter("imageQuality", "2")
                .build()
        return myGet(url)
    }

    override fun parseImages(html: String): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
            try {
                val obj = JSONObject(html).getJSONObject("response")
                val host = obj.getJSONArray("hostList").getString(0)
                val arr = obj.getJSONArray("mangaSectionImages")
                val query = obj.getString("query")
                for (i in 0 until arr.length()) {
                    Log.d("md5",host+arr.getString(i)+query)
                    list.add(ImageUrl(i+1, "$host${arr.getString(i)}$query",false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        return list
    }

    override fun getCheckRequest(cid: String): Request? {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        val obj = JSONObject(html).getJSONObject("response")
        var update = obj.getString("mangaNewestTime")
        return StringUtils.match("(\\d+-\\d+-\\d+)",update,1)
    }

    override fun parseCategory(html: String, page: Int): List<Comic>? {
        val list = ArrayList<Comic>()
        val body = Node(html)
        for (node in body.list("ul.mh-list > li > div.mh-item")) {
            val title = node.text("div > h2.title > a")
            val cover = StringUtils.match("\\((.*?)\\)", node.attr("p.mh-cover", "style"), 1)
            val cid = StringUtils.match("/\\d+/(\\d+)/\\d+",cover,1)
//            cover = cover.replace("_\\d+x\\d+\\.","_1080x830.")
            val author = node.textWithSubstring("p.author", 3)
            list.add(Comic(TYPE, cid, title, cover, null, author))
        }
        return list
    }

    private class Category : MangaCategory() {

        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var path = (args[CATEGORY_SUBJECT] + " " + args[CATEGORY_AREA] + " " + args[CATEGORY_PROGRESS] + " " + args[CATEGORY_ORDER]).trim { it <= ' ' }
            path = path.replace("\\s+".toRegex(), "-")
            return StringUtils.format("http://www.dm5.com/manhua-list-%s-p%%d", path)
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String,String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("热血", "tag31"))
            list.add(Pair.create("恋爱", "tag26"))
            list.add(Pair.create("校园", "tag1"))
            list.add(Pair.create("百合", "tag3"))
            list.add(Pair.create("耽美", "tag27"))
            list.add(Pair.create("冒险", "tag2"))
            list.add(Pair.create("后宫", "tag8"))
            list.add(Pair.create("科幻", "tag25"))
            list.add(Pair.create("战争", "tag12"))
            list.add(Pair.create("悬疑", "tag17"))
            list.add(Pair.create("推理", "tag33"))
            list.add(Pair.create("搞笑", "tag37"))
            list.add(Pair.create("奇幻", "tag14"))
            list.add(Pair.create("魔法", "tag15"))
            list.add(Pair.create("恐怖", "tag29"))
            list.add(Pair.create("神鬼", "tag20"))
            list.add(Pair.create("历史", "tag4"))
            list.add(Pair.create("同人", "tag30"))
            list.add(Pair.create("运动", "tag34"))
            list.add(Pair.create("绅士", "tag36"))
            list.add(Pair.create("机战", "tag40"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>>? {
            val list = ArrayList<Pair<String,String>>()
            list.add(Pair.create("日韩", "area36"))
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("港台", "area35"))
            list.add(Pair.create("内地", "area37"))
            list.add(Pair.create("欧美", "area38"))
            return list
        }

        public override fun hasProgress(): Boolean {
            return true
        }

        public override fun getProgress(): List<Pair<String, String>>? {
            val list = ArrayList<Pair<String,String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载", "st1"))
            list.add(Pair.create("完结", "st2"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>>? {
            val list = ArrayList<Pair<String,String>>()
            list.add(Pair.create("更新", "s2"))
            list.add(Pair.create("人气", ""))
            list.add(Pair.create("新品上架", "s18"))
            return list
        }

    }

    override fun getHeader(): Headers? {
        return Headers.of("Referer", "http://www.dm5.com/dm5api/","X-Yq-Yqci","{\"le\": \"zh\"}","User-Agent","okhttp/3.11.0","clubReferer","http://mangaapi.manhuaren.com/")
    }

    companion object {

        const val TYPE = 13
        val DEFAULT_TITLE = "漫画人"

        private val baseUrl = "http://mangaapi.manhuaren.com"
        private val c = "4e0a48e1c0b54041bce9c8f0e036124d"
        private val pageSize = 20
        private val baseHttpUrl = HttpUrl.parse(baseUrl)!!

        val defaultSource: Source
            get() = Source(null, DEFAULT_TITLE, TYPE, true)
    }
}
