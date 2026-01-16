package com.good4.code.data.repository

import com.good4.code.domain.CodeStatus

val CodeWithDetails.statusEnum: CodeStatus
    get() = CodeStatus.fromValue(status)
