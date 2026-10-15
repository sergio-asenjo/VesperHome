package com.sergioasenjo.vesperhome.launcher

import androidx.lifecycle.ViewModelProvider
import com.sergioasenjo.vesperhome.VesperHomeApplication

internal fun launcherViewModelFactory(application: VesperHomeApplication): ViewModelProvider.Factory {
    val container = application.container
    return LauncherViewModel.factory(
        container.managedApplicationsRepository,
        container.categoryRepository,
        container.homeRepository,
        container.launcherSettingsRepository,
        container.wallpaperRepository
    )
}

internal fun jellyfinMusicViewModelFactory(application: VesperHomeApplication): ViewModelProvider.Factory {
    val container = application.container
    return JellyfinMusicViewModel.factory(
        application,
        { container.jellyfinApiRepository },
        container.jellyfinPreferencesRepository,
        container.dreamStateTracker
    )
}
