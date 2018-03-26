package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class BaseActivity extends AppCompatActivity {
    private CompositeDisposable mDisposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        mDisposables = new CompositeDisposable();
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
