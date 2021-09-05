package com.saeed.rssreader.domain.model

sealed class ErrorModel {

    object Network : ErrorModel()

    object NotFound : ErrorModel()

    object AccessDenied : ErrorModel()

    object ServiceUnavailable : ErrorModel()

    object Canceled : ErrorModel()

    object Unknown : ErrorModel()
}