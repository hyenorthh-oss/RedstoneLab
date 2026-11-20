package com.redstonelab.app.engine

data class SimulationState(
    val isRunning: Boolean = false,
    val tickRateHz: Float = 5.0f, // 1 to 20 ticks per second (Standard Redstone = 10 ticks/s)
    val totalTicks: Long = 0L,
    val activeSignalCount: Int = 0,
    val lastStepDurationMs: Long = 0L
)
