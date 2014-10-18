package org.corveecitoyenne.extraexpress;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SubmitFragment extends Fragment {
    public static final String ARG_IMG_BTYES = "img_bytes";
    private static final String TAG = SubmitFragment.class.getSimpleName();
    private View mRoot;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = null;
    private String mPictureLocation = null;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SumbitFragment.
     */
    public static SubmitFragment newInstance() {
        SubmitFragment fragment = new SubmitFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SubmitFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity()) //
                .addApi(LocationServices.API) //
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        runLocationTask();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                }) //
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.submit_picture, container, false);
        runImageTask();
        mRoot.findViewById(R.id.mtl311).setOnClickListener(new EmailSubmitListener("311citoyen@gmail.com"));
        mRoot.findViewById(R.id.mtlprk).setOnClickListener(new EmailSubmitListener("311citoyen@gmail.com"));
        mRoot.findViewById(R.id.mtlstm).setOnClickListener(new EmailSubmitListener("311citoyen@gmail.com"));
        return mRoot;
    }

    private void runImageTask() {
        byte[] bytes = getArguments().getByteArray(ARG_IMG_BTYES);
        AsyncTask<byte[], Void, Drawable> convert = new AsyncTask<byte[], Void, Drawable>() {
            @Override
            protected Drawable doInBackground(byte[]... params) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(params[0]);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Matrix matrix = new Matrix();
                matrix.postRotate(90, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true);
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "311citoyen");
                if (!file.mkdirs()) {
                    Log.e(TAG, "Directory not created");
                }
                File pic = new File(file, "" + Calendar.getInstance().getTimeInMillis() + ".jpg");
                Log.d(TAG, pic.getAbsolutePath());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(pic);
                    fos.write(params[0]);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mPictureLocation = pic.getAbsolutePath();
                ExifInterface eif = null;
                try {
                    eif = new ExifInterface(pic.getAbsolutePath());
                    eif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
                    eif.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                updateLocation();

                return new BitmapDrawable(getActivity().getResources(), rotatedBitmap);
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                ((ImageView) mRoot.findViewById(R.id.photo)).setImageDrawable(drawable);
                ((ImageView) mRoot.findViewById(R.id.photo)).setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        };
        convert.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, bytes); // TODO KEEP THAT TO ENSURE NO CONCURRENT ACCESS BETWEEN TASKS
    }

    private void runLocationTask() {
        AsyncTask<String, Void, Void> task =
                new AsyncTask<String, Void, Void>() {

                    @Override
                    protected Void doInBackground(String... params) {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        Log.d(TAG, "GOT LOCATION");
                        updateLocation();
                        return null;
                    }
                };
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void updateLocation() {
        String GPS_DATE_FORMAT_STR = "yyyy:MM:dd";
        String GPS_TIME_FORMAT_STR = "kk/1,mm/1,ss/1";

        SimpleDateFormat mGPSDateStampFormat = new SimpleDateFormat(GPS_DATE_FORMAT_STR);
        SimpleDateFormat mGPSTimeStampFormat = new SimpleDateFormat(GPS_TIME_FORMAT_STR);
        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        mGPSDateStampFormat.setTimeZone(tzUTC);
        mGPSTimeStampFormat.setTimeZone(tzUTC);


        if (mLastLocation != null && mPictureLocation != null) {
            Log.d(TAG, "Updating location");
            ExifInterface eif = null;
            try {
                eif = new ExifInterface(mPictureLocation);

                eif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2DMS(mLastLocation.getLatitude()));
                eif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, dec2DMS(mLastLocation.getLongitude()));
                if (mLastLocation.getLatitude() > 0)
                    eif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
                else
                    eif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                if (mLastLocation.getLongitude() > 0)
                    eif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
                else
                    eif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
                eif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "0/1000");
                eif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0");
                eif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "ASCII");
                eif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, mGPSDateStampFormat.format(mLastLocation.getTime()));
                eif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, mGPSTimeStampFormat.format(mLastLocation.getTime()));

                eif.saveAttributes();

                File f = new File(mPictureLocation);
                Uri contentUri = Uri.fromFile(f);
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                getActivity().sendBroadcast(mediaScanIntent);


                Intent newPicture = new Intent(Camera.ACTION_NEW_PICTURE);
                newPicture.setType("image/jpeg");
                newPicture.setData(contentUri);
                getActivity().sendBroadcast(newPicture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    String dec2DMS(double coord) {
        coord = coord > 0 ? coord : -coord;  // -105.9876543 -> 105.9876543
        String sOut = Integer.toString((int) coord) + "/1,";   // 105/1,
        coord = (coord % 1) * 60;         // .987654321 * 60 = 59.259258
        sOut = sOut + Integer.toString((int) coord) + "/1, ";   // 105/1,59/1,
        coord = (coord % 1) * 600000;             // .259258 * 60000 = 15555
        sOut = sOut + Integer.toString((int) coord) + "/10000";   // 105/1,59/1,15555/1000
        return sOut;
    }

    private class EmailSubmitListener implements View.OnClickListener {
        private String mDest;

        public EmailSubmitListener(String dest) {
            mDest = dest;
        }

        @Override
        public void onClick(View v) {
            final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{mDest});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Relevé de dégradation");
            String mapsUrl = String.format(Locale.ENGLISH, "http://maps.google.com/maps?t=h&q=loc:%f,%f&z=17", mLastLocation.getLatitude(), mLastLocation.getLongitude());
            emailIntent.putExtra(Intent.EXTRA_TEXT, mapsUrl);
            //has to be an ArrayList
            ArrayList<Uri> uris = new ArrayList<Uri>();
            //convert from paths to Android friendly Parcelable Uri's
            File fileIn = new File(mPictureLocation);
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            final PackageManager pm = getActivity().getPackageManager();
            final List<ResolveInfo> matches = pm.queryIntentActivities(emailIntent, 0);
            ResolveInfo best = null;
            for (final ResolveInfo info : matches)
                if (info.activityInfo.packageName.endsWith(".gm") ||
                        info.activityInfo.name.toLowerCase().contains("gmail")) best = info;
            if (best != null){
                emailIntent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
            startActivity(emailIntent);} else{
                getActivity().startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        }
    }
}
