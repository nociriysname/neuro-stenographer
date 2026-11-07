package ru.nuclearhack.stenographer.common.uikit.udf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * База для ViewModel экранов: MVVM снаружи, однонаправленный поток данных (MVI) внутри.
 *
 * - [state] — единственный источник правды для экрана; Composable только рисует его;
 * - [onAction] — единственный вход: всё, что делает пользователь, приходит сюда как [A];
 * - [effects] — одноразовые события (навигация, системные диалоги, снекбары), которым не место в state.
 *
 * Нет редьюсеров, Store и DSL — только дисциплина. Если экрану понадобится undo/redo
 * (редактор расшифровки), чистый редьюсер можно завести локально внутри наследника.
 *
 * Важно для Metro: наследник, который регистрируется через `@ContributesIntoMap`, должен явно
 * указывать `binding<ViewModel>()`, иначе Metro привяжет его к промежуточному типу [UdfViewModel].
 */
abstract class UdfViewModel<S : Any, A : Any, E : Any>(initialState: S) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    protected val currentState: S
        get() = mutableState.value

    abstract fun onAction(action: A)

    protected fun updateState(transform: S.() -> S) {
        mutableState.update(transform)
    }

    protected fun sendEffect(effect: E) {
        effectChannel.trySend(effect)
    }
}
