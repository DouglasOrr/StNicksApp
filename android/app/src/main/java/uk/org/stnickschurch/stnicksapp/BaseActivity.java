package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Events;

public class BaseActivity extends AppCompatActivity {
    private CompositeDisposable mDisposeOnDestroy;
    private CompositeDisposable mDisposeOnPause;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        mDisposeOnDestroy = new CompositeDisposable();

        // Very basic error/message presentation (toasts are bad!)
        Events events = Events.SINGLETON.get(this);
        disposeOnDestroy(Events.SINGLETON.get(this).events(Events.Level.INFO)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Events.Event>() {
                    @Override
                    public void accept(Events.Event event) {
                        Toast.makeText(BaseActivity.this, event.message, Toast.LENGTH_LONG).show();
                    }
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDisposeOnPause = new CompositeDisposable();
    }

    protected void disposeOnDestroy(Disposable disposable) {
        mDisposeOnDestroy.add(disposable);
    }

    protected void disposeOnPause(Disposable disposable) {
        mDisposeOnPause.add(disposable);
    }

    @Override
    protected void onPause() {
        mDisposeOnPause.dispose();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mDisposeOnDestroy.dispose();
        super.onDestroy();
    }
}
