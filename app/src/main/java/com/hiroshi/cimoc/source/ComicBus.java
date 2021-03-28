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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ComicBus extends MangaParser {

    public static final int TYPE = 14;
    public static final String DEFAULT_TITLE = "无限漫画";

    private String _cid = "";
    private String _path = "";

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public ComicBus(Source source) {
        init(source, new Category());
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "http://m.comicbus.com/data/search.aspx?k=";
        if (page == 1) {
            keyword = URLEncoder.encode(keyword,"big5");
            url = url.concat(keyword);
            return new Request.Builder().url(url)
                    .addHeader("origin","http://m.comicbus.com")
                    .addHeader("referer","https://m.comicbus.com/")
                    .build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page,String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".cat_list td")) {
            @Override
            protected Comic parse(Node node) {
                if(node.isNodeExist(".picborder")) {
                    String cid = node.hrefWithSubString(".picborder a", 13, -6);
                    String title = node.text(".pictitle");
                    String cover = node.src(".picborder img").trim();
                    cover = "http://m.comicbus.com/" + cover;
                    return new Comic(TYPE, cid, title, cover, null, null);
                }
                else return null;
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "http://app.6comic.com:88/info/".concat(cid)+".html";
        _cid = cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        String[] result = html.split("\\|");
        String title = result[4];
        String cover = "http://app.6comic.com:88/pics/0/"+result[1]+".jpg";
        String update = StringUtils.match("\\d+-\\d+-\\d+",result[9],0);
        String intro = result[10].substring(2).trim();
        boolean status = isFinish(result[7]);
        comic.setInfo(title, cover, update, intro, null, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        String rresult = post(new Request.Builder().url("http://app.6comic.com:88/comic/".concat(_cid)+".html").build());
        if (StringUtils.match("(<!--ch-->).+",rresult,1)!=null){
            rresult = rresult.replace("<!--ch-->","|");
        }
        if (rresult.indexOf("|")==2){
            rresult = rresult.substring(3);
        }
        List<Chapter> list = new LinkedList<>();
        if (rresult.contains("|")) {
            String[] result = rresult.split("\\|");
            for (int i = 0; i < result.length; i++) {
                String title = StringUtils.match("\\d+ (.*)", result[i], 1);
                String path = String.valueOf(i);
                list.add(new Chapter(title, path));
            }
        }else {
            list.add(new Chapter("为动画", ""));
        }
        Collections.reverse(list);
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = "http://app.6comic.com:88/comics/".concat(_cid)+".html";
        _path = path;
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html){
        List<ImageUrl> list = new ArrayList<>();
        String[] rresult = html.split("\\|");
        int num = Integer.parseInt(_path);
        String result = rresult[num];
        String[] r = result.split(" ");
        String name = r[0];
        String imgserver = r[1];
        String name1 = r[2];
        int pagenum = Integer.parseInt(r[3]);
        String last = r[4];
        for (int i = 0; i < pagenum; i++) {
            String last1 = last.substring(((i%100) / 10) + 3 * (i % 10), ((i%100) / 10) + 3 + 3 * (i % 10));
            list.add(new ImageUrl(i + 1, "http://img" + imgserver + ".8comic.com/" + name1 + "/" + _cid + "/" + name + "/" + StringUtils.format("%03d", i + 1) + "_" + last1 + ".jpg", false));
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        String[] result = html.split("\\|");
        return StringUtils.match("\\d+-\\d+-\\d+",result[9],0);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        String[] result = html.split("\\|");
        for (int i = 0;i<result.length-1;i++) {
            Matcher matcher = Pattern.compile("(\\d+),,.*?,.*?,([^,]+).*?").matcher(result[i]);
            matcher.find();
            String cid = matcher.group(1);
            String title = matcher.group(2);
            String cover = "http://app.6comic.com:88/pics/0/"+cid+".jpg";
            list.add(new Comic(TYPE, cid, title, cover, "", null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("http://app.6comic.com:88%s-%%d.html",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("每日更新", "/i/update"));
            list.add(Pair.create("热门", "/i/hot"));
            list.add(Pair.create("完结经典", "/i/best"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://www.comicbus.xyz");
    }

    @Override
    public String getDecode(){
        return "big5";
    }

    private static String post(Request request) {
        return getResponseBody(App.getHttpClient(), request);
    }

    private static String getResponseBody(OkHttpClient client, Request request){
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                byte[] bodybytes = response.body().bytes();
                return new String(bodybytes, "big5");
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
