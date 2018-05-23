package uk.org.stnickschurch.stnicksapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Player;
import uk.org.stnickschurch.stnicksapp.core.Sermon;
import uk.org.stnickschurch.stnicksapp.core.SermonDownload;
import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class MediaFragment extends Fragment {
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormat.forPattern("EEE d MMMM y");
    public class SermonViewHolder extends RecyclerView.ViewHolder {
        Sermon mSermon;
        @BindView(R.id.item_sermon) View mRoot;
        @BindView(R.id.text_sermon_title) TextView mTitle;
        @BindView(R.id.text_sermon_passage) TextView mPassage;
        @BindView(R.id.text_sermon_speaker) TextView mSpeaker;
        @BindView(R.id.text_sermon_time) TextView mTime;

        public SermonViewHolder(View root) {
            super(root);
            ButterKnife.bind(this, root);
        }

        @OnClick(R.id.item_sermon)
        public void click() {
            MediaFragment.this.showDialog(mSermon);
        }

        public void bindTo(final Sermon sermon) {
            mSermon = sermon;
            mTitle.setText(sermon.title);
            mPassage.setText(sermon.passage);
            mSpeaker.setText(sermon.speaker);
            mTime.setText(sermon.getTime().toString(TIME_FORMAT));
        }
    }

    public class SermonListAdapter extends Utility.ObserverListAdapter<Sermon, SermonViewHolder> {
        public SermonListAdapter() {
            super(new DiffUtil.ItemCallback<Sermon>() {
                @Override
                public boolean areItemsTheSame(Sermon oldItem, Sermon newItem) {
                    return oldItem.id.equals(newItem.id);
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

    private Disposable mSermonsListDisposable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_media, container, false);
        RecyclerView recycler = root.findViewById(R.id.recyclerview_media);
        recycler.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        SermonListAdapter adapter = new SermonListAdapter();
        recycler.setAdapter(adapter);
        mSermonsListDisposable = Store.SINGLETON.get(getContext())
                .recentSermons()
                .subscribe(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSermonsListDisposable.dispose();
    }

    private void executeShowDialog(final Sermon sermon, Optional<SermonDownload> download) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(sermon.passage);
        builder.setMessage(Joiner.on("\n").join(
                sermon.title,
                sermon.speaker,
                sermon.getTime().toString(DateTimeFormat.mediumDate())));

        boolean hasRecord = download.isPresent();
        boolean hasFile = hasRecord && download.get().local_path != null;
        builder.setPositiveButton(R.string.sermon_dialog_play,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Player.SINGLETON.get(getContext()).play(sermon);
            }
        });
        if (hasFile) {
            builder.setNeutralButton(R.string.sermon_dialog_delete,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Store.SINGLETON.get(getContext()).delete(sermon);
                }
            });
        } else {
            builder.setNeutralButton(
                    hasRecord ? R.string.sermon_dialog_retry : R.string.sermon_dialog_download,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Store.SINGLETON.get(getContext()).download(sermon);
                }
            });
        }
        builder.show();
    }

    private void showDialog(final Sermon sermon) {
        // TODO: add to unsubscribe list?
        Store.SINGLETON.get(getContext())
                .getDownload(sermon)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Optional<SermonDownload>>() {
            @Override
            public void accept(Optional<SermonDownload> download) {
                executeShowDialog(sermon, download);
            }
        });
    }
}
