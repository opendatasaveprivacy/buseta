package com.alvinhkh.buseta.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.views.MapView;

/**
 * Override Mapview to handle map drag in bottom sheet correctly
 */
public class CustomOsmMapView extends MapView {

    public CustomOsmMapView(final Context context,
                            MapTileProviderBase tileProvider,
                            final Handler tileRequestCompleteHandler, final AttributeSet attrs) {
        this(context, tileProvider, tileRequestCompleteHandler, attrs, Configuration.getInstance().isMapViewHardwareAccelerated());

    }

    public CustomOsmMapView(final Context context,
                            MapTileProviderBase tileProvider,
                            final Handler tileRequestCompleteHandler, final AttributeSet attrs, boolean hardwareAccelerated) {
        super(context, tileProvider, tileRequestCompleteHandler,
                attrs, hardwareAccelerated);
    }


    public CustomOsmMapView(final Context context, final AttributeSet attrs) {
        this(context, null, null, attrs);
    }

    public CustomOsmMapView(final Context context) {
        this(context, null, null, null);
    }


    public CustomOsmMapView(final Context context,
                            final MapTileProviderBase aTileProvider) {
        this(context, aTileProvider, null);
    }

    public CustomOsmMapView(final Context context,
                            final MapTileProviderBase aTileProvider,
                            final Handler tileRequestCompleteHandler) {
        this(context, aTileProvider, tileRequestCompleteHandler,
                null);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        /**
         * Request all parents to relinquish the touch events
         */
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(ev);
    }
}