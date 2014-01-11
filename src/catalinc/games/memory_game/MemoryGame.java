package catalinc.games.memory_game;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MemoryGame extends Activity {

    private static final int MENU_NEW_GAME = 1;
    private static final int MENU_RESUME   = 2;
    private static final int MENU_EXIT     = 3;

    private GameThread mGameThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.memorygame_layout);

        final GameView mGameView = (GameView) findViewById(R.id.main);
        mGameView.setStatusView((TextView) findViewById(R.id.status));
        mGameView.setScoreView((TextView) findViewById(R.id.score));

        mGameThread = mGameView.getGameThread();
        if (savedInstanceState != null) {
            mGameThread.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameThread.pause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGameThread.saveState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_NEW_GAME, 0, R.string.menu_new_game);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
        menu.add(0, MENU_EXIT, 0, R.string.menu_exit);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case MENU_NEW_GAME:
                mGameThread.startNewGame();
                break;
            case MENU_EXIT:
                finish();
                break;
            case MENU_RESUME:
                mGameThread.unPause();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mGameThread.onBack()) {
            finish();
        }
    }
}
