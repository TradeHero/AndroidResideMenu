package com.special.residemenu;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * User: special Date: 13-12-10 Time: 下午10:44 Mail: specialcyci@gmail.com
 */
public class ResideMenu extends FrameLayout implements GestureDetector.OnGestureListener {

  private ImageView mShadow;
  private ImageView mBackground;
  private LinearLayout mMenuLayout;
  private ScrollView mScrollViewMenu;

  private AnimatorSet mScaleUpContent;
  private AnimatorSet mScaleDownContent;
  /**
   * the decorview of the activity
   */
  private ViewGroup viewDecor;
  /**
   * the viewgroup of the activity
   */
  private ViewGroup mainContent;
  /**
   * the flag of menu open status
   */
  private boolean isOpened;
  private GestureDetector gestureDetector;
  private float shadow_ScaleX;
  /**
   * the view which don't want to intercept touch event
   */
  private List<View> ignoredViews;
  private List<View> menuItems;
  private DisplayMetrics displayMetrics = new DisplayMetrics();
  private OnMenuListener menuListener;
  private TouchDisableView touchDisableView;
  private boolean enableSwipeLeftToRight;
  private boolean enableSwipeRightToLeft;

  public ResideMenu(Context context) {
    super(context);
    initViews(context);
  }

  private void initViews(Context context) {
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.residemenu, this);

