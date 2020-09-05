package com.hiroshi.cimoc.manager;

import android.util.SparseArray;

import com.hiroshi.cimoc.component.AppGetter;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.model.SourceDao;
import com.hiroshi.cimoc.model.SourceDao.Properties;
import com.hiroshi.cimoc.parser.Parser;
import com.hiroshi.cimoc.source.ComicBus;
import com.hiroshi.cimoc.source.Dmzj;
import com.hiroshi.cimoc.source.EHentai;
import com.hiroshi.cimoc.source.Erocool;
import com.hiroshi.cimoc.source.Locality;
import com.hiroshi.cimoc.source.MHRen;
import com.hiroshi.cimoc.source.ManHuaDB;
import com.hiroshi.cimoc.source.MangaRaw;
import com.hiroshi.cimoc.source.Null;
import com.hiroshi.cimoc.source.manganelo;
import com.hiroshi.cimoc.source.rawqq;

import java.util.List;

import okhttp3.Headers;
import rx.Observable;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourceManager {

    private static SourceManager mInstance;

    private SourceDao mSourceDao;
    private SparseArray<Parser> mParserArray = new SparseArray<>();

    private SourceManager(AppGetter getter) {
        mSourceDao = getter.getAppInstance().getDaoSession().getSourceDao();
    }

    public Observable<List<Source>> list() {
        return mSourceDao.queryBuilder()
                .orderAsc(Properties.Type)
                .rx()
                .list();
    }

    public Observable<List<Source>> listEnableInRx() {
        return mSourceDao.queryBuilder()
                .where(Properties.Enable.eq(true))
                .orderAsc(Properties.Type)
                .rx()
                .list();
    }

    public List<Source> listEnable() {
        return mSourceDao.queryBuilder()
                .where(Properties.Enable.eq(true))
                .orderAsc(Properties.Type)
                .list();
    }

    public Source load(int type) {
        return mSourceDao.queryBuilder()
                .where(Properties.Type.eq(type))
                .unique();
    }

    public long insert(Source source) {
        return mSourceDao.insert(source);
    }

    public void update(Source source) {
        mSourceDao.update(source);
    }

    public Parser getParser(int type) {
        Parser parser = mParserArray.get(type);
        if (parser == null) {
            Source source = load(type);
            switch (type) {
                case Dmzj.TYPE:
                    parser = new Dmzj(source);
                    break;
                case ComicBus.TYPE:
                    parser = new ComicBus(source);
                    break;
                case MHRen.TYPE:
                    parser = new MHRen(source);
                    break;
                case ManHuaDB.TYPE:
                    parser = new ManHuaDB(source);
                    break;
                case rawqq.TYPE:
                    parser = new rawqq(source);
                    break;
                case MangaRaw.TYPE:
                    parser = new MangaRaw(source);
                    break;
                case manganelo.TYPE:
                    parser = new manganelo(source);
                    break;
                case Erocool.TYPE:
                    parser = new Erocool(source);
                    break;
                case EHentai.TYPE:
                    parser = new EHentai(source);
                    break;
                case Locality.TYPE:
                    parser = new Locality();
                    break;
                default:
                    parser = new Null();
                    break;
            }
            mParserArray.put(type, parser);
        }
        return parser;
    }

    public class TitleGetter {

        public String getTitle(int type) {
            return getParser(type).getTitle();
        }

    }

    public class HeaderGetter {

        public Headers getHeader(int type) {
            return getParser(type).getHeader();
        }

    }

    public static SourceManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (SourceManager.class) {
                if (mInstance == null) {
                    mInstance = new SourceManager(getter);
                }
            }
        }
        return mInstance;
    }

}
