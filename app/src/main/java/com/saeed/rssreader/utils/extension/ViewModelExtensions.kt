package com.saeed.rssreader.utils.extension

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.saeed.rssreader.data.error.ErrorMapperExceptionHandlerImp
import com.saeed.rssreader.domain.model.ResultModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

fun <T> ViewModel.callOnceApi(
    scope: CoroutineScope,
    liveData: MutableLiveData<ResultModel<T>>,
    flow: suspend () -> Flow<ResultModel<T>>
) {
    if (liveData.value == null || liveData.value is ResultModel.Error) {
        Timber.d( "callOnceApi: ")
        scope.launch {
            try {
                flow.invoke().cancellable()
                    .onEach {
                        liveData.value = it
                    }.collect()
            } catch (ex: CancellationException) {
                liveData.value = ResultModel.Error(ErrorMapperExceptionHandlerImp().getError(ex))
            }
        }
    }
}