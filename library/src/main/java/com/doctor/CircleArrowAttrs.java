package com.doctor;

/**
 * desc:
 * created by hjl on 2017/1/7 9:20
 */

public interface CircleArrowAttrs {
    /**
     * @param width 圆环的宽度
     */
    void setCircleWidth(float width);
    float getCircleWidth();

    /**
     * @param colors 圆环的颜色数组
     */
    void setCircleSchameColors(int... colors);
    int[] getCircleSchameColors();

    /**
     * @param size 三角形的大小
     */
    void setCircleArrowSize(float size);
    float getCircleArrowSize();

    /**
     * @param degree 开始旋转的角度
     */
    void setCircleStartDegree(float degree);
    float getCircleStartDegree();

    /**
     * @param flag 是否自动自由旋转
     */
    void setAutoStart(boolean flag);
    boolean isAutoStart();
}
