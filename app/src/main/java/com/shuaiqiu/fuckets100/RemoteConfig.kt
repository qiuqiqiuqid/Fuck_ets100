package com.shuaiqiu.fuckets100

/**
 * 远程配置数据类
 *
 * @param latestVersionCode 最新版本号
 * @param updateUrl 新版下载地址
 * @param isKillSwitchOn 远程锁定开关
 * @param isForce 是否强制更新
 * @param updateMessage 更新弹窗正文内容
 * @param noticeMessage 启动时的 Toast 公告
 */
data class RemoteConfig(
    val latestVersionCode: Int,
    val updateUrl: String,
    val isKillSwitchOn: Boolean,
    val isForce: Boolean,
    val updateMessage: String,
    val noticeMessage: String
)

/**
 * 更新状态数据类
 * 用于封装更新检查的返回结果
 */
data class UpdateStatus(
    val isKillSwitch: Boolean,      // 是否 KillSwitch 锁定
    val showDialog: Boolean,        // 是否显示更新弹窗
    val message: String,            // 弹窗内容（用于 UpdateDialog）
    val isForce: Boolean,           // 是否强制更新
    val updateUrl: String,          // 更新链接
    val noticeMessage: String      // 公告内容（用于 Toast）
)
