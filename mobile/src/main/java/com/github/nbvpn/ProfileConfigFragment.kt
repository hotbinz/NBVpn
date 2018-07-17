/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.nbvpn

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.support.design.widget.Snackbar
import android.support.v14.preference.SwitchPreference
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceDataStore
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import com.github.nbvpn.App.Companion.app
import com.github.nbvpn.database.Profile
import com.github.nbvpn.database.ProfileManager
import com.github.nbvpn.plugin.PluginConfiguration
import com.github.shadowsocks.plugin.PluginContract
import com.github.nbvpn.plugin.PluginManager
import com.github.shadowsocks.plugin.PluginOptions
import com.github.nbvpn.preference.DataStore
import com.github.nbvpn.preference.IconListPreference
import com.github.nbvpn.preference.OnPreferenceDataStoreChangeListener
import com.github.nbvpn.preference.PluginConfigurationDialogFragment
import com.github.nbvpn.utils.Action
import com.github.nbvpn.utils.DirectBoot
import com.github.nbvpn.utils.Key
import com.takisoft.fix.support.v7.preference.EditTextPreference
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers
import com.takisoft.fix.support.v7.preference.SimpleMenuPreference

class ProfileConfigFragment : PreferenceFragmentCompatDividers(), Toolbar.OnMenuItemClickListener,
        Preference.OnPreferenceChangeListener, OnPreferenceDataStoreChangeListener {
    companion object {
        private const val REQUEST_CODE_PLUGIN_CONFIGURE = 1
    }

    private var profileId = -1
    private lateinit var isProxyApps: SwitchPreference
    private lateinit var plugin: IconListPreference
    private lateinit var pluginConfigure: EditTextPreference
    private lateinit var pluginConfiguration: PluginConfiguration
    private lateinit var receiver: BroadcastReceiver
    private lateinit var proxyType: SimpleMenuPreference

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DataStore.privateStore
        val activity = requireActivity()
        profileId = activity.intent.getIntExtra(Action.EXTRA_PROFILE_ID, -1)
        addPreferencesFromResource(R.xml.pref_profile)
        if (Build.VERSION.SDK_INT >= 25 && activity.getSystemService(UserManager::class.java).isDemoUser) {
            findPreference(Key.host).summary = "shadowsocks.example.org"
            findPreference(Key.remotePort).summary = "1337"
            findPreference(Key.password).summary = "\u2022".repeat(32)
        }
        //代理方式
        proxyType = findPreference(Key.proxyType) as SimpleMenuPreference
        proxyType.setOnPreferenceChangeListener { _, newValue ->
            initProxyType(newValue.toString())
            true
        }
        val profile = ProfileManager.getProfile(profileId) ?: Profile()
        initProxyType(profile.proxyType)
        //APP分流
        val serviceMode = DataStore.serviceMode
        findPreference(Key.remoteDns).isEnabled = serviceMode != Key.modeProxy
        isProxyApps = findPreference(Key.proxyApps) as SwitchPreference
        isProxyApps.isEnabled = serviceMode == Key.modeVpn
        isProxyApps.setOnPreferenceClickListener {
            startActivity(Intent(activity, AppManager::class.java))
            isProxyApps.isChecked = true
            false
        }
        //DNS判断
        findPreference(Key.udpdns).isEnabled = serviceMode != Key.modeProxy
        //插件设置
        plugin = findPreference(Key.plugin) as IconListPreference
        pluginConfigure = findPreference(Key.pluginConfigure) as EditTextPreference
        plugin.unknownValueSummary = getString(R.string.plugin_unknown)
        plugin.setOnPreferenceChangeListener { _, newValue ->
            pluginConfiguration = PluginConfiguration(pluginConfiguration.pluginsOptions, newValue as String)
            DataStore.plugin = pluginConfiguration.toString()
            DataStore.dirty = true
            pluginConfigure.isEnabled = newValue.isNotEmpty()
            pluginConfigure.text = pluginConfiguration.selectedOptions.toString()
            if (PluginManager.fetchPlugins()[newValue]?.trusted == false)
                Snackbar.make(view!!, R.string.plugin_untrusted, Snackbar.LENGTH_LONG).show()
            true
        }
        pluginConfigure.onPreferenceChangeListener = this
        initPlugins()
        receiver = app.listenForPackageChanges(false) { initPlugins() }
        DataStore.privateStore.registerChangeListener(this)
    }

    private fun initProxyType(newValue:String) {
        if("socks5" == newValue || "http" == newValue) {
            findPreference(Key.password).isVisible = false
            findPreference(Key.method).isVisible = false
        }
        else {
            findPreference(Key.password).isVisible = true
            findPreference(Key.method).isVisible = true
        }
    }

    private fun initPlugins() {
        val plugins = PluginManager.fetchPlugins()
        plugin.entries = plugins.map { it.value.label }.toTypedArray()
        plugin.entryValues = plugins.map { it.value.id }.toTypedArray()
        plugin.entryIcons = plugins.map { it.value.icon }.toTypedArray()
        plugin.entryPackageNames = plugins.map { it.value.packageName }.toTypedArray()
        pluginConfiguration = PluginConfiguration(DataStore.plugin)
        plugin.value = pluginConfiguration.selected
        plugin.init()
        plugin.checkSummary()
        pluginConfigure.isEnabled = pluginConfiguration.selected.isNotEmpty()
        pluginConfigure.text = pluginConfiguration.selectedOptions.toString()
    }

    private fun showPluginEditor() {
        val bundle = Bundle()
        bundle.putString("key", Key.pluginConfigure)
        bundle.putString(PluginConfigurationDialogFragment.PLUGIN_ID_FRAGMENT_TAG, pluginConfiguration.selected)
        displayPreferenceDialog(PluginConfigurationDialogFragment(), Key.pluginConfigure, bundle)
    }

    fun saveAndExit() {
        val profile = ProfileManager.getProfile(profileId) ?: Profile()
        profile.id = profileId
        profile.deserialize()
        ProfileManager.updateProfile(profile)
        ProfilesFragment.instance?.profilesAdapter?.deepRefreshId(profileId)
        if (DataStore.profileId == profileId && DataStore.directBootAware) DirectBoot.update()
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        isProxyApps.isChecked = DataStore.proxyApps // fetch proxyApps updated by AppManager
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean = try {
        val selected = pluginConfiguration.selected
        pluginConfiguration = PluginConfiguration(pluginConfiguration.pluginsOptions +
                (pluginConfiguration.selected to PluginOptions(selected, newValue as? String?)), selected)
        DataStore.plugin = pluginConfiguration.toString()
        DataStore.dirty = true
        true
    } catch (exc: IllegalArgumentException) {
        Snackbar.make(view!!, exc.localizedMessage, Snackbar.LENGTH_LONG).show()
        false
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String?) {
        if (key != Key.proxyApps && findPreference(key) != null) DataStore.dirty = true
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == Key.pluginConfigure) {
            val intent = PluginManager.buildIntent(pluginConfiguration.selected, PluginContract.ACTION_CONFIGURE)
            if (intent.resolveActivity(requireContext().packageManager) != null)
                startActivityForResult(intent.putExtra(PluginContract.EXTRA_OPTIONS,
                        pluginConfiguration.selectedOptions.toString()), REQUEST_CODE_PLUGIN_CONFIGURE) else {
                showPluginEditor()
            }
        } else super.onDisplayPreferenceDialog(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PLUGIN_CONFIGURE) when (resultCode) {
            Activity.RESULT_OK -> {
                val options = data?.getStringExtra(PluginContract.EXTRA_OPTIONS)
                pluginConfigure.text = options
                onPreferenceChange(null, options)
            }
            PluginContract.RESULT_FALLBACK -> showPluginEditor()

        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.action_delete -> {
            val activity = requireActivity()
            AlertDialog.Builder(activity)
                    .setTitle(R.string.delete_confirm_prompt)
                    .setPositiveButton(R.string.yes, { _, _ ->
                        ProfileManager.delProfile(profileId)
                        activity.finish()
                    })
                    .setNegativeButton(R.string.no, null)
                    .create()
                    .show()
            true
        }
        R.id.action_apply -> {
            saveAndExit()
            true
        }
        else -> false
    }

    override fun onDestroy() {
        DataStore.privateStore.unregisterChangeListener(this)
        app.unregisterReceiver(receiver)
        super.onDestroy()
    }
}
