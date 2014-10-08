package org.corveecitoyenne.extraexpress;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayInputStream;
import java.net.URL;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SubmitFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SubmitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SubmitFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private View mRoot;
    private GoogleApiClient mLocationProvider;

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
        mLocationProvider = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addApi(LocationServices.API).build();
        mLocationProvider.connect();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationProvider.disconnect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.submit_picture, container, false);
        return mRoot;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setImageBytes(byte[] bytes) {
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

                return new BitmapDrawable(getActivity().getResources(), rotatedBitmap);
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                ((ImageView) mRoot.findViewById(R.id.photo)).setImageDrawable(drawable);
                ((ImageView) mRoot.findViewById(R.id.photo)).setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        };
        convert.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bytes);
    }

    public void startLocationUpdate() {
        final ImageView map = (ImageView) mRoot.findViewById(R.id.map);
        map.post(new Runnable() {
            @Override
            public void run() {
                Location l = LocationServices.FusedLocationApi.getLastLocation(mLocationProvider);
                Uri.Builder builder = Uri.parse("https://maps.googleapis.com/maps/api/staticmap").buildUpon();
                builder.appendQueryParameter("center", l.getLatitude() + "," + l.getLongitude());
                builder.appendQueryParameter("markers", "color:red|"+l.getLatitude() + "," + l.getLongitude());
                builder.appendQueryParameter("zoom","13");
                builder.appendQueryParameter("size", map.getMeasuredWidth() + "x" + map.getMeasuredHeight());
                builder.appendQueryParameter("maptype", "roadmap");
                Log.d("MAP", builder.build().toString());
            }
        });

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
    }

}