    mScrollViewMenu = (ScrollView) findViewById(R.id.sv_menu);
    mMenuLayout = (LinearLayout) findViewById(R.id.layout_menu);
    mShadow = (ImageView) findViewById(R.id.iv_shadow);
    mBackground = (ImageView) findViewById(R.id.iv_background);
  }

  /**
   * Deprecated use attachTo(ViewGroup) instead
   */
  @Deprecated
  public void attachToActivity(Activity activity) {
    attachTo((ViewGroup) activity.getWindow().getDecorView());
  }

  public void attachTo(ViewGroup targetViewGroup) {
    viewDecor = targetViewGroup;

    menuItems = new ArrayList<View>();
    ignoredViews = new ArrayList<View>();

    gestureDetector = new GestureDetector(this);

    touchDisableView = new TouchDisableView(getContext());
    View mainView = viewDecor.getChildAt(0);
    viewDecor.removeViewAt(0);
    touchDisableView.setContent(mainView);
    mainContent = touchDisableView;
    touchDisableView.setOnTouchListener(new OnCancelMenuByTouchListener());

    viewDecor.addView(touchDisableView);

    setShadowScaleXByOrientation();
    buildAnimationSet();
  }

  private void setShadowScaleXByOrientation() {
    int orientation = getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      shadow_ScaleX = 0.5335f;
    } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      shadow_ScaleX = 0.56f;
    }
  }

  /**
   * set the menu background picture;
   */
  public void setBackground(int imageResource) {
    mBackground.setImageResource(imageResource);
  }

  /**
   * the visiblity of shadow under the activity view;
   */
  public void setShadowVisible(boolean isVisible) {
    if (isVisible) {
      mShadow.setImageResource(R.drawable.shadow);
    } else {
      mShadow.setImageBitmap(null);
    }
  }

  /**
   * add a single items;
   */
  public void addMenuItem(ResideMenuItem menuItem) {
    this.menuItems.add(menuItem);
  }

  /**
   * add a single items; TODO This method is a hack due to time constraint
   */
  public void addMenuItem(View menuItem) {
    this.menuItems.add(menuItem);
  }

  /**
   * set the menu items by array list;
   */
  public void setMenuItems(List<View> menuItems) {
    mMenuLayout.removeAllViews();
    this.menuItems = menuItems;
  }

  public List<View> getMenuItems() {
    return menuItems;
  }

  /**
   * if you need to do something on the action of closing or opening menu, set the listener here.
   */
  public void setMenuListener(OnMenuListener menuListener) {
    this.menuListener = menuListener;
  }

  public OnMenuListener getMenuListener() {
    return menuListener;
  }

  /**
   * we need the call the method before the menu show, because the padding of activity can't get at
   * the moment of onCreateView();
   */
  private void setViewPadding() {
    this.setPadding(mainContent.getPaddingLeft(), mainContent.getPaddingTop(),
        mainContent.getPaddingRight(), mainContent.getPaddingBottom());
  }

  /**
   * show the reside menu;
   */
  public void openMenu() {
    if (!isOpened) {
      isOpened = true;
      showOpenMenuRelative();
      touchDisableView.setTouchDisable(true);
    }
  }

  private void removeMenuLayout() {
    ViewGroup parent = ((ViewGroup) mScrollViewMenu.getParent());
    parent.removeView(mScrollViewMenu);
  }

  /**
   * close the reside menu;
   */
  public void closeMenu() {
    if (isOpened) {
      isOpened = false;
      mScaleUpContent.start();
      touchDisableView.setTouchDisable(false);
    }
  }

  /**
   * return the flag of menu status;
   */
  public boolean isOpened() {
    return isOpened;
  }

  /**
   * call the method relative to open menu;
   */
  private void showOpenMenuRelative() {
    setViewPadding();
    mScaleDownContent.start();
    // remove self if has not remove
    if (getParent() != null) {
      viewDecor.removeView(this);
    }
    if (mScrollViewMenu.getParent() != null) {
      removeMenuLayout();
    }

    viewDecor.addView(this, 0);
    viewDecor.addView(mScrollViewMenu);
  }

  private Animator.AnimatorListener animationListener = new Animator.AnimatorListener() {
    @Override
    public void onAnimationStart(Animator animation) {
      if (isOpened) {
        showMenuDelay();
        if (menuListener != null) {
          menuListener.openMenu();
        }
      }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
      // reset the view;
      if (!isOpened) {
        viewDecor.removeView(ResideMenu.this);
        viewDecor.removeView(mScrollViewMenu);
        if (menuListener != null) {
          menuListener.closeMenu();
        }
      }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
  };

  private void showMenuDelay() {
    for (int i = 0; i < menuItems.size(); i++) {
      showMenuItem(menuItems.get(i), i);
    }
  }

  /**
   * @param menuIndex the position of the menu;
   */
  private void showMenuItem(View menuItem, int menuIndex) {
    if (menuItem.getParent() == null) {
      mMenuLayout.addView(menuItem);
    }

    ViewHelper.setAlpha(menuItem, 0);
    AnimatorSet scaleUp = new AnimatorSet();
    scaleUp.playTogether(ObjectAnimator.ofFloat(menuItem, "translationX", -100.f, 0.0f),
        ObjectAnimator.ofFloat(menuItem, "alpha", 0.0f, 1.0f));

    scaleUp.setInterpolator(AnimationUtils.loadInterpolator(getContext(),
        android.R.anim.anticipate_overshoot_interpolator));

    scaleUp.setStartDelay(50 * menuIndex);
    scaleUp.setDuration(400).start();
  }

  private void buildAnimationSet() {

    AnimatorSet scaleDownShadowAnimation = buildScaleDownAnimation(mShadow, shadow_ScaleX, 0.59f);
    mScaleDownContent = buildScaleDownAnimation(mainContent, 0.5f, 0.5f);
    mScaleDownContent.playTogether(scaleDownShadowAnimation);
    mScaleDownContent.addListener(animationListener);

    AnimatorSet scaleUpShadowAnimation = buildScaleUpAnimation(mShadow, 1.0f, 1.0f);
    mScaleUpContent = buildScaleUpAnimation(mainContent, 1.0f, 1.0f);
    mScaleUpContent.playTogether(scaleUpShadowAnimation);
    mScaleUpContent.addListener(animationListener);
  }

  /**
   * a helper method to build scale down animation;
   */
  private AnimatorSet buildScaleDownAnimation(View target, float targetScaleX, float targetScaleY) {
    // set the pivotX and pivotY to scale;
    int pivotX = (int) (getScreenWidth() * 1.5);
    int pivotY = (int) (getScreenHeight() * 0.5);

    ViewHelper.setPivotX(target, pivotX);
    ViewHelper.setPivotY(target, pivotY);

    AnimatorSet scaleDown = new AnimatorSet();
    scaleDown.playTogether(ObjectAnimator.ofFloat(target, "scaleX", targetScaleX),
        ObjectAnimator.ofFloat(target, "scaleY", targetScaleY));

    scaleDown.setInterpolator(
        AnimationUtils.loadInterpolator(getContext(), android.R.anim.decelerate_interpolator));

    scaleDown.setDuration(250);
    return scaleDown;
  }

  /**
   * a helper method to build scale up animation;
   */
  private AnimatorSet buildScaleUpAnimation(View target, float targetScaleX, float targetScaleY) {
    AnimatorSet scaleUp = new AnimatorSet();
    scaleUp.playTogether(ObjectAnimator.ofFloat(target, "scaleX", targetScaleX),
        ObjectAnimator.ofFloat(target, "scaleY", targetScaleY));

    scaleUp.setDuration(250);
    return scaleUp;
  }

  /**
   * if there ware some view you don't want reside menu to intercept their touch event,you can use
   * the method to set.
   */
  public void addIgnoredView(View v) {
    ignoredViews.add(v);
  }

  /**
   * remove the view from ignored view list;
   */
  public void removeIgnoredView(View v) {
    ignoredViews.remove(v);
  }

  /**
   * clear the ignored view list;
   */
  public void clearIgnoredViewList() {
    ignoredViews.clear();
  }

  /**
   * if the motion evnent was relative to the view which in ignored view list,return true;
   */
  private boolean isInIgnoredView(MotionEvent ev) {
    Rect rect = new Rect();
    for (View v : ignoredViews) {
      v.getGlobalVisibleRect(rect);
      if (rect.contains((int) ev.getX(), (int) ev.getY())) {
        return true;
      }
    }
    return false;
  }

  //region Gesture listener
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    gestureDetector.onTouchEvent(ev);
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onDown(MotionEvent motionEvent) {
    return false;
  }

  @Override
  public void onShowPress(MotionEvent motionEvent) {

  }

  @Override
  public boolean onSingleTapUp(MotionEvent motionEvent) {
    return false;
  }

  @Override
  public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
    return false;
  }

  @Override
  public void onLongPress(MotionEvent motionEvent) {

  }

  @Override
  public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {

    if (isInIgnoredView(motionEvent) || isInIgnoredView(motionEvent2)) {
      return false;
    }

    int distanceX = (int) (motionEvent2.getX() - motionEvent.getX());
    int distanceY = (int) (motionEvent2.getY() - motionEvent.getY());
    int screenWidth = getScreenWidth();

    if (Math.abs(distanceY) > screenWidth * 0.3) {
      return false;
    }

    if (Math.abs(distanceX) > screenWidth * 0.3) {
      if (enableSwipeLeftToRight && distanceX > 0 && !isOpened) {
        // from left to right;
        openMenu();
      } else if (enableSwipeRightToLeft && distanceX < 0 && isOpened) {
        // from right th left;
        closeMenu();
      }
    }

    return false;
  }
  //endregion

  public int getScreenHeight() {
    WindowManager windowManager =
        (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    windowManager.getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.heightPixels;
  }

  public int getScreenWidth() {
    WindowManager windowManager =
        (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    windowManager.getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.widthPixels;
  }

  public void setEnableSwipeLeftToRight(boolean enableSwipeLeftToRight) {
    this.enableSwipeLeftToRight = enableSwipeLeftToRight;
  }

  public void setEnableSwipeRightToLeft(boolean enableSwipeRightToLeft) {
    this.enableSwipeRightToLeft = enableSwipeRightToLeft;
  }

  public void setEnableSwipe(boolean enableSwipe) {
    this.enableSwipeLeftToRight = enableSwipe;
    this.enableSwipeRightToLeft = enableSwipe;
  }

  public interface OnMenuListener {

    /**
     * the method will call on the finished time of opening menu's animation.
     */
    public void openMenu();

    /**
     * the method will call on the finished time of closing menu's animation  .
     */
    public void closeMenu();
  }

  private class OnCancelMenuByTouchListener implements OnTouchListener {
    @Override public boolean onTouch(View v, MotionEvent event) {
      if (isOpened && touchDisableView.isTouchDisabled()) {
        closeMenu();
        return true;
      } else {
        return false;
      }
    }
  }
}