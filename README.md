# Fuck ets100 - e听说答案提取器 📱

[![GPL-3.0 License](https://img.shields.io/badge/License-GPL%203.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-7F52FF)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/compose-1.10.3-4285F4)](https://www.jetbrains.com/compose-multiplatform)
![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)



你是否还在面对数不尽的听说题而难堪，是否还在为没有人给你提供 e听说答案而困扰，是否还在为了达到老师要求的听说 30 分而感到不可能完成 ❓

现在，这款神器的诞生可以彻底解决你的困扰 ✨
<img src="fe_logo.png" width="150" alt="Fuck ets100 Logo">


## 🎯 这个软件干什么的

Fuck ets100 是一款专门为 e听说（ETS 100）用户打造的答案提取工具 📖

只要你的手机已经安装了 e听说 App，Fuck ets100 就能自动读取里面的答案数据，让你不用打开 e听说 也能随时查看答案 👀

**主要功能：**
- 📚 一键读取 e听说里的所有试卷答案
- 🔍 支持多种题型：听说信息、信息转述、阅读理解等
- 📱 操作简单，点开就能用
- 🌙 支持深色模式，护眼舒适


**具体实现方法可以看看** [@ets_100_答案读取说明](/ets读取/ETS_100_答案提取说明.md)

---
## 🚀 怎么用

### 第一步：准备工作

在使用 Fuck ets100 之前，你需要先准备好一种"通行证"来读取 e听说 的数据。有四种方式可以选择：

| 方式 | 难度 | 说明 |
|------|------|------|
| 零宽字符直读 | ⭐ 最简单 | **推荐！** 利用特殊漏洞，无需额外App |
| Shizuku | ⭐ 简单 | 需要安装 Shizuku App，适合大多数用户 |
| Root | ⭐⭐ 中等 | 需要手机已经 Root |
| 文件选择 | ⭐ 简单 | 最简单，但需要每次手动选择文件夹 |

**强烈推荐使用"零宽字符直读"**，这是最简单的方法，不需要安装任何额外App ~

### 第二步：安装并打开 Fuck ets100

1. 从 [ releases 页面](https://github.com/qiuqiqiuqid/Fuck_ets100/releases) 下载最新版本的 APK
2. 安装到你的手机上
3. 打开 Fuck ets100 App

### 第三步：授权"通行证"

- 如果选择 **零宽字符直读**（推荐）：
  1. 在 Fuck ets100 里点击"零宽字符直读"
  2. 如果提示"不支持"，请尝试其他方式
  3. 自动完成授权，超简单！

- 如果选择 **Shizuku**：
  1. 先去应用商店安装 [Shizuku](https://shizuku.rikka.app/)
  2. 在 Fuck ets100 里点击"使用 Shizuku"，按照提示授权
  
- 如果选择 **Root**：确保你的手机已经获取了 Root 权限

- 如果选择 **文件选择**：
  1. 点击"选择文件夹"
  2. 找到你的 data 数据文件夹（在手机存储 `/Android/data/` 里面）
  3. 选中文件夹即可

### 第四步：开始使用

授权成功后，回到首页点击"开始读取"，稍等几秒，所有试卷就会显示出来 📋

点击任意一份试卷，就能看到里面的题目和答案啦 🎉

---

## 🔧 怎么做的

很多小伙伴可能会好奇，这个软件是怎么"偷"出答案的呢 ❓

其实原理很简单 👇

e听说 App 把答案数据存在手机本地的一个文件夹里，我们只需要有办法读取这个文件夹，就能拿到里面的答案 📁

Fuck ets100 提供了四种读取方式：

1. **零宽字符直读方式**：利用一个特殊的"漏洞"，让手机以为自己不需要权限就能读取文件（最推荐 ✨）
2. **Shizuku 方式**：通过 Shizuku 这个工具，用管理员权限去读取文件
3. **Root 方式**：直接用 Root 权限读取
4. **文件选择方式**：让用户自己选择 e听说的数据文件夹

读取到数据后，Fuck ets100 会自动解析里面的 JSON 文件，把题目和答案提取出来，按照我们熟悉的格式显示出来 😊




**关于数据安全：**
- Fuck ets100 不会上传你的任何数据
- 所有操作都在你手机本地完成

---

## 📋 后续打算

Fuck ets100 还会继续更新迭代，以下是一些计划中的功能 💡

**计划中：**
- 🎨 优化界面，让看答案更舒服
- 📤 支持导出答案为文本或 PDF
- 🔄 自动检测 e听说 更新，同步最新试卷
- 📊 显示更多统计信息，比如正确率等
- 🌐 支持更多 e听说 以外的平台

如果你有什么好的建议或想法，欢迎在 [issues](https://github.com/qiuqiqiuqid/Fe/issues) 里提出来 ~

---

## ⚠️ 注意事项

- 本软件仅供学习交流使用，请勿用于考试作弊等违规行为
- 使用前请确保你已经购买了 e听说 的正版服务
- 如果 e听说 更新后软件无法使用，请耐心等待新版本发布

---

## 🙏 感谢

Fuck ets100 的诞生离不开这些"小伙伴"的帮助 💕

**人类贡献者：**
- [Shizuku](https://shizuku.rikka.app/) - 让你不用 Root 也能管理文件
- [Jetpack Compose](https://developer.android.com/compose) - 让界面开发更简单
- [hicccc77](https://github.com/hicccc77) - 提供了全新的读取逻辑([WeFlow](https://github.com/hicccc77/WeFlow)作者)
- [leitianshuo1337](https://github.com/code-leitianshuo) - 提供了全新api读取逻辑

**AI 贡献者：**
- **Google Gemini 3.1 Pro** - 设计师担当，负责前端设计和大体框架搭建，让界面美观又好用
- **MiniMax M2.7** - 程序猿担当，负责核心功能实现和代码编写，让技术方案稳定又高效

---

**有问题？找作者：**
- GitHub: [issues 页面](https://github.com/qiuqiqiuqid/Fe/issues)
- 抖音:[抖音主页](https://v.douyin.com/P0GrWYTqi4s/)
- b站:[b站主页](https://space.bilibili.com/2116040615h)


祝你使用愉快 🎉
