package com.hiroshi.cimoc.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.hiroshi.cimoc.App;
import com.hiroshi.cimoc.R;
import com.hiroshi.cimoc.fresco.ControllerBuilderProvider;
import com.hiroshi.cimoc.manager.SourceManager;
import com.hiroshi.cimoc.model.Comic;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Hiroshi on 2016/7/3.
 */
public class ResultAdapter2 extends BaseAdapter<Comic> {

    private ControllerBuilderProvider mProvider;
    private SourceManager.TitleGetter mTitleGetter;

    class ResultViewHolder extends BaseViewHolder {
        @BindView(R.id.item_grid_image2) SimpleDraweeView comicImage;
        @BindView(R.id.item_grid_title2) TextView comicTitle;
//        @BindView(R.id.item_grid_subtitle2) TextView comicSource;

        ResultViewHolder(View view) {
            super(view);
        }
    }

    public ResultAdapter2(Context context, List<Comic> list) {
        super(context, list);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_grid_explore, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Comic comic = mDataSet.get(position);
        ResultViewHolder viewHolder = (ResultViewHolder) holder;
        viewHolder.comicTitle.setText(comic.getTitle());
//        viewHolder.comicSource.setText(mTitleGetter.getTitle(comic.getSource()));
        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(comic.getCover()))
//                .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels, App.mCoverHeightPixels))
                .build();
        viewHolder.comicImage.setController(mProvider.get(comic.getSource()).setImageRequest(request).build());
    }

    public void setProvider(ControllerBuilderProvider provider) {
        mProvider = provider;
    }

    public void setTitleGetter(SourceManager.TitleGetter getter) {
        mTitleGetter = getter;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int offset = parent.getWidth() / 90;
                outRect.set(offset, 0, offset, (int) (2.8 * offset));
            }
        };
    }

}
