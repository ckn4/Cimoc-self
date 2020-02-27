package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.App;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.JsonIterator;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class rawlh extends MangaParser{

    public static final int TYPE = 41;
    public static final String DEFAULT_TITLE = "Rawlh";

    public rawlh(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = "";
        if (page!= 1) return null;
        url = "https://loveheaven.net/app/manga/controllers/search.single.php?q="+keyword;
        return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        final String jsonString = StringUtils.match("\"data\":(.*)", html, 1);
        if (jsonString!=null) {
            try {
                return new JsonIterator(new JSONArray(jsonString)) {
                    @Override
                    protected Comic parse(JSONObject object) {
                        try {
                            String cid = object.getString("onclick");
                            cid = StringUtils.match("window.location='\\/(.*)'",cid,1);
                            String title = object.getString("primary").replace("- RAW", "").trim();
                            String cover = object.getString("image").replace("\\","");
                            if (cover.contains("farm1")||cover.contains("farm2")) {
                                cover = "asset:///icon.png";
                            }
                            return new Comic(TYPE, cid, title, cover, null, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://loveheaven.net/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
//        String title = body.text("ul.manga-info > h1").replace("- Raw","").trim();
        String cover = body.src("div.well.info-cover > img");
        if (cover.contains("farm1")||cover.contains("farm2")){
            cover = "asset:///icon.png";
        }
//        String update = body.text("#tab-chapper > div > ul > table > tbody > tr:nth-child(1) > td:nth-child(2) > i > time");
        String id = StringUtils.match("load_Comment\\((\\d+)\\)",html,1);
        String p = post(new Request.Builder().url("https://loveheaven.net/app/manga/controllers/cont.pop.php?action=pop&id="+id).build());
        String title = StringUtils.match("Other Name:</strong> (.*?)</p>",p,1);
        if (title.contains("Updating")){
            title = body.text("ul.manga-info > h1").replace("- Raw","").trim();
        }
        if (title.contains(",")){
            title = title.substring(0,title.indexOf(","));
        }
        String update = StringUtils.match("(\\d{4}-\\d{2}-\\d{2})",p,1);
        String author = "";
        String intro = "";
        boolean status = isFinish(body.text("li:contains(Status) > a"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list("#tab-chapper > div > ul > table > tbody > tr")) {
            String title = node.text("td > a > b");
//                title = title.substring(title.length() - 3);
            title = Pattern.compile("[^0-9.]").matcher(title).replaceAll("");
            String path = node.href("td > a");
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://loveheaven.net/%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        int i =0;
        for (Node node : new Node(html).list("img.chapter-img")) {
            i = i + 1;
//            String src = node.attr("data-original").trim();
            String src = node.attr("data-src").trim();
            if (src.contains("farm1")||src.contains("farm2")) {
                src = "asset:///icon.png";
            }
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
        String id = StringUtils.match("load_Comment\\((\\d+)\\)",html,1);
        String p = post(new Request.Builder().url("https://loveheaven.net/app/manga/controllers/cont.pop.php?action=pop&id="+id).build());
        return StringUtils.match("(\\d{4}-\\d{2}-\\d{2})",p,1);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("div.col-lg-12.col-md-12.row-list > .media > a.pull-left.link-list")) {
            String cid = node.href();
//            String title = node.attr("img","alt");
            String cover = node.src("img");
            if (cover.contains("farm1")||cover.contains("farm2")){
                cover = "asset:///icon.png";
            }
            String id = node.attr("img","onmouseenter");
            if (id!=null)
                id = id.substring(5,id.length()-1);
            String p = post(new Request.Builder().url("https://loveheaven.net/app/manga/controllers/cont.pop.php?action=pop&id="+id).build());
            String title = StringUtils.match("Other Name:</strong> (.*?)</p>",p,1);
            if (title.contains("Updating")){
                title = node.attr("img","alt").replace("- Raw","").trim();
            }
            if (title.contains(",")){
                title = title.substring(0,title.indexOf(","));
            }
            String update = StringUtils.match("(\\d{4}-\\d{2}-\\d{2})",p,1);
            list.add(new Comic(TYPE, cid, title, cover, update, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://loveheaven.net/manga-list.html?listType=pagination&page=%%d&sort=%s&sort_type=DESC",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "last_update"));
            list.add(Pair.create("热门", "views"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://loveheaven.net");
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
}


