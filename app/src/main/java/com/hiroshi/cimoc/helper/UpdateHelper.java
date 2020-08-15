package com.hiroshi.cimoc.helper;

import com.hiroshi.cimoc.manager.PreferenceManager;
import com.hiroshi.cimoc.model.DaoSession;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.source.ComicBus;
import com.hiroshi.cimoc.source.Dmzj;
import com.hiroshi.cimoc.source.EHentai;
import com.hiroshi.cimoc.source.Erocool;
import com.hiroshi.cimoc.source.MHRen;
import com.hiroshi.cimoc.source.ManHuaDB;
import com.hiroshi.cimoc.source.MangaRaw;
import com.hiroshi.cimoc.source.Onemh;
import com.hiroshi.cimoc.source.manganelo;
import com.hiroshi.cimoc.source.rawqq;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class UpdateHelper {

    private static final int VERSION = 162;

    public static void update(PreferenceManager manager, final DaoSession session) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        //之前版本version默认为1，新安装version默认为0
        if (version != VERSION) {
            switch (version) {
                case 0:
                    initSource(session);
                    break;
                case 151:
                    session.getSourceDao().insert(Erocool.getDefaultSource());
                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 61");
                case 153:
                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 44");
                case 154:
                    session.getSourceDao().insert(rawqq.getDefaultSource());
                    session.getSourceDao().insert(MangaRaw.getDefaultSource());
                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 41");
                case 158:
                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 3");
                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 15");
                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 21");
            }
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }
    }

    /**
     * 初始化图源
     */
    private static void initSource(DaoSession session) {
        List<Source> list = new ArrayList<>();
        list.add(Dmzj.getDefaultSource());
        list.add(ComicBus.getDefaultSource());
        list.add(MHRen.Companion.getDefaultSource());
        list.add(ManHuaDB.getDefaultSource());
        list.add(Onemh.getDefaultSource());
        list.add(rawqq.getDefaultSource());
        list.add(MangaRaw.getDefaultSource());
        list.add(manganelo.getDefaultSource());
        list.add(Erocool.getDefaultSource());
        list.add(EHentai.getDefaultSource());
        session.getSourceDao().insertOrReplaceInTx(list);
    }

}
