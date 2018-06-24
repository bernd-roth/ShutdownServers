package at.co.netconsulting.at.co.netconsulting.general;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class CustomArrayAdapter extends ArrayAdapter{
    private int notifyCalling = 0;
    private int pos;

    public CustomArrayAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, @NonNull Object[] objects) {
        super(context, resource, objects);
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull Object[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, @NonNull List objects) {
        super(context, resource, objects);
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public void add(@Nullable Object object) {
        super.add(object);
    }

    @Override
    public void addAll(@NonNull Collection collection) {
        super.addAll(collection);
    }

    @Override
    public void addAll(Object[] items) {
        super.addAll(items);
    }

    @Override
    public void insert(@Nullable Object object, int index) {
        super.insert(object, index);
    }

    @Override
    public void remove(@Nullable Object object) {
        super.remove(object);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void sort(@NonNull Comparator comparator) {
        super.sort(comparator);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        notifyCalling=1;
    }

    public void notifyDataSetChanged(int pos) {
        super.notifyDataSetChanged();
        notifyCalling=1;
        setPos(pos);
    }

    public void notifyDataSetChanged(int pos, boolean cancel) {
        super.notifyDataSetChanged();
        notifyCalling=0;
        setPos(pos);
    }

    @Override
    public void setNotifyOnChange(boolean notifyOnChange) {
        super.setNotifyOnChange(notifyOnChange);
    }

    @NonNull
    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Nullable
    @Override
    public Object getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getPosition(@Nullable Object item) {
        return super.getPosition(item);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the current item from ListView
        View view = super.getView(position,convertView,parent);

        int pos = getPos();
        if(notifyCalling==1 && position == pos){
            Log.d("getView - if - position", String.valueOf(position));
            view.setBackgroundColor(Color.GREEN);
        }else if(notifyCalling ==1 && position < getPos()){
            Log.d("getView - elseif - position", String.valueOf(position));
            view.setBackgroundColor(Color.RED);
        }else if (position % 2 == 1) {
                view.setBackgroundColor(Color.LTGRAY);
            } else {
                view.setBackgroundColor(Color.WHITE);
            }
        return view;
    }

    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
    }

    @Override
    public void setDropDownViewTheme(@Nullable Resources.Theme theme) {
        super.setDropDownViewTheme(theme);
    }

    @Nullable
    @Override
    public Resources.Theme getDropDownViewTheme() {
        return super.getDropDownViewTheme();
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    @Override
    public CharSequence[] getAutofillOptions() {
        return super.getAutofillOptions();
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}
