package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.exoplayer2.ui.PlayerControlView;

import org.joda.time.format.DateTimeFormat;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import uk.org.stnickschurch.stnicksapp.core.Downloader;
import uk.org.stnickschurch.stnicksapp.core.Sermon;
import uk.org.stnickschurch.stnicksapp.core.Sermons;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class MediaFragment extends Fragment {
    public static class SermonViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_sermon) View mRoot;
        //@BindView(R.id.text_sermon_series) TextView mSeries;
        @BindView(R.id.text_sermon_title) TextView mTitle;
        @BindView(R.id.text_sermon_passage) TextView mPassage;
        @BindView(R.id.text_sermon_speaker) TextView mSpeaker;
        @BindView(R.id.text_sermon_time) TextView mTime;
        public SermonViewHolder(View root) {
            super(root);
            ButterKnife.bind(this, root);
        }
        public void bindTo(final Sermon sermon) {
            //mSeries.setText(sermon.series);
            mTitle.setText(sermon.title);
            mPassage.setText(sermon.passage);
            mSpeaker.setText(sermon.speaker);
            mTime.setText(sermon.time.toString(DateTimeFormat.forPattern("EEE d MMMM y")));
            mRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((Home) mRoot.getContext()).downloader().playing.onNext(sermon);
                }
            });
        }
    }

    public static class SermonListAdapter extends Utility.ObserverListAdapter<Sermon, SermonViewHolder> {
        public SermonListAdapter() {
            super(Utility.<Sermon>identityDiff());
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_media, container, false);

        RecyclerView recycler = root.findViewById(R.id.recyclerview_media);
        recycler.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        SermonListAdapter adapter = new SermonListAdapter();
        recycler.setAdapter(adapter);
        Downloader downloader = ((Home) getActivity()).downloader();
        downloader.sermons
            .map(new Function<Sermons, List<Sermon>>() {
                @Override
                public List<Sermon> apply(Sermons sermons) throws Exception {
                    return sermons.list;
                }
            })
            .subscribe(adapter);

        root.<PlayerControlView>findViewById(R.id.player_media).setPlayer(downloader.player);
        downloader.playing.subscribe(new Consumer<Sermon>() {
            @Override
            public void accept(Sermon sermon) throws Exception {
                root.<PlayerControlView>findViewById(R.id.player_media).setVisibility(View.VISIBLE);
            }
        });
        return root;
    }
}
