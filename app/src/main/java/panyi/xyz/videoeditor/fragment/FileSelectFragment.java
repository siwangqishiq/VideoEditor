/*
 * Copyright 2013 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package panyi.xyz.videoeditor.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.module.MediaQuery;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.TimeUtil;

/**
 * selec a file for video image edit
 *
 */
public class FileSelectFragment extends Fragment {
	private List<SelectFileItem> mFileItemList = new ArrayList<SelectFileItem>(8);

	private RecyclerView mListView;

	@Override
	public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_file_select_list, null);
		loadVideoFileData();
		return v;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mListView = getView().findViewById(R.id.file_list_view);

		mListView.setLayoutManager(new GridLayoutManager(getContext() , 3));
		mListView.setAdapter(new FileItemListAdapter());
	}

	private void loadVideoFileData(){
		mFileItemList.clear();
		mFileItemList.addAll(MediaQuery.queryVideoFileData(getActivity()));
	}

	private void onClickItem(final int pos , final SelectFileItem fileItem){
		Intent result = new Intent();
		result.putExtra("data", fileItem);
		getActivity().setResult(Activity.RESULT_OK, result);
		getActivity().finish();
	}


	/**
	 *
	 */
	private class FileItemListAdapter extends RecyclerView.Adapter<FileItemViewHolder>{

		@NonNull
		@Override
		public FileItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			final View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.view_select_file_item,
					parent, false);
			return new FileItemViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(@NonNull FileItemViewHolder holder, int position) {
			holder.refresh(position , mFileItemList.get(position));
		}

		@Override
		public int getItemCount() {
			return mFileItemList.size();
		}
	}

	private class FileItemViewHolder extends RecyclerView.ViewHolder{
		ImageView imgView;
		TextView descTextView;

		public FileItemViewHolder(@NonNull View itemView) {
			super(itemView);

			imgView = itemView.findViewById(R.id.file_image);
			descTextView = itemView.findViewById(R.id.file_desc);
		}

		public void refresh(final int pos , final SelectFileItem itemData){
			descTextView.setText(TimeUtil.videoTimeDuration(itemData.duration));
			Glide.with(getContext()).load(Uri.fromFile(new File(itemData.path))).into(imgView);

			itemView.setOnClickListener((v)->{
				onClickItem(pos , itemData);
			});
		}
	}//end inner class
}
