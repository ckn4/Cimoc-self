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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class MangaRaw extends MangaParser{

    public static final int TYPE = 44;
    public static final String DEFAULT_TITLE = "MangaRaw";

    public MangaRaw(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        if (page!= 1) return null;
        keyword = URLEncoder.encode(keyword,"UTF-8");
        url = "http://manga1000.com/?s="+keyword;
        return new Request.Builder()
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list("#main article")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.hrefWithSubString("h3.entry-title > a",22);
                cid = cid.substring(0,cid.length()-1);
                String title = node.text("h3.entry-title > a");
                if (title.contains("(Raw – Free)")){
                    title = title.replace("(Raw – Free)","");
                }
                String cover = node.attr("img", "src");
                cover = cover.replaceAll("https://","http://");
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "http://manga1000.com/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String cover = body.src(".entry-content img");
        cover = cover.replaceAll("https://","http://");
        String title = body.text(".entry-header > h1.entry-title");
        if (title.contains("(Raw – Free)")){
            title = title.replace("(Raw – Free)","");
        }
        String update = body.text(".chaplist p:eq(0)");
        String author = "";
        String intro = body.text(".entry-content > p:eq(2)");
        boolean status = false;
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list(".chaplist a")) {
            String title = node.text();
            title = Pattern.compile("[^0-9.]").matcher(title).replaceAll("");
            String path = node.hrefWithSubString(22);
            path = path.substring(0,path.length()-1);
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("http://manga1000.com/%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        List<String> list1 = new ArrayList<>();
        list1.add(StringUtils.match("class=\"aligncenter\" src=\"(.*?)\"",html,1));
        Matcher mpage = Pattern.compile("data-src=\"(.*?)\"").matcher(html);
        while (mpage.find()) {
            list1.add(mpage.group(1));
        }
        for (int i = 0; i < list1.size(); ++i) {
            String image = list1.get(i);
            if (image.contains("https://"))
                image = image.replaceAll("https://","http://");
            list.add(new ImageUrl(i + 1, image, false));
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text(".chaplist p:eq(0)");
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("#main article")) {
            String cid = node.hrefWithSubString("h3.entry-title > a",22);
            cid = cid.substring(0,cid.length()-1);
            String cover = node.src("img");
            cover = cover.replaceAll("https://","http://");
            String title = node.text(".entry-title > a");
            if (title.contains("(Raw – Free)")){
                title = title.replace("(Raw – Free)","");
            }
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("http://manga1000.com/%s/page/%%d/",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "newmanga"));
            list.add(Pair.create("热门", ""));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer","https://manga1000.com/","User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
    }

}

