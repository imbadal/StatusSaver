package com.inningsstudio.statussaver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast


object SharedUtils {

    fun rateUs(context: Context) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=" + context.packageName)
            )
        )
    }

    fun contactUs(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>("care.inningsstudio@gmail.com"))
        intent.putExtra(Intent.EXTRA_TEXT, "Hello InningsStudio Team, Hello InningsStudio Team,\n")
        intent.data = Uri.parse("mailto:")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(
                context, "There is no application that support this action",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun privacyPolicy(context: Context) {
        context.startActivity(Intent(context, PrivacyPolicyActivity::class.java))
    }

    fun shareApp(context: Context) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        share.putExtra(Intent.EXTRA_SUBJECT, "Share Status Saver App")
        share.putExtra(
            Intent.EXTRA_TEXT,
            "https://play.google.com/store/apps/details?id=com.inningsstudio.statussaver"
        )
        context.startActivity(Intent.createChooser(share, "Share App"))
    }

}