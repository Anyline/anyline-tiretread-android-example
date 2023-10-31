package io.anyline.tiretread.demo

sealed class Response<T> {

    class Success<T>(
        val result: T
    ) : Response<T>()

    data class Error<T>(
        val message: String
    ) : Response<T>()

    class Loading<T> : Response<T>()
}