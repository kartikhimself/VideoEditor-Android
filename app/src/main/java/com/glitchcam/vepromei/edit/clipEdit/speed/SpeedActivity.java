package com.glitchcam.vepromei.edit.clipEdit.speed;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsVideoClip;
import com.meicam.sdk.NvsVideoTrack;
import com.glitchcam.vepromei.R;
import com.glitchcam.vepromei.base.BaseActivity;
import com.glitchcam.vepromei.edit.adapter.CurveSpeedViewAdapter;
import com.glitchcam.vepromei.edit.clipEdit.SingleClipFragment;
import com.glitchcam.vepromei.edit.clipEdit.util.CurveSpeedUtil;
import com.glitchcam.vepromei.edit.data.BackupData;
import com.glitchcam.vepromei.edit.data.ChangeSpeedCurveInfo;
import com.glitchcam.vepromei.edit.interfaces.OnItemClickListener;
import com.glitchcam.vepromei.edit.interfaces.OnTitleBarClickListener;
import com.glitchcam.vepromei.edit.view.CustomTitleBar;
import com.glitchcam.vepromei.edit.view.EditChangeSpeedCurveView;
import com.glitchcam.vepromei.edit.view.EditChangeSpeedView;
import com.glitchcam.vepromei.utils.AppManager;
import com.glitchcam.vepromei.utils.TimelineUtil;
import com.glitchcam.vepromei.utils.dataInfo.ClipInfo;
import com.glitchcam.vepromei.utils.dataInfo.TimelineData;

import java.util.ArrayList;
import java.util.List;

public class SpeedActivity extends BaseActivity {

    private CustomTitleBar mTitleBar;
    private RelativeLayout mBottomLayout;
    private SingleClipFragment mClipFragment;
    private EditChangeSpeedView mEditChangeSpeedView;
    private RelativeLayout rl_select_mode;
    private ImageView iv_conventional,iv_curve,iv_confirm;
    private TextView tv_conventional,tv_curve;
    private CurveSpeedViewAdapter mAdapter;
    private RecyclerView rv_curve;
    private RelativeLayout rl_curve;
    private TextView tv_reset;
    private NvsStreamingContext mStreamingContext;
    private NvsTimeline mTimeline;
    private ArrayList<ClipInfo> mClipArrayList;
    private int mCurClipIndex = 0;
    private static final float floatZero = 0.000001f;
    private List<ChangeSpeedCurveInfo> changeSpeedCurveInfoList;
    private int mCurrentSelectedCurvePosition = 0;
    private EditChangeSpeedCurveView changeSpeedCurveView;
    private ImageView iv_confirm_curve;
    private boolean isEditCurveSpeed = false;
    private boolean hasCurveSpeed = false;

    private float mSpeed = 1.0f;
    private boolean keepAudioPitch  = true;
    private String mCurveSpeed;
    //??????????????????
    private boolean isPlaying = false;
    //?????????????????? timeline ??????
    private long mPlayTimestamp;
    //?????????????????????????????????????????????????????????????????????????????????
    private boolean seekCurveTimeline = false;
    @Override
    protected int initRootView() {
        mStreamingContext = NvsStreamingContext.getInstance();
        return R.layout.activity_speed;
    }

    @Override
    protected void initViews() {
        mTitleBar = findViewById(R.id.title_bar);
        mBottomLayout = findViewById(R.id.bottomLayout);
        mEditChangeSpeedView = findViewById(R.id.change_speed_view);
        rl_select_mode = findViewById(R.id.rl_select_change_mode);
        iv_conventional = findViewById(R.id.iv_change_speed_conventional);
        iv_curve = findViewById(R.id.iv_change_speed_curve);
        iv_confirm = findViewById(R.id.iv_confirm);
        tv_conventional = findViewById(R.id.tv_change_speed_conventional);
        tv_curve = findViewById(R.id.tv_change_speed_curve);
        rv_curve = findViewById(R.id.rv_curve);
        rl_curve = findViewById(R.id.rl_curve_type);
        changeSpeedCurveView = findViewById(R.id.change_speed_curve_view);
        iv_confirm_curve = findViewById(R.id.iv_confirm_curve);
        tv_reset = findViewById(R.id.tv_reset);
        initCurveView();
    }

