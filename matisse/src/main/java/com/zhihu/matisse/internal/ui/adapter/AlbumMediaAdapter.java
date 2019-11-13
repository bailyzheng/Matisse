/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.ui.widget.MediaGrid;

import java.util.Date;

public class AlbumMediaAdapter extends
        RecyclerViewCursorAdapter<RecyclerView.ViewHolder> implements
        MediaGrid.OnMediaGridClickListener {

    private static final int VIEW_TYPE_CAPTURE = 0x01;
    private static final int VIEW_TYPE_MEDIA = 0x02;
    private final SelectedItemCollection mSelectedCollection;
    private final Drawable mPlaceholder;
    private SelectionSpec mSelectionSpec;
    private CheckStateListener mCheckStateListener;
    private OnMediaClickListener mOnMediaClickListener;
    private RecyclerView mRecyclerView;
    private int mImageResize;
    private Activity mActivity;
    ImageView animView;

    public AlbumMediaAdapter(Activity activity, SelectedItemCollection selectedCollection, RecyclerView recyclerView) {
        super(null);
        mActivity = activity;
        mSelectionSpec = SelectionSpec.getInstance();
        mSelectedCollection = selectedCollection;

        TypedArray ta = activity.getTheme().obtainStyledAttributes(new int[]{R.attr.item_placeholder});
        mPlaceholder = ta.getDrawable(0);
        ta.recycle();

        mRecyclerView = recyclerView;
        animView = new ImageView(mActivity);
        animView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CAPTURE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_capture_item, parent, false);
            CaptureViewHolder holder = new CaptureViewHolder(v);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getContext() instanceof OnPhotoCapture) {
                        ((OnPhotoCapture) v.getContext()).capture();
                    }
                }
            });
            return holder;
        } else if (viewType == VIEW_TYPE_MEDIA) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_grid_item, parent, false);
            return new MediaViewHolder(v);
        }
        return null;
    }

    @Override
    protected void onBindViewHolder(final RecyclerView.ViewHolder holder, Cursor cursor) {
        if (holder instanceof CaptureViewHolder) {
            CaptureViewHolder captureViewHolder = (CaptureViewHolder) holder;
            Drawable[] drawables = captureViewHolder.mHint.getCompoundDrawables();
            TypedArray ta = holder.itemView.getContext().getTheme().obtainStyledAttributes(
                    new int[]{R.attr.capture_textColor});
            int color = ta.getColor(0, 0);
            ta.recycle();

            for (int i = 0; i < drawables.length; i++) {
                Drawable drawable = drawables[i];
                if (drawable != null) {
                    final Drawable.ConstantState state = drawable.getConstantState();
                    if (state == null) {
                        continue;
                    }

                    Drawable newDrawable = state.newDrawable().mutate();
                    newDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    newDrawable.setBounds(drawable.getBounds());
                    drawables[i] = newDrawable;
                }
            }
            captureViewHolder.mHint.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
        } else if (holder instanceof MediaViewHolder) {
            MediaViewHolder mediaViewHolder = (MediaViewHolder) holder;

            final Item item = Item.valueOf(cursor);
            mediaViewHolder.mMediaGrid.preBindMedia(new MediaGrid.PreBindInfo(
                    getImageResize(mediaViewHolder.mMediaGrid.getContext()),
                    mPlaceholder,
                    mSelectionSpec.countable,
                    holder
            ));
            mediaViewHolder.mMediaGrid.bindMedia(item);
            mediaViewHolder.mMediaGrid.setOnMediaGridClickListener(this);
            if (!mSelectionSpec.countable && mSelectionSpec.multiSelectable) {
                mediaViewHolder.mMediaGrid.setSelectViewVisible(false);
            }
            setCheckStatus(item, mediaViewHolder.mMediaGrid);
        }
    }

    private void setCheckStatus(Item item, MediaGrid mediaGrid) {
        if (mSelectionSpec.countable) {
            int checkedNum = mSelectedCollection.checkedNumOf(item);
            if (checkedNum > 0) {
                mediaGrid.setCheckEnabled(true);
                mediaGrid.setCheckedNum(checkedNum);
            } else {
                if (mSelectedCollection.maxSelectableReached()) {
                    mediaGrid.setCheckEnabled(false);
                    mediaGrid.setCheckedNum(CheckView.UNCHECKED);
                } else {
                    mediaGrid.setCheckEnabled(true);
                    mediaGrid.setCheckedNum(checkedNum);
                }
            }
        } else {
            boolean selected = mSelectedCollection.isSelected(item);
            if (selected) {
                mediaGrid.setCheckEnabled(true);
                mediaGrid.setChecked(true);
            } else {
                if (mSelectedCollection.maxSelectableReached()) {
                    mediaGrid.setCheckEnabled(false);
                    mediaGrid.setChecked(false);
                } else {
                    mediaGrid.setCheckEnabled(true);
                    mediaGrid.setChecked(false);
                }
            }
        }
    }

    @Override
    public void onThumbnailClicked(ImageView thumbnail, Item item, RecyclerView.ViewHolder holder) {
        if (!mSelectionSpec.countable && mSelectionSpec.multiSelectable) {
            updateSelectedItem(item, holder);
        } else {
            if (mSelectionSpec.showPreview) {
                if (mOnMediaClickListener != null) {
                    mOnMediaClickListener.onMediaClick(null, item, holder.getAdapterPosition());
                }
            } else {
                updateSelectedItem(item, holder);
            }
        }
    }

    @Override
    public void onCheckViewClicked(CheckView checkView, Item item, RecyclerView.ViewHolder holder) {
        updateSelectedItem(item, holder);
    }

    private void updateSelectedItem(Item item, RecyclerView.ViewHolder holder) {
        if (mSelectionSpec.countable) {
            int checkedNum = mSelectedCollection.checkedNumOf(item);
            if (checkedNum == CheckView.UNCHECKED) {
                if (assertAddSelection(holder.itemView.getContext(), item)) {
                    mSelectedCollection.add(item);
                    notifyCheckStateChanged();
                }
            } else {
                mSelectedCollection.remove(item);
                notifyCheckStateChanged();
            }
        } else {
            if (!mSelectionSpec.multiSelectable && mSelectedCollection.isSelected(item)) {
                mSelectedCollection.remove(item);
                notifyCheckStateChanged();
            } else {
                if (assertAddSelection(holder.itemView.getContext(), item)) {
                    if (mSelectionSpec.multiSelectable) {
                        item.selectTime = new Date().getTime();
                    }
                    mSelectedCollection.add(item);
                    int[] startLocation = new int[2];// 一个整型数组，用来存储按钮的在屏幕的X、Y坐标
                    MediaViewHolder mediaViewHolder = (MediaViewHolder) holder;
                    SelectionSpec.getInstance().imageEngine.loadThumbnail(mActivity, getImageResize(mediaViewHolder.mMediaGrid.getContext()),
                            mPlaceholder, animView, item.getContentUri());
                    holder.itemView.getLocationInWindow(startLocation);// 这是获取购买按钮的在屏幕的X、Y坐标（这也是动画开始的坐标）
                    setAnim(animView, startLocation);
                    notifyCheckStateChanged();
                }
            }
        }
    }

    private void notifyCheckStateChanged() {
        notifyDataSetChanged();
        if (mCheckStateListener != null) {
            mCheckStateListener.onUpdate();
        }
    }

    @Override
    public int getItemViewType(int position, Cursor cursor) {
        return Item.valueOf(cursor).isCapture() ? VIEW_TYPE_CAPTURE : VIEW_TYPE_MEDIA;
    }

    private boolean assertAddSelection(Context context, Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(context, cause);
        return cause == null;
    }


    public void registerCheckStateListener(CheckStateListener listener) {
        mCheckStateListener = listener;
    }

    public void unregisterCheckStateListener() {
        mCheckStateListener = null;
    }

    public void registerOnMediaClickListener(OnMediaClickListener listener) {
        mOnMediaClickListener = listener;
    }

    public void unregisterOnMediaClickListener() {
        mOnMediaClickListener = null;
    }

    public void refreshSelection() {
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first == -1 || last == -1) {
            return;
        }
        Cursor cursor = getCursor();
        for (int i = first; i <= last; i++) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(first);
            if (holder instanceof MediaViewHolder) {
                if (cursor.moveToPosition(i)) {
                    setCheckStatus(Item.valueOf(cursor), ((MediaViewHolder) holder).mMediaGrid);
                }
            }
        }
    }

    private int getImageResize(Context context) {
        if (mImageResize == 0) {
            RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
            int spanCount = ((GridLayoutManager) lm).getSpanCount();
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int availableWidth = screenWidth - context.getResources().getDimensionPixelSize(
                    R.dimen.media_grid_spacing) * (spanCount - 1);
            mImageResize = availableWidth / spanCount;
            mImageResize = (int) (mImageResize * mSelectionSpec.thumbnailScale);
        }
        return mImageResize;
    }

    public interface CheckStateListener {
        void onUpdate();
    }

    public interface OnMediaClickListener {
        void onMediaClick(Album album, Item item, int adapterPosition);
    }

    public interface OnPhotoCapture {
        void capture();
    }

    private static class MediaViewHolder extends RecyclerView.ViewHolder {

        private MediaGrid mMediaGrid;

        MediaViewHolder(View itemView) {
            super(itemView);
            mMediaGrid = (MediaGrid) itemView;
        }
    }

    private static class CaptureViewHolder extends RecyclerView.ViewHolder {

        private TextView mHint;

        CaptureViewHolder(View itemView) {
            super(itemView);

            mHint = (TextView) itemView.findViewById(R.id.hint);
        }
    }

    private ViewGroup anim_mask_layout;
    private void setAnim(final View v, int[] startLocation) {
        if (anim_mask_layout == null) {
            anim_mask_layout = createAnimLayout();
        }
        anim_mask_layout.removeAllViews();
        anim_mask_layout.addView(v);

        final View view = addViewToAnimLayout(anim_mask_layout, v,
                startLocation);
        int[] endLocation = new int[2];// 存储动画结束位置的X、Y坐标
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        int height = display.getHeight();
        endLocation [0] = 100;
        endLocation [1] = height - 100;

        // 计算位移
        int endX = 0 - startLocation[0] + 40;// 动画位移的X坐标
        int endY = endLocation[1] - startLocation[1];// 动画位移的y坐标
        TranslateAnimation translateAnimationX = new TranslateAnimation(0,
                endX, 0, 0);
        translateAnimationX.setInterpolator(new LinearInterpolator());
        translateAnimationX.setRepeatCount(0);// 动画重复执行的次数
        translateAnimationX.setFillAfter(true);

        TranslateAnimation translateAnimationY = new TranslateAnimation(0, 0,
                0, endY);
        translateAnimationY.setInterpolator(new AccelerateInterpolator());
        translateAnimationY.setRepeatCount(0);// 动画重复执行的次数
        translateAnimationX.setFillAfter(true);

        final AnimationSet set = new AnimationSet(false);
        set.setFillAfter(false);
        set.addAnimation(translateAnimationY);
        set.addAnimation(translateAnimationX);
        set.setDuration(400);// 动画的执行时间
        view.startAnimation(set);
        // 动画监听事件
        set.setAnimationListener(new Animation.AnimationListener() {
            // 动画的开始
            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
                //    Log.e("动画","asdasdasdasd");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }

            // 动画的结束
            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
                set.cancel();
                animation.cancel();
            }
        });
    }

    private ViewGroup createAnimLayout() {
        ViewGroup rootView = (ViewGroup) mActivity.getWindow().getDecorView();
        LinearLayout animLayout = new LinearLayout(mActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        animLayout.setLayoutParams(lp);
        animLayout.setId(Integer.MAX_VALUE);
        animLayout.setBackgroundResource(android.R.color.transparent);
        rootView.addView(animLayout);
        return animLayout;
    }

    private View addViewToAnimLayout(final ViewGroup parent, final View view,
                                     int[] location) {
        int x = location[0];
        int y = location[1];
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = x;
        lp.topMargin = y;
        view.setLayoutParams(lp);
        return view;
    }
}
