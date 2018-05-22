package uk.org.stnickschurch.stnicksapp.core;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;


/**
 * Defines a relation between our Sermon table, the DownloadManager database & filesystem.
 */
@Entity(tableName = "sermon_download")
@ForeignKey(entity = Sermon.class,
            parentColumns = {"id"}, childColumns = {"sermon_id"},
            onDelete = ForeignKey.SET_NULL, onUpdate = ForeignKey.SET_NULL)
public class SermonDownload {
    @NonNull @ColumnInfo(name = "sermon_id") @PrimaryKey
    public String sermon_id;

    @Nullable @ColumnInfo(name = "download_id")
    public Long download_id;

    @Nullable @ColumnInfo(name = "local_path")
    public String local_path;

    public SermonDownload() {
        sermon_id = null;
        download_id = null;
        local_path = null;
    }
    public SermonDownload(@NonNull String sermon_id, long download_id) {
        this.sermon_id = sermon_id;
        this.download_id = download_id;
        this.local_path = null;
    }

    // Object equality

    @Override
    public boolean equals(Object that_) {
        if (that_ == null || this.getClass() != that_.getClass()) {
            return false;
        }
        SermonDownload that = (SermonDownload) that_;
        return Objects.equal(this.sermon_id, that.sermon_id)
                && Objects.equal(this.download_id, that.download_id)
                && Objects.equal(this.local_path, that.local_path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.sermon_id, this.download_id, this.local_path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sermon_id", sermon_id)
                .add("download_id", download_id)
                .add("local_path", local_path)
                .toString();
    }
}
