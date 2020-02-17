package com.hiroshi.cimoc.source;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class mkzhan  extends MangaParser{

    public static final int TYPE = 33;
    public static final String DEFAULT_TITLE = "漫客栈";

    public mkzhan(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        if (keyword.startsWith("id===")){
            if (page == 1) {
                keyword = keyword.replaceFirst("id===","");
                url = StringUtils.format("https://www.mkzhan.com/%s/",keyword);
                return new Request.Builder().url(url).build();
            }
            return null;
        }else {
            if (page == 1) {
                url = "https://www.mkzhan.com/search/?keyword=" + keyword;
                return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                        .url(url)
                        .build();
            }
            return null;
        }
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        final Node body = new Node(html);
        if (body.text(".search_head")!=null&&body.text(".search_head").contains("很遗憾"))
            return null;
        else if (!body.list(".common-comic-item").isEmpty()){
            return new NodeIterator(body.list(".common-comic-item")) {
                @Override
                protected Comic parse(Node node) {
                    String cid = node.href("a");
                    cid = cid.substring(1,cid.length()-1);
                    String title = node.text(".comic__title > a");
                    String cover = node.attr("img", "data-src");
                    String update = "";
                    String author = null;
                    return new Comic(TYPE, cid, title, cover, update, author);
                }
            };
        }
        else return new NodeIterator(body.list(".de-info__box")) {
            @Override
            protected Comic parse(Node node) {
                try {
                    ///214290/802364.html
                    String cid = StringUtils.match("/(\\d+)/\\d+\\.html",body.href(".comic-handles > .btn--read"),1);
                    String title = node.text("p");
                    String cover = node.attr("img","data-src").trim();
                    String author = node.text(".comic-author > .name > a");
                    String update = "";
                    return new Comic(TYPE, cid, title, cover, update, author);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://www.mkzhan.com/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text("body > div.de-info-wr > div.container--response > div > p");
        String cover = body.attr("body > div.de-info-wr > div.container--response > div > div.de-info__cover > img","data-src");
        String update = body.text("span.update-time").substring(0,10);
        String author = body.text(".name > a");
        String intro = body.text(".intro-total");
        boolean status = isFinish(body.text("body > div.container--response.de-container-wr.clearfix > div.de-container > div.de-chapter > div.de-chapter__title > span"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list(".chapter__list.clearfix > ul > li")) {
            String title = node.text("a");
//                title = title.substring(title.length() - 3);
//            title = Pattern.compile("[^0-9.]").matcher(title).replaceAll("");
            String path = node.attr("a","data-hreflink");
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://www.mkzhan.com%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
//        https.*?page-\d+
        Matcher mUrl = Pattern.compile("https.*?page-\\d+").matcher(html);
        int i =0;
        while(mUrl.find()){
            list.add(new ImageUrl(++i, mUrl.group(), false));
        }

        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text("span.update-time").substring(0,10);
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://www.mkzhan.com");
    }


}

