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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class gfmh extends MangaParser{

    public static final int TYPE = 34;
    public static final String DEFAULT_TITLE = "古风漫画";
    private String _cid = "";

    public gfmh(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = "";
        if(page == 1)
            url = StringUtils.format("https://m.gufengmh8.com/search/?keywords=%s", keyword);
        return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".itemBox")) {
            @Override
            protected Comic parse(Node node) {
                if (node.text("div.itemTxt > p:nth-child(4) > span.date").equals("-")){
                    return null;
                }
                String cid = node.href(".itemImg > a").substring(31);
                cid = cid.substring(0,cid.length()-1);
                String title = node.attr(".itemImg > a > mip-img","alt");
                String cover = node.src(".itemImg > a > mip-img");
//                String update = node.text("div.itemTxt > p:nth-child(4) > span.date").substring(0,10);
                String author = null;
                return new Comic(TYPE, cid, title, cover, "", author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder().url("https://m.gufengmh8.com/manhua/"+cid).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String title = body.text(".view-sub.autoHeight > h1");
        String cover = body.src("#Cover mip-img");
        String update = body.text("dl:contains(更新于) > dd");
        String author = "";
        String intro = body.text("p.txtDesc.autoHeight");
        if (intro!=null){
            intro = intro.substring(3);
        }
        boolean status = false;
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list(".Drama.autoHeight li")) {
            String title = node.text("a");
//                title = title.substring(title.length() - 3);
//            title = Pattern.compile("[^0-9.]").matcher(title).replaceAll("");
            String path = node.href("a").replace(".html","-1.html");
            list.add(new Chapter(title, path));
        }
        Collections.reverse(list);
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://m.gufengmh8.com%s", path);
        _cid = url;
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        Matcher p = Pattern.compile("chapterImages = \\[(.*?)\\]").matcher(html);
        if (!p.find()) return null;
        Matcher p1 = Pattern.compile("chapterPath = \"(.*?)\"").matcher(html);
        if (!p1.find()) return null;
        Matcher p2 = Pattern.compile("pageImage = \"(.*?)\"").matcher(html);
        if (!p2.find()) return null;
        String chapterPath = p1.group(1);
        String path[] = p.group(1).replaceAll("\"", "").split(",");
        String domain = StringUtils.match("res\\.(.*?)\\.com",p2.group(1),1);
        for (int i = 1; i <= path.length; i++) {
            list.add(new ImageUrl(i, "https://res."+domain+".com/"+chapterPath + path[i-1], false));
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text("dl:contains(更新于) > dd");
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list(".list-comic")) {
            String cid = node.href(".txtA").substring(31);
            cid = cid.substring(0,cid.length()-1);
            String title = node.text(".txtA");
            String cover = node.src("mip-img");
//                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim().substring(0, 10);
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            //https://www.manhuadui.com/update/$page/
            return StringUtils.format("https://m.gufengmh8.com/%s/?page=%%d",args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("分类发布", "list/post"));
//            list.add(Pair.create("分类发布降序", "list/-post"));
            list.add(Pair.create("分类更新", "list/update"));
//            list.add(Pair.create("分类更新降序", "list/-update"));
            list.add(Pair.create("分类点击", "list/click"));
//            list.add(Pair.create("分类点击降序", "list/-click"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", _cid);
    }


}

