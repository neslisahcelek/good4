package com.good4.business.presentation.dashboard

import com.good4.code.domain.CodeStatus

val RecentCodeUiModel.statusEnum: CodeStatus
    get() = CodeStatus.fromValue(status)
