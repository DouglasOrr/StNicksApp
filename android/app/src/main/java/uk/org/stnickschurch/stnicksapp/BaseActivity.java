package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.joda.time.Instant;
import org.joda.time.Period;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import uk.org.stnickschurch.stnicksapp.core.Utility;

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

    protected Period getPeriod(int id) {
        return Utility.PERIOD_SHORT_FORMAT.parsePeriod(getString(id).replace(" ", ""));
    }

    protected long getPeriodMs(int id) {
        return getPeriod(id).toDurationFrom(Instant.now()).getMillis();
    }

    @Override
    protected void onDestroy() {
        mDisposables.dispose();
        super.onDestroy();
    }
}
