package com.alvinhkh.buseta.ui.setting

import android.app.ActivityManager
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import android.view.MenuItem
import android.widget.FrameLayout

import com.alvinhkh.buseta.R
// import com.alvinhkh.buseta.utils.AdViewUtil
import com.alvinhkh.buseta.utils.NightModeUtil
// import com.google.android.gms.ads.AdView

abstract class BasePreferenceActivity : AppCompatPreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    // private var adView: AdView? = null

    // private var adViewContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTaskDescription(getString(R.string.app_name))
        setContentView(R.layout.activity_setting)
        setSupportActionBar(findViewById(R.id.toolbar))

        // adViewContainer = findViewById(R.id.adView_container)
        // adView = AdViewUtil.banner(adViewContainer!!, adView, true)

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // adView = AdViewUtil.banner(adViewContainer!!, adView, true)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key.matches("app_theme".toRegex())) {
            NightModeUtil.update(this)
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        if (NightModeUtil.update(this)) {
            recreate()
            return
        }
        // if (adView != null) {
        //    adView!!.resume()
        // }
    }

    override fun onPause() {
        // if (adView != null) {
        //    adView!!.pause()
        // }
        super.onPause()
    }

    public override fun onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setTaskDescription(title: String) {
        // overview task
        if (Build.VERSION.SDK_INT >= 28) {
            setTaskDescription(ActivityManager.TaskDescription(title, R.mipmap.ic_launcher,
                    ContextCompat.getColor(this, R.color.colorPrimary600)))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            setTaskDescription(ActivityManager.TaskDescription(title, bm,
                    ContextCompat.getColor(this, R.color.colorPrimary600)))
        }
    }

}