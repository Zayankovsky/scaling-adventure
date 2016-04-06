package com.example.zayankovsky.homework;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ImageListFragment extends Fragment {
    /**
     * The fragment argument representing the section number
     * for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * The fragment argument representing the column count
     * for this fragment.
     */
    private static final String ARG_COLUMN_COUNT = "column-count";

    /**
     * The fragment argument representing number of images to download at a time
     * for this fragment.
     */
    private static final String ARG_BATCH_SIZE = "batch_size";

    private int mSectionNumber = 0;
    private int mColumnCount = 4;
    private int mBatchSize = 20;
    private OnImageListInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ImageListFragment() {}

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ImageListFragment newInstance(int sectionNumber, int columnCount, int batchSize) {
        ImageListFragment fragment = new ImageListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putInt(ARG_BATCH_SIZE, batchSize);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mSectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            mBatchSize = getArguments().getInt(ARG_BATCH_SIZE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_list_fragment, container, false);
        View list = view.findViewById(R.id.list);

        // Set the adapter
        if (list instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) list;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            ImageListAdapter adapter = new ImageListAdapter(mListener, mSectionNumber, mBatchSize, view);

            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.left = 5;
                    outRect.top = 5;
                    outRect.bottom = 5;
                }
            });

            if (view instanceof SwipeRefreshLayout) {
                final SwipeRefreshLayout layout = (SwipeRefreshLayout) view;
                if (mSectionNumber == 2) {
                    layout.setEnabled(false);
                } else {
                    layout.setOnRefreshListener(adapter);
                }
            }
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnImageListInteractionListener) {
            mListener = (OnImageListInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnImageListInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public int getSectionNumber() {
        return mSectionNumber;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnImageListInteractionListener {
        void onImageListInteraction(ImageListAdapter.ViewHolder holder);
    }
}
