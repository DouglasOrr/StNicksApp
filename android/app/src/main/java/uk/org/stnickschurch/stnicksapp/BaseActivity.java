package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Events;

public class BaseActivity extends AppCompatActivity {
    private CompositeDisposable mDisposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        mDisposables = new CompositeDisposable();

        // Very basic error/message presentation (toasts are bad!)
        Events events = Events.SINGLETON.get(this);
        disposeOnDestroy(Observable.merge(events.errors, events.messages)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String message) {
                        Toast.makeText(BaseActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }));
    }

    protected void disposeOnDestroy(Disposable disposable) {
        mDisposables.add(disposable);
    }

    @Override
    protected void onDestroy() {
        mDisposables.dispose();
        super.onDestroy();
    }
}
