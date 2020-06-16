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
import com.hiroshi.cimoc.utils.AESCryptUtil;
import com.hiroshi.cimoc.utils.DecryptionUtils;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class Onemh extends MangaParser{

    public static final int TYPE = 36;
    public static final String DEFAULT_TITLE = "One漫画";

    private String key = "JRUIFMVJDIWE569j";

    public Onemh(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
            if (page == 1) {
                String url = StringUtils.format("https://www.ohmanhua.com/search?searchString=%s",keyword);
                return new Request.Builder()
                        .url(url)
                        .build();
            }
            return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list("dl.fed-deta-info")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.hrefWithSubString("dd.fed-deta-content > h1 > a",1);
                cid = cid.substring(0,cid.length()-1);
                String title = node.text("dd.fed-deta-content > h1");
                String cover = node.attr("dt.fed-deta-images > a","data-original").trim();
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://www.ohmanhua.com/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String title = body.text("dl.fed-deta-info > dd > h1");
        String cover = body.attr("dl.fed-deta-info > dt > a","data-original");
        String update = body.text("dl.fed-deta-info > dd li:contains(更新) > a");
        String author = body.text("dl.fed-deta-info > dd li:contains(作者) > a");
        String intro = body.text("dl.fed-deta-info > dd li:has(div) > div");
        boolean status = isFinish(body.text("dl.fed-deta-info > dd > li:contains(状态) > a"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list("div.fed-tabs-boxs div.fed-play-item.fed-drop-item.fed-visible > div.all_data_list li")) {
            String title = node.text("a");
            String path = node.href("a");
            path = path.replaceAll("/","---");
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        path = path.replaceAll("---","/");
        String url = StringUtils.format("https://www.ohmanhua.com%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();

        String C_data = StringUtils.match("var C_DATA='(.*?)'",html,1);
        String urls = "";
        int num = 0;
        try {
            C_data = DecryptionUtils.base64Decrypt(C_data);
            String direct_urls = AESCryptUtil.decrypt(C_data,key);
//            String urls__direct = StringUtils.match("urls__direct:\"(.*?)\"",direct_urls,1);
//            urls = DecryptionUtils.base64Decrypt(urls__direct);
            String domain = StringUtils.match("domain:\"(.*?)\"",direct_urls,1);
            urls = StringUtils.match("imgpath:\"(.*?)\"",direct_urls,1);
            String Snum = StringUtils.match("totalimg:(.*?),",direct_urls,1);
            if (Snum!=null)
            num = Integer.parseInt(Snum);
//        String[] url = urls.split("|SEPARATER|");
        for (int i=1;i<=num;i++){
            list.add(new ImageUrl(i, "http://" + domain + StringUtils.format("/comic/%s%04d.jpg", urls, i), false));
        }
        }catch (IOException e){
            return null;
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text("dl.fed-deta-info > dd li:contains(更新) > a");
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("li.fed-list-item")) {
            String cid = node.hrefWithSubString(".fed-list-title",1);
            cid = cid.substring(0,cid.length()-1);
            String title = node.text(".fed-list-title");
            String cover = node.attr(".fed-list-pics","data-original").trim();
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://www.ohmanhua.com/%s&page=%%d",args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "show?orderBy=update"));
            list.add(Pair.create("排行榜", "show?"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return null;
    }


}

