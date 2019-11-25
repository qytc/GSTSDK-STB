package io.qytc.stb;

import android.animation.Animator;
import android.view.View;

public class ShowLayoutAnimListener implements Animator.AnimatorListener {

    private View mParent;
    private int mflag;//0 ,打开     1,关闭
    private View mChild;

    public ShowLayoutAnimListener(View parent, int flag, View child) {
        mParent = parent;
        mflag = flag;
        mChild = child;
    }

    @Override
    public void onAnimationStart(Animator animator) {
        if (mflag == 0 && mParent != null) {
            mParent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (mflag == 0 && mChild != null) {
            mChild.requestFocus();
        } else if (mflag == 1 && mParent != null) {
            mParent.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAnimationCancel(Animator animator) {

    }

    @Override
    public void onAnimationRepeat(Animator animator) {

    }
}
