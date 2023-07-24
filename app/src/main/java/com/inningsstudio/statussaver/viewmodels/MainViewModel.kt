package com.inningsstudio.statussaver.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.inningsstudio.statussaver.Const.CLICKED_INDEX
import com.inningsstudio.statussaver.FileUtils
import com.inningsstudio.statussaver.StatusModel
import com.inningsstudio.statussaver.StatusViewActivity

class MainViewModel : ViewModel() {

    val statusList = mutableListOf<StatusModel>()

    fun fetchStatus(applicationContext: Context, uri: String) {
        val allStatus = FileUtils.getStatus(applicationContext, uri)
        statusList.clear()
        statusList.addAll(allStatus)
    }

    fun onStatusClicked(context: Context, clickedIndex: Int) {
        val intent = Intent(context, StatusViewActivity::class.java)
        intent.putExtra(CLICKED_INDEX, clickedIndex)
        context.startActivity(intent)
    }

}