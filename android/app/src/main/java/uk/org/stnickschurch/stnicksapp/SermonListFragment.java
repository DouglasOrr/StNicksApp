package uk.org.stnickschurch.stnicksapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import uk.org.stnickschurch.stnicksapp.core.Notifications;
import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;
import uk.org.stnickschurch.stnicksapp.data.DataView;
import uk.org.stnickschurch.stnicksapp.data.Sermon;
import uk.org.stnickschurch.stnicksapp.data.SermonQuery;

public class SermonListFragment extends Fragment {
    public class SermonViewHolder extends RecyclerView.ViewHolder {
        Sermon mSermon;
        @BindView(R.id.item_sermon) View mRoot;
        @BindView(R.id.text_sermon_title) TextView mTitle;
        @BindView(R.id.text_sermon_time) TextView mTime;
        @BindView(R.id.text_sermon_snippet) TextView mSnippet;

        public SermonViewHolder(View root) {
            super(root);
            ButterKnife.bind(this, root);
        }

        @OnClick(R.id.item_sermon)
        public void click() {
            SermonListFragment.this.showDialog(mSermon);
        }

        public void bindTo(final Sermon sermon) {
            mSermon = sermon;
            String title = DataView.uiTitle(sermon);
            mTitle.setText(Html.fromHtml(title));
            mTime.setText(DataView.date(sermon));
            String snippet;
            if (sermon.title.hasSnippet()) {
                snippet = sermon.title.snippet;
            } else if (sermon.speaker.hasSnippet()) {
                snippet = getString(R.string.sermon_snippet_speaker,sermon.speaker.snippet);
            } else {
                snippet = "";
            }
            mSnippet.setText(snippet.equals(title) ? "" : Html.fromHtml(snippet));
        }
    }

    public class SermonListAdapter extends Utility.ObserverListAdapter<Sermon, SermonViewHolder> {
        public SermonListAdapter() {
            super(new DiffUtil.ItemCallback<Sermon>() {
                @Override
                public boolean areItemsTheSame(Sermon oldItem, Sermon newItem) {
                    return Objects.equal(oldItem.id, newItem.id);
                }
                @Override
                public boolean areContentsTheSame(Sermon oldItem, Sermon newItem) {
                    return Objects.equal(oldItem, newItem);
                }
            });
        }
        @NonNull
        @Override
        public SermonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SermonViewHolder(
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(R.layout.item_sermon, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull SermonViewHolder holder, int position) {
            holder.bindTo(getItem(position));
        }
    }

    private CompositeDisposable mDisposeOnDestroy;
    final BehaviorSubject<SermonQuery> mQuery = BehaviorSubject.createDefault(
            new SermonQuery("", false));
    @BindView(R.id.sermon_list_items) RecyclerView mItems;
    @BindView(R.id.sermon_list_downloaded_switch) Switch mDownloadedSwitch;
    @BindView(R.id.sermon_list_filter) EditText mFilter;

    public SermonListFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_sermon_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_sermons:
                Store.SINGLETON.get(getContext()).sync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_sermon_list, container, false);
        ButterKnife.bind(this, root);
        mDisposeOnDestroy = new CompositeDisposable();

        mItems.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        SermonListAdapter adapter = new SermonListAdapter();
        mItems.setAdapter(adapter);
        mDisposeOnDestroy.add(Store.SINGLETON.get(getContext())
                .listSermons(mQuery)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter)
        );
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposeOnDestroy.dispose();
        mDisposeOnDestroy = null;
    }

    @OnClick(R.id.sermon_list_downloaded_switch_label)
    public void clickDownloadedLabel() {
        mDownloadedSwitch.toggle();
    }

    @OnCheckedChanged(R.id.sermon_list_downloaded_switch)
    public void changeDownloadedSwitch(boolean checked) {
        mQuery.onNext(new SermonQuery(mQuery.getValue().search_text, checked));
    }

    @OnTextChanged(R.id.sermon_list_filter)
    public void changeFilter(CharSequence text) {
        mQuery.onNext(new SermonQuery(text.toString(), mQuery.getValue().downloaded_only));
    }

    private void showDialog(final Sermon sermon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_AlertDialogStyle)
            .setTitle(Html.fromHtml(DataView.uiTitle(sermon)))
            .setMessage(Html.fromHtml(DataView.longDescription(sermon, "<br/>")))
            .setIcon(R.drawable.ic_popup)
            .setPositiveButton(R.string.sermon_dialog_play,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PlaybackService.Client.SINGLETON.get(getContext())
                                .start(PlaybackService.ACTION_PLAY, sermon.id, null);
                    }
                });
        if (sermon.download == Sermon.DownloadState.DOWNLOADED) {
            builder.setNeutralButton(R.string.sermon_dialog_delete,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Store.SINGLETON.get(getContext()).deleteDownload(sermon.id);
                        }
                    });
        } else {
            builder.setNeutralButton(
                    sermon.download == Sermon.DownloadState.ATTEMPTED
                            ? R.string.sermon_dialog_retry
                            : R.string.sermon_dialog_download,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Notifications.SINGLETON.get(getContext()).download(sermon);
                        }
                    });
        }
        builder.show();
    }
}
