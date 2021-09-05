package com.saeed.rssreader.domain.model

sealed class ResultModel<out T> {

    data class Success<out T>(val data: T) : ResultModel<T>()

    data class Error<T>(val error: ErrorModel) : ResultModel<T>()

    object Loading : ResultModel<Nothing>()

}