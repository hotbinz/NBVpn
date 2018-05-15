package com.github.nbvpn.plugin

import com.github.nbvpn.App.Companion.app

object NoPlugin : Plugin() {
    override val id: String get() = ""
    override val label: CharSequence get() = app.getText(com.github.nbvpn.R.string.plugin_disabled)
}
