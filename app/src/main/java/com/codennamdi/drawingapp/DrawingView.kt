package com.codennamdi.drawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private lateinit var mDrawPath: CustomPath
    private lateinit var mCanvasBitmap: Bitmap
    private lateinit var mDrawPaint: Paint
    private lateinit var mCanvasPaint: Paint
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private lateinit var canvas: Canvas
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPath = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    //This method initializes the attribute of the ViewForDrawing class.
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint.color = color
        mDrawPaint.style = Paint.Style.STROKE
        mDrawPaint.strokeJoin = Paint.Join.ROUND
        mDrawPaint.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
//        mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap)
    }

    //This method is called when a stroke is drawn on the canvas as a part of the painting.
    //We can change Canvas to Canvas?, if it fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap, 0f, 0f, mCanvasPaint)

        for (path in mPaths) {
            mDrawPaint.strokeWidth = path.brushThickness
            mDrawPaint.color = path.color
            canvas.drawPath(path, mDrawPaint)
        }

        if (!mDrawPath.isEmpty) {
            mDrawPaint.strokeWidth = mDrawPath.brushThickness
            mDrawPaint.color = mDrawPath.color
            canvas.drawPath(mDrawPath, mDrawPaint)
        }
    }

    //This method acts as an event listener when a touch event is detected on a device.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath.color = color
                mDrawPath.brushThickness = mBrushSize

                mDrawPath.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath.moveTo(touchX, touchY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath.lineTo(touchX, touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath)
                mDrawPath = CustomPath(color, mBrushSize)
            }

            else -> return false
        }

        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        mDrawPaint.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint.color = color
    }

    //The unClickUndo function. Responsible for undoing the drawing paths
    fun unClickUndo() {
        if (mPaths.size > 0) {
            mUndoPath.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }

    //The unClickRedo function. Responsible for redoing the drawing paths
    fun unClickRedo() {
        if (mPaths.size > 0) {
            mPaths.add(mUndoPath.removeAt(mPaths.size - 1))
            invalidate()
        } else {
            Log.d("REDO_ERROR", "Something went wrong with REDO action")
        }
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path()
}