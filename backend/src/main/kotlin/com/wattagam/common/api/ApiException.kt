package com.wattagam.common.api

class ApiException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
