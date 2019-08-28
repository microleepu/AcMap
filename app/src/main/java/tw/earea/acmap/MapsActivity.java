package tw.earea.acmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    //New variables for Current Place Picker
    private static final String TAG ="MapsActivity";
    ListView lstPlaces;
    private PlacesClient mPlaceClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //The geographical location where the device is currently located. That is ,the last-known
    //location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    //A default location (Sydney,Australia) and default zoom to use where location permission is
    //not granted.  25.0335788,121.5635684
//    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private final LatLng mDefaultLocation = new LatLng(25.0334733, 121.5649612);
    private static final int DEFAULT_ZOOM = 17;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLoocationPermissionGranted;

    private static final int M_MAX_ENTRIES = 10;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lstPlaces = (ListView) findViewById(R.id.listPlaces);

        String apiKey=getString(R.string.google_maps_key);

        Places.initialize(getApplicationContext(),apiKey);
        mPlaceClient = Places.createClient(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_geolocate:
                pickCurrentPlace();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getLocationPermission() {
        mLoocationPermissionGranted =false;
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            mLoocationPermissionGranted = true;
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        getDeviceLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLoocationPermissionGranted = false;
        switch (requestCode){
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:{
                if(grantResults.length>0&&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mLoocationPermissionGranted =true;
                }
            }
        }
        //ssuper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

   //     GoogleMapOptions options = new GoogleMapOptions();
   //     options.mapType(GoogleMap.MAP_TYPE_SATELLITE).compassEnabled(false).rotateGesturesEnabled(false).tiltGesturesEnabled(false);

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
        //25.0335788,121.5635684
        LatLng sydney = new LatLng(25.0334733, 121.5649612);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        //PASTE THE LINES BELOW THIS COMMENT

        //Enable the zoom controls for the map
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //Prompt the user for permission.
        getLocationPermission();
    }

    private void getCurrentPlaceLikeihoods(){
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,Place.Field.LAT_LNG);
        @SuppressWarnings("MissingPermission") final FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            return;
        }
        Task<FindCurrentPlaceResponse> placeResponse = mPlaceClient.findCurrentPlace(request);
        placeResponse.addOnCompleteListener(this,
                new OnCompleteListener<FindCurrentPlaceResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                        if(task.isSuccessful()){
                            FindCurrentPlaceResponse response = task.getResult();
                            int count;
                            if(response.getPlaceLikelihoods().size()<M_MAX_ENTRIES){
                                count = response.getPlaceLikelihoods().size();
                            }else{
                                count = M_MAX_ENTRIES;
                            }
                            int i = 0;
                            mLikelyPlaceNames = new String[count];
                            mLikelyPlaceAddresses = new String[count];
                            mLikelyPlaceAttributions = new String[count];
                            mLikelyPlaceLatLngs = new LatLng[count];

                            for(PlaceLikelihood placeLikelihood:response.getPlaceLikelihoods()){
                                Place currPlace = placeLikelihood.getPlace();
                                mLikelyPlaceNames[i]=currPlace.getName();
                                mLikelyPlaceAddresses[i]=currPlace.getAddress();
                                mLikelyPlaceAttributions[i]=(currPlace.getAttributions()==null)?null:String.join("",currPlace.getAttributions());
                                mLikelyPlaceLatLngs[i]=currPlace.getLatLng();

                                String currLatLng = (mLikelyPlaceLatLngs[i]==null)?"":mLikelyPlaceLatLngs[i].toString();
                                Log.i(TAG,String.format("Place" + currPlace.getName()+"has likelihood:"+placeLikelihood.getLikelihood()+"at"+currLatLng));
                                i++;
                                if(i>(count -1)){
                                    break;
                                }
                            };
                            fillPlacesList();
                        }else{
                            Exception exception = task.getException();
                            if(exception instanceof ApiException){
                                ApiException apiException = (ApiException) exception;
                                Log.e(TAG,"Place not found:"+apiException.getStatusCode());
                            }
                        }
                    }
                });
    }

    private void getDeviceLocation(){
        try{
            if(mLoocationPermissionGranted){
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    return;
                }
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if(location!=null){
                            mLastKnownLocation = location;
                            Log.d(TAG,"Latitude:"+mLastKnownLocation.getLatitude());
                            Log.d(TAG,"Longitude:"+mLastKnownLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()),DEFAULT_ZOOM));
                        }else{
                            Log.d(TAG,"Current location is null.Using defaults.");
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                        }
                        getCurrentPlaceLikeihoods();
                    }
                });
            }
        }catch (Exception e){
            Log.e("Exception:%s",e.getMessage());
        }
    }

    private void pickCurrentPlace(){
        if(mMap==null){
            return;
        }
        if(mLoocationPermissionGranted){
            getDeviceLocation();
        }else{
            Log.i(TAG,"The user did not grant location permission.");

            mMap.addMarker(new MarkerOptions().title("DefaultLocation").position(mDefaultLocation).snippet("No places found,because location permission is disabled."));
            getLocationPermission();
        }
    }

    private AdapterView.OnItemClickListener listClickedHandler =new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LatLng markerLatLng = mLikelyPlaceLatLngs[position];
            String markerSnippet = mLikelyPlaceAddresses[position];
            if(mLikelyPlaceAttributions[position]!=null){
                markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[position];
            }
            mMap.addMarker(new MarkerOptions().title(mLikelyPlaceNames[position]).position(markerLatLng).snippet(markerSnippet));
        }
    };

    private void fillPlacesList(){
        ArrayAdapter<String> placesAdapter= new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mLikelyPlaceNames);
        lstPlaces.setAdapter(placesAdapter);
        lstPlaces.setOnItemClickListener(listClickedHandler);
    }

}

