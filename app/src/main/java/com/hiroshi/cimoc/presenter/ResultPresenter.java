package com.hiroshi.cimoc.presenter;

import com.hiroshi.cimoc.core.Manga;
import com.hiroshi.cimoc.manager.SourceManager;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.Parser;
import com.hiroshi.cimoc.ui.view.ResultView;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by Hiroshi on 2016/7/4.
 */
public class ResultPresenter extends BasePresenter<ResultView> {

    private static final int STATE_NULL = 0;
    private static final int STATE_DOING = 1;
    private static final int STATE_DONE = 3;

    private static class State {
        int source;
        int page;
        int state;
    }

    private SourceManager mSourceManager;
    private State[] mStateArray;

    private String keyword;
    private int error = 0;

    public ResultPresenter(int[] source, String keyword) {
        this.keyword = keyword;
        if (source != null) {
            initStateArray(source);
        }
    }

    @Override
    protected void onViewAttach() {
        mSourceManager = SourceManager.getInstance(mBaseView);
        if (mStateArray == null) {
            initStateArray(loadSource());
        }
    }

    private void initStateArray(int[] source) {
        mStateArray = new State[source.length];
        for (int i = 0; i != mStateArray.length; ++i) {
            mStateArray[i] = new State();
            mStateArray[i].source = source[i];
            mStateArray[i].page = 0;
            mStateArray[i].state = STATE_NULL;
        }
    }

    private int[] loadSource() {
        List<Source> list = mSourceManager.listEnable();
        int[] source = new int[list.size()];
        for (int i = 0; i != source.length; ++i) {
            source[i] = list.get(i).getType();
        }
        return source;
    }

    public void loadCategory() {
        if (mStateArray[0].state == STATE_NULL) {
            Parser parser = mSourceManager.getParser(mStateArray[0].source);
            mStateArray[0].state = STATE_DOING;
            if (mStateArray[0].source==1||mStateArray[0].source==8){
                mCompositeSubscription.add(Manga.getCategoryComic(parser, keyword, ++mStateArray[0].page-1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<List<Comic>>() {
                            @Override
                            public void call(List<Comic> list) {
                                mBaseView.onLoadSuccess(list);
                                mStateArray[0].state = STATE_NULL;
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                throwable.printStackTrace();
                                if (mStateArray[0].page == 1) {
                                    mBaseView.onLoadFail();
                                }
                            }
                        }));
            }else
            mCompositeSubscription.add(Manga.getCategoryComic(parser, keyword, ++mStateArray[0].page)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Comic>>() {
                        @Override
                        public void call(List<Comic> list) {
                            mBaseView.onLoadSuccess(list);
                            mStateArray[0].state = STATE_NULL;
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                            if (mStateArray[0].page == 1) {
                                mBaseView.onLoadFail();
                            }
                        }
                    }));
        }
    }

    public void loadSearch() {
        if (mStateArray.length == 0) {
            mBaseView.onSearchError();
            return;
        }
        for (final State obj : mStateArray) {
            if (obj.state == STATE_NULL) {
                Parser parser = mSourceManager.getParser(obj.source);
                obj.state = STATE_DOING;
                mCompositeSubscription.add(Manga.getSearchResult(parser, keyword, ++obj.page)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Comic>() {
                            @Override
                            public void call(Comic comic) {
                                mBaseView.onSearchSuccess(comic);
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                throwable.printStackTrace();
                                if (obj.page == 1) {
                                    obj.state = STATE_DONE;
                                    if (++error == mStateArray.length) {
                                        mBaseView.onSearchError();
                                    }
                                }
                            }
                        }, new Action0() {
                            @Override
                            public void call() {
                                obj.state = STATE_NULL;
                            }
                        }));
            }
        }
    }

}
