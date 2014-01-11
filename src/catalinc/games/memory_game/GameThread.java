package catalinc.games.memory_game;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Handles animation, game logic and user input.
 */
public class GameThread extends Thread {

    private final SurfaceHolder mSurfaceHolder;
    private final Handler       mStatusHandler;
    private final Handler       mScoreHandler;
    private final Context       mContext;

    private static final int STATE_READY     = 0;
    private static final int STATE_PLAYING   = 1;
    private static final int STATE_PAUSE     = 2;
    private static final int STATE_GAME_OVER = 3;

    private volatile boolean mRun;
    private final    Object  mRunLock;
    private          int     mState;

    private int mCanvasHeight;
    private int mCanvasWidth;

    private Paint[] mTilesColorPalette;
    private Paint   mBackgroundPaint;
    private Paint   mHiddenTilePaint;
    private int     mRows;
    private int     mCols;

    private Grid mGrid;
    private int  solved;

    private MotionEvent mTouchEvent;

    private StopWatch mStopWatch;

    private static final int PHYS_FPS = 20;


    GameThread(final SurfaceHolder surfaceHolder,
               final Context context,
               final Handler statusHandler,
               final Handler scoreHandler) {
        mSurfaceHolder = surfaceHolder;
        mStatusHandler = statusHandler;
        mScoreHandler = scoreHandler;
        mContext = context;

        mRun = false;
        mRunLock = new Object();
        mState = STATE_READY;

        mCanvasHeight = 1;
        mCanvasWidth = 1;

        // based on Solarized color palette
        mTilesColorPalette = new Paint[]{paintFromColorString("#002B36"),   // background
                                         paintFromColorString("#073642"),   // hidden tile
                                         paintFromColorString("#93a1a1"),   // tile...
                                         paintFromColorString("#eee8d5"),
                                         paintFromColorString("#b58900"),
                                         paintFromColorString("#cb4b16"),
                                         paintFromColorString("#dc322f"),
                                         paintFromColorString("#d33682"),
                                         paintFromColorString("#6c71c4"),
                                         paintFromColorString("#268bd2"),
                                         paintFromColorString("#2aa198"),
                                         paintFromColorString("#859900")};  // ...color

        mBackgroundPaint = mTilesColorPalette[0];
        mHiddenTilePaint = mTilesColorPalette[1];

        mRows = 5;
        mCols = 4;

        mGrid = Grid.EMPTY;
        solved = 0;

        mStopWatch = new StopWatch();
    }

    /**
     * The game loop.
     */
    @Override
    public void run() {
        int skipTicks = 1000 / PHYS_FPS;
        long mNextGameTick = SystemClock.uptimeMillis();
        while (mRun) {
            Canvas canvas = null;
            try {
                canvas = mSurfaceHolder.lockCanvas(null);
                if (canvas != null) {
                    synchronized (mSurfaceHolder) {
                        if (mState == STATE_PLAYING) {
                            updateState();
                        }
                        synchronized (mRunLock) {
                            if (mRun) {
                                updateDisplay(canvas);
                            }
                        }
                    }
                }
            } finally {
                if (canvas != null) {
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            mNextGameTick += skipTicks;
            long sleepTime = mNextGameTick - SystemClock.uptimeMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // don't care
                }
            }
        }
    }

    void setRunning(boolean running) {
        synchronized (mRunLock) {
            mRun = running;
        }
    }

    void saveState(Bundle map) {
        synchronized (mSurfaceHolder) {
            map.putInt("state", mState);
            map.putInt("solved", solved);
            map.putSerializable("grid", mGrid);
            map.putSerializable("stopWatch", mStopWatch);
        }
    }

    void restoreState(Bundle map) {
        synchronized (mSurfaceHolder) {
            setState(map.getInt("state"));
            solved = map.getInt("solved");
            mGrid = (Grid) map.getSerializable("grid");
            mStopWatch = (StopWatch) map.getSerializable("stopWatch");
        }
    }

    void onTouch(MotionEvent event) {
        synchronized (mSurfaceHolder) {
            switch (mState) {
                case STATE_READY:
                    startNewGame();
                    break;
                case STATE_PLAYING:
                    if (mTouchEvent == null) {
                        mTouchEvent = event;
                    }
                    break;
                case STATE_PAUSE:
                    unPause();
                    break;
                case STATE_GAME_OVER:
                    setState(STATE_READY);
            }
        }
    }

    boolean onBack() {
        synchronized (mSurfaceHolder) {
            if (mState == STATE_PLAYING) {
                pause();
                return false;
            }
            return true;
        }
    }

    void pause() {
        synchronized (mSurfaceHolder) {
            if (mState == STATE_PLAYING) {
                mStopWatch.pause();
                setState(STATE_PAUSE);
            }
        }
    }

    void unPause() {
        synchronized (mSurfaceHolder) {
            mStopWatch.resume();
            setState(STATE_PLAYING);
        }
    }

    void startNewGame() {
        synchronized (mSurfaceHolder) {
            setupGrid();
            mStopWatch.start();
            setState(STATE_PLAYING);
        }
    }

