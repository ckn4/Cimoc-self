package com.hiroshi.cimoc.manager;

import android.util.SparseArray;

import com.hiroshi.cimoc.component.AppGetter;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.model.SourceDao;
import com.hiroshi.cimoc.model.SourceDao.Properties;
import com.hiroshi.cimoc.parser.Parser;
import com.hiroshi.cimoc.source.BaiNian;
import com.hiroshi.cimoc.source.Cartoonmad;
import com.hiroshi.cimoc.source.ComicBus;
import com.hiroshi.cimoc.source.Dmzj;
import com.hiroshi.cimoc.source.EHentai;
import com.hiroshi.cimoc.source.HHSSEE;
import com.hiroshi.cimoc.source.IKanman;
import com.hiroshi.cimoc.source.Locality;
import com.hiroshi.cimoc.source.MH50;
import com.hiroshi.cimoc.source.MHRen;
import com.hiroshi.cimoc.source.ManHuaDB;
import com.hiroshi.cimoc.source.MangaDog;
import com.hiroshi.cimoc.source.Null;
import com.hiroshi.cimoc.source.Tenmanga;
import com.hiroshi.cimoc.source.gfmh;
import com.hiroshi.cimoc.source.manganelo;
import com.hiroshi.cimoc.source.mkzhan;
import com.hiroshi.cimoc.source.nhentai;
import com.hiroshi.cimoc.source.rawlh;
import com.hiroshi.cimoc.source.rawqq;
import com.hiroshi.cimoc.source.rawqv;
import com.hiroshi.cimoc.source.xinxmh;

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
                case IKanman.TYPE:
                    parser = new IKanman(source);
                    break;
                case Dmzj.TYPE:
                    parser = new Dmzj(source);
                    break;
                case HHSSEE.TYPE:
                    parser = new HHSSEE(source);
                    break;
                    //test
                case rawqv.TYPE:
                    parser = new rawqv(source);
                    break;
                case nhentai.TYPE:
                    parser = new nhentai(source);
                    break;
                case MH50.TYPE:
                    parser = new MH50(source);
                    break;
                case ManHuaDB.TYPE:
                    parser = new ManHuaDB(source);
                    break;
                case rawlh.TYPE:
                    parser = new rawlh(source);
                    break;
                case manganelo.TYPE:
                    parser = new manganelo(source);
                    break;
                case mkzhan.TYPE:
                    parser = new mkzhan(source);
                    break;
                case gfmh.TYPE:
                    parser = new gfmh(source);
                    break;
                case Tenmanga.TYPE:
                    parser = new Tenmanga(source);
                    break;
                case MHRen.TYPE:
                    parser = new MHRen(source);
                    break;
                case Cartoonmad.TYPE:
                    parser = new Cartoonmad(source);
                    break;
                case BaiNian.TYPE:
                    parser = new BaiNian(source);
                    break;
                case rawqq.TYPE:
                    parser = new rawqq(source);
                    break;
                case ComicBus.TYPE:
                    parser = new ComicBus(source);
                    break;
                case EHentai.TYPE:
                    parser = new EHentai(source);
                    break;
                case MangaDog.TYPE:
                    parser = new MangaDog(source);
                    break;
                case xinxmh.TYPE:
                    parser = new xinxmh(source);
                    break;
                    //testend
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
