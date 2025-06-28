package com.inningsstudio.statussaver.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.inningsstudio.statussaver.Const.CLICKED_INDEX
import com.inningsstudio.statussaver.Const.IS_SAVED
import com.inningsstudio.statussaver.FileUtils
import com.inningsstudio.statussaver.StatusModel
import com.inningsstudio.statussaver.StatusPathDetector
import com.inningsstudio.statussaver.StatusViewActivity

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }
    
    val statusList = mutableListOf<StatusModel>()
    val savedStatusList = mutableListOf<StatusModel>()

    fun fetchStatus(applicationContext: Context, uri: String) {
        Log.d(TAG, "Fetching status with URI: $uri")
        
        // Check if WhatsApp is installed
        if (!StatusPathDetector.isWhatsAppInstalled(applicationContext) && 
            !StatusPathDetector.isWhatsAppBusinessInstalled(applicationContext) &&
            !StatusPathDetector.isGBWhatsAppInstalled(applicationContext) &&
            !StatusPathDetector.isYoWhatsAppInstalled(applicationContext)) {
            
            Log.w(TAG, "No WhatsApp variants found installed")
            statusList.clear()
            return
        }
        
        // Try to get status using the provided URI or auto-detect
        val allStatus = FileUtils.getStatus(applicationContext, uri)
        statusList.clear()
        statusList.addAll(allStatus)
        
        Log.d(TAG, "Found ${statusList.size} status items")
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