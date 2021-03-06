package com.giz.museum;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.giz.customize.CardSlideTransformer;
import com.giz.database.MuseumRecord;
import com.giz.database.RecordDB;
import com.giz.customize.CustomToast;
import com.giz.utils.BitmapUtils;
import com.giz.utils.CoverFlowEffectTransformer;

import java.io.File;
import java.util.List;

public class RecordDetailActivity extends AppCompatActivity {

    private static final String TAG = "RecordDetailActivity";
    private static final String EXTRA_NAME = "museumName";

    private ViewPager mViewPager;
    private RecordDetailAdapter mRecordDetailAdapter;
    private TextView mRdTitleTv;
    private ImageView mBackIcon;

    private String mMuseumName;

    public static Intent newIntent(Context context, String museumName) {
        Intent intent = new Intent(context, RecordDetailActivity.class);
        intent.putExtra(EXTRA_NAME, museumName);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindowAnimations();
        setContentView(R.layout.activity_record_detail);
        mMuseumName = getIntent().getStringExtra(EXTRA_NAME);

        mViewPager = findViewById(R.id.rd_pager);
        mRdTitleTv = findViewById(R.id.rd_title);
        mBackIcon = findViewById(R.id.rd_back);

        mRdTitleTv.setText(mMuseumName + "记录集");
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setPageTransformer(true, new CardSlideTransformer());
        initEvents();
        updateView();
    }

    private void initEvents() {
        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setupWindowAnimations() {
        Fade fade = (Fade)TransitionInflater.from(this).inflateTransition(R.transition.activity_fade);
        getWindow().setEnterTransition(fade);
    }

    private class RecordDetailAdapter extends PagerAdapter {

        private List<MuseumRecord> mMuseumRecords;

        private RecordDetailAdapter(List<MuseumRecord> records){
            mMuseumRecords = records;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            final View view = LayoutInflater.from(RecordDetailActivity.this).inflate(R.layout.pager_item_record, null);

            final MuseumRecord record = mMuseumRecords.get(position);
            final TextView content = view.findViewById(R.id.record_content);
            final ImageView contentSwitch = view.findViewById(R.id.record_switch);
            ((ImageView)view.findViewById(R.id.record_picture)).setImageBitmap(BitmapUtils.getBitmapFromPath(record.getPicturePath()));
            ((TextView)view.findViewById(R.id.record_location)).setText(record.getName());
            ((TextView)view.findViewById(R.id.record_date)).setText(record.getRecordDate());
            content.setText(record.getContent());

            ViewTreeObserver contentObserver = content.getViewTreeObserver();
            contentObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    content.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    final int contentHeight = content.getMeasuredHeight();
                    Log.d("RecordActivity", String.valueOf(contentHeight));
                    content.setHeight(0);

                    contentSwitch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(content.getHeight() == 0){
                                AnimatedVectorDrawableCompat drawableCompat = AnimatedVectorDrawableCompat.create(
                                        RecordDetailActivity.this, R.drawable.av_up_to_down);
                                contentSwitch.setImageDrawable(drawableCompat);
                                ((Animatable)contentSwitch.getDrawable()).start();
                                ValueAnimator animator = ObjectAnimator.ofInt(content, "height", 0, contentHeight);
                                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                animator.setDuration(400);
                                animator.start();
                            }else{
                                AnimatedVectorDrawableCompat drawableCompat = AnimatedVectorDrawableCompat.create(
                                        RecordDetailActivity.this, R.drawable.av_down_to_up);
                                contentSwitch.setImageDrawable(drawableCompat);
                                ((Animatable)contentSwitch.getDrawable()).start();
                                ValueAnimator animator = ObjectAnimator.ofInt(content, "height", contentHeight, 0);
                                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                animator.setDuration(400);
                                animator.start();
                            }
                        }
                    });
                }
            });

            (view.findViewById(R.id.record_share_icon)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    view.findViewById(R.id.record_share_icon).setVisibility(View.GONE);
                    view.findViewById(R.id.record_delete_icon).setVisibility(View.GONE);
                    view.setDrawingCacheEnabled(true);
                    view.buildDrawingCache();
                    Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
                    view.setDrawingCacheEnabled(false);
                    view.findViewById(R.id.record_share_icon).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.record_delete_icon).setVisibility(View.VISIBLE);
                    Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(RecordDetailActivity.this.getContentResolver(), bitmap, null, null));

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.setType("image/*");
                    startActivity(Intent.createChooser(shareIntent, "分享记录到"));
                }
            });

            (view.findViewById(R.id.record_delete_icon)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(RecordDetailActivity.this)
                            .setTitle("删除该记录吗")
                            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RecordDB.get(RecordDetailActivity.this).removeMuseumRecord(record.getRecordDate());
                                    File file = new File(record.getPicturePath());
                                    if(file.delete()){
                                        CustomToast.make(RecordDetailActivity.this, "删除成功").show();
                                    }else{
                                        CustomToast.make(RecordDetailActivity.this, "图片未删除").show();
                                    }
//                                    updateView();
                                }
                            })
                            .setNegativeButton("取消", null).show();
                }
            });
            view.findViewById(R.id.record_picture).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityOptionsCompat optionsCompat1 = ActivityOptionsCompat.makeScaleUpAnimation(v,
                            v.getWidth()/2, v.getHeight()/2, 0,0);
                    Log.d(TAG, "onClick: ");
                    startActivity(ImageDetailActivity.newIntent(RecordDetailActivity.this, ((ImageView)v).getDrawable()), optionsCompat1.toBundle());
                }
            });

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            ((ImageView)((View)object).findViewById(R.id.record_switch)).setImageResource(R.drawable.av_up_to_down);
            container.removeView((View)object);
        }

        @Override
        public int getCount() {
            return mMuseumRecords.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return (view == o);
        }

        private void setMuseumRecords(List<MuseumRecord> museumRecords) {
            mMuseumRecords = museumRecords;
        }
    }

    private void updateView() {
        if(mRecordDetailAdapter == null){
            List<MuseumRecord> records = RecordDB.get(this).getMuseumRecordsByName(mMuseumName);
            mRecordDetailAdapter = new RecordDetailAdapter(records);
            mViewPager.setAdapter(mRecordDetailAdapter);
        }else{
            List<MuseumRecord> records = RecordDB.get(this).getMuseumRecordsByName(mMuseumName);
            mRecordDetailAdapter.setMuseumRecords(records);
            mRecordDetailAdapter.notifyDataSetChanged();
        }
    }

}
