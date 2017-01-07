package com.doctor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import static com.doctor.CircleArrowProgress.Status.STATUS_IDLE;
import static com.doctor.CircleArrowProgress.Status.STATUS_REBOUND;
import static com.doctor.CircleArrowProgress.Status.STATUS_RUNNING;
import static com.doctor.CircleArrowProgress.Status.STATUS_STOPPING;


/**
 * desc:
 * created by hjl on 2017/1/3 8:04
 */

public class CircleArrowProgress extends View implements CircleArrowAnima, CircleArrowAttrs {

    private final static String TAG = CircleArrowProgress.class.getSimpleName();

    private Paint mPaint;
    private Paint mSquarePaint;
    private RectF mRectF;
    private float mCircleWidth;

    private int[] mColorSchemeColors = new int[1];
    private int mColorsIndex = 0;

    private boolean mAutoStart;
    private float mStartDegree;
    private float mOffsetDegree = 0f;

    private final float mMinArcDegree = 15f;
    private final float mMaxArcDegree = 295f;
    private final float mMiddleArcDegree = 240f;
    private float mCurrentArcDegree = 0f;

    //三角形箭头的高度, 底边默认是高的2倍
    private float mSquareSize;
    private float mSquareScale = 1f;
    private final float mMaxSquareScale = 0.5f;


    private final int mPeriodDuration = 1500;
    private final int mEnlargeAnimationDuration = 575;
    private final int mShrinkAnimationDuration = 575;
    private final int mAnimationDelayDuration = 175;

    private float mLastUniformValue;
    private float mLastEnlargeValue;
    private float mLastShrinkValue;
    private float mLastReboundValue;
    private float mLastShrinkSquareValue;

    private AnimatorSet mAnimatorSet;
    //圆环扩张动画
    private ValueAnimator mEnlargeAnimator;
    //圆环收缩动画
    private ValueAnimator mShrinkAnimator;

    private Status mStatus = STATUS_IDLE;

    public enum Status {
        STATUS_IDLE, //空闲状态
        STATUS_REBOUND, //从IDLE到RUNNing的过渡状态
        STATUS_RUNNING, //圆环正在旋转状态
        STATUS_STOPPING //正在停止
    }

    public CircleArrowProgress(Context context) {
        this(context, null);
    }

