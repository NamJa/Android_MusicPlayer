package com.example.mymusicplayer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BaseAdapterEx extends BaseAdapter {
    Context context = null;
    ArrayList<MusicData> data = null;
    LayoutInflater layoutInflater = null;
    Drawable img;

    class ViewHolder
    {
        ImageView listViewAlbumArt;
        TextView musicTitle;
    }

    public BaseAdapterEx(Context context, ArrayList<MusicData> data)
    {
        this.context = context;
        this.data = data;
        layoutInflater = LayoutInflater.from(context);
    }

    public int getCount()
    {
        return data.size();
    }

    public long getItemId(int position)
    {
        return position;
    }

    public MusicData getItem(int position)
    {
        return data.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View itemLayout = convertView;
        ViewHolder viewHolder = null;

        if(itemLayout == null) {
            itemLayout = layoutInflater.inflate(R.layout.list_view_item_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.listViewAlbumArt = itemLayout.findViewById(R.id.listViewalbumArt);
            viewHolder.musicTitle = itemLayout.findViewById(R.id.musicTitleText);
            itemLayout.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)itemLayout.getTag();
        }

        viewHolder.listViewAlbumArt.setImageURI(Uri.parse(data.get(position).getAlbumArtUri()));
        viewHolder.musicTitle.setText(data.get(position).getTitle());

        return itemLayout;
    }
}
