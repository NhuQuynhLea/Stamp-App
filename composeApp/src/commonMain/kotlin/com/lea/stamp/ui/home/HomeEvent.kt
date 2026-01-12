package com.lea.stamp.ui.home

sealed class HomeEvent {
    data class AddWater(val amount: Float) : HomeEvent()
    data class UpdateWeight(val weight: Float) : HomeEvent()
}
