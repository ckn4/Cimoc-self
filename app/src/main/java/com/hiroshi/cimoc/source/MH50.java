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
import com.hiroshi.cimoc.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by WinterWhisper on 2019/2/25.
 */
public class MH50 extends MangaParser {

    public static final int TYPE = 21;
    public static final String DEFAULT_TITLE = "50漫画";

    private String iv = "ABCDEF1G34123412";
    private String strKey = "123456781234567G";

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public MH50(Source source) {
        init(source, new Category());
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        if (page == 1) {
            String url = StringUtils.format("https://m.manhuadui.com/search/?keywords=%s&page=%d", keyword, page);
            return new Request.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
                    .url(url)
                    .build();

        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page,String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".UpdateList > .itemBox")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.hrefWithSplit(".itemTxt > a", 1);
                String title = node.text(".itemTxt > a");
                String cover = node.src(".itemImg > a > img");
                if (cover.startsWith("//")) cover = "https:" + cover;
                if (cover.contains("mh.manhuazj")){
                    cover = "asset:///icon.png";
                }
//                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim().substring(0, 10);
                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim();
                if (!update.equals("-")){
                    update = update.substring(0,10);
                }
                String author = node.text(".itemTxt > p.txtItme:eq(1)");
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("https://m.manhuadui.com/manhua/%s/", cid);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
                .url(url)
                .build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String intro = body.text("#full-des");
        if (intro == null){
            intro = body.text("#simple-des");
        }
        String title = body.text("#comicName");
        String cover = body.src("#Cover > img");
        if (cover.startsWith("//")) cover = "https:" + cover;
        if (cover.contains("mh.manhuazj")){
            cover = "asset:///icon.png";
        }
        String author = body.text(".Introduct_Sub > .sub_r > .txtItme:eq(0)");
        String update = body.text(".Introduct_Sub > .sub_r > .txtItme:eq(4)").trim();
        if (!update.equals("-")){
            update = update.substring(0,10);
        }
        boolean status = isFinish(body.text(".Introduct_Sub > .sub_r > .txtItme:eq(2) > a:eq(3)"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        List<Chapter> list1 = new LinkedList<>();
        List<Chapter> list2 = new LinkedList<>();
        List<Chapter> list3 = new LinkedList<>();
        List<Chapter> list4 = new LinkedList<>();
        List<Chapter> list5 = new LinkedList<>();
        Node body = new Node(html);
        for (Node node : body.list("#chapter-list-1 > li > a")) {
            String title = node.text("span");
            String path = StringUtils.split(node.href(), "/", 3);
            list1.add(new Chapter(title, path));
        }
        Collections.reverse(list1);
        //番外
        for (Node node : body.list("#chapter-list-3 > li > a")) {
            String title = node.text("span");
            String path = StringUtils.split(node.href(), "/", 3);
            list2.add(new Chapter(title, path));
        }
        Collections.reverse(list2);
        //其它
        for (Node node : body.list("#chapter-list-5 > li > a")) {
            String title = node.text("span");
            String path = StringUtils.split(node.href(), "/", 3);
            list3.add(new Chapter(title, path));
        }
        Collections.reverse(list3);
        for (Node node : body.list("#chapter-list-7 > li > a")) {
            String title = "[同人]"+node.text("span");
            String path = StringUtils.split(node.href(), "/", 3);
            list5.add(new Chapter(title, path));
        }
        Collections.reverse(list5);
        for (Node node : body.list("#chapter-list-4 > li > a")) {
            String title = "[单]"+node.text("span");
            String path = StringUtils.split(node.href(), "/", 3);
            list4.add(new Chapter(title, path));
        }
        Collections.reverse(list4);
        list.addAll(list1);
        list.addAll(list2);
        list.addAll(list3);
        list.addAll(list5);
        list.addAll(list4);
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://www.manhuadui.com/manhua/%s/%s", cid, path);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
                .url(url)
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String arrayString = StringUtils.match("var chapterImages = \"(.*?)\";", html, 1);
        String imagePath = StringUtils.match("var chapterPath = ([\\s\\S]*?);", html, 1).replace("\"", "");

        arrayString = AESCryptUtil.decrypt(arrayString,strKey,iv);

        if (arrayString != null) {
            try {
                arrayString = arrayString.substring(1,arrayString.length()-1);
                String[] array = arrayString.split(",");
                for (int i = 0; i != array.length; ++i) {
                    String imageUrl;
                    if (array[i].startsWith("\"http")) {
                        imagePath = "showImage.php?url=";
                        imageUrl = "https://mhcdn.manhuazj.com/" + imagePath + array[i].replace("\"", "").replace("\\/", "/");
//                    else if (array[i].startsWith("\"http")) {
//                        imageUrl = array[i].replace("\"", "");
                    } else {
//                        imageUrl = "https://res02.333dm.com/" + imagePath + array[i].replace("\"", "");
                        imageUrl = "https://mhcdn.manhuazj.com/" + imagePath + array[i].replace("\"", "");
                    }
                    list.add(new ImageUrl(i + 1, imageUrl, false));
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
//        return new Node(html).text(".Introduct_Sub > .sub_r > .txtItme:eq(4)");
        String update = new Node(html).text(".Introduct_Sub > .sub_r > .txtItme:eq(4)").trim();
        if (!update.equals("-")){
            update = update.substring(0,10);
        }
        return update;
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        if (!body.list("#comic-items > .list-comic").isEmpty()){
            for (Node node : body.list("#comic-items > .list-comic")) {
                String cid = node.hrefWithSplit("a", 1);
                String title = node.text(".txtA");
                String cover = node.src("img");
                if (cover.startsWith("//")) cover = "https:" + cover;
                if (cover.contains("mh.manhuazj")) {
                    cover = "asset:///icon.png";
                }
//                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim().substring(0, 10);
                list.add(new Comic(TYPE, cid, title, cover, null, null));
            }
        }else {
            for (Node node : body.list(".UpdateList > .itemBox")) {
                String cid = node.hrefWithSplit(".itemTxt > a", 1);
                String title = node.text(".itemTxt > a");
                String cover = node.src(".itemImg > a > img");
                if (cover.startsWith("//")) cover = "https:" + cover;
                if (cover.contains("mh.manhuazj")) {
                    cover = "asset:///icon.png";
                }
//                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim().substring(0, 10);
                String update = node.text(".itemTxt > p.txtItme:eq(3)").trim();
                if (!update.equals("-")) {
                    update = update.substring(0, 10);
                }
                String author = node.text(".itemTxt > p.txtItme:eq(1)");
                list.add(new Comic(TYPE, cid, title, cover, update, author));
            }
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            //https://www.manhuadui.com/update/$page/
            return StringUtils.format("https://m.manhuadui.com/%s/?page=%%d",args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("日漫更新", "list/riben/update"));
            list.add(Pair.create("全部更新", "update"));
            list.add(Pair.create("分类点击", "list/click"));
            list.add(Pair.create("分类更新", "list/update"));
            list.add(Pair.create("分类发布", "list/post"));
            list.add(Pair.create("完结点击", "list/wanjie/click"));
            list.add(Pair.create("完结更新", "list/wanjie/update"));
            list.add(Pair.create("完结发布", "list/wanjie/post"));
            return list;
        }

    }


    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://m.manhuadui.com/");
    }

}
