package com.yuyang.wirelesstransmission;

/**
 * Created by vista on 2017/9/28.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

public class CircleProgress extends View{
    public synchronized void setProgress(long progress){
        if(progress>=0&&progress<=max) {
            this.progress = progress;
            super.invalidate();
        }
    }
    public void setStrokeWidth(int width){
        strokeWidth_r=width;
    }
    public synchronized void setMax(long max){
        this.max=max;
    }
    public synchronized void setFgColor(int color){
        fgColor=color;
        super.invalidate();
    }
    public long getMax(){
        return max;
    }
    private void initProperty(AttributeSet attrs){
        TypedArray tArray= context.obtainStyledAttributes(attrs,R.styleable.CircleProgressBar);
        mR=tArray.getInteger(R.styleable.CircleProgressBar_r,getWidth()/2);
        bgColor=tArray.getColor(R.styleable.CircleProgressBar_bgColor, ContextCompat.getColor(context,R.color.cpbg));
        fgColor=tArray.getColor(R.styleable.CircleProgressBar_fgColor, ContextCompat.getColor(context,R.color.cpfg));
        drawStyle=tArray.getInt(R.styleable.CircleProgressBar_drawStyle, 0);
        progress=tArray.getInt(R.styleable.CircleProgressBar_progress,50);
        strokeWidth_r=tArray.getInteger(R.styleable.CircleProgressBar_strokeWidth,45);
        max=tArray.getInteger(R.styleable.CircleProgressBar_max, 100);
    }
    public CircleProgress(Context context,AttributeSet attrs){
        super(context,attrs);
        this.context=context;
        this.paint=new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setStyle(Paint.Style.STROKE);
        initProperty(attrs);
    }
    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int center=getWidth()/2;
        int strokeWidth= getWidth()/strokeWidth_r;
        mR=getWidth()/2-strokeWidth/2;
        this.paint.setColor(bgColor);
        this.paint.setStrokeWidth(strokeWidth);
        canvas.drawCircle(center, center, mR, this.paint);
        this.paint.setColor(fgColor);
        if(drawStyle==0){
            this.paint.setStyle(Paint.Style.STROKE);
            opt=false;
        }else{
            this.paint.setStyle(Paint.Style.FILL);
            opt=true;
        }
        int top=0;
        int bottom=getWidth();
        RectF oval = new RectF((float) (top+strokeWidth*1.45), (float)(top+strokeWidth*1.45),(float)(bottom-strokeWidth*1.45), (float)(bottom-strokeWidth*1.45));
        canvas.drawArc(oval,270,360*((float)progress/max),opt,paint);
    }

    protected void onMeasure(int paramInt1, int paramInt2)
    {
        super.onMeasure(paramInt2, paramInt2);
    }


    private long progress;
    private int mR;
    private int bgColor;
    private int fgColor;
    private int drawStyle;
    private int strokeWidth_r;
    private long max;
    private Context context;
    private Paint paint;
    private boolean opt;
}