    private void initCurveView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        rv_curve.setLayoutManager(layoutManager);
        mAdapter = new CurveSpeedViewAdapter(this);
        rv_curve.setAdapter(mAdapter);
        changeSpeedCurveInfoList = CurveSpeedUtil.listSpeedFromJson(this);
        mAdapter.updateData(changeSpeedCurveInfoList);
    }

    @Override
    protected void initTitle() {
        mTitleBar.setTextCenter(R.string.speed);
        mTitleBar.setBackImageVisible(View.GONE);
    }

    @Override
    protected void initData() {
        mClipArrayList =  BackupData.instance().cloneClipInfoData();
        mCurClipIndex = BackupData.instance().getClipIndex();
        if(mCurClipIndex < 0 || mCurClipIndex >= mClipArrayList.size())
            return;
        ClipInfo clipInfo = mClipArrayList.get(mCurClipIndex);
        if(clipInfo == null)
            return;
        mTimeline = TimelineUtil.createSingleClipTimeline(clipInfo,true);
        if(mTimeline == null)
            return;
        initClipFragment();

        updateSpeedSeekBar(clipInfo.getSpeed(),clipInfo.isKeepAudioPitch());
        //????????????????????????????????????
        ChangeSpeedCurveInfo changeSpeedCurveInfo = clipInfo.getmCurveSpeed();
        if(null != changeSpeedCurveInfo){
            int index = changeSpeedCurveInfo.index;
            mCurrentSelectedCurvePosition = index;
            changeSpeedCurveInfoList.get(index).speed = changeSpeedCurveInfo.speed;
            mAdapter.setSelectedPosition(mCurrentSelectedCurvePosition);
        }
    }

    /**
     * ????????????????????????
     * @param speedVal
     */
    private void updateSpeedSeekBar(float speedVal,boolean keepAudioPitch){
        this.mSpeed = speedVal;
        this.keepAudioPitch = keepAudioPitch;
        mEditChangeSpeedView.setSpeed(speedVal,!keepAudioPitch);
    }
    @Override
    protected void initListener() {
        mTitleBar.setOnTitleBarClickListener(new OnTitleBarClickListener() {
            @Override
            public void OnBackImageClick() {
                removeTimeline();
            }

            @Override
            public void OnCenterTextClick() {

            }

            @Override
            public void OnRightTextClick() {

            }
        });
        iv_conventional.setOnClickListener(this);
        iv_curve.setOnClickListener(this);
        iv_confirm.setOnClickListener(this);
        mEditChangeSpeedView.setOnFunctionListener(new EditChangeSpeedView.OnFunctionListener() {
            @Override
            public void onConfirm(float speed, boolean changeVoice) {
                //saveChangeSpeedAndQuit();
                //????????????????????????????????? ???????????????
                mEditChangeSpeedView.setVisibility(View.GONE);
                rl_select_mode.setVisibility(View.VISIBLE);
            }

            @Override
            public void onChangeVoice(float speed,boolean changeVoice) {
                changeSpeed(speed,!changeVoice);
            }

            @Override
            public void onSpeedChanged(float speed, boolean changeVoice) {
                if(hasCurveSpeed){

                    //?????????????????????????????????????????????
                    mCurrentSelectedCurvePosition = 0;
                    mAdapter.setSelectedPosition(mCurrentSelectedCurvePosition);
                    //??????????????????????????????
                    ChangeSpeedCurveInfo changeSpeedCurveInfo = changeSpeedCurveInfoList.get(0);
                    changeSpeedForCurve(changeSpeedCurveInfo.speed);
                    hasCurveSpeed = false;
                    mCurveSpeed = "";
                    upDataClipDuration();
                }

                changeSpeed(speed,!changeVoice);
            }
        });
        iv_confirm_curve.setOnClickListener(this);

        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {

                hasCurveSpeed = pos!=0;
                //?????????????????????????????????
                ChangeSpeedCurveInfo changeSpeedCurveInfo = changeSpeedCurveInfoList.get(pos);
                //???????????????????????????????????????
                if(mCurrentSelectedCurvePosition == pos && pos!=0){
                    changeSpeedCurveView.setVisibility(View.VISIBLE);
                    tv_reset.setVisibility(View.VISIBLE);
                    rv_curve.setVisibility(View.INVISIBLE);
                    isEditCurveSpeed = true;
                    //TODO ???????????????????????????????????????
                    changeSpeedCurveView.setInfo(changeSpeedCurveInfo);
                    changeSpeedCurveView.setClipDuration(getClipDuration());
                    //?????? ???????????????????????????  ?????????????????????????????? ?????????0??????
                    if(mPlayTimestamp == 0){
                        playVideo(0);
                    }else{
                        //???????????????????????????????????????????????? ???????????????seek?????????????????????
                        long bzPosition = mPlayTimestamp;
                        if(!TextUtils.isEmpty(mCurveSpeed)){
                            bzPosition = getClipPositionFromTimelinePosition(bzPosition);
                        }
                        //????????????????????????
                        changeSpeedCurveView.upDataPlayProgress(bzPosition);
                        //stopPlayVideo();
                    }
                }else{
                    //??????????????? ???????????????????????????
                    mAdapter.setSelectedPosition(pos);
                    mCurrentSelectedCurvePosition = pos;
                    mCurveSpeed = changeSpeedCurveInfo.speed;
                    //TODO ?????????????????????
                    changeSpeedForCurve(mCurveSpeed);
                    //??????????????????????????????  ????????????????????????
                    if(pos == 0){
                        stopPlayVideo();
                    }else{
                        //???0????????????
                        playVideo(0);
                        hasCurveSpeed = true;
                    }

                }
            }
        });
        tv_reset.setOnClickListener(this);
        changeSpeedCurveView.setOnFunctionListener(new EditChangeSpeedCurveView.OnFunctionListener() {
            @Override
            public void onChangePoint(boolean addPoint) {
                // ?????? ?????? point
                stopPlayVideo();
            }

            @Override
            public void onTimelineMove(long timePoint) {
                seekCurveTimeline = true;
                //????????????????????????
                seekTimeline(timePoint);

            }

            @Override
            public void onSpeedChanged(String speed,long timePoint) {
                seekCurveTimeline = false;
                changeSpeedCurveInfoList.get(mCurrentSelectedCurvePosition).speed = speed;
                mCurveSpeed = speed;
                hasCurveSpeed = true;
                changeSpeedForCurve(speed);
                //seek??????
                seekTimeline(timePoint);

            }

            @Override
            public void onActionDown() {
                isPlaying = mClipFragment.getCurrentEngineState() == NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK;
            }

            @Override
            public void onActionUp(long timePoint) {
                if (!isPlaying) {
                    //????????? ?????????????????????
                    long timeLineTimePoint = getTimelinePositionFromClipPosition(timePoint);
                    playVideo(timeLineTimePoint);
                } else {
                    //????????????????????????????????????????????????????????????
                    if(!seekCurveTimeline){

                        //????????? ???0??????
                        playVideo(0);
                    }
                }
                seekCurveTimeline = false;
            }

            @Override
            public void onSelectPoint() {
                stopPlayVideo();
            }
        });
        //???????????????????????????
        mClipFragment.setVideoFragmentCallBack(new SingleClipFragment.VideoFragmentListener() {
            @Override
            public void playBackEOF(NvsTimeline timeline) {
                if(changeSpeedCurveView.getVisibility() ==View.VISIBLE){
                    changeSpeedCurveView.upDataPlayProgress(0);
                }
            }

            @Override
            public void playStopped(NvsTimeline timeline) {
            }

            @Override
            public void playbackTimelinePosition(NvsTimeline timeline, long stamp) {

                if(changeSpeedCurveView.getVisibility() ==View.VISIBLE){
                    //???????????????????????????????????????????????? ???????????????seek?????????????????????
                    long bzPosition = stamp;
                    if(!TextUtils.isEmpty(mCurveSpeed)){
                        //timeline???????????? ?????????clip??????????????????
                        bzPosition = getClipPositionFromTimelinePosition(stamp);
                    }
                    //????????????????????????
                    changeSpeedCurveView.upDataPlayProgress(bzPosition);
                }
            }

            @Override
            public void streamingEngineStateChanged(int state) {

            }
        });
        mClipFragment.setVideoFragmentSeekListener(new SingleClipFragment.VideoFragmentSeekListener() {
            @Override
            public void onSeekBarChanged(long timeStamp) {
                mPlayTimestamp = timeStamp;
                //
                if(changeSpeedCurveView.getVisibility() == View.VISIBLE){
                    //???????????????????????????????????????????????? ???????????????seek?????????????????????
                    long bzPosition = timeStamp;
                    if(!TextUtils.isEmpty(mCurveSpeed)){
                        bzPosition = getClipPositionFromTimelinePosition(timeStamp);
                    }
                    //????????????????????????
                    changeSpeedCurveView.upDataPlayProgress(bzPosition);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_confirm:
                saveChangeSpeedAndQuit();
                break;

            case R.id.iv_change_speed_conventional:
                iv_conventional.setImageResource(R.mipmap.change_speed_conventional_selected);
                iv_curve.setImageResource(R.mipmap.change_speed_curve);
                tv_conventional.setTextColor(getResources().getColor(R.color.change_speed_selected));
                tv_curve.setTextColor(getResources().getColor(R.color.ccffffff));
                rl_select_mode.setVisibility(View.INVISIBLE);

                mEditChangeSpeedView.setVisibility(View.VISIBLE);
                rl_curve.setVisibility(View.GONE);
                break;

            case R.id.iv_change_speed_curve:
                iv_conventional.setImageResource(R.mipmap.change_speed_conventional);
                iv_curve.setImageResource(R.mipmap.change_speed_curve_selected);
                tv_conventional.setTextColor(getResources().getColor(R.color.ccffffff));
                tv_curve.setTextColor(getResources().getColor(R.color.change_speed_selected));
                rl_select_mode.setVisibility(View.INVISIBLE);

                mEditChangeSpeedView.setVisibility(View.GONE);
                rl_curve.setVisibility(View.VISIBLE);
                break;

            case R.id.iv_confirm_curve:
                //????????????????????????????????????
                //?????????????????? ?????????????????? ??? ?????????????????????item??????
                if(isEditCurveSpeed){
                    changeSpeedCurveView.setVisibility(View.GONE);
                    tv_reset.setVisibility(View.INVISIBLE);
                    rv_curve.setVisibility(View.VISIBLE);
                    isEditCurveSpeed = false;
                    stopPlayVideo();

                }else {
                    //??????????????????????????????item???????????????
                    //????????????????????????????????????????????????
                    rl_curve.setVisibility(View.GONE);
                    rl_select_mode.setVisibility(View.VISIBLE);
                    //??????????????????????????????????????????
                    resetCurveSpeedValue(mCurrentSelectedCurvePosition);
                    //????????????
                    stopPlayVideo();
                }
                break;

            case R.id.tv_reset:
                //???????????????
                ChangeSpeedCurveInfo changeSpeedCurveInfo = changeSpeedCurveInfoList.get(mCurrentSelectedCurvePosition);
                changeSpeedCurveInfo.speed = changeSpeedCurveInfo.speedOriginal;

                changeSpeedForCurve(changeSpeedCurveInfo.speed);
                changeSpeedCurveView.setInfo(changeSpeedCurveInfo);
                changeSpeedCurveView.setClipDuration(getClipDuration());
                break;
        }

    }

    /**
     * ???????????????????????????????????????????????????????????????
     * @param mCurrentSelectedCurvePosition  ??????????????? ????????????????????????????????????
     */
    private void resetCurveSpeedValue(int mCurrentSelectedCurvePosition) {

        for(int i = 1; i<changeSpeedCurveInfoList.size() ; i++){
            if(mCurrentSelectedCurvePosition == i){
                continue;
            }
            ChangeSpeedCurveInfo changeSpeedCurveInfo = changeSpeedCurveInfoList.get(i);
            changeSpeedCurveInfo.speed = changeSpeedCurveInfo.speedOriginal;
        }
    }

    /**
     * ?????????????????? ?????????????????????
     */
    private void saveChangeSpeedAndQuit(){
        mClipArrayList.get(mCurClipIndex).setSpeed(mSpeed);
        mClipArrayList.get(mCurClipIndex).setKeepAudioPitch(keepAudioPitch);
        if(hasCurveSpeed){
            mClipArrayList.get(mCurClipIndex).setmCurveSpeed(changeSpeedCurveInfoList.get(mCurrentSelectedCurvePosition));
        }else{
            mClipArrayList.get(mCurClipIndex).setmCurveSpeed(null);

        }
        BackupData.instance().setClipInfoData(mClipArrayList);
        removeTimeline();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        AppManager.getInstance().finishActivity();
    }

    /**
     * ????????????
     * @param speed  ?????????
     * @param keepAudioPitch  ????????????
     */
    private void changeSpeed(float speed,boolean keepAudioPitch){
        this.mSpeed = speed;
        this.keepAudioPitch = keepAudioPitch;
        NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
        if(videoTrack == null)
            return;
        NvsVideoClip videoClip = videoTrack.getClipByIndex(0);
        if(videoClip == null)
            return;
        videoClip.changeSpeed(speed,keepAudioPitch);
        upDataClipDuration();

    }

    /**
     * ????????????????????????
     * @param speed
     */
    private void changeSpeedForCurve(String speed){
        NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
        if(videoTrack == null)
            return;
        NvsVideoClip videoClip = videoTrack.getClipByIndex(0);
        if(videoClip == null)
            return;
        boolean isSucess = videoClip.changeCurvesVariableSpeed(speed, true);
        Log.e("changeSpeedForCurve", "??????????????????" + (isSucess ? "??????" : "??????"));
        //????????????
        upDataClipDuration();
    }

    /**
     * ????????????????????????????????????
     */
    private void upDataClipDuration(){
        if(null != mClipFragment){

            mClipFragment.updateTotalDuration();
        }
    }
    @Override
    public void onBackPressed() {
        removeTimeline();
        AppManager.getInstance().finishActivity();
        super.onBackPressed();
    }

    private void removeTimeline(){
        TimelineUtil.removeTimeline(mTimeline);
        mTimeline = null;
    }

    private void initClipFragment() {
        mClipFragment = new SingleClipFragment();
        mClipFragment.setFragmentLoadFinisedListener(new SingleClipFragment.OnFragmentLoadFinisedListener() {
            @Override
            public void onLoadFinished() {
                mClipFragment.seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
            }
        });
        mClipFragment.setTimeline(mTimeline);
        Bundle bundle = new Bundle();
        bundle.putInt("titleHeight",mTitleBar.getLayoutParams().height);
        bundle.putInt("bottomHeight",mBottomLayout.getLayoutParams().height);
        bundle.putInt("ratio", TimelineData.instance().getMakeRatio());
        mClipFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .add(R.id.spaceLayout, mClipFragment)
                .commit();
        getFragmentManager().beginTransaction().show(mClipFragment);
    }

    /**
     * ???????????????????????????
     * @return
     */
    private long getClipDuration() {
        NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
        if(videoTrack == null)
            return 0;
        NvsVideoClip videoClip = videoTrack.getClipByIndex(0);
        if(videoClip == null)
            return 0;
        return videoClip.getTrimOut() - videoClip.getTrimIn();
    }

    /**
     * ????????????
     */
    private void stopPlayVideo(){
        if(null != mClipFragment){
            mClipFragment.stopEngine();
        }
    }

    /**
     * ??????????????????
     */
    private void playVideo(long start){
        if(null != mClipFragment){
            NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
            if(videoTrack == null)
                return;
            NvsVideoClip videoClip = videoTrack.getClipByIndex(0);
            if(videoClip == null)
                return ;
            mClipFragment.playVideo(start,videoClip.getTrimOut());
        }
    }

    /**
     * ?????????????????????????????????????????????seek
     * @param timestamp  clip?????????????????? ???????????????timeline?????????
     */
    private void seekTimeline(long timestamp){
        if(null != mClipFragment){
            long timelinePosition =getTimelinePositionFromClipPosition(timestamp);
            mClipFragment.updateCurPlayTime(timelinePosition);
            mClipFragment.seekTimeline(timelinePosition, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        }
    }

    /**
     * ????????????clip?????????????????????timeline??????
     * @param timestamp
     * @return
     */
    private long getTimelinePositionFromClipPosition(long timestamp){
        //??????????????????????????????
        NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
        if(videoTrack == null)
            return timestamp;
        NvsVideoClip videoClip = videoTrack.getClipByIndex(0);
        if(videoClip == null)
            return timestamp;
        long timelinePosition = videoClip.GetTimelinePosByClipPosCurvesVariableSpeed(timestamp) + videoClip.getTrimIn();
        return timelinePosition;
    }

    /**
     * ????????????timeline???????????????????????????clip??????
     * @param timelinePosition
     * @return
     */
    private long getClipPositionFromTimelinePosition(long timelinePosition){
        //??????????????????????????????
        NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
        if(videoTrack == null)
            return timelinePosition;
        NvsVideoClip videoClip = videoTrack.getClipByIndex(0);
        if(videoClip == null)
            return timelinePosition;
        //timeline???????????? ?????????clip??????????????????
        long bzPosition = videoClip.GetClipPosByTimelinePosCurvesVariableSpeed(timelinePosition) - videoClip.getTrimIn();
        return bzPosition;
    }
}
