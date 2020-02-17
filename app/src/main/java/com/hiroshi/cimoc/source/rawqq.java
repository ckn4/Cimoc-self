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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class rawqq extends MangaParser{

    public static final int TYPE = 42;
    public static final String DEFAULT_TITLE = "Rawqq";

    public rawqq(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
            if (page!= 1) return null;
            RequestBody body = new FormBody.Builder()
                    .add("query", keyword)
                    .build();
            url = "https://hanascan.com/ender.php";
            return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                    .url(url)
                    .post(body)
                    .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list("#suggestsearchz > ul")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("a").substring(21);
                String title = node.attr("a", "title").replace("- Raw", "").trim();
                String cover = node.src("img").trim();
                String author = null;
                return new Comic(TYPE, cid, title, cover, null, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://hanascan.com/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
//        String title = body.text("ul.manga-info > h1").replace("- Raw","").trim();
        String cover = body.src("div.well.info-cover > img");
        String id = StringUtils.match("load_Comment\\((\\d+)\\)",html,1);
        String p = post(new Request.Builder().url("https://hanascan.com/app/manga/controllers/cont.pop.php?action=pop&id="+id).build());
        String title = StringUtils.match("Other name:</strong> (.*?)</p>",p,1);
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
        for (Node node : new Node(html).list("#tab-chapper > div > p")) {
            String title = node.text(".title > a > b");
//                title = title.substring(title.length() - 3);
            title = Pattern.compile("[^0-9.]").matcher(title).replaceAll("");
            String path = node.href(".title > a");
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://hanascan.com/%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        int i =0;
        for (Node node : new Node(html).list("#content > img")) {
            i = i + 1;
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
        String id = StringUtils.match("load_Comment\\((\\d+)\\)",html,1);
        String p = post(new Request.Builder().url("https://hanascan.com/app/manga/controllers/cont.pop.php?action=pop&id="+id).build());
        return StringUtils.match("(\\d{4}-\\d{2}-\\d{2})",p,1);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("div.col-lg-12.col-md-12.row-list > .media > a.pull-left.link-list")) {
            String cid = node.href();
//            String title = node.attr("img","alt");
            String cover = node.attr("img","data-original").trim();
            if (cover.contains("farm1")||cover.contains("farm2")){
                cover = "asset:///icon.png";
            }
            String id = node.attr("img","onmouseenter");
            if (id!=null)
                id = id.substring(5,id.length()-1);
            String p = post(new Request.Builder().url("https://hanascan.com/app/manga/controllers/cont.pop.php?action=pop&id="+id).build());
            String title = StringUtils.match("Other name:</strong> (.*?)</p>",p,1);
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
            return StringUtils.format("https://hanascan.com/manga-list.html?listType=pagination&page=%%d&sort=%s&sort_type=DESC",
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
//        return Headers.of("Referer", "https://rawqq.com");
        return null;
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

