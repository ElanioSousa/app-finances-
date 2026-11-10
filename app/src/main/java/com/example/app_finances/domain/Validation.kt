package com.example.app_finances.domain

/** Lança [IllegalArgumentException] com [message] quando o nome está vazio. */
fun requireName(name: String, message: String) {
    require(name.isNotBlank()) { message }
}
