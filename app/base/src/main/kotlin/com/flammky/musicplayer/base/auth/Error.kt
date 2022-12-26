package com.flammky.musicplayer.base.auth

open class AuthException(msg: String) : Exception(msg)
class ProviderNotFoundException(msg: String) : AuthException(msg)
class InvalidAuthDataException(msg: String) : AuthException(msg)
class AlreadyLoggedInException(msg: String) : AuthException(msg)
