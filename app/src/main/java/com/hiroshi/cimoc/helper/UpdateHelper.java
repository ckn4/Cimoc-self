package com.hiroshi.cimoc.helper;

import com.hiroshi.cimoc.manager.PreferenceManager;
import com.hiroshi.cimoc.model.DaoSession;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.source.ComicBus;
import com.hiroshi.cimoc.source.Dmzj;
import com.hiroshi.cimoc.source.EHentai;
import com.hiroshi.cimoc.source.HHSSEE;
import com.hiroshi.cimoc.source.MH50;
import com.hiroshi.cimoc.source.MHRen;
import com.hiroshi.cimoc.source.ManHuaDB;
import com.hiroshi.cimoc.source.MangaDog;
import com.hiroshi.cimoc.source.MangaRaw;
import com.hiroshi.cimoc.source.Onemh;
import com.hiroshi.cimoc.source.manganelo;
import com.hiroshi.cimoc.source.nhentai;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class UpdateHelper {

    private static final int VERSION = 151;

    public static void update(PreferenceManager manager, final DaoSession session) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        //之前版本version默认为1，新安装version默认为0
        if (version != VERSION) {
            initSource(session);
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }
    }

    /**
     * 初始化图源
     */
    private static void initSource(DaoSession session) {
        List<Source> list = new ArrayList<>();
        list.add(Dmzj.getDefaultSource());
        list.add(HHSSEE.getDefaultSource());
        list.add(ComicBus.getDefaultSource());
        list.add(MHRen.Companion.getDefaultSource());
        list.add(MangaDog.getDefaultSource());
        list.add(ManHuaDB.getDefaultSource());
        list.add(MH50.getDefaultSource());
        list.add(Onemh.getDefaultSource());
        list.add(MangaRaw.getDefaultSource());
        list.add(manganelo.getDefaultSource());
        list.add(nhentai.getDefaultSource());
        list.add(EHentai.getDefaultSource());
        session.getSourceDao().insertOrReplaceInTx(list);
    }

}
