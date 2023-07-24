package com.inningsstudio.statussaver.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.inningsstudio.statussaver.FileUtils
import com.inningsstudio.statussaver.StatusModel

class MainViewModel : ViewModel() {

    val statusList = mutableListOf<StatusModel>()

    fun fetchStatus(applicationContext: Context, uri: String) {
        val allStatus = FileUtils.getStatus(applicationContext, uri)
        statusList.clear()
        statusList.addAll(allStatus)
    }

}