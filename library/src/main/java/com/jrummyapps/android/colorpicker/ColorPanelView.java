/*
 * Copyright (C) 2017 JRummy Apps Inc.
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
 * Raw
 */

package com.jrummyapps.android.colorpicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * This class draws a panel which which will be filled with a color which can be set. It can be used to show the
 * currently selected color which you will get from the {@link ColorPickerView}.
 */
public class ColorPanelView extends FrameLayout {

  private final static int DEFAULT_BORDER_COLOR = 0xFF6E6E6E;

  private Drawable alphaPattern;
  private Paint borderPaint;
  private Paint colorPaint;
  private Paint alphaPaint;
  private Paint originalPaint;
  private Rect drawingRect;
  private Rect colorRect;
  private RectF centerRect = new RectF();
  private boolean showOldColor;

  /* The width in pixels of the border surrounding the color panel. */
  private int borderWidthPx;
  private int borderColor = DEFAULT_BORDER_COLOR;
  private int color = Color.BLACK;
  private int shape;

  public ColorPanelView(Context context) {
    this(context, null);
  }

  public ColorPanelView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ColorPanelView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs);
  }

  @Override public Parcelable onSaveInstanceState() {
    Bundle state = new Bundle();
    state.putParcelable("instanceState", super.onSaveInstanceState());
    state.putInt("color", color);
    return state;
  }

  @Override public void onRestoreInstanceState(Parcelable state) {
    if (state instanceof Bundle) {
      Bundle bundle = (Bundle) state;
      color = bundle.getInt("color");
      state = bundle.getParcelable("instanceState");
    }
    super.onRestoreInstanceState(state);
  }

  private void init(Context context, AttributeSet attrs) {
    TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.cpv_ColorPickerView);
    shape = a.getInt(R.styleable.cpv_ColorPickerView_shape, Shape.CIRCLE);
    showOldColor = a.getBoolean(R.styleable.cpv_ColorPickerView_showOldColor, false);
    if (showOldColor && shape != Shape.CIRCLE) {
      throw new IllegalStateException("Color preview is only available in circle mode");
    }
    borderColor = a.getColor(R.styleable.cpv_ColorPickerView_borderColor, DEFAULT_BORDER_COLOR);
    a.recycle();
    if (borderColor == DEFAULT_BORDER_COLOR) {
      // If no specific border color has been set we take the default secondary text color as border/slider color.
      // Thus it will adopt to theme changes automatically.
      final TypedValue value = new TypedValue();
      TypedArray typedArray = context.obtainStyledAttributes(value.data, new int[]{android.R.attr.textColorSecondary});
      borderColor = typedArray.getColor(0, borderColor);
      typedArray.recycle();
    }
    borderWidthPx = DrawingUtils.dpToPx(context, 1);
    borderPaint = new Paint();
    colorPaint = new Paint();
    if (showOldColor) {
      originalPaint = new Paint();
    }
    if (shape == Shape.CIRCLE) {
      Bitmap bitmap = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.cpv_alpha)).getBitmap();
      alphaPaint = new Paint();
      alphaPaint.setAntiAlias(true);
      BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
      alphaPaint.setShader(shader);
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    borderPaint.setColor(borderColor);
    colorPaint.setColor(color);
    if (shape == Shape.RECT) {
      if (borderWidthPx > 0) {
        canvas.drawRect(drawingRect, borderPaint);
      }
      if (alphaPattern != null) {
        alphaPattern.draw(canvas);
      }
      canvas.drawRect(colorRect, colorPaint);
    } else if (shape == Shape.CIRCLE) {
      final int outerRadius = getMeasuredWidth() / 2;
      if (borderWidthPx > 0) {
        canvas.drawCircle(getMeasuredWidth() / 2,
            getMeasuredHeight() / 2,
            outerRadius,
            borderPaint);
      }
      if (Color.alpha(color) < 255) {
        canvas.drawCircle(getMeasuredWidth() / 2,
            getMeasuredHeight() / 2,
            outerRadius - borderWidthPx, alphaPaint);
      }
      if (showOldColor) {
        canvas.drawArc(centerRect, 90, 180, true, originalPaint);
        canvas.drawArc(centerRect, 270, 180, true, colorPaint);
      } else {
        canvas.drawCircle(getMeasuredWidth() / 2,
            getMeasuredHeight() / 2,
            outerRadius - borderWidthPx,
            colorPaint);
      }
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (shape == Shape.RECT) {
      int width = MeasureSpec.getSize(widthMeasureSpec);
      int height = MeasureSpec.getSize(heightMeasureSpec);
      setMeasuredDimension(width, height);
    } else if (shape == Shape.CIRCLE) {
      super.onMeasure(widthMeasureSpec, widthMeasureSpec);
      setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (shape == Shape.RECT || showOldColor) {
      drawingRect = new Rect();
      drawingRect.left = getPaddingLeft();
      drawingRect.right = w - getPaddingRight();
      drawingRect.top = getPaddingTop();
      drawingRect.bottom = h - getPaddingBottom();
      if (showOldColor) {
        setUpCenterRect();
      } else {
        setUpColorRect();
      }
    }
  }

  private void setUpCenterRect() {
    final Rect dRect = drawingRect;
    int left = dRect.left + borderWidthPx;
    int top = dRect.top + borderWidthPx;
    int bottom = dRect.bottom - borderWidthPx;
    int right = dRect.right - borderWidthPx;
    centerRect = new RectF(left, top, right, bottom);
  }

  private void setUpColorRect() {
    final Rect dRect = drawingRect;
    int left = dRect.left + borderWidthPx;
    int top = dRect.top + borderWidthPx;
    int bottom = dRect.bottom - borderWidthPx;
    int right = dRect.right - borderWidthPx;
    colorRect = new Rect(left, top, right, bottom);
    alphaPattern = new AlphaPatternDrawable(DrawingUtils.dpToPx(getContext(), 4));
    alphaPattern.setBounds(Math.round(colorRect.left),
        Math.round(colorRect.top),
        Math.round(colorRect.right),
        Math.round(colorRect.bottom));
  }

  /**
   * Set the color that should be shown by this view.
   *
   * @param color
   *     the color value
   */
  public void setColor(int color) {
    this.color = color;
    Drawable selector = createSelector(color);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      int[][] states = new int[][]{
          new int[]{android.R.attr.state_pressed}
      };
      int[] colors = new int[]{shiftColor(color, 1.1f)};
      ColorStateList rippleColors = new ColorStateList(states, colors);
      setForeground(new RippleDrawable(rippleColors, selector, null));
    } else {
      setForeground(selector);
    }
    invalidate();
  }

  /**
   * Get the color currently show by this view.
   *
   * @return the color value
   */
  public int getColor() {
    return color;
  }

  /**
   * Set the original color. This is only used for previewing colors.
   *
   * @param color
   *     The original color
   */
  public void setOriginalColor(@ColorInt int color) {
    if (originalPaint != null) {
      originalPaint.setColor(color);
    }
  }

  /**
   * Set the color of the border surrounding the panel.
   *
   * @param color
   *     the color value
   */
  public void setBorderColor(int color) {
    borderColor = color;
    invalidate();
  }

  /**
   * @return the color of the border surrounding the panel.
   */
  public int getBorderColor() {
    return borderColor;
  }

  /**
   * Set the shape.
   *
   * @param shape
   *     Either {@link Shape#RECT} or {@link Shape#CIRCLE}.
   */
  public void setShape(@Shape int shape) {
    this.shape = shape;
    invalidate();
  }

  /**
   * Get the shape
   *
   * @return Either {@link Shape#RECT} or {@link Shape#CIRCLE}.
   */
  @Shape public int getShape() {
    return shape;
  }

  /**
   * Show a toast message with the hex color code below the view.
   */
  public void showHint() {
    final int[] screenPos = new int[2];
    final Rect displayFrame = new Rect();
    getLocationOnScreen(screenPos);
    getWindowVisibleDisplayFrame(displayFrame);
    final Context context = getContext();
    final int width = getWidth();
    final int height = getHeight();
    final int midy = screenPos[1] + height / 2;
    int referenceX = screenPos[0] + width / 2;
    if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
      referenceX = screenWidth - referenceX; // mirror
    }
    Toast cheatSheet = Toast.makeText(context, String.format("#%06X", 0xFFFFFF & color), Toast.LENGTH_SHORT);
    if (midy < displayFrame.height()) {
      // Show along the top; follow action buttons
      cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, referenceX,
          screenPos[1] + height - displayFrame.top);
    } else {
      // Show along the bottom center
      cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
    }
    cheatSheet.show();
  }

  private Drawable createSelector(int color) {
    ShapeDrawable darkerCircle = new ShapeDrawable(new OvalShape());
    darkerCircle.getPaint().setColor(translucentColor(shiftColor(color, 1.1f)));
    StateListDrawable stateListDrawable = new StateListDrawable();
    stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, darkerCircle);
    return stateListDrawable;
  }

  @ColorInt private int shiftColor(@ColorInt int color, @FloatRange(from = 0.0f, to = 2.0f) float by) {
    if (by == 1f) return color;
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[2] *= by;
    return Color.HSVToColor(hsv);
  }

  @ColorInt private int translucentColor(int color) {
    final float factor = 0.7f;
    int alpha = Math.round(Color.alpha(color) * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }

  @IntDef({Shape.RECT, Shape.CIRCLE})
  public @interface Shape {
    int RECT = 0;

    int CIRCLE = 1;
  }

}