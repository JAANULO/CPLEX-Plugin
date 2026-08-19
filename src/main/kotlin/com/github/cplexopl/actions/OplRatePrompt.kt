package com.github.cplexopl.actions

import com.github.cplexopl.settings.OplSettingsState
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationAction
import com.intellij.openapi.project.Project

object OplRatePrompt {
    fun showNotification(project: Project) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("CPLEX OPL")
        val notification = group.createNotification(
            "Enjoying CPLEX OPL Plugin?",
            "You have successfully run 5 models using CPLEX OPL. Consider rating the plugin on JetBrains Marketplace!",
            NotificationType.INFORMATION
        )

        notification.addAction(NotificationAction.createSimple("Rate Plugin") {
            BrowserUtil.browse("https://plugins.jetbrains.com/plugin/31125-cplex-opl/reviews")
            OplSettingsState.instance.neverShowRatePrompt = true
            notification.expire()
        })

        notification.addAction(NotificationAction.createSimple("Don't Ask Again") {
            OplSettingsState.instance.neverShowRatePrompt = true
            notification.expire()
        })

        notification.notify(project)
    }
}
