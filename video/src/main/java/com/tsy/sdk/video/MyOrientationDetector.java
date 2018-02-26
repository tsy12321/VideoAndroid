package com.tsy.sdk.video;

import android.content.Context;
import android.view.OrientationEventListener;

/**
 * Created by tsy on 2017/1/13.
 */

public class MyOrientationDetector extends OrientationEventListener {

    private int mOrientation = 0;

    public MyOrientationDetector( Context context ) {
        super(context );
    }

    public int getOrientation() {
        return mOrientation;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if( orientation > 315 && orientation<= 45) { //0度
            mOrientation = 0;
        } else if( orientation > 45 && orientation <= 135 ) { //90度
            mOrientation = 90;
        } else if( orientation > 135 && orientation <= 225 ) { //180度
            mOrientation = 180;
        } else if( orientation > 225 && orientation <= 315  ) { //270度
            mOrientation = 270;
        } else {
            mOrientation = 0;
        }
    }
}
