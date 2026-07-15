package com.shuaiqiu.fuckets100

sealed interface PaperSource {
    data class Local(
        val mode: ActivationMode,
        val resourceDirectoryNames: List<String>,
        val resourceModifiedTimes: Map<String, Long> = emptyMap()
    ) : PaperSource

    data class Cloud(
        val status: String,
        val homeworkIdentity: String,
        val baseUrl: String,
        val contents: List<CloudContent>
    ) : PaperSource
}

data class CloudContent(
    val groupName: String,
    val url: String
)
