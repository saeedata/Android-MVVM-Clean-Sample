package com.saeed.rssreader.domain.usecase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saeed.rssreader.domain.model.ResultModel
import com.saeed.rssreader.utils.extension.callOnceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class CallApiUseCase<T>(val flow: suspend () -> Flow<ResultModel<T>>, val loadStrategy:LoadStrategy = LoadStrategy.LOAD_REFRESHED_DATA) : ViewModel() {

    private var scope = CoroutineScope(Job() + viewModelScope.coroutineContext)
    private val _mutableLiveData = MutableLiveData<ResultModel<T>>()
    val liveData: LiveData<ResultModel<T>>
        get() = runBlocking {
            retry()
            _mutableLiveData
        }

    fun retry() {
        callOnceApi(scope, _mutableLiveData) {
            flow.invoke()
        }
    }

    fun cancel() {
        scope.cancel()
    }

    companion object {
        private const val LOAD_IF_CACHED = 0
         private const val LOAD_REFRESHED_DATA = 1
    }

    enum class LoadStrategy {
        LOAD_IF_CACHED,
        LOAD_REFRESHED_DATA
    }
}