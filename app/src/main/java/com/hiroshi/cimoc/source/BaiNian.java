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

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by ZhiWen on 2019/02/25.
 */

public class BaiNian extends MangaParser {

    public static final int TYPE = 35;
    public static final String DEFAULT_TITLE = "百年漫画";

    public BaiNian(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        if (page == 1) {
            url = "https://m.bnmanhua.com/index.php?m=vod-search";
        }

        RequestBody requestBodyPost = new FormBody.Builder()
                .add("wd", keyword)
                .build();
        return new Request.Builder()
                .addHeader("Referer", "https://m.bnmanhua.com/")
                .addHeader("Host", "m.bnmanhua.com")
//                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:65.0) Gecko/20100101 Firefox/65.0")
                .url(url)
                .post(requestBodyPost)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page,String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list("ul.tbox_m > li.vbox")) {
            @Override
            protected Comic parse(Node node) {
                String title = node.attr("a.vbox_t", "title");
                String cid = node.attr("a.vbox_t", "href").substring(7);
                String cover = node.attr("a.vbox_t > mip-img", "src");
                String update = node.text("h4:eq(2)"); // 从1开始
                return new Comic(TYPE, cid, title, cover, update, null);
            }
        };
    }


    @Override
    public Request getInfoRequest(String cid) {
        String url = cid.indexOf(".html") > 0 ? "http://m.bnmanhua.com/comic/".concat(cid)
                : "http://m.bnmanhua.com/comic/".concat(cid).concat(".html");
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String cover = body.attr("div.dbox > div.img > mip-img", "src");
        String title = body.text("div.dbox > div.data > h4");
        String intro = body.text("div.tbox_js");
        String author = body.text("div.dbox > div.data > p.dir").substring(3).trim();
//        String update = post(new Request.Builder().url(StringUtils.format("http://www.bnmanhua.com/comic/%s",_cid)).build());
//        update = StringUtils.match("<p>(\\d+-\\d+-\\d+)",update,1);
        String update = body.getLast("div.tabs_block > ul > li > a").text().trim();
        boolean status = isFinish(body.text("span.list_item"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        // 此处得到的章节列表是从 第1话 开始的
        for (Node node : new Node(html).list("div.tabs_block > ul > li > a")) {
            String title = node.text();
            String path = node.hrefWithSplit(2);
            // 将新得到的章节插入到链表的开头
            list.add(0, new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        cid = cid.substring(0, cid.length() - 5);
        String url = StringUtils.format("http://m.bnmanhua.com/comic/%s/%s.html", cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String str = StringUtils.match("z_img=\'\\[(.*?)\\]\'", html, 1);
        String str2 = StringUtils.match("z_yurl=\'(.*?)\'", html, 1);
        if (str != null && !str.equals("")) {
            try {
                String[] array = str.split(",");
                for (int i = 0; i != array.length; ++i) {
                    array[i] = array[i].substring(1,array[i].length()-1).replaceAll("\\\\","");
                    if (!array[i].contains("http")) {
                        list.add(new ImageUrl(i + 1, str2+array[i], false));
                    }else list.add(new ImageUrl(i + 1, array[i], false));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        // 这里表示的是 parseInfo 的更新时间
//        String update = post(new Request.Builder().url(StringUtils.format("http://www.bnmanhua.com/comic/%s",_cid)).build());
//        return StringUtils.match("<p>(\\d+-\\d+-\\d+)",update,1);
        return new Node(html).getLast("div.tabs_block > ul > li > a").text().trim();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("#list_img li")) {
            String cid = node.href("a").substring(7);
            String title = node.text("p");
            String cover = node.attr("img","data-src");
//                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim().substring(0, 10);
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://www.bnmanhua.com/page/%s/%%d.html",args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("最近更新", "new"));
//            list.add(Pair.create("分类发布降序", "list/-post"));
            list.add(Pair.create("排行榜", "hot"));
//            list.add(Pair.create("分类更新降序", "list/-update"));
            list.add(Pair.create("国产漫画", "guochan"));
//            list.add(Pair.create("分类点击降序", "list/-click"));
            list.add(Pair.create("日韩漫画", "rihan"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://m.bnmanhua.com");
    }
}
