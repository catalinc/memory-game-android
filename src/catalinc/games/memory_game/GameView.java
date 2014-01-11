package catalinc.games.memory_game;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread mGameThread;

    private TextView mStatusView;

    private TextView mScoreView;

    public GameView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        mGameThread = new GameThread(holder, context,
                                     new Handler() {
                                         @Override
                                         public void handleMessage(Message m) {
                                             mStatusView.setVisibility(m.getData().getInt("vis"));
                                             mStatusView.setText(m.getData().getString("text"));
                                         }
                                     },
                                     new Handler() {
                                         @Override
                                         public void handleMessage(Message m) {
                                             mScoreView.setVisibility(m.getData().getInt("vis"));
                                             mScoreView.setText(m.getData().getString("text"));
                                         }
                                     }
        );

        setFocusable(true);
    }

    public void setStatusView(TextView textView) {
        mStatusView = textView;
    }

    public void setScoreView(TextView textView) {
        mScoreView = textView;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            mGameThread.pause();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mGameThread.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mGameThread.setRunning(true);
        mGameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        mGameThread.setRunning(false);
        while (retry) {
            try {
                mGameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mGameThread.onTouch(event);
        }
        return true;
    }

    GameThread getGameThread() {
        return mGameThread;
    }

}
