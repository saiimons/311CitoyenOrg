package org.corveecitoyenne.extraexpress;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
}
