package com.saeed.rssreader.data.error

import com.saeed.rssreader.domain.error.ErrorMapperExceptionHandler
import com.saeed.rssreader.domain.model.ErrorModel
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection

class ErrorMapperExceptionHandlerImp : ErrorMapperExceptionHandler {

    override fun getError(throwable: Throwable): ErrorModel {
        return when (throwable) {
            is IOException -> ErrorModel.Network
            is HttpException -> {
                when (throwable.code()) {
                    // no cache found in case of no network, thrown by retrofit -> treated as network error
//                   UNSATISFIABLE_REQUEST -> ErrorEntity.Network

                    // not found
                    HttpURLConnection.HTTP_NOT_FOUND -> ErrorModel.NotFound

                    // access denied
                    HttpURLConnection.HTTP_FORBIDDEN -> ErrorModel.AccessDenied

                    // unavailable service
                    HttpURLConnection.HTTP_UNAVAILABLE -> ErrorModel.ServiceUnavailable

                    // all the others will be treated as unknown error
                    else -> ErrorModel.Unknown
                }
            }
            else -> ErrorModel.Unknown
        }
    }
}