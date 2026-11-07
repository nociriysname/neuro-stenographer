package ru.nuclearhack.stenographer.di

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * Корневой граф приложения.
 *
 * Явных привязок почти нет: реализации портов domain (@ContributesBinding), ViewModel'и
 * (@ContributesIntoMap) и Activity (@ActivityKey) Metro собирает сам из всех модулей на classpath :app.
 * Ошибка в графе (нет реализации, цикл) — это ошибка компиляции, а не краш в рантайме.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : MetroAppComponentProviders, ViewModelGraph {

    @Provides
    fun provideContext(application: Application): Context = application

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
