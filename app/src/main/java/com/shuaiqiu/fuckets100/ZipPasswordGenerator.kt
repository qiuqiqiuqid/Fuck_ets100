package com.shuaiqiu.fuckets100

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * ZIP 密码生成器
 * 喵~ 用于从 ETS100 下载的 ZIP 文件生成解压密码喵！
 * 
 * 算法说明：
 * 1. 提取 ZIP 文件尾部 336 字节
 * 2. 验证文件签名（MSTCHINA 或 EPLAT）
 * 3. 提取 128 字节种子数据（偏移 16-143）
 * 4. 第一次 MD5 -> 32字符 hex
 * 5. 第二次 MD5 -> 32字符 hex
 * 6. 拼接 = 64字符密码
 */
object ZipPasswordGenerator {

    private const val TAG = "ZipPasswordGenerator"
    private const val FOOTER_SIZE = 336
    private const val SEED_START = 16
    private const val SEED_END = 143  // 128 字节

    /**
     * 从 ZIP 文件生成解压密码
     * @param zipFilePath ZIP 文件路径
     * @return 解压密码（64字符）
     */
    fun generatePassword(zipFilePath: String): String? {
        return try {
            val file = File(zipFilePath)
            if (!file.exists()) {
                Log.e(TAG, "ZIP 文件不存在: $zipFilePath")
                return null
            }
            
            // 读取文件尾部 336 字节
            val footer = readFooter(file)
            if (footer == null) {
                Log.e(TAG, "无法读取 ZIP 文件尾部")
                return null
            }
            
            generatePasswordFromFooter(footer)
        } catch (e: Exception) {
            Log.e(TAG, "生成密码失败: ${e.message}", e)
            null
        }
    }

    /**
     * 从 ZIP 文件字节数据生成解压密码
     * @param zipData ZIP 文件的字节数据
     * @return 解压密码（64字符）
     */
    fun generatePasswordFromBytes(zipData: ByteArray): String? {
        return try {
            if (zipData.size < FOOTER_SIZE) {
                Log.e(TAG, "ZIP 数据太小，无法提取尾部")
                return null
            }
            
            // 提取尾部 336 字节
            val footer = zipData.takeLast(FOOTER_SIZE).toByteArray()
            generatePasswordFromFooter(footer)
        } catch (e: Exception) {
            Log.e(TAG, "从字节数据生成密码失败: ${e.message}", e)
            null
        }
    }

    /**
     * 从尾部数据生成密码
     */
    private fun generatePasswordFromFooter(footer: ByteArray): String? {
        // 验证文件签名
        // 签名位置1: footer[0:8] == b'MSTCHINA'
        // 签名位置2: footer[144:149] == b'EPLAT'
        val signature1Valid = footer.sliceArray(0..7).contentEquals("MSTCHINA".toByteArray())
        val signature2Valid = footer.sliceArray(144..148).contentEquals("EPLAT".toByteArray())
        
        if (!signature1Valid && !signature2Valid) {
            Log.e(TAG, "无效的文件签名")
            return null
        }
        
        Log.d(TAG, "文件签名验证通过")
        
        // 提取 128 字节种子数据 (偏移 16-143)
        val seed = footer.sliceArray(SEED_START..SEED_END)
        Log.d(TAG, "种子数据长度: ${seed.size} 字节")
        
        // 第一次 MD5
        val firstMd5 = md5Hex(seed)
        Log.d(TAG, "第一次 MD5: ${firstMd5}")
        
        // 第二次 MD5
        val secondMd5 = md5Hex(firstMd5.toByteArray(Charsets.UTF_8))
        Log.d(TAG, "第二次 MD5: ${secondMd5}")
        
        // 拼接最终密码 (64字符)
        return firstMd5.uppercase() + secondMd5.uppercase()
    }

    /**
     * 读取文件尾部 336 字节
     */
    private fun readFooter(file: File): ByteArray? {
        return try {
            val fileSize = file.length()
            if (fileSize < FOOTER_SIZE) {
                Log.e(TAG, "文件太小: $fileSize bytes")
                return null
            }
            
            FileInputStream(file).use { fis ->
                // 跳到文件末尾前 FOOTER_SIZE 字节
                fis.skip(fileSize - FOOTER_SIZE)
                
                // 读取尾部数据
                val buffer = ByteArray(FOOTER_SIZE)
                val bytesRead = fis.read(buffer)
                
                if (bytesRead == FOOTER_SIZE) {
                    buffer
                } else {
                    Log.e(TAG, "读取字节数不正确: $bytesRead")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取文件尾部失败: ${e.message}", e)
            null
        }
    }

    /**
     * 计算数据的 MD5 哈希，返回十六进制字符串
     */
    private fun md5Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证 ZIP 文件是否有效
     * 喵~ 使用 Java 的 ZipFile 检查喵！
     */
    fun isValidZipFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() < FOOTER_SIZE) {
                return false
            }
            
            // 尝试打开 ZIP 文件
            ZipFile(file).use { zip ->
                // 如果能打开就是有效的 ZIP
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "ZIP 文件验证失败: ${e.message}")
            false
        }
    }
}