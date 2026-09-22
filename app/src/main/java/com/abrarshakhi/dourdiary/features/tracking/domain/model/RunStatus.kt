package com.abrarshakhi.dourdiary.features.tracking.domain.model

enum class RunStatus {
    IDLE,

    ACTIVE,

    AUTO_PAUSED,

    PAUSED,

    FINISHED,
    ;

    val isPaused: Boolean get() = this == AUTO_PAUSED || this == PAUSED

    val isRecording: Boolean get() = this == ACTIVE || this == AUTO_PAUSED || this == PAUSED
}
