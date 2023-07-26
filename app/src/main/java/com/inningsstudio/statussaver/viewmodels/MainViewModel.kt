package com.inningsstudio.statussaver.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.inningsstudio.statussaver.Const.CLICKED_INDEX
import com.inningsstudio.statussaver.Const.IS_SAVED
import com.inningsstudio.statussaver.FileUtils
import com.inningsstudio.statussaver.StatusModel
import com.inningsstudio.statussaver.StatusViewActivity

class MainViewModel : ViewModel() {

    val statusList = mutableListOf<StatusModel>()
    val savedStatusList = mutableListOf<StatusModel>()

    fun fetchStatus(applicationContext: Context, uri: String) {
        val allStatus = FileUtils.getStatus(applicationContext, uri)
        statusList.clear()
        statusList.addAll(allStatus)
    }

    fun fetchSavedStatus(applicationContext: Context) {
        val allStatus = FileUtils.getSavedStatus(applicationContext)
        savedStatusList.clear()
        savedStatusList.addAll(allStatus)
    }

    fun onStatusClicked(context: Context, clickedIndex: Int, isFromSaved: Boolean = false) {
        val intent = Intent(context, StatusViewActivity::class.java)
        intent.putExtra(CLICKED_INDEX, clickedIndex)
        intent.putExtra(IS_SAVED, isFromSaved)
        context.startActivity(intent)
    }

}