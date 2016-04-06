package com.example.zayankovsky.homework;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Xml;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public class ImageListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ImageListFragment.OnImageListInteractionListener {

    private ViewPager mViewPager;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    static final String FILE_URI = "fileUri";
    private Uri fileUri;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = sharedPref.getString("theme", "light").equals("dark");
        setTheme(darkTheme ? R.style.DarkAppTheme_NoActionBar : R.style.LightAppTheme_NoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of fileUri from saved state
            fileUri = savedInstanceState.getParcelable(FILE_URI);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        int columnCount = Integer.parseInt(sharedPref.getString("column_count", "4"));
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), columnCount);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.email)});
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_gallery);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BR_TL, darkTheme ?
                new int[]{0xFF4CAF50, 0xFF388E3C, 0xFF1B5E20} : new int[]{0xFFC8E6C9, 0xFF81C784, 0xFF4CAF50}
        );
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);

        navigationView.getHeaderView(0).setBackground(gradientDrawable);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    navigationView.setCheckedItem(R.id.nav_gallery);
                } else if (position == 1) {
                    navigationView.setCheckedItem(R.id.nav_fotki);
                } else if (position == 2) {
                    navigationView.setCheckedItem(R.id.nav_cache);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ImageWorker.init(this, displayMetrics.widthPixels, displayMetrics.densityDpi, columnCount);

        ImageCache.init(
                this, Integer.parseInt(sharedPref.getString("memory_cache_size", "50")) * 1024,
                sharedPref.getBoolean("clear_disk_cache", false)
        );

        if (ImageWorker.getFotkiSize() == 0) {
            String stringUrl = "http://api-fotki.yandex.ru/api/podhistory/poddate;2012-04-01T12:00:00Z/";
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new DownloadXmlTask().execute(stringUrl);
            }
        }

        if (ImageWorker.getGallerySize() == 0) {
            Cursor mCursor = MediaStore.Images.Media.query(
                    getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.TITLE}
            );

            if (mCursor != null) {
                int indexId = mCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                int indexTitle = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE);
                while (mCursor.moveToNext()) {
                    ImageWorker.addToGallery(
                            mCursor.getString(indexTitle),
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(indexId))
                    );
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_gallery) {
            mViewPager.setCurrentItem(0);
        } else if (id == R.id.nav_fotki) {
            mViewPager.setCurrentItem(1);
        } else if (id == R.id.nav_cache) {
            mViewPager.setCurrentItem(2);
        } else if (id == R.id.nav_camera) {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            fileUri = getOutputMediaFileUri(); // create a file to save the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        } else if (id == R.id.nav_rate) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {getResources().getString(R.string.email)});
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
     public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save current fileUri
        savedInstanceState.putParcelable(FILE_URI, fileUri);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                );
            } else {
                saveImageToGallery();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                saveImageToGallery();
            }
        }
    }

    @Override
    public void onImageListInteraction(ImageListAdapter.ViewHolder holder) {
        final Intent i = new Intent(this, ImageDetailActivity.class);
        i.putExtra(ImageDetailActivity.SECTION_NUMBER, holder.sectionNumber);
        i.putExtra(ImageDetailActivity.POSITION, holder.position);
        BitmapDrawable drawable = (BitmapDrawable) holder.mImageView.getDrawable();
        if (drawable != null) {
            ImageWorker.init(drawable.getBitmap());
            ActivityOptions options = ActivityOptions.makeThumbnailScaleUpAnimation(
                    holder.mImageView, drawable.getBitmap(), 0, 0
            );
            startActivity(i, options.toBundle());
        }
        else {
            startActivity(i);
        }
    }

    /** Create a file Uri for saving an image */
    private Uri getOutputMediaFileUri() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Create the storage directory if it does not exist
        if (mediaStorageDir == null || ! mediaStorageDir.exists() && ! mediaStorageDir.mkdirs()) {
            return null;
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");

        return Uri.fromFile(mediaFile);
    }

    private void saveImageToGallery() {
        try {
            String url = MediaStore.Images.Media.insertImage(getContentResolver(), fileUri.getPath(), fileUri.getLastPathSegment(), null);
            ImageWorker.addToGallery(fileUri.getLastPathSegment(), Uri.parse(url));
            //noinspection ResultOfMethodCallIgnored
            new File(fileUri.getPath()).delete();
        } catch (FileNotFoundException ignored) {}
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final int mColumnCount;

        public SectionsPagerAdapter(FragmentManager fm, int columnCount) {
            super(fm);
            mColumnCount = columnCount;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a ImageListFragment.
            return ImageListFragment.newInstance(position, mColumnCount);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.gallery);
                case 1:
                    return getResources().getString(R.string.fotki);
                case 2:
                    return getResources().getString(R.string.cache);
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            if (((ImageListFragment) object).getSectionNumber() == 1) {
                return POSITION_NONE;
            } else {
                return POSITION_UNCHANGED;
            }
        }
    }

    // Implementation of AsyncTask used to download XML feed from fotki.yandex.ru.
    private class DownloadXmlTask extends AsyncTask<String, Void, List<FotkiImage>> {
        @Override
        protected List<FotkiImage> doInBackground(String... urls) {
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                return new ArrayList<>();
            } catch (XmlPullParserException e) {
                return new ArrayList<>();
            }
        }

        @Override
        protected void onPostExecute(List<FotkiImage> result) {
            ImageWorker.init(result);
            if (!isDestroyed()) {
                mViewPager.getAdapter().notifyDataSetChanged();
            }
        }
    }

    // Uploads XML from fotki.yandex.ru, parses it. Returns List of image urls.
    private List<FotkiImage> loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
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
            String title = "";
            SortedMap<Integer, String> urls = new TreeMap<>();
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                switch (parser.getName()) {
                    case "title":
                        title = readTitle(parser);
                        break;
                    case "f:img":
                        readUrl(parser, urls);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
            return new FotkiImage(title, urls);
        }

        // Processes title tags in the feed.
        private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, "title");
            String title = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "title");
            return title;
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

        // For the tags title and summary, extracts their text values.
        private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
            String result = "";
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.getText();
                parser.nextTag();
            }
            return result;
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
