package com.example.zayankovsky.homework;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link RecyclerView.Adapter} that can display an image and makes a call to the
 * specified {@link ImageListFragment.OnImageListInteractionListener}.
 */
public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {

    private final ImageListFragment.OnImageListInteractionListener mListener;
    private final int mSectionNumber;

    public ImageListAdapter(ImageListFragment.OnImageListInteractionListener listener, int sectionNumber) {
        mListener = listener;
        mSectionNumber = sectionNumber;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.sectionNumber = mSectionNumber;
        holder.position = position;
        if (mSectionNumber == 1) {
            ImageWorker.loadYandexThumbnail(position, holder.mImageView);
        } else {
            ImageWorker.loadThumbnail(position, holder.mImageView);
        }
        holder.mTextView.setText(String.valueOf(position + 1));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onImageListInteraction(holder);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mSectionNumber == 1) {
            return ImageWorker.getNumberOfUrls();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;
        public int sectionNumber;
        public int position;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.image);
            mTextView = (TextView) view.findViewById(R.id.text);
        }
    }
}
