package com.saeed.rssreader.utils.extension

import com.saeed.rssreader.domain.error.ErrorMapperExceptionHandler
import com.saeed.rssreader.domain.model.ResultModel
import kotlinx.coroutines.flow.*
import timber.log.Timber

fun <T> Flow<T>.toResult(errorHandler: ErrorMapperExceptionHandler): Flow<ResultModel<T>> = this
    .map<T, ResultModel<T>> {
        Timber.d( "Success: ")
        ResultModel.Success(it)
    }.onStart {
        Timber.d( "Loading: ")
        emit(ResultModel.Loading)
    }.catch {
        Timber.e( "Error: $it")
        emit(ResultModel.Error(errorHandler.getError(it)))
    }

