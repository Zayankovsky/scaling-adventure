package com.example.zayankovsky.homework.ui;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.zayankovsky.homework.R;
import com.example.zayankovsky.homework.util.FotkiImage;
import com.example.zayankovsky.homework.util.FotkiWorker;
import com.example.zayankovsky.homework.util.GalleryWorker;
import com.example.zayankovsky.homework.util.ResourcesWorker;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link RecyclerView.Adapter} that can display an image and makes a call to the
 * specified {@link ImageListFragment.OnImageListInteractionListener}.
 */
public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder>
        implements SwipeRefreshLayout.OnRefreshListener {

    private final ImageListFragment.OnImageListInteractionListener mListener;
    private final int mSectionNumber;
    private final int mBatchSize;
    private final View mParent;
    private boolean lastLoadFromNetworkSuccessful = false;

    public ImageListAdapter(ImageListFragment.OnImageListInteractionListener listener,
                            int sectionNumber, int batchSize, View parent) {
        mListener = listener;
        mSectionNumber = sectionNumber;
        mBatchSize = batchSize;
        mParent = parent;

        switch (sectionNumber) {
            case 0:
                if (GalleryWorker.size() == 0 && ImageListActivity.isReadExternalStoragePermissionGranted()) {
                    updateGallery();
                }
                break;
            case 1:
                if (FotkiWorker.size() == 0) {
                    updateFotki("http://api-fotki.yandex.ru/api/podhistory/poddate/?limit=" + mBatchSize);
                }
                break;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.sectionNumber = mSectionNumber;

        switch (mSectionNumber) {
            case 0:
                GalleryWorker.loadThumbnail(position, holder.mImageView);
                holder.mTextView.setText(String.valueOf(position + 1));
                break;
            case 1:
                FotkiWorker.loadThumbnail(position, holder.mImageView);
                holder.mTextView.setText(
                        new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(FotkiWorker.getPODDate())
                );

                if (lastLoadFromNetworkSuccessful && position == FotkiWorker.size() - 1) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(FotkiWorker.getLastPODDate());
                    calendar.add(Calendar.SECOND, -1);

                    updateFotki("http://api-fotki.yandex.ru/api/podhistory/poddate"
                                    + new SimpleDateFormat(";yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calendar.getTime())
                                    + "/?limit=" + mBatchSize);
                }
                break;
            case 2:
                ResourcesWorker.loadThumbnail(position, holder.mImageView);
                holder.mTextView.setText(String.valueOf(position + 1));
                break;
        }

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
        switch (mSectionNumber) {
            case 0:
                return GalleryWorker.size();
            case 1:
                return FotkiWorker.size();
            case 2:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }

    @Override
    public void onRefresh() {
        switch (mSectionNumber) {
            case 0:
                int itemCount = GalleryWorker.size();
                GalleryWorker.clear();
                notifyItemRangeRemoved(0, itemCount);
                updateGallery();
                break;
            case 1:
                itemCount = FotkiWorker.size();
                FotkiWorker.clear();
                notifyItemRangeRemoved(0, itemCount);
                updateFotki("http://api-fotki.yandex.ru/api/podhistory/poddate/?limit=" + mBatchSize);
                break;
        }
    }

    private void updateGallery() {
        Cursor mCursor = MediaStore.Images.Media.query(
                mParent.getContext().getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.TITLE}
        );

        if (mCursor != null) {
            int indexId = mCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID);
            int indexTitle = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE);
            while (mCursor.moveToNext()) {
                GalleryWorker.add(
                        mCursor.getString(indexTitle),
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(indexId))
                );
                notifyItemInserted(GalleryWorker.size() - 1);
            }
            mCursor.close();
        }

        if (mParent instanceof SwipeRefreshLayout) {
            ((SwipeRefreshLayout) mParent).setRefreshing(false);
        }
    }

    private void updateFotki(String stringUrl) {
        ConnectivityManager connMgr = (ConnectivityManager) mParent.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadXmlTask().execute(stringUrl);
        } else {
            lastLoadFromNetworkSuccessful = false;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;
        public int sectionNumber;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.image);
            mTextView = (TextView) view.findViewById(R.id.text);
        }
    }

    // Implementation of AsyncTask used to download XML feed from fotki.yandex.ru.
    private class DownloadXmlTask extends AsyncTask<String, List<FotkiImage>, List<FotkiImage>> {
        boolean loadFromFileSuccessful = false;

        @Override
        protected List<FotkiImage> doInBackground(String... urls) {
            if (FotkiWorker.size() == 0) {
                try {
                    //noinspection unchecked
                    publishProgress(loadFromFile());
                } catch (IOException ignored) {}
            }

            try {
                return loadFromNetwork(urls[0]);
            } catch (IOException | XmlPullParserException ignored) {
                return new ArrayList<>();
            }
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(List<FotkiImage>... progress) {
            if (!progress[0].isEmpty()) {
                loadFromFileSuccessful = true;
                FotkiWorker.add(progress[0]);
                notifyItemRangeInserted(FotkiWorker.size() - progress[0].size(), progress[0].size());
            }
        }

        @Override
        protected void onPostExecute(List<FotkiImage> result) {
            if (lastLoadFromNetworkSuccessful = !result.isEmpty()) {
                if (loadFromFileSuccessful) {
                    int itemCount = FotkiWorker.size();
                    FotkiWorker.clear();
                    notifyItemRangeRemoved(0, itemCount);
                }
                FotkiWorker.add(result);
                notifyItemRangeInserted(FotkiWorker.size() - result.size(), result.size());
                try {
                    FotkiWorker.save(mParent.getContext().openFileOutput("fotki.dat", Context.MODE_PRIVATE));
                } catch (IOException ignored) {}
            }

            if (mParent instanceof SwipeRefreshLayout) {
                ((SwipeRefreshLayout) mParent).setRefreshing(false);
            }
        }
    }

    private List<FotkiImage> loadFromFile() throws IOException {
        DataInputStream inputStream = new DataInputStream(
                new BufferedInputStream(mParent.getContext().openFileInput("fotki.dat"))
        );

        int fotkiSize = inputStream.readInt();
        List<FotkiImage> fotki = new ArrayList<>(fotkiSize);

        for (int i = 0; i < fotkiSize; ++i) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date podDate = null;
            try {
                podDate = dateFormat.parse(inputStream.readUTF());
            } catch (ParseException ignored) {}

            int urlsSize = inputStream.readInt();
            SortedMap<Integer, String> urls = new TreeMap<>();
            for (int j = 0; j < urlsSize; ++j) {
                urls.put(inputStream.readInt(), inputStream.readUTF());
            }

            Date published = null;
            try {
                published = dateFormat.parse(inputStream.readUTF());
            } catch (ParseException ignored) {}

            fotki.add(new FotkiImage(inputStream.readUTF(), inputStream.readUTF(), published, urls, podDate));
        }

        inputStream.close();
        return fotki;
    }

    // Uploads XML from fotki.yandex.ru, parses it. Returns List of FotkiImage objects.
    private List<FotkiImage> loadFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        // Instantiate the parser
        YandexFotkiXmlParser yandexFotkiXmlParser = new YandexFotkiXmlParser();
        List<FotkiImage> fotki = null;

        try {
            stream = downloadUrl(urlString);
            fotki = yandexFotkiXmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // YandexFotkiXmlParser returns a List (called "fotki") of SortedMap objects.
        // Each SortedMap object represents a single image in the XML feed.
        return fotki;
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private class YandexFotkiXmlParser {
        // We don't use namespaces
        private final String ns = null;

        public List<FotkiImage> parse(InputStream in) throws XmlPullParserException, IOException {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                parser.nextTag();
                return readFeed(parser);
            } finally {
                in.close();
            }
        }

        private List<FotkiImage> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
            List<FotkiImage> fotki = new ArrayList<>();

            parser.require(XmlPullParser.START_TAG, ns, "feed");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                // Starts by looking for the entry tag
                if (name.equals("entry")) {
                    fotki.add(readFotkiImage(parser));
                } else {
                    skip(parser);
                }
            }
            return fotki;
        }

        // Parses the contents of an image. If it encounters a title or f:img tag, hands it off
        // to their respective "read" methods for processing. Otherwise, skips the tag.
        private FotkiImage readFotkiImage(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG, ns, "entry");
            String author = "";
            String title = "";
            Date published = null;
            SortedMap<Integer, String> urls = new TreeMap<>();
            Date podDate = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                switch (parser.getName()) {
                    case "author":
                        author = readAuthor(parser);
                        break;
                    case "title":
                        title = readText(parser, "title");
                        break;
                    case "published":
                        published = readDate(parser, "published");
                        break;
                    case "f:img":
                        readUrl(parser, urls);
                        break;
                    case "f:pod-date":
                        podDate = readDate(parser, "f:pod-date");
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
            return new FotkiImage(author, title, published, urls, podDate);
        }

        // Processes author tags in the feed.
        private String readAuthor(XmlPullParser parser) throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, "author");
            String author = "";
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (parser.getName().equals("name")) {
                    author = readText(parser, "name");
                } else {
                    skip(parser);
                }
            }
            return author;
        }

        // For the tags name and title, extracts their text values.
        private String readText(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, tag);
            String result = "";
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.getText();
                parser.nextTag();
            }
            parser.require(XmlPullParser.END_TAG, ns, tag);
            return result;
        }

        // Processes f:img tags in the feed.
        private void readUrl(XmlPullParser parser, SortedMap<Integer, String> urls) throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, "f:img");
            int width = Integer.parseInt(parser.getAttributeValue(null, "width"));
            String url = parser.getAttributeValue(null, "href");
            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, ns, "f:img");
            urls.put(width, url);
        }

        // Processes dates in the feed.
        private Date readDate(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, tag);
            String date = "";
            if (parser.next() == XmlPullParser.TEXT) {
                date = parser.getText();
                parser.nextTag();
            }
            parser.require(XmlPullParser.END_TAG, ns, tag);
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(date);
            } catch (ParseException e) {
                return null;
            }
        }

        private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException();
            }
            int depth = 1;
            while (depth != 0) {
                switch (parser.next()) {
                    case XmlPullParser.END_TAG:
                        depth--;
                        break;
                    case XmlPullParser.START_TAG:
                        depth++;
                        break;
                }
            }
        }
    }
}
