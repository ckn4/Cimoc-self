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

public class manganelo extends MangaParser{

    public static final int TYPE = 51;
    public static final String DEFAULT_TITLE = "Manganelo";

    private String _cid = "";

    public manganelo(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        keyword = keyword.replace(" ","_");
        String url = StringUtils.format("https://manganelo.com/search/story/%s?page=%d", keyword,page);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".panel-search-story > .search-story-item")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("a");
                if (cid.contains("manganelo"))cid = cid.substring(28);
                else cid = cid.substring(27);
                String title = node.attr("a img","alt");
                String cover = node.src("a img");
                String update = node.text("span:contains(Updated)");
                update = StringUtils.match("(\\w+-\\d+-\\d+)",update,1);
                return new Comic(TYPE, cid, title, cover, update, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        _cid = cid;
        cid = "https://manganelo.com/manga/"+cid;
        return new Request.Builder().url(cid).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String title = body.text(".story-info-right h1");
        String cover = body.src(".story-info-left img");
        String update = body.text(".story-info-right-extent");
        update = StringUtils.match("([a-zA-Z]{3} \\d+\\,\\d+)",update,1);
        String author = "";
        String intro = "";
        boolean status = isFinish(body.text(".story-info-right td:contains(Status)"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list(".row-content-chapter > li")) {
            String title = node.text("a");
//                title = title.substring(title.length() - 3);
            title = StringUtils.match("Chapter (.*)|Ch\\.(.*)",title,1);
            if (title==null){
                title = node.text("a");
            }
            int length = 30+_cid.length()+1;
            String path = node.href("a").substring(length);
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        path = "https://manganelo.com/chapter/"+_cid+"/"+path;
        return new Request.Builder().url(path).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        int i =0;
        for (Node node : new Node(html).list(".container-chapter-reader > img")) {
            i = i + 1;
//            String src = node.attr("data-original").trim();
            String src = node.src();
            list.add(new ImageUrl(i, src, false));
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        Node body = new Node(html);
        String update = body.text(".story-info-right-extent");
        return StringUtils.match("([a-zA-Z]{3} \\d+\\,\\d+)",update,1);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list(".content-genres-item")) {
            String cid = node.href("a");
            if (cid.contains("manganelo"))cid = cid.substring(28);
            else cid = cid.substring(27);
            String title = node.attr("a","title");
            String cover = node.src("a > img");
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://manganelo.com/genre-all?type=%s&category=all&state=all&page=%%d",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", ""));
            list.add(Pair.create("热门", "topview"));
            list.add(Pair.create("新上架", "newest"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://manganelo.com");
    }


}



