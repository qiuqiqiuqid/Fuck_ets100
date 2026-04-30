package com.shuaiqiu.fuckets100

/**
 * 远程配置数据类
 *
 * @param latestVersionCode 最新版本号
 * @param updateUrl 新版下载地址
 * @param isKillSwitchOn 远程锁定开关
 * @param noticeMessage 公告或锁定提示
 */
data class RemoteConfig(
    val latestVersionCode: Int,
    val updateUrl: String,
    val isKillSwitchOn: Boolean,
    val noticeMessage: String
)
