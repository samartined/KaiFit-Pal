package com.tfg.kaifit_pal

import android.app.Application

/**
 * Application class, which owns the [AppContainer] of the process.
 *
 * ViewModels get the repositories through `(application as KaiFitApp).container`. The container,
 * and with it every data file, is only created on first use, so starting the app does no disk
 * work here.
 */
class KaiFitApp : Application() {

    /** App-wide dependencies, created on first access. */
    val container: AppContainer by lazy { AppContainer(this) }
}
