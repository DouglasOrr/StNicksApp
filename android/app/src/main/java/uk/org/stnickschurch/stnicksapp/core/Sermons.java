package uk.org.stnickschurch.stnicksapp.core;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Sermons {
    public final List<Sermon> list;
    public Sermons(List<Sermon> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("list", list)
                .toString();
    }

    public static Sermons fromJson(JSONObject obj) throws JSONException {
        JSONArray objSermons = obj.getJSONArray("sermons");
        ImmutableList.Builder<Sermon> sermons = ImmutableList.builder();
        for (int i = 0; i < objSermons.length(); ++i) {
            sermons.add(Sermon.fromJson(objSermons.getJSONObject(i)));
        }
        return new Sermons(sermons.build());
    }
}
