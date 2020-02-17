package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.App;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Hiroshi on 2016/7/26.
 */
public class EHentai extends MangaParser {

    public static final int TYPE = 62;
    public static final String DEFAULT_TITLE = "EHentai";

    private String _url = "";

    public EHentai(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = "";
        url = StringUtils.format("http://e-hentai.org/?page=%d&f_doujinshi=1&f_manga=1&f_artistcg=1&f_gamecg=1&f_western=1&f_non-h=1&f_imageset=1&f_cosplay=1&f_asianporn=1&f_misc=1&f_search=%s&f_apply=Apply+Filter", page-1, keyword);
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(final String html, int page, String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list("div.ido > div > table.itg > tbody > tr:has(td[class^=gl2c])")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.hrefWithSubString("td.gl3c > a", 23, -2);
                cid = cid.replaceAll("/","---");
                String title = node.text(".glink");
                String cover = node.attr(".glthumb img","data-src").trim();
                if (cover.equals("")){
                    cover = node.src(".glthumb img").trim();
                }
                title = title.replaceFirst("\\[.*?\\]\\s*", "");
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        cid = cid.replaceAll("---","/");
        String url = StringUtils.format("http://e-hentai.org/g/%s", cid);
        _url = url;
        return new Request.Builder().url(url).header("Cookie", "nw=1").build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String update = body.textWithSubstring("#gdd > table > tbody > tr:eq(0) > td:eq(1)", 0, 10);
        String title = body.text("#gn");
        String intro = body.text("#gj");
        String author = body.text("#taglist > table > tbody > tr > td:eq(1) > div > a[id^=ta_artist]");
//        String cover = body.href("#gdt > .gdtm > div > a");
        String cover = "";
        Matcher m = Pattern.compile("background:transparent url\\((.*?)\\)").matcher(html);
        if (m.find())
            cover = m.group(1);
        comic.setInfo(title, cover, update, intro, author, true);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        list.add(0, new Chapter("全一话", "1"));
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return new Request.Builder().url(_url).header("Cookie", "nw=1").build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        Node body = new Node(html);
        String length = body.textWithSplit("#gdd > table > tbody > tr:eq(5) > td:eq(1)", " ", 0);
        int size = Integer.parseInt(length) % 40 == 0 ? Integer.parseInt(length) / 40 : Integer.parseInt(length) / 40 + 1;
        for (int i = 0; i != size; ++i) {
            String html2 = post(new Request.Builder().url(StringUtils.format(_url+"/?p=%d",i)).build());
            Node body2 = new Node(html2);
            int count = 0;
            for (Node node : body2.list(".gdtm a")) {
                list.add(new ImageUrl(++count, node.href(), true));
            }
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder().url(url).header("Cookie", "nw=1").build();
    }

    @Override
    public String parseLazy(String html, String url) {
        return new Node(html).src("#img").trim();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("div.ido > div > table.itg > tbody > tr:has(td[class^=gl2c])")) {
                String cid = node.hrefWithSubString("td.gl3c > a", 23, -2);
                cid = cid.replaceAll("/","---");
                String title = node.text(".glink");
                String cover = node.attr(".glthumb img","data-src").trim();
                if (cover.equals("")){
                    cover = node.src(".glthumb img").trim();
                }
                title = title.replaceFirst("\\[.*?\\]\\s*", "");
                list.add(new Comic(TYPE, cid, title, cover, null, null));
            }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public String getFormat(String... args) {
            return StringUtils.format("%spage=%%d",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("汉化", "https://e-hentai.org/?f_doujinshi=on&f_manga=on&f_artistcg=on&f_gamecg=on&f_western=on&f_non-h=on&f_imageset=on&f_cosplay=on&f_asianporn=on&f_misc=on&f_search=chinese&f_apply=Apply+Filter&"));
            list.add(Pair.create("首页", "https://e-hentai.org/?"));
            list.add(Pair.create("Doujinshi", "https://e-hentai.org/?f_cats=1021&"));
            list.add(Pair.create("NonH", "https://e-hentai.org/?f_cats=767&"));
            list.add(Pair.create("manga", "https://e-hentai.org/?f_cats=1019&"));
            list.add(Pair.create("image set", "https://e-hentai.org/?f_cats=991&"));
            list.add(Pair.create("Artisi CG", "https://e-hentai.org/?f_cats=1015&"));
            list.add(Pair.create("Game CG", "https://e-hentai.org/?f_cats=1007&"));
            return list;
        }


    }

    private static String post(Request request) {
        return getResponseBody(App.getHttpClient(), request);
    }

    private static String getResponseBody(OkHttpClient client, Request request){
        Response response = null;
        int i = 0;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            }
            else {
                do {
                    response = client.newCall(request).execute();
                    i++;
                }
                while (!response.isSuccessful()&&i<3);
                if (response.isSuccessful()) {
                    return response.body().string();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    @Override
    public Headers getHeader() {
       // return Headers.of("Referer", "https://e-hentai.org");
        return null;
    }

}
