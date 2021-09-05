package com.saeed.rssreader.domain.error

import com.saeed.rssreader.domain.model.ErrorModel

interface ErrorMapperExceptionHandler {

    fun getError(throwable: Throwable): ErrorModel

}