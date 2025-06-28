package com.inningsstudio.statussaver.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inningsstudio.statussaver.domain.usecase.DetectStatusPathsUseCase
import com.inningsstudio.statussaver.domain.usecase.GetStatusesUseCase

class MainViewModelFactory(
    private val getStatusesUseCase: GetStatusesUseCase,
    private val detectStatusPathsUseCase: DetectStatusPathsUseCase,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(getStatusesUseCase, detectStatusPathsUseCase, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 