    public CircleArrowProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleArrowProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleArrowProgress, defStyleAttr, 0);
        mCircleWidth = typedArray.getDimension(R.styleable.CircleArrowProgress_CircleWidth, 3f);
        mColorSchemeColors[0] = typedArray.getColor(R.styleable.CircleArrowProgress_CircleColor, Color.BLACK);
        mSquareSize = typedArray.getDimension(R.styleable.CircleArrowProgress_CircleArrowSize, 15f);
        mStartDegree = typedArray.getFloat(R.styleable.CircleArrowProgress_CircleStartDegree, -120f);
        mAutoStart = typedArray.getBoolean(R.styleable.CircleArrowProgress_CircleAutoStart, false);
        typedArray.recycle();
        init();
    }

    private void init() {
        /**
         * 初始化绘制环形的画笔
         */
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(mCircleWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mColorSchemeColors[mColorsIndex]);

        /**
         * 初始化绘制三角箭头的画笔
         */
        mSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSquarePaint.setStrokeWidth(0.5f);
        mSquarePaint.setStyle(Paint.Style.FILL);
        mSquarePaint.setColor(mColorSchemeColors[mColorsIndex]);
        mSquarePaint.setAlpha(0);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createRectF();
    }

    private void createRectF() {
        /**
         * 计算矩形的大小与位置, 使矩形的长宽总是相等
         */
        float size = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(), getHeight() - getPaddingTop() - getPaddingBottom());
        float left = 0f + mCircleWidth + getPaddingLeft();
        float top = 0f + mCircleWidth + getPaddingTop();
        float right = size - mCircleWidth + getPaddingLeft();
        float bottom = size - mCircleWidth + getPaddingTop();

        /**
         * 生成矩形
         */
        mRectF = new RectF(left, top, right, bottom);
    }


    @Override
    public void setPercent(float value) {
        if (mStatus != STATUS_IDLE) {
            return;
        }

        //透明度变化
        int alpha = (int) (255 * value);
        alpha = alpha > 255 ? 255 : alpha;
        alpha = alpha < 100 ? 100 : alpha;
        mPaint.setAlpha(alpha);
        mSquarePaint.setAlpha(alpha);

        //三角形大小变化, 实质是画布的缩放
        mSquareScale = value / 2f > mMaxSquareScale ? mMaxSquareScale : value / 2f;

        //圆环弧度变化
        float degree = value * 1000 / 3;
        mOffsetDegree = mStartDegree;
        mOffsetDegree += degree;
        mCurrentArcDegree = degree > mMaxArcDegree ? mMaxArcDegree : degree;
        postInvalidate();
    }

    @Override
    public void stop() {
        setStatusStopping();
    }

    private void setStatusIdle() {
        mStatus = STATUS_IDLE;
    }

    private void setStatusRebound() {
        if (mStatus != STATUS_STOPPING) mStatus = STATUS_REBOUND;
    }

    private void setStatusRunning() {
        if (mStatus == STATUS_REBOUND) mStatus = STATUS_RUNNING;
    }

    private void setStatusStopping() {
        if (mStatus != STATUS_IDLE) mStatus = STATUS_STOPPING;
    }

    //重置参数
    private void reset() {
        mColorsIndex = 0;
        mPaint.setColor(mColorSchemeColors[0]);
        mSquarePaint.setColor(mColorSchemeColors[0]);
        mOffsetDegree = mStartDegree;
        mCurrentArcDegree = 0;
    }

    @Override
    public void start() {
        if (mStatus != STATUS_IDLE) {
            return;
        }
        setStatusRebound();
        mPaint.setAlpha(255);
        mSquarePaint.setAlpha(255);
        if (mSquareScale > mMaxSquareScale) mSquareScale = 0f;
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet
                .play(getReboundAnimator())
                .with(getUniformAnimator())
                .with(getShrinkSquareAnimator());
        mAnimatorSet.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /**
         * 重置开始角度值
         */
        if (mOffsetDegree > 360f) mOffsetDegree %= 360f;

        /**
         * 绘制矩形内切扇形弧
         */
        canvas.drawArc(mRectF, mOffsetDegree, mCurrentArcDegree, false, mPaint);

        //如果当前的状态为圆环正在旋转, 则不用进行三角形的绘制操作
        if (mStatus == STATUS_RUNNING) {
            return;
        }

        /**
         * 获取一些绘制三角形并正确放置必要参数
         */
        //得到圆的直径, 也就是矩形的边长
        float size = Math.min(mRectF.width(), mRectF.height());
        //计算得出圆弧半径
        float radius = size / 2f;
        //计算得出圆弧当前旋转的笛卡尔坐标系与x轴正方向的角度
        float angle = 360f - (mOffsetDegree + mCurrentArcDegree);
        //得到矩形的中心点, 也就是圆心
        float centerX = mRectF.centerX();
        float centerY = mRectF.centerY();
        //计算出等腰三角形箭头的三个点, 使三角形底边于高的交点位于圆心
        float aPointX = centerX - mSquareSize;
        float aPointY = centerY;
        float bPointX = centerX + mSquareSize;
        float bPointY = centerY;
        float cPointX = centerX;
        float cPointY = centerY - mSquareSize;
        //连接三角形的三个点
        Path squarePath = new Path();
        squarePath.moveTo(aPointX, aPointY);
        squarePath.lineTo(bPointX, bPointY);
        squarePath.lineTo(cPointX, cPointY);
        squarePath.lineTo(cPointX, cPointY);

        /**
         * 得出决定三角形位置的直接参数
         */
        //根据angle计算出画布在x轴及y轴的偏移量
        float offestX = (float) (radius * Math.sin(Math.PI * angle / 180));
        float offestY = (float) (radius * Math.cos(Math.PI * angle / 180));
        //根据圆心坐标及开始角度和扇形角度计算画布需要旋转的角度
        float rotateDegree = mOffsetDegree + mCurrentArcDegree - 180f;

        /**
         * 绘制三角形
         */
        canvas.save();
        canvas.translate(offestY, -offestX);
        canvas.rotate(rotateDegree, centerX, centerY);
        canvas.scale(mSquareScale, mSquareScale, centerX, centerY);
        canvas.drawPath(squarePath, mSquarePaint);
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAutoStart && getVisibility() == VISIBLE) {
            start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        abortAnimation();
        reset();
        super.onDetachedFromWindow();
    }

    private void abortAnimation() {
        if (mAnimatorSet != null && mAnimatorSet.isStarted()) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }
        if (mEnlargeAnimator != null && mEnlargeAnimator.isStarted()) {
            mEnlargeAnimator.cancel();
            mEnlargeAnimator = null;
        }
        if (mShrinkAnimator != null && mShrinkAnimator.isStarted()) {
            mShrinkAnimator.cancel();
            mShrinkAnimator = null;
        }
        setStatusIdle();
    }

    private ValueAnimator getShrinkSquareAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, mSquareScale);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float offsetValue = value - mLastShrinkSquareValue;
                mSquareScale -= offsetValue;
                mLastShrinkSquareValue = value;
            }
        });
        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration((long) Math.abs(mSquareScale * 400));
        mLastShrinkSquareValue = 0f;
        return animator;
    }

    private ValueAnimator getReboundAnimator() {
        final boolean isShrink = mCurrentArcDegree > mMiddleArcDegree;
        float shrinkLength = mCurrentArcDegree - mMinArcDegree;
        float enlargeLength = mMiddleArcDegree - mCurrentArcDegree + mMinArcDegree;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, isShrink ? shrinkLength : enlargeLength);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                float value = (float) animation.getAnimatedValue();
                float offsetValue = value - mLastReboundValue;
                if (isShrink) shrink(offsetValue);
                else enlarge(offsetValue);
                mLastReboundValue = value;
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isShrink) startEnlargeAnimaion();
                else startShrinkAnimation();
            }
        });
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(((long) (isShrink ? shrinkLength : enlargeLength)));
        mLastReboundValue = 0f;
        return animator;
    }

    private void shrink(float offsetValue) {
        mCurrentArcDegree -= offsetValue;
        mOffsetDegree += offsetValue;
    }

    private void enlarge(float offsetValue) {
        mCurrentArcDegree += offsetValue;
    }

    private ValueAnimator getUniformAnimator() {
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float offsetValue = value - mLastUniformValue;
                mOffsetDegree += offsetValue;
                mLastUniformValue = value;
                postInvalidate();
                if (mStatus == STATUS_IDLE) {
                    animator.setRepeatCount(1);
                }
            }
        });
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(mPeriodDuration);
        mLastUniformValue = 0f;
        return animator;
    }

    private void startEnlargeAnimaion() {
        setStatusRunning();
        if (mEnlargeAnimator == null) {
            mEnlargeAnimator = ValueAnimator.ofFloat(0f, mMiddleArcDegree);
            mEnlargeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    float offsetValue = value - mLastEnlargeValue;
                    enlarge(offsetValue);
                    mLastEnlargeValue = value;
                }
            });
            mEnlargeAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mPaint.setColor(mColorSchemeColors[mColorsIndex = ++mColorsIndex % mColorSchemeColors.length]);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    startShrinkAnimation();
                }
            });
            mEnlargeAnimator.setInterpolator(new DecelerateInterpolator());
            mEnlargeAnimator.setDuration(mEnlargeAnimationDuration);
            mEnlargeAnimator.setStartDelay(mAnimationDelayDuration);
        }
        mLastEnlargeValue = 0;
        mEnlargeAnimator.start();
    }

    private void startShrinkAnimation() {
        setStatusRunning();
        if (mShrinkAnimator == null || mStatus == STATUS_STOPPING) {
            if (mStatus == STATUS_STOPPING) {
                mShrinkAnimator = ValueAnimator.ofFloat(0f, mCurrentArcDegree);
            } else {
                mShrinkAnimator = ValueAnimator.ofFloat(0f, mMiddleArcDegree);
            }
            mShrinkAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    float offsetValue = value - mLastShrinkValue;
                    shrink(offsetValue);
                    mLastShrinkValue = value;
                }
            });
            mShrinkAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (mStatus == STATUS_RUNNING) startEnlargeAnimaion();
                    else if (mStatus == STATUS_STOPPING || mStatus == STATUS_IDLE) {
                        abortAnimation();
                        reset();
                    }
                }
            });
            mShrinkAnimator.setInterpolator(new DecelerateInterpolator());
            mShrinkAnimator.setDuration(mShrinkAnimationDuration);
            mShrinkAnimator.setStartDelay(mAnimationDelayDuration);
        }
        mLastShrinkValue = 0;
        mShrinkAnimator.start();
    }

    @Override
    public void setCircleWidth(float width) {
        this.mCircleWidth = width;
        createRectF();
    }

    @Override
    public float getCircleWidth() {
        return mCircleWidth;
    }

    @Override
    public void setCircleSchameColors(int... colors) {
        this.mColorSchemeColors = colors;
        this.mColorsIndex = -1;
    }

    @Override
    public int[] getCircleSchameColors() {
        return mColorSchemeColors;
    }

    @Override
    public void setCircleArrowSize(float size) {
        this.mSquareSize = size;
    }

    @Override
    public float getCircleArrowSize() {
        return mSquareSize;
    }

    @Override
    public void setCircleStartDegree(float degree) {
        this.mStartDegree = degree;
    }

    @Override
    public float getCircleStartDegree() {
        return mStartDegree;
    }

    @Override
    public void setAutoStart(boolean flag) {
        this.mAutoStart = flag;
    }

    @Override
    public boolean isAutoStart() {
        return mAutoStart;
    }
}
