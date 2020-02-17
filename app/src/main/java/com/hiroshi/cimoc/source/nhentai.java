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

public class nhentai extends MangaParser{

    public static final int TYPE = 61;
    public static final String DEFAULT_TITLE = "Nhentai";
    private String uri = "";

    public nhentai(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        url = StringUtils.format("https://nhentai.net/search/?q=%s&page=%d", keyword, page);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".container .gallery")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("a").replaceAll("/",",");
                String title = node.text("a .caption");
                String cover = node.attr("img","data-src");
                String update = "";
                String author = null;
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        cid = cid.replaceAll(",","/");
        String url = "https://nhentai.net".concat(cid);
        return new Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36").build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text("#info h2");
        if (title==null){
            title = body.text("#info h1");
        }
        String cover = body.attr("#cover img","data-src");
        String update = StringUtils.match("<div>Uploaded <time datetime=\"(\\d+-\\d+-\\d+).*?\">.*?</time></div>",html,1);
        String author = body.text("#tags > div:contains(Artists) > span > a");
        author = StringUtils.match("(.*)\\(\\d*\\)",author,1);
        String intro = body.text("#info h2");
        intro = intro==title?null:intro;
        comic.setInfo(title, cover, update, intro, author, true);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : new Node(html).list("#bigcontainer")) {
            i = i + 1;
            String title = "全"+i+"话";
            String path = node.href("#cover a");
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://nhentai.net%s", path);
        uri = url;
        return new Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36").build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String p = StringUtils.match("num-pages\">(\\d*)<",html,1);
        int jpg = Integer.parseInt(p);
        for (int i = 1; i <= jpg; ++i) {
            String p1 = "/"+i+"/";
            String p2 = uri.replace("/1/",p1);
            list.add(new ImageUrl(i, p2, true));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36")
                .url(url).build();
    }

    @Override
    public String parseLazy(String html, String url) {
       return StringUtils.match("https:\\/\\/i\\.nhentai\\.net\\/galleries\\/.*\\.(jpg|png)",html,0);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list(".container.index-container > .gallery")) {
            String cid = node.href("a").replaceAll("/",",");
            String title = node.text("a > .caption");
            String cover = node.attr("img","data-src");
            String author = "";
            // String update = node.text("p.zl"); 要解析好麻烦
            list.add(new Comic(TYPE, cid, title, cover, null, author));
        }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public String getFormat(String... args) {
            return StringUtils.format("%s?page=%%d",
                    args[CATEGORY_SUBJECT]);
        }
        ///?page=2
        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("汉化", "https://nhentai.net/language/chinese/"));
            list.add(Pair.create("首页", "https://nhentai.net/"));
            list.add(Pair.create("日文", "https://nhentai.net/language/japanese/"));
            list.add(Pair.create("lolicon", "https://nhentai.net/tag/lolicon/"));
            list.add(Pair.create("校服", "https://nhentai.net/tag/schoolgirl-uniform/"));
            list.add(Pair.create("rape", "https://nhentai.net/tag/rape/"));
            list.add(Pair.create("无码", "https://nhentai.net/tag/uncensored/"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://nhentai.net","User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36");
//        return Headers.of("Referer", "https://nhentai.net");
    }

}

