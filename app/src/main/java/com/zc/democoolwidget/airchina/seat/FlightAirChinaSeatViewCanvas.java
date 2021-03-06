package com.zc.democoolwidget.airchina.seat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.zc.democoolwidget.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by NEU on 2018/5/3.
 * 值机的新机型图
 * cols   列结构
 tourClass  仓位
 planeType  机型
 rows   一行座位的集合
 rownum   座位行号   EMPTY：代表空行，LAGA：代表卫生间餐食   COLS: 代表列标行     其他：具体行号代表普通座位
 wingFlag   此行座位所处的机型位置    0：机翼外  1： 机翼开始 2：机翼内部 3：机翼结束
 des       0：代表正常显示 1：代表延后半行显示
 display   “ ”：代表座位上显示空  其他：代表座位上显示内容
 optional  当前位置的属性    empty:代表无座位空 LA：盥洗室 GA：厨房 =:过道 E：安全出口  * 可以选择的座位 $付钱的可选座位
 seatType  当前行座位的类型  1：头等公务类型座椅  2：普通类型座椅
 */

public class FlightAirChinaSeatViewCanvas extends View {


    private Paint mPaintBitmap;
    private Paint mPaintOther;
    private Paint mPaintText;
    private Paint mPaintLine;
    private Paint mPaintBigLine;
    /**座位之间的间距是座位的几分之几*/
    private static final float SEAT_HOW_HOW_LENGTH = 6;
    /**可选的座位*/
    private Map<String, RectF> mSeats = new HashMap<>();
    /**选中座位  通过选择的乘机人position存储*/
    private List<String> mSeatSelecting = new ArrayList<>();
    /**预选座位*/
    private List<String> bookSeatNoList = new ArrayList<>();
    /**不可选座位*/
    private List<String> mSeatSelected = new ArrayList<>();

    /**每一行的合并单元个数*/
    private float leftAllIndex = 0;
    /**图片的所有集合  集合的key  通过图片名+大小命名  做个缓存*/
    private Map<String,Object> bitmapAllInfo = new HashMap<>();
    /**view的宽度*/
    private int viewWidth;
    /**座位图的list*/
    private List<Map<String,Object>> seatList;
    /**当前选中的值机乘客的position*/
    private int indexCurrentPerson = 0;
    /**第一个可选座位的坐标*/
    private float firstSelectSeatPosition = 0;
    /**飞机边框的左右边距离*/
    private float paddingLeftRight = 0;
    /**飞机边框的左右边距离大线条*/
    private float paddingLeftRightBig = 0;
    /**飞机座位的的左右边距离  距离手机屏幕左右*/
    private float paddingLeftRightSeat = 0;
    /**飞机头的大小 根据屏幕宽度来适配*/
    private int start_position_y = 0;
    /**是否可取消座位*/
    private boolean isCanCancelSeat = false;
    /**  所有值机大空间座位信息*/
    public Map<String,Object> big_space_seat;
    public FlightAirChinaSeatViewCanvas(Activity context, List<Map<String,Object>> seatList, List<String> bookingSeatList, boolean isCanCancelSeat) {
        super(context);
        this.seatList = seatList;
        big_space_seat=new HashMap<>();
        bookSeatNoList = bookingSeatList;
        this.isCanCancelSeat = isCanCancelSeat;
        mSeatSelecting.addAll(bookingSeatList);
        if (mSeatSelecting.size()==0) {//没有选中的  默认选中""
            mSeatSelecting.add("");
        }
        viewWidth = context.getWindowManager().getDefaultDisplay().getWidth();
        start_position_y = viewWidth * 4/5;
        paddingLeftRight = viewWidth/14;
        paddingLeftRightBig = paddingLeftRight + dip2px(5);
        paddingLeftRightSeat = paddingLeftRight + dip2px(15);

        getViewHeight();
        initPaint();
    }

