package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class Tenmanga extends MangaParser{

    public static final int TYPE = 52;
    public static final String DEFAULT_TITLE = "Tenmanga";

    public Tenmanga(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
//        keyword = keyword.replace(" ","_");
        String url = "";
        if (page==1)
        url = StringUtils.format("http://my.tenmanga.com/search/es/?wd=%s", keyword);
        return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list("#list_container li")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("dt > a");
                String title = node.attr("dt > a","title");
                String cover = node.src("dt > a > img");
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder().url(cid+"?waring=1").build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".book-info > h1").replace("Manga","").trim();
        String cover = body.src("img");
        String update = body.text(".chapter-box li:eq(1) .add-time.page-hidden");
//        update = StringUtils.match("(\\w+-\\d+-\\d+)",update,1);
        String author = body.text(".book-info p:contains(Author(s)) > a");
        String intro = body.text(".book-info p:contains(Manga Summary) > span");
        boolean status = isFinish(body.text(".book-info p:contains(Status) > a"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list(".chapter-box > li:has(.chapter-name.long)")) {
            String title = node.text(".chapter-name.short");
//                title = title.substring(title.length() - 3);
//            title = StringUtils.match("Chapter (.*)|Ch\\.(.*)",title,1);
            String path = node.href(".chapter-name.short > a");
            if (title==null) {
                title = node.text(".chapter-name.long");
                path = node.href(".chapter-name.long > a");
            }
            if (title.contains("new")) {
                title = title.replace("new", "");
            }
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return new Request.Builder().url(path).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        Node node = new Node(html);
        int i =0;
        String pages = node.text("a.pic_download");
        pages = StringUtils.match("of (\\d+)",pages,1);
        int num = Integer.parseInt(pages);
        for (;i<=num;) {
            i = i + 1;
//            String src = node.attr("data-original").trim();
            //next_page = "http://www.tenmanga.com/chapter/Naruto6891/487122-2.html";
            String src = StringUtils.match("next_page = \"(.*?/\\d+-)\\d+.html\"",html,1);
            list.add(new ImageUrl(i, src+i+".html", true));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder()
//                .addHeader("Referer", url)
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url).build();
    }

    @Override
    public String parseLazy(String html, String url) {
        return new Node(html).src(".pic_box img");
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text(".chapter-box li:eq(1) .add-time.page-hidden");
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("#list_container li")) {
            String cid = node.href("dt > a");
            String title = node.attr("dt > a","title");
            String cover = node.src("dt > a > img");
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("http://www.tenmanga.com/list/%s",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "New-Update"));
            list.add(Pair.create("热门", "Hot-Book"));
            list.add(Pair.create("新上架", "New-Book"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://www.tenmanga.com");
    }


}



