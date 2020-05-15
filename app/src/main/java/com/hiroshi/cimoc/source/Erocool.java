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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class Erocool extends MangaParser{

    public static final int TYPE = 63;
    public static final String DEFAULT_TITLE = "Erocool";
    private String _uri = "";

    public Erocool(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = StringUtils.format("https://ja.erocool.net/search/q_%s/page/%d", keyword, page);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".list-wrapper > a")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href().replaceAll("/",",");
                String title = node.text(".caption");
                if (node.attr("data-tags").contains("29963")) title = "[中]"+title;
                else if (node.attr("data-tags").contains("6346")) title = "[日]"+title;
                else title = "[英]"+title;
                String cover = node.attr(".list-content","style");
                cover = StringUtils.match("background-image:url\\((.*?)\\);",cover,1);
                String update = "";
                String author = null;
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        cid = cid.replaceAll(",","/");
        String url = "https://ja.erocool.net".concat(cid);
        _uri = url;
        return new Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36").build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html).getChild("#comicdetail");
        String title = body.text("h1");
        String cover = body.src("img");
        String update = body.text(".listdetail_box > div:contains(投稿日) .ld_body");
        String author = null;
        String intro = body.text("h1");
        comic.setInfo(title, cover, update, intro, author, true);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        String title = "全1话";
        String path = "";
        list.add(new Chapter(title, path));
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = _uri;
        return new Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36").build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        Node body = new Node(html);
        int i =0;
        for (Node node:body.list("img.vimg.lazyload")) {
            i=i+1;
            list.add(new ImageUrl(i, node.attr("data-src"), false));
        }
        return list;
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list(".list-wrapper > a")) {
            String cid = node.href().replaceAll("/",",");
            String title = node.text(".caption");
            if (node.attr("data-tags").contains("29963")) title = "[中]"+title;
            else if (node.attr("data-tags").contains("6346")) title = "[日]"+title;
            else title = "[英]"+title;
            String cover = node.attr(".list-content","style");
            cover = StringUtils.match("background-image:url\\((.*?)\\);",cover,1);
            String author = null;
            list.add(new Comic(TYPE, cid, title, cover, null, author));
        }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://ja.erocool.net%spage/%%d",
                    args[CATEGORY_SUBJECT]);
        }
        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("日本語", "/language/japanese/"));
            list.add(Pair.create("中国語", "/language/chinese/"));
            list.add(Pair.create("新着", "/latest/popular/"));
            list.add(Pair.create("人気-本日", "/rank/day/"));
            list.add(Pair.create("人気-週間", "/rank/week/"));
            list.add(Pair.create("人気-月間", "/rank/month/"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return null;
    }

}

