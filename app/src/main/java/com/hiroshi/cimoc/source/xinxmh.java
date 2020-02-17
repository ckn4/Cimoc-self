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
import com.hiroshi.cimoc.utils.DecryptionUtils;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class xinxmh extends MangaParser{

    public static final int TYPE = 16;
    public static final String DEFAULT_TITLE = "新新漫画";

    public xinxmh(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        url = StringUtils.format("https://so.177mh.net/k.php?k=%s&p=%d", keyword, page);
        return new Request.Builder()
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".ar_list_co dl,.ar_list_co li")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("h1:eq(0) a:eq(0), span:eq(0) a:eq(0)").substring(22);
                String title = node.text("h1:eq(0) a:eq(0), span:eq(0) a:eq(0)");
                String cover = node.src("img");
                String update = "";
                String author = null;
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        cid = "https://m.177mh.net/"+cid;
        return new Request.Builder().url(cid).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".coc_info > h1");
        String cover = body.src(".coc_info > img");
        String update = body.text("p.update").replace("\"","").substring(3);
        String author = body.text("p.author > a");
        String intro = "";
        boolean status = isFinish(body.text("p.state > a"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        if (Pattern.compile("<ul class=\"chapLiList-cont chapter hide\"><li>暂无章节</li>").matcher(html).find()){
            list.add(new Chapter("暂无章节", "1"));
        }else for (Node node : new Node(html).list("#chapter-warp li")) {
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
        String url = StringUtils.format("https://m.177mh.net/%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String str = StringUtils.match("eval\\(.*\\)", html, 0);
        if (str != null) {
            try {
                String str1 = DecryptionUtils.evalDecrypt(str, "msg",1);
                String[] array = str1.split("\\|");
                String img_s = DecryptionUtils.evalDecrypt(str, "img_s",1);
                img_s = img_s.replace(".0","");
                for (int i = 0; i != array.length; ++i) {
                    list.add(new ImageUrl(i + 1, "https://hws.readingbox.net/h"+img_s+"/" + array[i], false));
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
        return new Node(html).text("p.update").replace("\"","").substring(3);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list(".ar_list_co dl,.ar_list_co li")) {
            String cid = node.href("h1:eq(0) a:eq(0), span a").substring(1);
            String title = node.text("h1:eq(0) a:eq(0), span a");
            String cover = node.src("img");
            list.add(new Comic(TYPE, cid, title, cover, null, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://www.177mh.net/%s_%%d.html",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("完结漫画", "wanjie/index"));
            list.add(Pair.create("连载漫画", "lianzai/index"));
            list.add(Pair.create("热血机战", "rexue/index"));
            list.add(Pair.create("科幻未来", "kehuan/index"));
            list.add(Pair.create("恐怖惊悚", "kongbu/index"));
            list.add(Pair.create("推理悬疑", "xuanyi/index"));
            list.add(Pair.create("滑稽搞笑", "gaoxiao/index"));
            list.add(Pair.create("恋爱生活", "love/index"));
            list.add(Pair.create("体育竞技", "tiyu/index"));
            list.add(Pair.create("纯情少女", "chunqing/index"));
            list.add(Pair.create("魔法奇幻", "qihuan/index"));
            list.add(Pair.create("武侠经典", "wuxia/index"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://m.177mh.net");
    }

}


