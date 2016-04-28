package com.example.zayankovsky.homework.ui;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.zayankovsky.homework.R;
import com.example.zayankovsky.homework.util.FotkiWorker;
import com.example.zayankovsky.homework.util.GalleryWorker;
import com.example.zayankovsky.homework.util.ImageCache;
import com.example.zayankovsky.homework.util.ImageWorker;
import com.example.zayankovsky.homework.util.ResourcesWorker;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ImageListFragment.OnImageListInteractionListener {

    private ViewPager mViewPager;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final String FILE_URI = "fileUri";
    private Uri fileUri;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 200;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 210;

    private static boolean myPermissionsGrantedReadExternalStorage = false;

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
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager(), columnCount, Integer.parseInt(sharedPref.getString("batch_size", "20"))
        );

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
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
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

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        navigationView.setCheckedItem(R.id.nav_gallery);
                        break;
                    case 1:
                        navigationView.setCheckedItem(R.id.nav_fotki);
                        break;
                    case 2:
                        navigationView.setCheckedItem(R.id.nav_cache);
                        break;
                }
            }
        });

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ImageWorker.init(displayMetrics.widthPixels, columnCount);
        FotkiWorker.init(getResources());

        ImageCache.init(
                this, Integer.parseInt(sharedPref.getString("memory_cache_size", "50")) * 1024,
                sharedPref.getBoolean("clear_disk_cache", false)
        );

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            );
        } else {
            myPermissionsGrantedReadExternalStorage = true;
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
        switch (item.getItemId()) {
            case R.id.nav_gallery:
                mViewPager.setCurrentItem(0);
                break;
            case R.id.nav_fotki:
                mViewPager.setCurrentItem(1);
                break;
            case R.id.nav_cache:
                mViewPager.setCurrentItem(2);
                break;
            case R.id.nav_camera:
                // create Intent to take a picture and return control to the calling application
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                fileUri = getOutputMediaFileUri(); // create a file to save the image
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

                // start the image capture Intent
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                break;
            case R.id.nav_rate:
                intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.email)});
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
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
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted, yay!
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                    myPermissionsGrantedReadExternalStorage = true;
                    mViewPager.getAdapter().notifyDataSetChanged();
                    break;
                case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                    saveImageToGallery();
                    break;
            }
        }
    }

    @Override
    public void onImageListInteraction(ImageListAdapter.ViewHolder holder) {
        final Intent i = new Intent(this, ImageDetailActivity.class);
        i.putExtra(ImageDetailActivity.SECTION_NUMBER, holder.sectionNumber);
        i.putExtra(ImageDetailActivity.POSITION, holder.getAdapterPosition());
        BitmapDrawable drawable = (BitmapDrawable) holder.mImageView.getDrawable();
        if (drawable != null) {
            if (holder.sectionNumber == 1) {
                FotkiWorker.init(drawable.getBitmap());
            }
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
            GalleryWorker.add(fileUri.getLastPathSegment(), Uri.parse(url));
            //noinspection ResultOfMethodCallIgnored
            new File(fileUri.getPath()).delete();
        } catch (FileNotFoundException ignored) {}
    }

    public static boolean isReadExternalStoragePermissionGranted() {
        return myPermissionsGrantedReadExternalStorage;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final int mColumnCount;
        private final int mBatchSize;

        public SectionsPagerAdapter(FragmentManager fm, int columnCount, int batchSize) {
            super(fm);
            mColumnCount = columnCount;
            mBatchSize = batchSize;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a ImageListFragment.
            return ImageListFragment.newInstance(position, mColumnCount, mBatchSize);
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
            if (((ImageListFragment) object).getSectionNumber() == 0) {
                return POSITION_NONE;
            }
            return super.getItemPosition(object);
        }
    }
}
