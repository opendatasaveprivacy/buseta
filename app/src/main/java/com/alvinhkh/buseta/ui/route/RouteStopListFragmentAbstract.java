package com.alvinhkh.buseta.ui.route;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.follow.model.Follow;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.utils.Utils;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


// TODO: better way to find nearest stop
// TODO: keep (nearest) stop on top
public abstract class RouteStopListFragmentAbstract extends Fragment implements
        ArrayListRecyclerViewAdapter.OnClickItemListener,
        SwipeRefreshLayout.OnRefreshListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        AppBarLayout.OnOffsetChangedListener {

    private final static String SHOW_MAP_STATE_KEY = "SHOW_MAP_STATE_KEY";

    private final static String SCROLL_POSITION_STATE_KEY = "SCROLL_POSITION_STATE_KEY";

    protected final CompositeDisposable disposables = new CompositeDisposable();

    private static ArrivalTimeDatabase arrivalTimeDatabase = null;

    protected RouteStopListAdapter adapter;

    protected SwipeRefreshLayout swipeRefreshLayout;

    protected RecyclerView recyclerView;

    protected View emptyView;

    protected ProgressBar progressBar;

    protected TextView emptyText;

    protected Route route;

    protected RouteStop navToStop = null;

    protected Integer scrollToPos = 0;

    protected GoogleMap map;

    protected List<Pair<Double, Double>> mapCoordinates = new ArrayList<>();

    protected Boolean hasMapCoordinates = false;

    private List<RouteStop> routeStops = new ArrayList<>();

    private Boolean isShowMapFragment = false;

    private SupportMapFragment mapFragment = null;

    private HashMap<String, Marker> markerMap = new HashMap<>();

    public RouteStopListFragmentAbstract() {
    }

    private final Handler initLoadHandler = new Handler();

    private final Runnable initLoadRunnable = new Runnable() {
        @Override
        public void run() {
            if (getView() != null) {
                if (getUserVisibleHint()) {
                    refreshHandler.post(refreshRunnable);
                    adapterUpdateHandler.post(adapterUpdateRunnable);
                    if (isShowMapFragment) {
                        showMapFragment();
                    }
                    if (getActivity() != null) {
                        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
                        if (fab != null) {
                            fab.setOnClickListener(v -> onRefresh());
                        }
                    }
                } else {
                    refreshHandler.removeCallbacksAndMessages(null);
                    adapterUpdateHandler.removeCallbacksAndMessages(null);
                }
                initLoadHandler.removeCallbacksAndMessages(null);
            } else {
                initLoadHandler.postDelayed(this, 5000);  // try every 5 sec
            }
        }
    };

    protected final Handler adapterUpdateHandler = new Handler();

    protected final Runnable adapterUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            adapterUpdateHandler.postDelayed(this, 30000);  // refresh every 30 sec
        }
    };

    private Integer refreshInterval = 0;

    protected final Handler refreshHandler = new Handler();

    protected final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (refreshInterval > 0) {
                onRefresh();
                refreshHandler.postDelayed(this, refreshInterval * 1000);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            route = getArguments().getParcelable(C.EXTRA.ROUTE_OBJECT);
        }
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }
        arrivalTimeDatabase = ArrivalTimeDatabase.Companion.getInstance(getContext());
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.ETA_UPDATE))
                .share().subscribeWith(etaObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.FOLLOW_UPDATE))
                .share().subscribeWith(followObserver()));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        emptyView = rootView.findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        progressBar = rootView.findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        emptyText = rootView.findViewById(R.id.empty_text);
        if (emptyText != null) {
            emptyText.setText(R.string.message_loading);
        }
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        if (getFragmentManager() == null) return rootView;
        adapter = new RouteStopListAdapter(getFragmentManager(), recyclerView, route);
        adapter.setOnClickItemListener(this);
        if (!TextUtils.isEmpty(route.getDescription())) {
            adapter.add(new Item(Item.TYPE_HEADER, route.getDescription()));
        }
        if (getContext() != null && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(getContext())
                    .getLastLocation()
                    .addOnSuccessListener(location -> adapter.setCurrentLocation(location));
        }
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(this);

        if (getArguments() != null) {
            navToStop = getArguments().getParcelable(C.EXTRA.STOP_OBJECT);
        }
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        // isShowMapFragment = preferences != null && preferences.getBoolean("load_map", false);

        Guideline guideTopInfo = rootView.findViewById(R.id.guideline);
        if (guideTopInfo != null) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
            params.guidePercent = .0f;
            guideTopInfo.setLayoutParams(params);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getView() != null) {
            AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
            if (appBar != null) {
                Guideline guideTopInfo = getView().findViewById(R.id.guideline);
                if (guideTopInfo != null) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                    params.guidePercent = isShowMapFragment ? .45f : .0f;
                    guideTopInfo.setLayoutParams(params);
                }
            }
            initLoadHandler.postDelayed(initLoadRunnable, 10);
            if (mapFragment == null && isShowMapFragment) {
                showMapFragment();
            }
        }
        if (getContext() != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (preferences != null) {
                refreshInterval = Integer.parseInt(preferences.getString("load_eta", "0"));
            }
        }
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void setUserVisibleHint(boolean isUserVisible) {
        super.setUserVisibleHint(isUserVisible);
        initLoadHandler.post(initLoadRunnable);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (mapFragment != null) {
            if (getChildFragmentManager() != null) {
                getChildFragmentManager().beginTransaction().remove(mapFragment).commitAllowingStateLoss();
            }
            mapFragment = null;
        }
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOW_MAP_STATE_KEY, isShowMapFragment);
        if (recyclerView != null) {
            Integer lastFirstVisiblePosition = ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .findFirstVisibleItemPosition();
            outState.putInt(SCROLL_POSITION_STATE_KEY, lastFirstVisiblePosition);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(SHOW_MAP_STATE_KEY, false)) {
                isShowMapFragment = true;
                showMapFragment();
            }
            scrollToPos = savedInstanceState.getInt(SCROLL_POSITION_STATE_KEY, 0);
        }
    }

    @Override
    public void onDestroy() {
        if (mapFragment != null) {
            if (getChildFragmentManager() != null) {
                getChildFragmentManager().beginTransaction().remove(mapFragment).commitAllowingStateLoss();
            }
        }
        disposables.clear();
        initLoadHandler.removeCallbacksAndMessages(null);
        refreshHandler.removeCallbacksAndMessages(null);
        adapterUpdateHandler.removeCallbacksAndMessages(null);
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_route, menu);
        MenuItem showMapMenuItem = menu.findItem(R.id.action_show_map);
        if (getContext() != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            showMapMenuItem.setVisible(preferences == null || !preferences.getBoolean("load_map", false));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_notice:
                if (route != null) {
                    Intent intent = new Intent(getContext(), RouteAnnounceActivity.class);
                    intent.putExtra(C.EXTRA.ROUTE_OBJECT, route);
                    startActivity(intent);
                    return true;
                }
                break;
            case R.id.action_show_map:
                isShowMapFragment = !isShowMapFragment;
                showMapFragment();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("load_wheelchair_icon") || key.equals("load_wifi_icon")) {
            // to reflect changes when toggle display icon
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyDataSetChanged();
            }
        } else if (key.equals("load_map")) {
            isShowMapFragment = sharedPreferences != null && sharedPreferences.getBoolean("load_map", false);
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
            showMapFragment();
        }
    }

    @Override
    public void onRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (getContext() != null && adapter != null && adapter.getDataItemCount() > 0) {
            ArrayList<RouteStop> routeStopList = new ArrayList<>();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getItem(i).getType() == TYPE_DATA &&
                        adapter.getItem(i).getObject() instanceof RouteStop) {
                    routeStopList.add((RouteStop) adapter.getItem(i).getObject());
                }
            }
            try {
                Intent intent = new Intent(getContext(), EtaService.class);
                intent.putParcelableArrayListExtra(C.EXTRA.STOP_LIST, routeStopList);
                getContext().startService(intent);
            } catch (IllegalStateException ignored) {}
        }
    }

    @Override
    public void onClickItem(Item item, int position) {
        if (item.getType() == Item.TYPE_DATA) {
            for (Marker marker: markerMap.values()) {
                marker.remove();
            }
            markerMap.clear();
            RouteStop stop = (RouteStop) item.getObject();
            if (map != null && stop != null && isShowMapFragment &&
                    !TextUtils.isEmpty(stop.getLatitude()) && !TextUtils.isEmpty(stop.getLongitude())) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(Double.parseDouble(stop.getLatitude()), Double.parseDouble(stop.getLongitude())), 18));
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (googleMap == null || getContext() == null) return;
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.3964, 114.1095), 10));
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapToolbarEnabled(false);
        options.compassEnabled(true);
        options.rotateGesturesEnabled(true);
        options.scrollGesturesEnabled(false);
        options.tiltGesturesEnabled(true);
        options.zoomControlsEnabled(false);
        options.zoomGesturesEnabled(true);
        googleMap.setBuildingsEnabled(false);
        googleMap.setIndoorEnabled(false);
        googleMap.setTrafficEnabled(false);
        googleMap.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        loadMapData(googleMap);
        if (getView() != null) {
            Guideline guideTopInfo = getView().findViewById(R.id.guideline);
            if (guideTopInfo != null) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                params.guidePercent = isShowMapFragment ? .4f : .0f;
                guideTopInfo.setLayoutParams(params);
            }
        }
        if (getActivity() != null &&
                getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
            if (appBar != null) {
                if (isShowMapFragment) {
                    appBar.addOnOffsetChangedListener(this);
                } else {
                    appBar.removeOnOffsetChangedListener(this);
                }
            }
        }
        if (routeStops != null && routeStops.size() > 0) {
            RouteStop stop = routeStops.get(0);
            if (!TextUtils.isEmpty(stop.getLatitude()) && !TextUtils.isEmpty(stop.getLongitude())) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(Double.parseDouble(stop.getLatitude()),
                                Double.parseDouble(stop.getLongitude())), 16));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() instanceof RouteStop) {
            RouteStop stop = (RouteStop) marker.getTag();
            if (stop != null && getContext() != null) {
                if (recyclerView != null) {
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                        @Override protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }
                    };
                    int i = 0;
                    for (Item item: adapter.getDataItems()) {
                        if (((RouteStop) item.getObject()).getSequence().equals(stop.getSequence())) {
                            smoothScroller.setTargetPosition(i);
                        }
                        i++;
                    }
                    recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                }
                if (map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Double.parseDouble(stop.getLatitude()), Double.parseDouble(stop.getLongitude())), 16));
                }
                try {
                    Intent intent = new Intent(getContext(), EtaService.class);
                    intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
                    if (getContext() != null) {
                        getContext().startService(intent);
                    }
                } catch (IllegalStateException ignored) {}
            }
        }
        return true;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // TODO: CoordinatorLayout.Behavior?
        if (getView() == null || recyclerView == null || recyclerView.getVisibility() != View.VISIBLE) return;
        Guideline guideline = getView().findViewById(R.id.guideline);
        if (guideline == null) return;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
        if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
            params.guidePercent = .2f;
        } else if (verticalOffset == 0) {
            params.guidePercent = .45f;
        } else {
            float p = 0.2f + (1.0f - (Math.abs(verticalOffset * 1.0f) / appBarLayout.getTotalScrollRange() * 1.0f)) * 0.25f;
            params.guidePercent = params.guidePercent * .9f + p *.1f;
        }
        guideline.setLayoutParams(params);
    }

    private void showMapFragment() {
        if (!isShowMapFragment) {
            if (mapFragment != null && getActivity() != null) {
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.remove(mapFragment);
                ft.commitAllowingStateLoss();
            }
            mapFragment = null;
            if (getView() != null) {
                Guideline guideTopInfo = getView().findViewById(R.id.guideline);
                if (guideTopInfo != null) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                    params.guidePercent = .0f;
                    guideTopInfo.setLayoutParams(params);
                }
            }
            if (getActivity() != null) {
                AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
                if (appBar != null) {
                    appBar.removeOnOffsetChangedListener(this);
                }
            }
            return;
        }
        if (getContext() == null) return;
        new Thread(() -> {
            try {
                if (mapFragment != null) return;
                mapFragment = SupportMapFragment.newInstance();
                if (!mapFragment.isAdded()) {
                    getChildFragmentManager().beginTransaction().add(R.id.map, mapFragment).runOnCommit(() -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (mapFragment != null) {
                                    mapFragment.getMapAsync(this);
                                }
                            });
                        }
                    }).commitAllowingStateLoss();
                }
            } catch (Exception ignored){ }
        }).start();
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                RouteStop routeStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (routeStop == null) return;
                if (routeStop.getRouteNo() == null || !routeStop.getRouteNo().equals(route.getName())) return;
                if (routeStop.getRouteSeq() == null || !routeStop.getRouteSeq().equals(route.getSequence())) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED) || bundle.getBoolean(C.EXTRA.FAIL)) {
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    int i = 0;
                    for (Item item : adapter.getItems()) {
                        if (item != null && item.getType() == TYPE_DATA && item.getObject() != null
                                && ((RouteStop) item.getObject()).getSequence() != null
                                && ((RouteStop) item.getObject()).getSequence().equals(routeStop.getSequence())) {
                            if (getContext() != null) {
                                if (arrivalTimeDatabase != null) {
                                    List<ArrivalTime> arrivalTimeList = ArrivalTime.Companion.getList(arrivalTimeDatabase, routeStop);
                                    for (ArrivalTime arrivalTime : arrivalTimeList) {
                                        if (arrivalTime == null) continue;
                                        if (arrivalTime.getLatitude() != 0.0 && arrivalTime.getLongitude() != 0.0) {
                                            // TODO: show bus location on map for all arrivalTime
                                            if (!TextUtils.isEmpty(arrivalTime.getPlate())) {
                                                IconGenerator iconFactory = new IconGenerator(getContext());
                                                Bitmap bmp = iconFactory.makeIcon(arrivalTime.getPlate());
                                                if (markerMap.containsKey(arrivalTime.getPlate())) {
                                                    markerMap.get(arrivalTime.getPlate()).remove();
                                                    markerMap.remove(arrivalTime.getPlate());
                                                }
                                                if (map != null) {
                                                    Marker marker = map.addMarker(new MarkerOptions()
                                                            .position(new LatLng(arrivalTime.getLatitude(), arrivalTime.getLongitude()))
                                                            .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
                                                    marker.setTag(arrivalTime);
                                                    markerMap.put(arrivalTime.getPlate(), marker);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            adapter.notifyItemChanged(i);
                            break;
                        }
                        i++;
                    }
                } else if (bundle.getBoolean(C.EXTRA.UPDATING)) {
                    if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(true);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
            }
        };
    }

    protected void onStopListError(Throwable e) {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (emptyText != null) {
            emptyText.setText(e.getMessage());
        }
    }

    private void loadMapData(GoogleMap map) {
        if (map != null && adapter != null) {

            map.clear();
            if (!hasMapCoordinates) {
                mapCoordinates.clear();
            }
            routeStops.clear();
            if (adapter.getItemCount() > 0 && getContext() != null) {
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    Item item = adapter.getItem(i);
                    if (item.getType() != Item.TYPE_DATA) continue;
                    RouteStop stop = (RouteStop) item.getObject();
                    routeStops.add(stop);
                    if (!TextUtils.isEmpty(stop.getLatitude()) && !TextUtils.isEmpty(stop.getLongitude())) {
                        if (!hasMapCoordinates) {
                            mapCoordinates.add(new Pair<>(Double.parseDouble(stop.getLatitude()), Double.parseDouble(stop.getLongitude())));
                        }
                        IconGenerator iconFactory = new IconGenerator(getContext());
                        Bitmap bmp = iconFactory.makeIcon(stop.getSequence() + ": " + stop.getName());
                        map.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(stop.getLatitude()), Double.parseDouble(stop.getLongitude())))
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp))).setTag(stop);
                    }
                }
                PolylineOptions singleLine = new PolylineOptions().width(20).zIndex(1)
                        .color(ContextCompat.getColor(getContext(), R.color.grey))
                        .startCap(new RoundCap()).endCap(new RoundCap());
                for (Pair<Double, Double> pair: mapCoordinates) {
                    if (pair.first == null || pair.second == null) continue;
                    singleLine.add(new LatLng(pair.first, pair.second));
                }
                if (hasMapCoordinates) {
                    map.addPolyline(singleLine);
                }
            }
        }
    }

    protected void onStopListComplete() {
        Boolean isScrollToPosition = false;
        Integer scrollToPosition = 0;
        loadMapData(map);
        if (getActivity() != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            FloatingActionButton fab = getActivity().findViewById(R.id.fab);
            if (fab != null && (preferences == null || Integer.parseInt(preferences.getString("load_eta", "0")) < 1)) {
                fab.show();
            }
        }
        refreshHandler.post(refreshRunnable);
        adapterUpdateHandler.post(adapterUpdateRunnable);
        if (adapter != null) {
            if (adapter.getItemCount() > 0) {
                if (route != null && navToStop != null
                        && route.getCompanyCode() != null && route.getName() != null
                        && route.getSequence() != null && route.getServiceType() != null
                        && navToStop.getCompanyCode() != null && navToStop.getName() != null
                        && navToStop.getSequence() != null && navToStop.getRouteServiceType() != null
                        && route.getCompanyCode().equals(navToStop.getCompanyCode())
                        && route.getName().equals(navToStop.getRouteNo())
                        && route.getSequence().equals(navToStop.getRouteSeq())
                        && route.getServiceType().equals(navToStop.getRouteServiceType())) {
                    for (int i = 0; i < adapter.getItemCount(); i++) {
                        Item item = adapter.getItem(i);
                        if (item.getType() != Item.TYPE_DATA) continue;
                        RouteStop stop = (RouteStop) item.getObject();
                        if (stop != null &&
                                stop.getName() != null &&
                                stop.getSequence() != null &&
                                stop.getName().equals(navToStop.getName()) &&
                                stop.getSequence().equals(navToStop.getSequence())
                                ) {
                            scrollToPosition = i;
                            isScrollToPosition = true;
                        }
                    }
                }
                if (scrollToPos > 0) {
                    scrollToPosition = scrollToPos;
                    isScrollToPosition = true;
                    scrollToPos = 0;
                }
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (isScrollToPosition) {
                        recyclerView.scrollToPosition(scrollToPosition);
                    }
                }
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
            if (map != null && routeStops.size() > 0 && scrollToPosition < routeStops.size()) {
                RouteStop stop = routeStops.get(scrollToPosition);
                if (!TextUtils.isEmpty(stop.getLatitude()) && !TextUtils.isEmpty(stop.getLongitude())) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Double.parseDouble(stop.getLatitude()),
                                    Double.parseDouble(stop.getLongitude())), 16));
                }
            }
        }
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                if (adapter != null && location != null) {
                    adapter.setCurrentLocation(location);
                }
            }
        };
    };

    private void startLocationUpdates() {
        if (getContext() == null || locationCallback == null) return;
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(10000 / 2);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.getFusedLocationProviderClient(getContext())
                    .requestLocationUpdates(locationRequest, locationCallback, null /* Looper */);
        }
    }

    private void stopLocationUpdates() {
        if (getContext() == null || locationCallback == null) return;
        LocationServices.getFusedLocationProviderClient(getContext()).removeLocationUpdates(locationCallback);
    }

    DisposableObserver<Intent> followObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Follow follow = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (follow == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    int i = 0;
                    for (Item item : adapter.getItems()) {
                        if (item != null && item.getType() == TYPE_DATA && item.getObject() != null &&
                                ((RouteStop) item.getObject()).getSequence() != null &&
                                ((RouteStop) item.getObject()).getSequence().equals(follow.getStopSeq())) {
                            adapter.notifyItemChanged(i);
                            break;
                        }
                        i++;
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }
}
