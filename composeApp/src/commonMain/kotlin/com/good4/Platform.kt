package com.good4

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

