package com.example.zayankovsky.homework;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ImageListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ImageListFragment.OnImageListInteractionListener {

    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("theme", "light");
        setTheme(theme.equals("dark") ? R.style.DarkAppTheme_NoActionBar : R.style.LightAppTheme_NoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

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
                GradientDrawable.Orientation.BR_TL, theme.equals("dark") ?
                new int[] {0xFF4CAF50, 0xFF388E3C, 0xFF1B5E20} : new int[] {0xFFC8E6C9, 0xFF81C784, 0xFF4CAF50}
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
            public void onPageScrollStateChanged(int state) {
            }
        });

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (ImageWorker.getNumberOfUrls() == 0) {
            String stringUrl = "http://api-fotki.yandex.ru/api/podhistory/poddate;2012-04-01T12:00:00Z/";
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new DownloadXmlTask().execute(stringUrl);
            }
        }

        ImageWorker.init(getResources(), connMgr, displayMetrics.widthPixels, displayMetrics.densityDpi, columnCount);
        ImageCache.init(this);
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
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
    public void onImageListInteraction(ImageListAdapter.ViewHolder holder) {
        final Intent i = new Intent(this, ImageDetailActivity.class);
        i.putExtra(ImageDetailActivity.SECTION_NUMBER, holder.sectionNumber);
        i.putExtra(ImageDetailActivity.POSITION, holder.position);
        BitmapDrawable drawable = (BitmapDrawable) holder.mImageView.getDrawable();
        if (drawable != null) {
            i.putExtra(ImageDetailActivity.THUMBNAIL_BITMAP, drawable.getBitmap());
            ActivityOptions options = ActivityOptions.makeThumbnailScaleUpAnimation(
                    holder.mImageView, drawable.getBitmap(), 0, 0
            );
            startActivity(i, options.toBundle());
        }
        else {
            startActivity(i);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private int mColumnCount;

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
    private class DownloadXmlTask extends AsyncTask<String, Void, List<SortedMap<Integer, String>>> {
        @Override
        protected List<SortedMap<Integer, String>> doInBackground(String... urls) {
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                return new ArrayList<>();
            } catch (XmlPullParserException e) {
                return new ArrayList<>();
            }
        }

        @Override
        protected void onPostExecute(List<SortedMap<Integer, String>> result) {
            ImageWorker.init(result);
            mViewPager.getAdapter().notifyDataSetChanged();
        }
    }

    // Uploads XML from fotki.yandex.ru, parses it. Returns List of image urls.
    private List<SortedMap<Integer, String>> loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        // Instantiate the parser
        YandexFotkiXmlParser yandexFotkiXmlParser = new YandexFotkiXmlParser();
        List<SortedMap<Integer, String>> images = null;

        try {
            stream = downloadUrl(urlString);
            images = yandexFotkiXmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // YandexFotkiXmlParser returns a List (called "images") of SortedMap objects.
        // Each SortedMap object represents a single image in the XML feed.
        return images;
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

        public List<SortedMap<Integer, String>> parse(InputStream in) throws XmlPullParserException, IOException {
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

        private List<SortedMap<Integer, String>> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
            List<SortedMap<Integer, String>> images = new ArrayList<>();

            parser.require(XmlPullParser.START_TAG, ns, "feed");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                // Starts by looking for the entry tag
                if (name.equals("entry")) {
                    images.add(readImage(parser));
                } else {
                    skip(parser);
                }
            }
            return images;
        }

        // Parses the contents of an image. If it encounters a f:img tag, hands it off
        // to readUrl methods for processing. Otherwise, skips the tag.
        private SortedMap<Integer, String> readImage(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG, ns, "entry");
            SortedMap<Integer, String> image = new TreeMap<>();
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("f:img")) {
                    readUrl(parser, image);
                } else {
                    skip(parser);
                }
            }
            return image;
        }

        // Processes f:img tags in the feed.
        private void readUrl(XmlPullParser parser, SortedMap<Integer, String> image) throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, "f:img");
            int width = Integer.parseInt(parser.getAttributeValue(null, "width"));
            String link = parser.getAttributeValue(null, "href");
            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, ns, "f:img");
            image.put(width, link);
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
