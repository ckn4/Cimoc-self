package com.hiroshi.cimoc.core;

import com.hiroshi.cimoc.App;
import com.hiroshi.cimoc.manager.SourceManager;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.parser.Parser;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/8/20.
 */
public class Manga {

    public static Observable<Comic> getSearchResult(final Parser parser, final String keyword, final int page) {
        return Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                try {
                    Request request = parser.getSearchRequest(keyword, page);
                    Random random = new Random();
                    String decode  = parser.getDecode();
                    String html = getResponseBody(App.getHttpClient(), request, decode);
                    SearchIterator iterator = parser.getSearchIterator(html, page, keyword);
                    if (iterator == null || iterator.empty()) {
                        throw new Exception();
                    }
                    while (iterator.hasNext()) {
                        Comic comic = iterator.next();
                        if (comic != null) {
                            subscriber.onNext(comic);
                            Thread.sleep(random.nextInt(200));
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<Chapter>> getComicInfo(final Parser parser, final Comic comic) {
        return Observable.create(new Observable.OnSubscribe<List<Chapter>>() {
            @Override
            public void call(Subscriber<? super List<Chapter>> subscriber) {
                try {
                    Request request = parser.getInfoRequest(comic.getCid());
                    String decode = parser.getDecode();
                    String html = getResponseBody(App.getHttpClient(), request, decode);
                    parser.parseInfo(html, comic);
                    request = parser.getChapterRequest(html, comic.getCid());
                    if (request != null) {
                        html = getResponseBody(App.getHttpClient(), request, decode);
                    }
                    List<Chapter> list = parser.parseChapter(html);
                    if (!list.isEmpty()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    } else {
                        throw new ParseErrorException();
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<Comic>> getCategoryComic(final Parser parser, final String format,
                                                           final int page) {
        return Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    Request request = parser.getCategoryRequest(format, page);
                    String html = "";
                    String decode = parser.getDecode();
                    if (parser.getHeader()!=null) {
                        if (parser.getHeader().get("User-Agent") != null) {
                            String UA = parser.getHeader().get("User-Agent");
                            String referer = parser.getHeader().get("Referer");
                            html = getResponseBody(App.getHttpClient(), request.newBuilder().addHeader("User-Agent", UA).addHeader("Referer", referer).build(), decode);
                        }
                        else html = getResponseBody(App.getHttpClient(), request, decode);
                    }
                    else html = getResponseBody(App.getHttpClient(), request, decode);
                    List<Comic> list = parser.parseCategory(html, page);
                    if (!list.isEmpty()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<ImageUrl>> getChapterImage(final Parser parser, final String cid,
                                                             final String path) {
        return Observable.create(new Observable.OnSubscribe<List<ImageUrl>>() {
            @Override
            public void call(Subscriber<? super List<ImageUrl>> subscriber) {
                String html;
                try {
                    Request request = parser.getImagesRequest(cid, path);
                    String decode = parser.getDecode();
                    html = getResponseBody(App.getHttpClient(), request, decode);
                    List<ImageUrl> list = parser.parseImages(html);
                    if (list.isEmpty()) {
                        throw new Exception();
                    } else {
                        for (ImageUrl imageUrl : list) {
                            imageUrl.setChapter(path);
                        }
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static List<ImageUrl> getImageUrls(Parser parser, String cid, String path) throws InterruptedIOException {
        List<ImageUrl> list = new ArrayList<>();
        Response response = null;
        try {
            Request request  = parser.getImagesRequest(cid, path);
            response = App.getHttpClient().newCall(request).execute();
            if (response.isSuccessful()) {
                list.addAll(parser.parseImages(response.body().string()));
            } else {
                throw new NetworkErrorException();
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return list;
    }

    public static String getLazyUrl(Parser parser, String url) throws InterruptedIOException {
        Response response = null;
        int i = 0;
        try {
            Request request = parser.getLazyRequest(url);
            OkHttpClient client = App.getHttpClient();
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return parser.parseLazy(response.body().string(), url);
            } else {
                do {
                    response = client.newCall(request).execute();
                    i++;
                }
                while (!response.isSuccessful()&&i<3);
                if (response.isSuccessful()) {
                    return parser.parseLazy(response.body().string(),url);
                }
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static Observable<String> loadLazyUrl(final Parser parser, final String url) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Request request = parser.getLazyRequest(url);
                String newUrl = null;
                String decode = parser.getDecode();
                try {
                    newUrl = parser.parseLazy(getResponseBody(App.getHttpClient(), request, decode), url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                subscriber.onNext(newUrl);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<String>> loadAutoComplete(final String keyword) {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                RequestBody body = new FormBody.Builder()
                        .add("t", keyword)
                        .add("user-agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                        .build();
                Request request = new Request.Builder()
                        .url("http://www.dm5.com/search.ashx")
                        .post(body)
                        .build();
                try {
                    Node html = new Node(getResponseBody(App.getHttpClient(), request, null));
                    List<String> list = new ArrayList<>();
                    for (Node node:html.list("span.left"))
                        list.add(node.text());
                    subscriber.onNext(list);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Comic> checkUpdate(
            final SourceManager manager, final List<Comic> list) {
        return Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(1500, TimeUnit.MILLISECONDS)
                        .readTimeout(1500, TimeUnit.MILLISECONDS)
                        .build();
                for (Comic comic : list) {
                    if (!comic.getFinish()){
                    Parser parser = manager.getParser(comic.getSource());
                    Request request = parser.getCheckRequest(comic.getCid());
                    try {
                        String decode = parser.getDecode();
                        String update = parser.parseCheck(getResponseBody(client, request, decode));
                        if (comic.getUpdate() != null && update != null && !comic.getUpdate().equals(update)) {
                            comic.setFavorite(System.currentTimeMillis());
                            comic.setUpdate(update);
                            comic.setHighlight(true);
                            subscriber.onNext(comic);
                            continue;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    }
                    subscriber.onNext(null);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io());
    }

    private static String getResponseBody(OkHttpClient client, Request request, String decode) throws NetworkErrorException {
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                byte[] bodybytes = response.body().bytes();
                String body = new String(bodybytes);
                if (decode == null) {
                    Matcher m = Pattern.compile("charset=([\\w\\-]+)").matcher(body);
                    if (m.find()) {
                        body = new String(bodybytes, m.group(1));
                    }
                }else body = new String(bodybytes, decode);
                return body;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new NetworkErrorException();
    }

    public static class ParseErrorException extends Exception {}

    public static class NetworkErrorException extends Exception {}

}