    void setSurfaceSize(int width, int height) {
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
            if (mState == STATE_PLAYING) {
                mGrid.setCanvasWidth(mCanvasWidth);
                mGrid.setCanvasHeight(mCanvasHeight);
            }
        }
    }

    private Paint paintFromColorString(String colorString) {
        Paint paint = new Paint();
        paint.setColor(Color.parseColor(colorString));
        return paint;
    }

    private void setupGrid() {
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        for (int i = 2; i < mTilesColorPalette.length; i++) {
            tiles.add(new Tile(i, Tile.STATE_HIDDEN));
            tiles.add(new Tile(i, Tile.STATE_HIDDEN));
        }
        Collections.shuffle(tiles);

        mGrid = new Grid(mRows, mCols, mCanvasHeight, mCanvasWidth);
        for (int row = 0; row < mRows; row++) {
            for (int col = 0; col < mCols; col++) {
                Tile tile = tiles.get(row * mCols + col);
                mGrid.setTileAt(tile, row, col);
            }
        }

        solved = 0;
    }

    private void setState(int mode) {
        synchronized (mSurfaceHolder) {
            mState = mode;
            Resources res = mContext.getResources();
            switch (mState) {
                case STATE_READY:
                    hideScoreText();
                    setStatusText(res.getString(R.string.state_ready));
                    break;
                case STATE_PLAYING:
                    hideStatusText();
                    break;
                case STATE_PAUSE:
                    setStatusText(res.getString(R.string.state_pause));
                    break;
                case STATE_GAME_OVER:
                    long elapsed = mStopWatch.elapsed() / 1000;
                    long minutes = elapsed / 60;
                    long seconds = elapsed - minutes * 60;
                    StringBuilder scoreText = new StringBuilder();

                    scoreText.append(res.getString(R.string.solved_in))
                             .append(' ');
                    if (minutes > 0) {
                        scoreText.append(minutes)
                                 .append(' ')
                                 .append(minutes == 1 ?
                                         res.getString(R.string.minute) : res.getString(R.string.minutes))
                                 .append(' ');
                    }
                    scoreText.append(seconds)
                             .append(' ')
                             .append(seconds == 1 ?
                                     res.getString(R.string.second) : res.getString(R.string.seconds));

                    setStatusText(res.getString(R.string.state_game_over));
                    setScoreText(scoreText.toString());

            }
        }
    }

    private void updateState() {
        if (mTouchEvent != null) {
            Tile touched = mGrid.getTileAtPoint(mTouchEvent.getX(), mTouchEvent.getY());
            if (touched != null && touched.getState() == Tile.STATE_HIDDEN) {
                Tile selected = mGrid.getSelectedTile();
                if (selected != null) {
                    mGrid.clearSelectedTile();
                    if (selected.getColor() == touched.getColor()) { // found a pair
                        selected.setState(Tile.STATE_SOLVED);
                        selected.setAnimationSteps(PHYS_FPS);
                        touched.setState(Tile.STATE_SOLVED);
                        touched.setAnimationSteps(PHYS_FPS);
                        solved += 2;
                        if (allSolved()) {
                            setState(STATE_GAME_OVER);
                        }
                    } else {
                        mGrid.selectTile(touched);
                    }
                } else {
                    mGrid.selectTile(touched);
                }
            }
            mTouchEvent = null;
        }
    }

    private boolean allSolved() {
        return solved == mGrid.getRows() * mGrid.getCols();
    }

    private void updateDisplay(Canvas canvas) {
        canvas.drawColor(mBackgroundPaint.getColor());

        // draw grid tiles
        for (int row = 0; row < mGrid.getRows(); row++) {
            for (int col = 0; col < mGrid.getCols(); col++) {
                Tile tile = mGrid.getTileAt(row, col);

                int left = col * mGrid.getTileWidth();
                int top = row * mGrid.getTileHeight();
                RectF rect = new RectF(left + 2, top + 2, left + mGrid.getTileWidth() - 2, top + mGrid.getTileHeight() - 2);

                Paint paint;
                switch (tile.getState()) {
                    case Tile.STATE_HIDDEN:
                        paint = mHiddenTilePaint;
                        break;
                    case Tile.STATE_SOLVED:
                        int animationSteps = tile.getAnimationSteps();
                        if (animationSteps > 0) {
                            paint = new Paint(mTilesColorPalette[tile.getColor()]);
                            paint.setAlpha(animationSteps * (255 / PHYS_FPS));
                            tile.setAnimationSteps(animationSteps - 1);
                        } else {
                            paint = mBackgroundPaint;
                        }
                        break;
                    case Tile.STATE_SELECTED:
                    default:
                        paint = mTilesColorPalette[tile.getColor()];
                }
                canvas.drawRoundRect(rect, 2, 2, paint);
            }
        }

    }

    private void setStatusText(String text) {
        Message msg = mStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", text);
        b.putInt("vis", View.VISIBLE);
        msg.setData(b);
        mStatusHandler.sendMessage(msg);
    }

    private void hideStatusText() {
        Message msg = mStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("vis", View.INVISIBLE);
        msg.setData(b);
        mStatusHandler.sendMessage(msg);
    }

    private void setScoreText(String text) {
        Message msg = mScoreHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", text);
        b.putInt("vis", View.VISIBLE);
        msg.setData(b);
        mScoreHandler.sendMessage(msg);
    }

    private void hideScoreText() {
        Message msg = mScoreHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("vis", View.INVISIBLE);
        msg.setData(b);
        mScoreHandler.sendMessage(msg);
    }

}