    public FlightAirChinaSeatViewCanvas(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public FlightAirChinaSeatViewCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private int dip2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int sp2px(float spValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    private void initPaint() {
        mPaintBitmap = new Paint();
        mPaintBitmap.setAntiAlias(true);
        mPaintBitmap.setColor(Color.WHITE);
        mPaintBitmap.setStyle(Paint.Style.FILL);

        mPaintText = new Paint();
        mPaintText.setColor(Color.GRAY);
        mPaintText.setAntiAlias(true);
        mPaintText.setTextSize(sp2px(14));
        mPaintText.setStyle(Paint.Style.FILL);

        mPaintBigLine = new Paint();
        mPaintBigLine.setColor(Color.parseColor("#BEBEBE"));
        mPaintBigLine.setAntiAlias(true);
        mPaintBigLine.setStrokeWidth(dip2px(4));
        mPaintBigLine.setStyle(Paint.Style.STROKE);
        mPaintBigLine.setStrokeCap(Paint.Cap.ROUND);

        mPaintLine = new Paint();
        mPaintLine.setColor(Color.GRAY);
        mPaintLine.setAntiAlias(true);
        mPaintLine.setStrokeWidth(dip2px(1));
        mPaintLine.setStyle(Paint.Style.STROKE);


        mPaintOther = new Paint();
        mPaintOther.setAntiAlias(true);
        mPaintOther.setColor(Color.rgb(138, 138, 138));
        mPaintOther.setStyle(Paint.Style.FILL);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(viewWidth, (int) viewHeightPoint + dip2px(350));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawView(canvas);
    }

    enum SeatState {
        Normal, Selected, Selecting
    }


    /**
     * @param i 行
     * @param j 列
     * @param seatMapInfo 座位信息
     */
    private void setSeat(int i,int j, Canvas canvas, float seatWH,Map<String, Object> seatMapInfo, boolean isShowColTitle) {
        String colTitle = MapUtils.getObject(seatMapInfo.get("colTitle"));
        String seatType = MapUtils.getObject(seatMapInfo.get("seatType"));
        String optional = MapUtils.getObject(seatMapInfo.get("optional"));//empty:代表无座位空 LA：盥洗室 GA：厨房 =:过道 E：安全出口  * 可以选择的座位 $付钱的可选座位
        String rownum = MapUtils.getObject(seatMapInfo.get("rownum"));
        String des = MapUtils.getObject(seatMapInfo.get("des"));//0：代表正常显示 1：代表延后半行显示
        String display = MapUtils.getObject(seatMapInfo.get("display"));//“ ”：代表座位上显示空  其他：代表座位上显示内容
        String seatNO = MapUtils.getObject(seatMapInfo.get("seatNO"));
        String priceLevel = MapUtils.getObject(seatMapInfo.get("priceLevel"));//大空间座位的显示颜色

        float top = i * (seatWH + dip2px(16)) + dip2px(20) + lastIndexPosition;
        if ("1".equals(des)) {//1：代表延后半行显示
            top = top + seatWH/2;
        } else if (isShowColTitle && j==0) {//画头等舱上面显示的ABC  第一个位置计算显示的距离是否需要
            float fontPosition = getFontHeight(mPaintText, colTitle);
            top = top + fontPosition;
            lastIndexPosition = lastIndexPosition + fontPosition;
        }
        float left =  (j + leftAllIndex) * seatWH + paddingLeftRightSeat+ (j + leftAllIndex + 1) * seatWH/SEAT_HOW_HOW_LENGTH;
        if ("=".equals(colTitle)) {//过道
            if ("E".equals(seatNO)) {//紧急出口
                canvas.drawBitmap(getSeatBitmap(seatWH, SeatState.Normal, "E"),
                        left,
                        top,
                        mPaintBitmap
                );
            } else {
                canvas.drawText( rownum, left + seatWH/2 - getFontLength(mPaintText, rownum)/2, top + seatWH/2f + getFontHeight(mPaintText, rownum)/2, mPaintText);
            }
        } else {
            if (isShowColTitle) {//画头等舱上面显示的ABC
                canvas.drawText( colTitle, left + seatWH/2f - getFontLength(mPaintText, colTitle)/2, top - dip2px(10), mPaintText);
            }

            RectF sRectF = new RectF(left, top, left + seatWH, top + seatWH);
            if (null!=seatMapInfo.get("optional") && TextUtils.isEmpty(optional)) {//不显示
                return;
            }
            if (!TextUtils.isEmpty(seatNO) && bookSeatNoList.size()>0 && bookSeatNoList.contains(seatNO)) {//预选座位
                if (firstSelectSeatPosition==0) {
                    firstSelectSeatPosition = top;
                }
                mSeats.put(seatNO, sRectF);
            } else if (!TextUtils.isEmpty(seatNO) && !"*".equals(optional) && !"$".equals(optional) && !"=".equals(seatNO)) {//不可选的  紧急出口
                if (!mSeatSelected.contains(seatNO)) {
                    mSeatSelected.add(seatNO);
                }
            } else if (!TextUtils.isEmpty(seatNO) && !"=".equals(seatNO) && !"E".equals(seatNO)) {//可选的座位
                if (firstSelectSeatPosition==0) {
                    firstSelectSeatPosition = top;
                }
                mSeats.put(seatNO, sRectF);
            }

            if (!TextUtils.isEmpty(seatNO)) {
                if (mSeatSelected.contains(seatNO)) {//不可选的座位
                    canvas.drawBitmap(getSeatBitmap(seatWH, SeatState.Selected, seatType)
                            , left,
                            top,
                            mPaintBitmap
                    );
                } else if (mSeatSelecting.contains(seatNO)) {//选中的座位
                    canvas.drawBitmap(getSeatBitmap(seatWH, SeatState.Selecting, seatType)
                            , left,
                            top,
                            mPaintBitmap
                    );

                } else {//可选的座位
                    if ("E".equals(seatNO)) {//紧急出口
                        canvas.drawBitmap(getSeatBitmap(seatWH, SeatState.Normal, "E"),
                                left,
                                top,
                                mPaintBitmap
                        );
                    } else {
                        if ("$".equals(optional)) {//大空间座位
                            big_space_seat.put(seatNO,seatMapInfo);
                            canvas.drawBitmap(getBigBitmap(seatWH, MapUtils.getObject(seatMapInfo.get("seatColor")), seatType),
                                    left,
                                    top,
                                    mPaintBitmap
                            );
                        } else {
                            canvas.drawBitmap(getSeatBitmap(seatWH, SeatState.Normal, seatType),
                                    left,
                                    top,
                                    mPaintBitmap
                            );
                        }
                    }
                }
            }



            if (!TextUtils.isEmpty(display)) {
                mPaintOther.setColor(Color.BLACK);
                mPaintOther.setStyle(Paint.Style.FILL);
                mPaintOther.setTextSize(seatWH / 2f);
                canvas.drawText(
                        display, left + seatWH / 2f - getFontLength(mPaintOther, display) / 2,
                        top + seatWH / 2f + getFontHeight(mPaintOther, display) / 2,
                        mPaintOther
                );
            }

        }

    }

    /**画厕所餐食
     * @param i 行
     * @param j 列
     * @param seatMapInfo 座位信息
     *laGaLength 表示厕所餐食合并占的座位数
     * */
    private void setWCFood(int i, int j, Canvas canvas, float seatWH,Map<String, Object> seatMapInfo) {
        float top = i * (seatWH + dip2px(16)) + dip2px(20) + lastIndexPosition;
        float left =  (j + leftAllIndex) * seatWH + paddingLeftRightSeat +(j + leftAllIndex + 1) * seatWH/SEAT_HOW_HOW_LENGTH;
        String optional = MapUtils.getObject(seatMapInfo.get("optional"));

        if (!"=".equals(optional) && !TextUtils.isEmpty(optional)) {//过道
            float laGaLength = 1;
            try {
                if (optional.contains(".")) {
                    laGaLength = Float.parseFloat(getNumberString(optional));
                } else {
                    laGaLength = Integer.parseInt(getNumberString(optional));
                }
            }catch (Exception e){}
            leftAllIndex =  leftAllIndex + laGaLength -1;
            RectF rectFLAGA = new RectF(left, top, left + laGaLength*seatWH  + (laGaLength-1)*seatWH/SEAT_HOW_HOW_LENGTH, top + seatWH);

            mPaintOther.setStyle(Paint.Style.STROKE);
            mPaintOther.setColor(Color.GRAY);
            canvas.drawRoundRect(rectFLAGA,dip2px(5),dip2px(5), mPaintOther);

            Bitmap lagaBitmap = getLAGABitmap(seatWH / 2, optional);
            canvas.drawBitmap(lagaBitmap,
                    rectFLAGA.centerX()-seatWH/2/2,
                    top+ Math.abs(seatWH-lagaBitmap.getHeight()) / 2,
                    mPaintOther);
        }
    }

    private String getNumberString (String matcher) {
        matcher = matcher.replaceAll("[a-zA-Z]","" );
        return matcher;
    }
    /**上个舱位的最后坐标*/
    private float lastIndexPosition = 540;
    /**view的最后一行的坐标*/
    private float viewHeightPoint = 0;
    //四条飞机线
    private Path pathBigLeftLine = new Path();
    private Path pathBigRightLine = new Path();
    private Path pathLeftLine = new Path();
    private Path pathRightLine = new Path();
    /**画舱位*/
    private void drawView(Canvas canvas) {
        mSeats.clear();
        lastIndexPosition = start_position_y;

        // 画飞机头
        RectF ovalHead = new RectF(paddingLeftRight, dip2px(10),viewWidth - paddingLeftRight,start_position_y *2);
        canvas.drawArc(ovalHead, 180, 180, false, mPaintLine);//小弧形
        RectF ovalHeadBig = new RectF(paddingLeftRightBig, dip2px(15),viewWidth -paddingLeftRightBig,start_position_y *2);
        canvas.drawArc(ovalHeadBig, 180, 180, false, mPaintBigLine);//大弧形
        RectF ovalIn = new RectF(paddingLeftRightBig, dip2px(80)-start_position_y *2,viewWidth - paddingLeftRightBig ,dip2px(75));
        canvas.drawArc(ovalIn, 66, 48, false, mPaintLine);//里面的小弧形

        float wingFlagEndPositionY = 0;

        //座位相关的逻辑
        for (int index = 0; index < seatList.size();index++) {//不同舱位的List
            Map<String, Object> seatMapInfo = seatList.get(index);
            String cols = MapUtils.getObject(seatMapInfo.get("cols"));//ABC显示
            int length = cols.length();
            List<Map<String,Object>> rowsList = (List<Map<String, Object>>) seatMapInfo.get("rows");//行数
            float seatSize = 1 + ((length + 1) / (length * SEAT_HOW_HOW_LENGTH));//一行显示的座位个数
            float seatWH = ((viewWidth - paddingLeftRightSeat * 2) / (length * (seatSize)));//座位的大小
            boolean isShowColTitle = true;//是否显示title  ABC
            for (int i = 0; i < rowsList.size(); i++) {//行
                Map<String, Object> rowsMapInfo = rowsList.get(i);
                String wingFlag = MapUtils.getObject(rowsMapInfo.get("wingFlag"));
                String des = "";//是否向下移动半个位置的标识符
                List<Map<String,Object>> rowList = (List<Map<String, Object>>) rowsMapInfo.get("row");
                if (null!=rowList && rowList.size()>0) {
                    String rownum = MapUtils.getObject(rowsMapInfo.get("rownum"));
                    leftAllIndex = 0;//清除上个行的合并个数
                    for (int j = 0; j < rowList.size(); j++) {//每行的列
                        Map<String, Object> rowMapInfo = rowList.get(j);
                        String optional = MapUtils.getObject(rowMapInfo.get("optional"));
                        if ("LAGA".equals(rownum) || optional.contains("LA")|| optional.contains("GA") || optional.contains("BB")
                                || optional.contains("ST")) {//厕所餐食 宝宝摇篮 楼梯
                            setWCFood(i, j, canvas, seatWH, rowMapInfo);
                        } else {
                            String colstring = cols.substring(j+(int)leftAllIndex, j +(int)leftAllIndex+ 1);
                            rowMapInfo.put("colTitle",colstring);
                            rowMapInfo.put("rownum",rownum);
                            rowMapInfo.put("seatType",MapUtils.getObject(rowsMapInfo.get("seatType")));
                            rowMapInfo.put("wingFlag",wingFlag);//此行座位所处的机型位置    0：机翼外  1： 机翼开始 2：机翼内部 3：机翼结束
                            setSeat(i, j, canvas, seatWH, rowMapInfo, isShowColTitle);
                            if (j+1==rowList.size()) {//座位第一排显示
                                isShowColTitle = false;
                            }

                            String desInfo = MapUtils.getObject(rowMapInfo.get("des"));//0：代表正常显示 1：代表延后半行显示
                            if ("1".equals(desInfo) && TextUtils.isEmpty(des)) {//1：代表延后半行显示
                                des = desInfo;
                            }
                        }
                    }
                }

                //这里是画机翅膀逻辑
                if ("1".equals(wingFlag)) {//1： 机翼开始
                    float indexPosition = i * (seatWH + dip2px(16)) + 1*seatWH + lastIndexPosition;
                    if ("1".equals(des)) {//1：代表延后半行显示
                        indexPosition = indexPosition + seatWH / 2;
                    }

                    //左边的 机翅之前
                    canvas.drawLine(paddingLeftRightBig,start_position_y,paddingLeftRightBig,indexPosition+dip2px(5),mPaintBigLine);
                    canvas.drawLine(paddingLeftRightBig,indexPosition+dip2px(5),0,indexPosition + seatWH/2+dip2px(8),mPaintBigLine);
                    canvas.drawLine(paddingLeftRight,start_position_y,paddingLeftRight,indexPosition,mPaintLine);
                    canvas.drawLine(paddingLeftRight,indexPosition,0,indexPosition + seatWH/2,mPaintLine);
                    //右边的 机翅之前
                    canvas.drawLine(viewWidth -paddingLeftRightBig,start_position_y,viewWidth -paddingLeftRightBig,indexPosition+dip2px(5),mPaintBigLine);
                    canvas.drawLine(viewWidth -paddingLeftRightBig,indexPosition+dip2px(5),viewWidth,indexPosition + seatWH/2+dip2px(8),mPaintBigLine);
                    canvas.drawLine(viewWidth -paddingLeftRight,start_position_y,viewWidth -paddingLeftRight,indexPosition,mPaintLine);
                    canvas.drawLine(viewWidth -paddingLeftRight,indexPosition,viewWidth,indexPosition + seatWH/2,mPaintLine);

                } else if ("3".equals(wingFlag)) {//3： 机翼结束
                    wingFlagEndPositionY = i * (seatWH + dip2px(16)) + 2*seatWH + lastIndexPosition;
                    if ("1".equals(des)) {//1：代表延后半行显示
                        wingFlagEndPositionY = wingFlagEndPositionY + seatWH / 2;
                    }

                    canvas.drawLine(0, wingFlagEndPositionY + seatWH/2,paddingLeftRightBig,wingFlagEndPositionY,mPaintBigLine);
                    canvas.drawLine(viewWidth, wingFlagEndPositionY + seatWH/2,viewWidth - paddingLeftRightBig,wingFlagEndPositionY,mPaintBigLine);
                    canvas.drawLine(0,wingFlagEndPositionY + seatWH/2 + dip2px(8),paddingLeftRight,wingFlagEndPositionY+ dip2px(10),mPaintLine);
                    canvas.drawLine(viewWidth,wingFlagEndPositionY + seatWH/2+ dip2px(8),viewWidth -paddingLeftRight,wingFlagEndPositionY+ dip2px(10),mPaintLine);

                }

                boolean isLastIndex = false;
                if ((i+1)==rowsList.size()) {
                    isLastIndex = true;
                }
                if (isLastIndex) {
                    lastIndexPosition = (i+1) * (seatWH + dip2px(16)) + paddingLeftRightBig + lastIndexPosition;
                }
            }
        }

        int padding = 0;
        if (wingFlagEndPositionY==0) {//没有机翅的时候从最初机头位置画线
            wingFlagEndPositionY =  start_position_y;
        } else {//有飞机翅膀  这时候飞机的机翅结束的地方  外面的细线需要向下移动一段距离  不然就和粗线重合了
            padding = dip2px(10);
        }
        //  这里画飞机边框
        canvas.drawLine(paddingLeftRightBig,wingFlagEndPositionY,paddingLeftRight +dip2px(5), viewHeightPoint,mPaintBigLine);//左边big
        canvas.drawLine(viewWidth -paddingLeftRightBig,wingFlagEndPositionY,viewWidth -paddingLeftRightBig, viewHeightPoint,mPaintBigLine);//右边big
        canvas.drawLine(paddingLeftRight,wingFlagEndPositionY + padding,paddingLeftRight, viewHeightPoint,mPaintLine);//左边
        canvas.drawLine(viewWidth - paddingLeftRight,wingFlagEndPositionY + padding,viewWidth -paddingLeftRight, viewHeightPoint,mPaintLine);//右边


        drawLastBottom(canvas);
    }

    /**画飞机屁股的弧线直线*/
    private void drawLastBottom (Canvas canvas) {

        pathLeftLine.reset();
        pathRightLine.reset();
        pathBigLeftLine.reset();
        pathBigRightLine.reset();
        //画飞机屁股边框
        pathBigLeftLine.moveTo(paddingLeftRightBig, viewHeightPoint);
        pathBigLeftLine.quadTo(paddingLeftRightBig,viewHeightPoint +dip2px(190) ,viewWidth/2,viewHeightPoint + dip2px(325));
        pathLeftLine.moveTo(paddingLeftRight, viewHeightPoint);
        pathLeftLine.quadTo(paddingLeftRight,viewHeightPoint +dip2px(191) ,viewWidth/2,viewHeightPoint + dip2px(330));

        pathBigRightLine.moveTo(viewWidth -paddingLeftRightBig, viewHeightPoint);
        pathBigRightLine.quadTo(viewWidth -paddingLeftRightBig,viewHeightPoint +dip2px(190) ,viewWidth/2-dip2px(1),viewHeightPoint + dip2px(325));
        pathRightLine.moveTo(viewWidth -paddingLeftRight, viewHeightPoint);
        pathRightLine.quadTo(viewWidth -paddingLeftRight,viewHeightPoint +dip2px(191) ,viewWidth/2,viewHeightPoint + dip2px(330));

        // 画飞机屁股里面竖着的弧线直线
        pathLeftLine.moveTo(viewWidth/2,viewHeightPoint + dip2px(326));
        pathLeftLine.lineTo(viewWidth/2,viewHeightPoint + dip2px(26));
        pathLeftLine.quadTo(viewWidth/2 - dip2px(30),viewHeightPoint + dip2px(150),viewWidth/2,viewHeightPoint+ dip2px(326));
        pathLeftLine.quadTo(viewWidth/2 + dip2px(30),viewHeightPoint + dip2px(150),viewWidth/2,viewHeightPoint+ dip2px(26));
        //画左边弧线
        pathLeftLine.moveTo((float) (paddingLeftRightBig * (1+0.3)),viewHeightPoint+dip2px(80));
        pathLeftLine.quadTo(viewWidth/4 - dip2px(20), viewHeightPoint+dip2px(140),viewWidth/2 - dip2px(15),viewHeightPoint + dip2px(140));
        pathLeftLine.moveTo((float) (paddingLeftRightBig * (1+0.9)),viewHeightPoint+dip2px(145));
        pathLeftLine.quadTo(viewWidth/4 - dip2px(10), viewHeightPoint+dip2px(180),viewWidth/2 - dip2px(14),viewHeightPoint + dip2px(180));
        pathLeftLine.moveTo((float) (paddingLeftRightBig * (1+1.7)),viewHeightPoint+dip2px(200));
        pathLeftLine.quadTo(viewWidth/4 + dip2px(10), viewHeightPoint+dip2px(220),viewWidth/2 - dip2px(12),viewHeightPoint + dip2px(220));
        //画右边弧线
        pathLeftLine.moveTo(viewWidth - (float) (paddingLeftRightBig * (1+0.3)),viewHeightPoint+dip2px(80));
        pathLeftLine.quadTo(viewWidth * 3/4 + dip2px(20), viewHeightPoint+dip2px(140),viewWidth/2 + dip2px(15),viewHeightPoint + dip2px(140));
        pathLeftLine.moveTo(viewWidth - (float) (paddingLeftRightBig * (1+0.9)),viewHeightPoint+dip2px(145));
        pathLeftLine.quadTo(viewWidth* 3/4 + dip2px(10), viewHeightPoint+dip2px(180),viewWidth/2 + dip2px(14),viewHeightPoint + dip2px(180));
        pathLeftLine.moveTo(viewWidth - (float) (paddingLeftRightBig * (1+1.7)),viewHeightPoint+dip2px(200));
        pathLeftLine.quadTo(viewWidth* 3/4 - dip2px(10), viewHeightPoint+dip2px(220),viewWidth/2 + dip2px(12),viewHeightPoint + dip2px(220));
        //画飞机屁股翅膀
        pathLeftLine.moveTo(0,viewHeightPoint+dip2px(140));
        pathLeftLine.lineTo((float) (paddingLeftRight * (1+0.45)),viewHeightPoint+dip2px(100));
        pathRightLine.moveTo(viewWidth,viewHeightPoint+dip2px(140));
        pathRightLine.lineTo(viewWidth - (float) (paddingLeftRight * (1+0.45)),viewHeightPoint+dip2px(100));

        pathLeftLine.moveTo(0,viewHeightPoint+dip2px(230));
        pathLeftLine.lineTo((float) (paddingLeftRight * (1+1.75)),viewHeightPoint+dip2px(190));
        pathRightLine.moveTo(viewWidth,viewHeightPoint+dip2px(230));
        pathRightLine.lineTo(viewWidth- (float) (paddingLeftRight * (1+1.75)),viewHeightPoint+dip2px(190));

        pathLeftLine.moveTo(0,viewHeightPoint+dip2px(300));
        pathLeftLine.lineTo((float) (paddingLeftRight * (1+3.5)),viewHeightPoint+dip2px(260));
        pathRightLine.moveTo(viewWidth,viewHeightPoint+dip2px(300));
        pathRightLine.lineTo(viewWidth - (float) (paddingLeftRight * (1+3.5)),viewHeightPoint+dip2px(260));

        canvas.drawPath(pathBigLeftLine,mPaintBigLine);
        canvas.drawPath(pathBigRightLine,mPaintBigLine);
        canvas.drawPath(pathLeftLine,mPaintLine);
        canvas.drawPath(pathRightLine,mPaintLine);
    }

    private Bitmap setBitmapSize(int iconId, float w) {
        Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), iconId);
        float s = w / bitmap.getWidth();
        bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * s),(int) (bitmap.getHeight() * s), true);
        return bitmap;
    }

    /***
     * @param seatType 1：头等公务类型座椅  2：普通类型座椅
     */
    private Bitmap getSeatBitmap(float width, SeatState type, String seatType) {
        if ("E".equals(seatType)) {//紧急出口
            Bitmap mBitmap_EXIT = (Bitmap) bitmapAllInfo.get("icon_exit" + width);
            if (mBitmap_EXIT == null) {
                mBitmap_EXIT = setBitmapSize(R.drawable.icon_exit, width);
                bitmapAllInfo.put("icon_exit" + width,mBitmap_EXIT);
            }
            return mBitmap_EXIT;
        } else  if ("1".equals(seatType)) {//1：头等公务类型座椅
            if (type == SeatState.Normal) {
                Bitmap mBitmapFirstSeatNormal = (Bitmap) bitmapAllInfo.get("icon_seat_yellow" + width);
                if (mBitmapFirstSeatNormal == null) {
                    mBitmapFirstSeatNormal = setBitmapSize(R.drawable.icon_seat_yellow, width);
                    bitmapAllInfo.put("icon_seat_yellow" + width,mBitmapFirstSeatNormal);
                }
                return mBitmapFirstSeatNormal;
            } else if (type == SeatState.Selected) {
                Bitmap mBitmapFirstSeatSelected = (Bitmap) bitmapAllInfo.get("icon_seat_gray" + width);
                if (mBitmapFirstSeatSelected == null) {
                    mBitmapFirstSeatSelected = setBitmapSize(R.drawable.icon_seat_gray, width);
                    bitmapAllInfo.put("icon_seat_gray" + width,mBitmapFirstSeatSelected);
                }
                return mBitmapFirstSeatSelected;

            } else if (type == SeatState.Selecting) {
                Bitmap mBitmapFirstSeatSelecting = (Bitmap) bitmapAllInfo.get("icon_seat_blue" + width);
                if (mBitmapFirstSeatSelecting == null) {
                    mBitmapFirstSeatSelecting = setBitmapSize(R.drawable.icon_seat_blue, width);
                    bitmapAllInfo.put("icon_seat_blue" + width,mBitmapFirstSeatSelecting);
                }
                return mBitmapFirstSeatSelecting;
            }
        } else {
            if (type == SeatState.Normal) {
                Bitmap mBitmapSeatNormal = (Bitmap) bitmapAllInfo.get("icon_rect_yellow" + width);
                if (mBitmapSeatNormal == null) {
                    mBitmapSeatNormal = setBitmapSize(R.drawable.icon_rect_yellow, width);
                    bitmapAllInfo.put("icon_rect_yellow" + width,mBitmapSeatNormal);
                }
                return mBitmapSeatNormal;
            } else if (type == SeatState.Selected) {
                Bitmap mBitmapSeatSelected = (Bitmap) bitmapAllInfo.get("icon_rect_gray" + width);
                if (mBitmapSeatSelected == null) {
                    mBitmapSeatSelected = setBitmapSize(R.drawable.icon_rect_gray, width);
                    bitmapAllInfo.put("icon_rect_gray" + width,mBitmapSeatSelected);
                }
                return mBitmapSeatSelected;

            } else if (type == SeatState.Selecting) {
                Bitmap mBitmapSeatSelecting = (Bitmap) bitmapAllInfo.get("icon_rect_blue" + width);
                if (mBitmapSeatSelecting == null) {
                    mBitmapSeatSelecting = setBitmapSize(R.drawable.icon_rect_blue, width);
                    bitmapAllInfo.put("icon_rect_blue" + width,mBitmapSeatSelecting);
                }
                return mBitmapSeatSelecting;
            }
        }

        return null;
    }


    /**洗手间餐食图标*/
    private Bitmap getLAGABitmap (float width, String type) {
        if (type.contains("LA")) {//洗手间
            Bitmap mBitmapLA = (Bitmap) bitmapAllInfo.get("icon_wc" + width);
            if (mBitmapLA == null) {
                mBitmapLA = setBitmapSize(R.drawable.icon_wc, width);
                bitmapAllInfo.put("icon_wc" + width,mBitmapLA);
            }
            return mBitmapLA;
        } else if (type.contains("BB")) {//婴儿摇篮
            Bitmap mBitmapBB = (Bitmap) bitmapAllInfo.get("icon_bb" + width);
            if (mBitmapBB == null) {
                mBitmapBB = setBitmapSize(R.drawable.icon_meal, width);
                bitmapAllInfo.put("icon_bb" + width,mBitmapBB);
            }
            return mBitmapBB;
        } else if (type.contains("ST")) {//楼梯
            Bitmap mBitmapST = (Bitmap) bitmapAllInfo.get("icon_st" + width);
            if (mBitmapST == null) {
                mBitmapST = setBitmapSize(R.drawable.icon_meal, width);
                bitmapAllInfo.put("icon_st" + width,mBitmapST);
            }
            return mBitmapST;
        } else {
            Bitmap mBitmapGA = (Bitmap) bitmapAllInfo.get("icon_meal" + width);
            if (mBitmapGA == null) {
                mBitmapGA = setBitmapSize(R.drawable.icon_meal, width);
                bitmapAllInfo.put("icon_meal" + width,mBitmapGA);
            }
            return mBitmapGA;
        }
    }
    /**值机大空间座位图标*/
    private Bitmap getBigBitmap (float width, String seatColor ,String type) {

        if ("RED".equals(seatColor)) { //红色座位
            Bitmap mBitmapSeatRed = (Bitmap) bitmapAllInfo.get("icon_rect_red" + width);
            if (mBitmapSeatRed == null) {
                mBitmapSeatRed = setBitmapSize(R.drawable.icon_rect_red, width);
                bitmapAllInfo.put("icon_rect_red" + width,mBitmapSeatRed);
            }
            return mBitmapSeatRed;
        } else if ("GREEN".equals(seatColor)) { //绿色座位
            Bitmap mBitmapSeatGreen = (Bitmap) bitmapAllInfo.get("icon_rect_green" + width);
            if (mBitmapSeatGreen == null) {
                mBitmapSeatGreen = setBitmapSize(R.drawable.icon_rect_green, width);
                bitmapAllInfo.put("icon_rect_green" + width,mBitmapSeatGreen);
            }
            return mBitmapSeatGreen;
        } else if ("BLUE_LESS".equals(seatColor)) {    //蓝色座位
            Bitmap mBitmapSeatBlue = (Bitmap) bitmapAllInfo.get("icon_rect_blue_air" + width);
            if (mBitmapSeatBlue == null) {
                mBitmapSeatBlue = setBitmapSize(R.drawable.icon_rect_blue_air, width);
                bitmapAllInfo.put("icon_rect_blue_air" + width,mBitmapSeatBlue);
            }
            return mBitmapSeatBlue;
        }else if ("PURPLE".equals(seatColor)) {    //紫色座位
            Bitmap mBitmapSeatBlue = (Bitmap) bitmapAllInfo.get("icon_pruple_seat_new_seat_s" + width);
            if (mBitmapSeatBlue == null) {
                mBitmapSeatBlue = setBitmapSize(R.drawable.icon_rect_blue_air, width);
                bitmapAllInfo.put("icon_pruple_seat_new_seat_s" + width,mBitmapSeatBlue);
            }
            return mBitmapSeatBlue;
        }else if ("LAKE_BLUE".equals(seatColor)) {    //浅蓝色座位
            Bitmap mBitmapSeatBlue = (Bitmap) bitmapAllInfo.get("icon_blue_less_seat_new_seat_s" + width);
            if (mBitmapSeatBlue == null) {
                mBitmapSeatBlue = setBitmapSize(R.drawable.icon_rect_blue_air, width);
                bitmapAllInfo.put("icon_blue_less_seat_new_seat_s" + width,mBitmapSeatBlue);
            }
            return mBitmapSeatBlue;
        }else if ("GREEN_LESS".equals(seatColor)) {    //浅绿色座位
            Bitmap mBitmapSeatBlue = (Bitmap) bitmapAllInfo.get("icon_rect_green_less_s" + width);
            if (mBitmapSeatBlue == null) {
                mBitmapSeatBlue = setBitmapSize(R.drawable.icon_rect_green_less_s, width);
                bitmapAllInfo.put("icon_rect_green_less_s" + width,mBitmapSeatBlue);
            }
            return mBitmapSeatBlue;
        }
        return null;
    }




    public float getFontLength(Paint paint, String str) {
        Rect rect = new Rect();
        paint.getTextBounds(str, 0, str.length(), rect);
        return rect.width();
    }

    public float getFontHeight(Paint paint, String str) {
        Rect rect = new Rect();
        paint.getTextBounds(str, 0, str.length(), rect);
        return rect.height();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                Iterator iter = mSeats.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String key = (String) entry.getKey();
                    RectF val = (RectF) entry.getValue();
                    if (val.contains(event.getX(), event.getY()+getScrollY())) {
                        if (!mSeatSelecting.contains(key)) {
                            if (mSeatSelecting.size() > indexCurrentPerson) {
                                mSeatSelecting.set(indexCurrentPerson,key);//切换乘机人选中座位   即多人选座
                                listener.onSeatChange(mSeatSelecting);
                                postInvalidate();
                            }
                        } else {
                            if (mSeatSelecting.size() > indexCurrentPerson && isCanCancelSeat) {//可取消座位
                                mSeatSelecting.set(indexCurrentPerson,"");
                                listener.onSeatChange(mSeatSelecting);
                                postInvalidate();
                            }
                        }
                    }
                }
                break;
        }
        return true;
    }

    /**获取view的总长度*/
    private void getViewHeight () {
        for (int index = 0; index < seatList.size();index++) {//不同舱位的List
            Map<String, Object> seatMapInfo = seatList.get(index);
            String cols = MapUtils.getObject(seatMapInfo.get("cols"));//ABC显示
            int length = cols.length();
            List<Map<String,Object>> rowsList = (List<Map<String, Object>>) seatMapInfo.get("rows");//行数
            float seatSize = 1 + ((length + 1) / (length * SEAT_HOW_HOW_LENGTH));//一行显示的座位个数
            float seatWH = ((viewWidth - dip2px(60)) / (length * (seatSize)));//座位的大小
            for (int i = 0; i < rowsList.size(); i++) {//行

                boolean isLastIndex = false;
                if ((i+1)==rowsList.size()) {
                    isLastIndex = true;
                }
                if (isLastIndex) {
                    lastIndexPosition = (i+1) * (seatWH + dip2px(16)) + 1*seatWH + lastIndexPosition;
                    viewHeightPoint = lastIndexPosition + dip2px(100);
                }
            }
        }
    }

    public String getBigSpaceSeatPrice(String seatNo){
        if(TextUtils.isEmpty(seatNo))
            return "0";
        Object mapInfo = big_space_seat.get(seatNo);
        if(mapInfo==null)
            return "0";

        String price=MapUtils.getObject(((Map<String,Object>)mapInfo).get("price"));
        if(TextUtils.isEmpty(price))
            return "0";
        return price;
    }

    public Map<String,Object> getBigSpackMap(String seatNo){
        if(TextUtils.isEmpty(seatNo))
            return null;
        Object mapInfo = big_space_seat.get(seatNo);
        if(mapInfo==null)
            return null;
        return (Map<String,Object>)mapInfo;
    }


    /**值机换座页面的退回预选座位*/
    public void setSelectingSeat (String seatNo) {
        mSeatSelecting.clear();
        mSeatSelecting.add(seatNo);
        postInvalidate();
    }
    /**清除选中的座位*/
    public void clearSelectingSeat () {
        if (mSeatSelecting.size()> indexCurrentPerson) {//清除所选乘机人的座位信息
            mSeatSelecting.set(indexCurrentPerson,"");
        }
    }

    public void setIndexCurrentPerson (int position) {
        this.indexCurrentPerson = position;
    }

    public int getIndexCurrentPerson () {
        return indexCurrentPerson ;
    }

    private SeatChangeListener listener;
    public void setSeatChangeListener(SeatChangeListener listener){
        this.listener = listener;
    }

    public interface SeatChangeListener{
        void onSeatChange(List<String> mSeatSelecting);
    }

    public float getFirstSelectSeatPosition() {
        return firstSelectSeatPosition;
    }

}
