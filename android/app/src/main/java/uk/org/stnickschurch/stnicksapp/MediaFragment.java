package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.common.base.Objects;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;
import uk.org.stnickschurch.stnicksapp.core.Player;
import uk.org.stnickschurch.stnicksapp.core.Sermon;
import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class MediaFragment extends Fragment {
    public static class SermonViewHolder extends RecyclerView.ViewHolder {
        public static final DateTimeFormatter TIME_FORMAT = DateTimeFormat.forPattern("EEE d MMMM y");

        Sermon mSermon;
        @BindView(R.id.item_sermon) View mRoot;
        @BindView(R.id.text_sermon_title) TextView mTitle;
        @BindView(R.id.text_sermon_passage) TextView mPassage;
        @BindView(R.id.text_sermon_speaker) TextView mSpeaker;
        @BindView(R.id.text_sermon_time) TextView mTime;
        @BindView(R.id.buttonbar_sermon_action) View mActionButtons;
        @BindView(R.id.button_sermon_retry) ImageButton mButtonRetry;
        @BindView(R.id.button_sermon_download) ImageButton mButtonDownload;
        @BindView(R.id.button_sermon_delete) ImageButton mButtonDelete;

        public SermonViewHolder(View root) {
            super(root);
            ButterKnife.bind(this, root);
        }

        private void action(Player.Action.Type type) {
            Player.SINGLETON.get(mRoot.getContext()).actions.onNext(new Player.Action(mSermon, type));
        }
        @OnClick(R.id.button_sermon_play)
        public void clickPlay() {
            action(Player.Action.Type.PLAY);
        }
        @OnClick(R.id.button_sermon_download)
        public void clickDownload() {
            action(Player.Action.Type.DOWNLOAD);
        }
        @OnClick(R.id.button_sermon_retry)
        public void clickRetry() {
            action(Player.Action.Type.DOWNLOAD);
        }
        @OnClick(R.id.button_sermon_delete)
        public void clickDelete() {
            action(Player.Action.Type.DELETE);
        }
        @OnClick(R.id.item_sermon)
        public void click() {
            mActionButtons.setVisibility(mActionButtons.getVisibility() != View.GONE ? View.GONE : View.VISIBLE);
        }

        public void bindTo(final Sermon sermon) {
            mSermon = sermon;
            mTitle.setText(sermon.title);
            mPassage.setText(sermon.passage);
            mSpeaker.setText(sermon.speaker);
            mTime.setText(sermon.getTime().toString(TIME_FORMAT));
            mActionButtons.setVisibility(View.GONE);
            switch (sermon.local.downloadState) {
                case NONE:
                    mButtonDownload.setVisibility(View.VISIBLE);
                    mButtonRetry.setVisibility(View.GONE);
                    mButtonDelete.setVisibility(View.GONE);
                    break;
                case DOWNLOADING:
                    mButtonDownload.setVisibility(View.GONE);
                    mButtonRetry.setVisibility(View.GONE);
                    mButtonDelete.setVisibility(View.GONE);
                    break;
                case FAILED:
                    mButtonDownload.setVisibility(View.GONE);
                    mButtonRetry.setVisibility(View.VISIBLE);
                    mButtonDelete.setVisibility(View.GONE);
                    break;
                case DOWNLOADED:
                    mButtonDownload.setVisibility(View.GONE);
                    mButtonRetry.setVisibility(View.GONE);
                    mButtonDelete.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    public static class SermonListAdapter extends Utility.ObserverListAdapter<Sermon, SermonViewHolder> {
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
}
