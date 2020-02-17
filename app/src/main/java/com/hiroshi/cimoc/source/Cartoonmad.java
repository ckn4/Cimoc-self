package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.RegexIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by FEILONG on 2017/12/21.
 */

public class Cartoonmad extends MangaParser {

    public static final int TYPE = 5;
    public static final String DEFAULT_TITLE = "动漫狂";

    public Cartoonmad(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        if (page != 1) return null;
        String url = "http://www.cartoonmad.com/m/?act=7";
        RequestBody body = new FormBody.Builder()
                .add("keyword", keyword)
                .build();
        return new Request.Builder().url(url).post(body).addHeader("Referer", "https://www.cartoonmad.com/").build();
    }

    @Override
    public SearchIterator getSearchIterator(final String html, int page, String keyword) {
        Pattern pattern = Pattern.compile("<a href=/m/comic/(\\d+)\\.html title=\"(.*?)\"><span class=\"covers\"></span><img src=\"(.*?)\"");
        Matcher matcher = pattern.matcher(html);
        return new RegexIterator(matcher) {
            @Override
            protected Comic parse(Matcher match) {
                String cid = match.group(1);
                String title = match.group(2);
                String cover = match.group(3);
                cover = "http://www.cartoonmad.com"+cover;
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://www.cartoonmad.com/comic/".concat(cid).concat(".html");
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Matcher mTitle = Pattern.compile("<meta name=\"Keywords\" content=\"(.*?),").matcher(html);
        String title = mTitle.find() ? mTitle.group(1) : "";
        Matcher mCover = Pattern.compile("<div class=\"cover\"></div><img src=\"(.*?)\"").matcher(html);
        String cover = mCover.find() ? mCover.group(1) : "";
        cover = "http://www.cartoonmad.com"+cover;
        String update = StringUtils.match("發表，最後更新日期 (\\d+/\\d+/\\d{4})",html,1);
        Matcher mInro = Pattern.compile("<META name=\"description\" content=\"(.*?)\">").matcher(html);
        String intro = mInro.find() ? mInro.group(1) : "";
        Matcher mstatus = Pattern.compile("<img src=\"/image/chap9\\.gif\" align=\"absmiddle\">").matcher(html);
        boolean status = mstatus.find();
        comic.setInfo(title, cover, update, intro, null, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        Matcher mChapter = Pattern.compile("<a href=(.*?) target=_blank>(.*?)</a>&nbsp;").matcher(html);
        while (mChapter.find()) {
            String title = mChapter.group(2);
            String path = mChapter.group(1);
            list.add(new Chapter(title, path));
        }
        Collections.reverse(list);
        return list;
    }


    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://www.cartoonmad.com%s", path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new ArrayList<>();
        Matcher pageMatcher = Pattern.compile("<a class=onpage>.*<a class=pages href=(.*)\\d{3}\\.html>(.*?)<\\/a>").matcher(html);
        if (!pageMatcher.find()) return null;
        int page = Integer.parseInt(pageMatcher.group(2));
        for (int i = 1; i <= page; ++i) {
            list.add(new ImageUrl(i, StringUtils.format("https://www.cartoonmad.com/comic/%s%03d.html", pageMatcher.group(1), i), true));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder()
                .addHeader("Referer", url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url).build();
    }

    @Override
    public String parseLazy(String html, String url) {
        Matcher m = Pattern.compile("<img src=\"(.*?)\" border=\"0\" oncontextmenu").matcher(html);
        if (m.find()) {
            String uri =  "https://www.cartoonmad.com/comic/" + m.group(1);
            return post(new Request.Builder().addHeader("Referer", url).url(uri).build());
        }
        return null;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return StringUtils.match("發表，最後更新日期 (\\d+/\\d+/\\d{4})",html,1);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        Pattern pattern = Pattern.compile("<a href=comic/(\\d+)\\.html title=\"(.*?)\"><span class=\"covers\"></span><img src=\"(.*?)\"");
        Matcher matcher = pattern.matcher(html);
        while(matcher.find()){
            String cid = matcher.group(1);
            String title = matcher.group(2);
            String cover = matcher.group(3);
            cover = "http://www.cartoonmad.com"+cover;
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://www.cartoonmad.com/%s.%%02d.html",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        public List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("最新更新", "newcm"));
            list.add(Pair.create("经典完结", "endcm"));
            list.add(Pair.create("热门连载", "hotrank"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://www.cartoonmad.com");
    }

    private static String post(Request request) {
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .followRedirects(false)
                .build();
        return getResponseBody(client, request);
    }

    private static String getResponseBody(OkHttpClient client, Request request){
        Response response = null;
        try {
            response = client.newCall(request).execute();
            return response.headers().get("Location");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

}
