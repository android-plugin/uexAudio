/*
 *  Copyright (C) 2014 The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.zywx.wbpalmstar.plugin.uexaudio;

import org.zywx.wbpalmstar.base.ResoureFinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MusicFileAdapter extends BaseAdapter {

	private String[] list_item;
	private LayoutInflater m_inflater;
	private onTextItemListener textListener=null;
	private Context context=null;

	public MusicFileAdapter(Context context,String[] _list){
		this.context=context;
		m_inflater = LayoutInflater.from(context);
		list_item=_list;
	}

	@Override
	public int getCount() {
		return list_item.length;
	}

	@Override
	public Object getItem(int position) {
		return list_item[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void setListener(onTextItemListener listener) {
		textListener = listener;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewCache viewCache = null;
		if (convertView == null){
			viewCache = new ViewCache();
			convertView = m_inflater.inflate(ResoureFinder.getInstance().getLayoutId(context, "plugin_audio_file_list_item"), null);
			viewCache.textview = (TextView) convertView.findViewById(ResoureFinder.getInstance().getId(context, "plugin_music_file_list_item_text"));	
			convertView.setTag(viewCache);
		}else{
			viewCache = (ViewCache) convertView.getTag();
		}
		viewCache.textview.setText(list_item[position].substring(list_item[position].lastIndexOf('/')+1));
		viewCache.textview.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_UP:
					if(textListener!=null){
						textListener.onItemClick(null, position);
					}
					break;
				case MotionEvent.ACTION_DOWN:
					break;
				}
				return false;
			}
		});
		return convertView;
	}

	private static class ViewCache {
		public TextView textview;
	}

	public static interface onTextItemListener{
		void onItemClick(Button button, int postion);
	}
}
