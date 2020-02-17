package com.hiroshi.cimoc.source;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.DecryptionUtils;
import com.hiroshi.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class MangaDog extends MangaParser{

    public static final int TYPE = 15;
    public static final String DEFAULT_TITLE = "漫画狗";

    public MangaDog(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        if(page == 1)
            url = StringUtils.format("https://dogemanga.com/?q=%s", keyword);
        return new Request.Builder()
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page, final String keyword) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".site-search-result")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href(".site-thumbnail-box a").substring(24);
                cid = cid.replaceAll("/","---");
                String title = node.attr(".site-thumbnail-box a > img","alt");
                String cover = node.src(".site-thumbnail-box a > img").trim();
                return new Comic(TYPE, cid, title, cover, "", null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        cid = cid.replaceAll("---","/");
        return new Request.Builder().url("https://dogemanga.com/m/"+cid).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".site-manga-info h2");
        String cover = body.src(".site-thumbnail-box img").trim();
        String update = body.getChild("#site-manga-all #site-manga-preview-box > div > a img").attr("alt").trim();
        String author = body.text(".site-manga-info h4");
        String intro = body.text(".site-manga-info p");
        boolean status = isFinish(body.text(".site-manga-info li:contains(連載狀態)"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list("#site-manga-all #site-manga-preview-box > div > a")) {
            String title = node.attr("img","alt");
            String path = node.href().substring(24).replaceAll("/","---");
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        path = path.replaceAll("---","/");
        String url = StringUtils.format("https://dogemanga.com/p/%s", path);
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html){
        List<ImageUrl> list = new LinkedList<>();
        String image = StringUtils.match("<script type=\"application/json\" id=\"site-args\">([\\s\\S]*?)</script>",html,1);
        try {
            image = DecryptionUtils.base64Decrypt(image);
        try {
            JSONObject object = new JSONObject(image);
            JSONArray array = object.getJSONArray("imageUris");
        for (int i = 0; i < array.length(); i++) {
            list.add(new ImageUrl(i+1, array.getString(i), false));
            }
        }catch (JSONException e){
            e.printStackTrace();
            }
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).getChild("#site-manga-all #site-manga-preview-box > div > a img").attr("alt").trim();
    }

    @Override
    public Headers getHeader() {
        return null;
    }


}


