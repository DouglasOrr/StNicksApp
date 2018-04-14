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

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import uk.org.stnickschurch.stnicksapp.core.Player;
import uk.org.stnickschurch.stnicksapp.core.Sermon;
import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class MediaFragment extends Fragment {
    public static class SermonViewHolder extends RecyclerView.ViewHolder {
        public static final DateTimeFormatter TIME_FORMAT = DateTimeFormat.forPattern("EEE d MMMM y");

        @BindView(R.id.item_sermon) View mRoot;
        @BindView(R.id.text_sermon_title) TextView mTitle;
        @BindView(R.id.text_sermon_passage) TextView mPassage;
        @BindView(R.id.text_sermon_speaker) TextView mSpeaker;
        @BindView(R.id.text_sermon_time) TextView mTime;

        public SermonViewHolder(View root) {
            super(root);
            ButterKnife.bind(this, root);
        }

        public void bindTo(final Sermon sermon) {
            mTitle.setText(sermon.title);
            mPassage.setText(sermon.passage);
            mSpeaker.setText(sermon.speaker);
            mTime.setText(sermon.getTime().toString(TIME_FORMAT));
            mRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Player.get(mRoot.getContext()).play(sermon);
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
        mSermonsListDisposable = Store.get(getContext())
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
