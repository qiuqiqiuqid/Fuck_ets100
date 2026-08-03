# ETS100 云端 API 与跨平台实现开发文档

> 本文档描述 ETS100 云端读取模式的完整协议、认证、下载、解压、答案解析与跨平台实现建议。目标是让开发者仅参考本文档，即可在 Android、Windows、macOS、Linux、iOS 或服务端环境中重新实现一个等价的云端答案读取程序。

[文档索引](./README.md) | [答案解析方法](./ets100答案解析方法.md) | [本地模式读取逻辑](./本地模式读取逻辑.md) | [云端模式逻辑](./云端模式逻辑文档.md) | [企业登录（畅言）](./畅言网页登录.md)

---

# 一、整体目标与运行模型

云端模式的核心目标：

```text
登录 ETS100 账号
    ↓
获取 parent_account_id
    ↓
获取云端作业列表
    ↓
下载每个题型对应的 ZIP 资源包
    ↓
根据 ZIP 尾部数据生成解压密码
    ↓
解压并读取 content.json
    ↓
按 structure_type 解析题目与答案
    ↓
展示或导出答案
```

## 适用场景

| 场景 | 说明 |
|------|------|
| Android App | 可复用当前 Fe 项目的实现方式 |
| 桌面客户端 | 可按 Python / Kotlin / C# / C++ 等语言重写 |
| 服务端脚本 | 可批量登录、获取作业、下载解析 |
| 跨平台 GUI | 可将本文档作为协议层与业务层说明 |

## 与本地读取模式的区别

| 对比项 | 本地读取模式 | 云端模式 |
|--------|-------------|---------|
| 数据来源 | 设备本地 ETS100 下载目录 | ETS100 API + CDN 资源包 |
| 权限需求 | 文件访问、Root、Shizuku 或漏洞直读 | 网络访问 + ETS100 账号 |
| 资源状态 | 依赖本地是否已经下载 | 依赖云端作业列表 |
| 登录影响 | 不影响官方客户端登录 | 可能顶掉官方客户端登录 |
| 离线能力 | 本地已有资源可离线 | 首次获取必须联网 |

---

# 二、全局配置

```python
API_BASE_URL = "https://api.ets100.com"
CDN_BASE_URL = "https://cdn.subject.ets100.com"
PID = "grlx"
SECRET_KEY = "555ffbe95ccf4e9535a110170b445ab8"
FOOTER_SIZE = 336
TIMEOUT_SECONDS = 30
```

## 固定请求常量

| 常量 | 当前实现值 | 说明 |
|------|------------|------|
| `sn` | `"test"` | 设备序列号 |
| `system` | `"4"` | 系统标识 |
| `local_ip` | `"127.0.0.1"` | 本地 IP |
| `global_client_version` | `"5.4.5"` | 全局客户端版本 |
| `sign_response` | `1` | 要求签名响应 |
| `User-Agent` | `"libcurl-agent/1.0"` | 模拟原客户端请求头 |

## 当前实现中的版本差异

| 场景 | `version` | `device_name` |
|------|-----------|---------------|
| 登录 `/user/login` | `"3"` | `"DESKTOP"` |
| 获取父账户 `/m/ecard/list` | `"3"` | 不传 |
| 获取作业 `/g/homework/list` | `"3"` | 不传 |
| 设备绑定 `/user/rebind-code` | `"2"` | `"1337"` |
| Token 刷新 `/user/rebind-code` | `"2"` | 不传 |

---

# 三、请求签名算法

## 3.1 请求体包装规则

每次 API 请求的业务体不是直接发送 `params`，而是先构造成数组格式：

```json
[
    {
        "r": "user/login",
        "params": {
            "sn": "test"
        }
    }
]
```

然后：

1. 将数组 JSON 紧凑序列化为字符串；
2. 使用 UTF-8 编码；
3. Base64 编码，且不换行；
4. 使用 Base64 字符串参与签名。

## 3.2 签名公式

```text
sign_string = PID + timestamp + body_base64 + SECRET_KEY
signature = MD5(sign_string).hexdigest().lowercase()
```

| 参数 | 说明 |
|------|------|
| `PID` | 固定值 `"grlx"` |
| `timestamp` | Unix 时间戳，单位秒 |
| `body_base64` | 请求业务数组 JSON 的 Base64 |
| `SECRET_KEY` | 固定密钥 |

## 3.3 最终请求体格式

```json
{
    "body": "base64(body_data_array_json)",
    "head": {
        "version": "1.0",
        "sign": "md5_signature",
        "pid": "grlx",
        "time": 1716537600
    }
}
```

## 3.4 Python 参考实现

```python
import base64
import hashlib
import json
import time

PID = "grlx"
SECRET_KEY = "555ffbe95ccf4e9535a110170b445ab8"


def compact_json(data):
    # 宝贝这里固定紧凑 JSON，避免空格影响 Base64 和签名喵~
    return json.dumps(data, ensure_ascii=False, separators=(",", ":"))


def md5_hex(text):
    # 宝贝这里用 UTF-8 计算 MD5，输出小写 hex 喵~
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def build_payload(route, params):
    timestamp = int(time.time())
    body_data = [{"r": route, "params": params}]
    body_json = compact_json(body_data)
    body_base64 = base64.b64encode(body_json.encode("utf-8")).decode("ascii")
    sign = md5_hex(PID + str(timestamp) + body_base64 + SECRET_KEY)
    return compact_json({
        "body": body_base64,
        "head": {
            "version": "1.0",
            "sign": sign,
            "pid": PID,
            "time": timestamp
        }
    })
```

---

# 四、HTTP 请求规范

## 4.1 API 请求头

```http
Host: api.ets100.com
User-Agent: libcurl-agent/1.0
Content-Type: application/x-www-form-urlencoded
Accept: */*
```

> 虽然 `Content-Type` 是 `application/x-www-form-urlencoded`，但当前实现实际发送的是 JSON 字符串内容。

## 4.2 CDN 下载请求头

```http
Host: cdn.subject.ets100.com
User-Agent: libcurl-agent/1.0
Accept: */*
```

## 4.3 HTTP 方法

| 类型 | 方法 |
|------|------|
| API 请求 | `POST` |
| CDN ZIP 下载 | `GET` |

## 4.4 响应格式兼容

实际响应可能是对象：

```json
{
    "body": {}
}
```

也可能是数组：

```json
[
    {
        "body": {}
    }
]
```

跨平台实现建议：

```text
如果响应以 [ 开头：取数组第一个元素作为响应对象
否则：直接按对象解析
```

---

# 五、机器码 device_code 生成

`device_code` 格式固定为：

```text
data_md5|mac_md5
```

其中 `data_md5` 和 `mac_md5` 均为 16 字符 MD5 子串，最终总长度为：

```text
16 + 1 + 16 = 33 字符
```

## 5.1 原始 Windows / Python 机器码算法

原始算法适合 Windows 桌面端：

```python
import hashlib


def md5_middle_16(text):
    # 宝贝这里取 MD5 的第 8 到 24 位，保持 ETS100 格式喵~
    return hashlib.md5(text.encode("utf-8")).hexdigest().upper()[8:24]


def generate_machine_code_windows(read_registry, get_mac_address):
    data = ""
    data += read_registry("HKLM", r"SOFTWARE\Microsoft\Windows NT\CurrentVersion", "ProductId")
    data += read_registry("HKLM", r"SOFTWARE\Microsoft\Cryptography", "MachineGuid")
    data += read_registry("HKLM", r"SOFTWARE\ETSKey", "InstallDate")

    mac_address = get_mac_address()
    data_md5 = md5_middle_16(data)
    mac_md5 = md5_middle_16(mac_address)
    return f"{data_md5}|{mac_md5}"
```

## 5.2 Android 当前实现算法

Android 当前项目没有读取真实硬件信息，而是首次随机生成并持久化：

```text
randomHex(16) → MD5 → 取 [8:24] = data_md5
randomHex(16) → MD5 → 取 [8:24] = mac_md5
device_code = data_md5 + "|" + mac_md5
```

特点：

| 特点 | 说明 |
|------|------|
| 首次生成 | 随机生成 |
| 后续使用 | 从本地持久化存储读取 |
| 登出行为 | 保留机器码 |
| 完全重置 | 清除本地存储后重新生成 |

## 5.3 跨平台推荐策略

不同平台可任选一种稳定策略：

| 平台 | 推荐来源 |
|------|----------|
| Windows | ProductId + MachineGuid + MAC |
| macOS | IOPlatformUUID + MAC |
| Linux | `/etc/machine-id` + MAC |
| Android | 随机生成后保存到 SharedPreferences / DataStore |
| iOS | 随机生成后保存到 Keychain |
| 服务端 | 随机生成后保存到配置文件或数据库 |

关键要求：

```text
同一个安装实例必须长期使用同一个 device_code。
不要每次启动都重新生成，否则容易触发设备绑定或登录异常。
```

---

# 六、接口一：用户登录 `/user/login`

## 6.1 请求信息

```http
POST https://api.ets100.com/user/login
```

业务路由：

```json
"r": "user/login"
```

## 6.2 请求参数

| 参数 | 类型 | 必填 | 当前值 / 来源 | 说明 |
|------|------|------|---------------|------|
| `sn` | string | 是 | `"test"` | 设备序列号 |
| `phone` | string | 是 | 用户输入 | 手机号 |
| `password` | string | 是 | 用户输入 | 明文密码 |
| `device_code` | string | 是 | 本地生成 | 33 字符机器码 |
| `device_name` | string | 是 | `"DESKTOP"` | 登录设备名 |
| `version` | string | 是 | `"3"` | 登录版本 |
| `local_ip` | string | 是 | `"127.0.0.1"` | 本地 IP |
| `system` | string | 是 | `"4"` | 系统标识 |
| `global_client_version` | string | 是 | `"5.4.5"` | 客户端版本 |
| `sign_response` | int | 是 | `1` | 签名响应 |

## 6.3 body_data 示例

```json
[
    {
        "r": "user/login",
        "params": {
            "sn": "test",
            "phone": "13800138000",
            "password": "password123",
            "device_code": "1234567890ABCDEF|FEDCBA0987654321",
            "device_name": "DESKTOP",
            "version": "3",
            "local_ip": "127.0.0.1",
            "system": "4",
            "global_client_version": "5.4.5",
            "sign_response": 1
        }
    }
]
```

## 6.4 成功响应

```json
{
    "body": {
        "token": "xxxxxxxxxxxxxxxx"
    }
}
```

## 6.5 失败响应与设备绑定

如果响应中存在：

```json
{
    "code": 30014,
    "msg": "..."
}
```

说明当前设备需要绑定，应继续调用 `/user/rebind-code` 进行设备绑定。

其他 `code != 0` 的情况可视为登录失败，直接展示 `msg`。

---

# 七、接口二：设备绑定 `/user/rebind-code`

## 7.1 触发时机

当 `/user/login` 返回 `code=30014` 时触发。

## 7.2 请求信息

```http
POST https://api.ets100.com/user/rebind-code
```

业务路由：

```json
"r": "user/rebind-code"
```

## 7.3 请求参数

| 参数 | 类型 | 必填 | 当前值 / 来源 | 说明 |
|------|------|------|---------------|------|
| `sn` | string | 是 | `"test"` | 设备序列号 |
| `phone` | string | 是 | 用户输入 | 手机号 |
| `email` | string | 是 | `""` | 空邮箱 |
| `password` | string | 是 | 用户输入 | 明文密码 |
| `code` | string | 是 | `"0"` | 固定值 |
| `version` | string | 是 | `"2"` | 绑定接口版本 |
| `device_name` | string | 是 | `"1337"` | 绑定设备名 |
| `device_code` | string | 是 | 本地生成 | 机器码 |
| `local_ip` | string | 是 | `"127.0.0.1"` | 本地 IP |
| `system` | string | 是 | `"4"` | 系统标识 |
| `global_client_version` | string | 是 | `"5.4.5"` | 客户端版本 |
| `sign_response` | int | 是 | `1` | 签名响应 |

## 7.4 成功响应

```json
{
    "code": 0,
    "body": {
        "token": "xxxxxxxxxxxxxxxx"
    }
}
```

绑定成功后可直接使用返回的 `token`，不需要再次调用登录接口。

---

# 八、接口三：Token 刷新 `/user/rebind-code`

当前源码中还保留了一个轻量 Token 刷新方法，同样使用 `/user/rebind-code`：

## 8.1 请求参数

```json
[
    {
        "r": "user/rebind-code",
        "params": {
            "token": "old_token",
            "version": "2",
            "system": "4"
        }
    }
]
```

## 8.2 响应

```json
{
    "body": {
        "token": "new_token"
    }
}
```

## 8.3 当前项目实际策略

当前 App 在加载作业列表前，主要策略不是调用轻量刷新，而是：

```text
用保存的 phone + password + device_code 重新调用 /user/login
    ↓
重新获取 parent_account_id
    ↓
保存新 token
```

这样兼容性更强，但也更容易触发官方客户端被顶号。

---

# 九、接口四：获取父账户 ID `/m/ecard/list`

## 9.1 请求信息

```http
POST https://api.ets100.com/m/ecard/list
```

业务路由：

```json
"r": "m/ecard/list"
```

## 9.2 请求参数

| 参数 | 类型 | 必填 | 当前值 / 来源 | 说明 |
|------|------|------|---------------|------|
| `sn` | string | 是 | `"test"` | 设备序列号 |
| `token` | string | 是 | 登录 token | 认证令牌 |
| `version` | string | 是 | `"3"` | 当前实现值 |
| `system` | string | 是 | `"4"` | 系统标识 |
| `global_client_version` | string | 是 | `"5.4.5"` | 客户端版本 |
| `sign_response` | int | 是 | `1` | 签名响应 |

## 9.3 成功响应

```json
{
    "body": {
        "0": {
            "parent_id": "123456"
        }
    }
}
```

## 9.4 解析规则

```text
body 是对象
    ↓
取第一个 key（通常是 "0"）
    ↓
读取 body[firstKey].parent_id
```

如果 `body` 为空或 `parent_id` 为空，则视为未找到账号信息。

---

# 十、接口五：获取作业列表 `/g/homework/list`

## 10.1 请求信息

```http
POST https://api.ets100.com/g/homework/list
```

业务路由：

```json
"r": "g/homework/list"
```

## 10.2 请求参数

| 参数 | 类型 | 当前值 | 说明 |
|------|------|--------|------|
| `sn` | string | `"test"` | 设备序列号 |
| `token` | string | 登录 token | 认证令牌 |
| `parent_account_id` | string | 父账户 ID | 来自 `/m/ecard/list` |
| `limit` | string | `"0"` | `0` 表示全部 |
| `status` | string | `"1"` | 作业状态 |
| `offset` | string | `"0"` | 偏移量 |
| `max_end_time` | string | `""` | 空 |
| `max_homework_id` | string | `""` | 空 |
| `min_end_time` | string | `""` | 空 |
| `min_homework_id` | string | `""` | 空 |
| `get_to_do_count` | int | `1` | 获取待完成数量 |
| `show_old_homework` | int | `1` | 显示旧作业 |
| `parent_homework_id` | string | `""` | 空 |
| `get_all_count` | int | `1` | 获取总数 |
| `check_pass` | int | `1` | 固定值 |
| `get_to_overtime_count` | int | `1` | 获取超时数量 |
| `version` | string | `"3"` | 当前实现值 |
| `system` | string | `"4"` | 系统标识 |
| `global_client_version` | string | `"5.4.5"` | 客户端版本 |
| `sign_response` | int | `1` | 签名响应 |

## 10.3 响应结构

```json
{
    "body": {
        "base_url": "https://cdn.subject.ets100.com",
        "data": [
            {
                "name": "作业名称",
                "struct": {
                    "contents": [
                        {
                            "group_name": "题型名称",
                            "url": "/path/to/file.zip"
                        }
                    ]
                }
            }
        ]
    }
}
```

## 10.4 解析后的通用数据结构

```typescript
interface HomeworkListResponse {
  baseUrl: string;
  homeworks: HomeworkInfo[];
}

interface HomeworkInfo {
  name: string;
  contents: HomeworkContent[];
}

interface HomeworkContent {
  groupName: string;
  url: string;
}
```

## 10.5 解析规则

```text
body.base_url → CDN 基础地址，如果为空则使用默认 CDN_BASE_URL
body.data[] → 作业数组
    item.name → 作业名
    item.struct.contents[] → 作业下的题型资源包列表
        content.group_name → 题型名称
        content.url → ZIP 资源路径
```

---

# 十一、云端资源下载

## 11.1 URL 拼接规则

```python
def build_zip_url(base_url, content_url):
    # 宝贝这里把 http 强制替换成 https，减少重定向和明文传输喵~
    if content_url.startswith("http://") or content_url.startswith("https://"):
        return content_url.replace("http://", "https://", 1)

    base = base_url.replace("http://", "https://", 1).rstrip("/")
    path = content_url if content_url.startswith("/") else "/" + content_url
    return base + path
```

示例：

```text
base_url = "https://cdn.subject.ets100.com"
url      = "/resource/abc123.zip"
结果     = "https://cdn.subject.ets100.com/resource/abc123.zip"
```

## 11.2 文件名提取

```text
zip_file_name = URL 最后一个 / 后面的部分，再去掉 ? 查询参数
```

示例：

```text
https://cdn.subject.ets100.com/path/abc123.zip?x=1
→ abc123.zip
```

## 11.3 缓存目录建议

不同平台可自行选择缓存路径：

| 平台 | 建议缓存目录 |
|------|--------------|
| Android | `context.cacheDir/cloud_homework/` |
| Windows | `%LOCALAPPDATA%/YourApp/cloud_homework/` |
| macOS | `~/Library/Caches/YourApp/cloud_homework/` |
| Linux | `~/.cache/your-app/cloud_homework/` |
| 服务端 | 工作目录下 `cache/cloud_homework/` |

缓存结构：

```text
cloud_homework/
├── abc123.zip
├── abc123/
│   └── content.json
├── def456.zip
└── def456/
    └── content.json
```

## 11.4 HTTPS 证书注意事项

当前 Android 实现中，由于 CDN 证书可能存在主机名不匹配问题（证书可能签发给 `default.chinanetcenter.com`），下载时跳过了 SSL 证书与主机名校验。

跨平台建议：

| 环境 | 建议 |
|------|------|
| 调试工具 | 可临时允许跳过证书验证 |
| 正式应用 | 尽量保留证书校验，失败时提示用户 |
| 服务端批处理 | 可配置是否跳过证书验证 |

风险：跳过证书校验可能遭遇中间人攻击。

---

# 十二、ZIP 验证、密码生成与解压

## 12.1 ZIP Magic Number 校验

下载完成后先读取文件前 4 字节：

| Magic Number | 说明 |
|-------------|------|
| `504B0304` | 标准 ZIP 文件 |
| `504B0506` | 空 ZIP |
| `504B0708` | ZIP64 / 分卷相关结构 |

如果不是上述值，说明下载内容可能不是 ZIP（例如错误页 HTML），应跳过或报错。

## 12.2 密码生成算法

```text
读取 ZIP 尾部 336 字节
    ↓
验证签名：footer[0:8] == "MSTCHINA" 或 footer[144:149] == "EPLAT"
    ↓
提取 seed = footer[16:144]，共 128 字节
    ↓
first_hex = MD5(seed).hex().upper()
    ↓
second_hex = MD5(first_hex ASCII bytes).hex().upper()
    ↓
password = first_hex + second_hex
```

## 12.3 Python 参考实现

```python
import hashlib

FOOTER_SIZE = 336


def generate_zip_password(zip_data: bytes) -> str:
    if len(zip_data) < FOOTER_SIZE:
        raise ValueError("宝贝 ZIP 太小啦，不能提取尾部数据喵~")

    footer = zip_data[-FOOTER_SIZE:]

    signature_valid = footer[0:8] == b"MSTCHINA" or footer[144:149] == b"EPLAT"
    if not signature_valid:
        raise ValueError("宝贝 ZIP 签名不对，可能不是 ETS100 资源包喵~")

    seed = footer[16:144]
    first_hex = hashlib.md5(seed).hexdigest().upper()
    second_hex = hashlib.md5(first_hex.encode("ascii")).hexdigest().upper()
    return first_hex + second_hex
```

## 12.4 解压要求

资源包通常是加密 ZIP，需要支持传统 ZIP 加密的库：

| 语言 | 推荐库 |
|------|--------|
| Python | `pyzipper` / `zipfile`（视加密方式而定） |
| Kotlin / Java | `zip4j` |
| C# | `SharpZipLib` / `DotNetZip` |
| C++ | `minizip` / `libzip` |
| Node.js | 支持加密 ZIP 的第三方库 |

解压后默认直接在解压目录查找：

```text
content.json
```

如果没有找到，可递归搜索 `content.json` 作为兼容策略。

---

# 十三、content.json 答案解析

云端资源包解压后，核心答案数据位于：

```text
content.json
```

## 13.1 通用解析流程

```text
读取 content.json
    ↓
解析 JSON
    ↓
读取 structure_type
    ↓
根据 structure_type 分发到对应解析器
    ↓
输出 Paper / Section / Question 或自定义结构
```

## 13.2 structure_type 与答案位置

| structure_type | 题型 | 答案位置 |
|----------------|------|----------|
| `collector.role` | 听选信息 / 回答问题 / 提问 | `info.question[].std[].value` |
| `collector.3q5a` | 广东高中 3问5答 | `info.question[].std[]` |
| `collector.choose` | 听后选择 | `info.xtlist[].answer`，选项在 `info.xtlist[].xxlist` |
| `collector.picture` | 信息转述 / 听后转述 | `info.std[].value` |
| `collector.fill` | 听后记录 / 填空 | `info.std[].value`，题号在 `info.std[].xth` |
| `collector.dialogue` | 回答问题 | `info.question[].std[].value` |
| `collector.read` | 模仿朗读 | 无标准答案，通常跳过 |

## 13.3 文本清洗规则

解析题干和答案时建议清洗：

```text
1. 移除 ets_th{数字} 前缀
2. 移除 HTML 标签，例如 <p>、<br>、</p>
3. 移除零宽空格 \u200B
4. 将 </p><p> 替换为换行
5. trim 前后空白
```

## 13.4 输出数据结构建议

```typescript
interface Paper {
  paperId: string | number;
  title: string;
  sections: Section[];
}

interface Section {
  caption: string;
  typeName: string;
  structureType: string;
  questions: Question[];
}

interface Question {
  order: number;
  sectionOrder: number;
  questionText: string;
  answers: string[];
  options?: string[];
}
```

更完整的题型解析细节可参考：[ets100答案解析方法.md](./ets100答案解析方法.md)。

---

# 十四、完整跨平台实现流程

```text
初始化本地存储
    ↓
读取或生成 device_code
    ↓
用户输入 phone/password
    ↓
调用 /user/login
    ├─ 成功：获取 token
    └─ code=30014：调用 /user/rebind-code 绑定并获取 token
    ↓
调用 /m/ecard/list 获取 parent_account_id
    ↓
保存 phone/password/token/parent_account_id/device_code
    ↓
调用 /g/homework/list 获取作业列表
    ↓
遍历 homework.struct.contents[]
    ↓
拼接 ZIP 下载 URL
    ↓
下载 ZIP 到缓存目录
    ↓
校验 ZIP Magic Number
    ↓
生成 ZIP 解压密码
    ↓
解压 ZIP
    ↓
读取 content.json
    ↓
根据 structure_type 解析答案
    ↓
展示 / 导出 / 缓存结果
```

---

# 十五、本地状态与缓存建议

## 15.1 认证信息存储

至少保存：

| Key | 说明 | 是否建议加密 |
|-----|------|--------------|
| `device_code` | 机器码 | 可不加密，但需要稳定保存 |
| `phone` | 手机号 | 建议加密 |
| `password` | 明文密码 | 强烈建议加密 |
| `token` | API Token | 建议加密 |
| `parent_account_id` | 父账户 ID | 可不加密 |
| `is_logged_in` | 登录状态 | 可不加密 |

## 15.2 密码保存策略

当前 Android 项目为了自动重新登录，会保存明文密码。跨平台实现可选择：

| 策略 | 优点 | 缺点 |
|------|------|------|
| 保存密码 | 可自动刷新 Token | 安全风险较高 |
| 只保存 Token | 更安全 | Token 过期后需重新登录 |
| 系统密钥链保存密码 | 安全性较好 | 实现复杂度更高 |

推荐：正式产品使用系统安全存储，例如 Android Keystore、iOS Keychain、Windows Credential Manager、macOS Keychain。

## 15.3 作业缓存状态

可维护以下状态：

```typescript
interface CloudHomeworkState {
  homeworkList: HomeworkInfo[];
  cloudBaseUrl: string;
  downloadedPapers: Map<string, Paper[]>;
  downloadedHomeworkNames: Set<string>;
  downloadingHomeworks: Set<string>;
  failedHomeworks: Set<string>;
  isLoading: boolean;
  error?: string;
}
```

---

# 十六、异常处理建议

| 阶段 | 常见异常 | 建议处理 |
|------|----------|----------|
| 登录 | 密码错误、验证码、设备绑定 | 展示服务端 `msg`，必要时走绑定流程 |
| 绑定 | token 为空、code 非 0 | 提示重新登录 |
| 获取父账户 | `body` 为空、无 `parent_id` | 提示账号信息异常 |
| 获取作业 | token 失效、网络失败 | 重新登录后重试 |
| 下载 ZIP | 404、SSL 失败、下载到 HTML | 检查 HTTP 状态和 Magic Number |
| 生成密码 | 文件太小、签名无效 | 说明不是 ETS100 加密资源包 |
| 解压 | 密码错误、ZIP 损坏 | 删除缓存后重试下载 |
| 解析 | 缺少 `content.json`、未知结构 | 跳过该资源并记录日志 |

---

# 十七、风险与注意事项

## 17.1 顶号风险

使用云端模式登录可能会导致 ETS100 官方客户端被退出登录。特别是采用“每次读取前自动重新登录”的实现时，更容易触发顶号。

建议：

```text
先使用云端工具读取答案，再打开 ETS100 官方客户端做题。
不要在考试、录音、提交过程中触发云端登录。
```

## 17.2 明文密码风险

如果为了自动登录保存密码，应明确告知用户并尽量使用系统安全存储。

## 17.3 SSL 证书跳过风险

跳过 HTTPS 证书校验会带来中间人攻击风险。调试工具可以开放配置，正式应用建议默认不跳过。

## 17.4 接口参数变动风险

ETS100 可能随时调整参数、版本号、签名规则或响应结构。实现时应保留详细日志，方便排查。

---

# 十八、最小可行开发清单

- [ ] 实现本地持久化存储
- [ ] 实现稳定 `device_code` 生成
- [ ] 实现 JSON 紧凑序列化
- [ ] 实现 Base64 编码
- [ ] 实现 MD5 签名
- [ ] 实现通用 POST 请求封装
- [ ] 实现 `/user/login`
- [ ] 实现 `code=30014` 后的 `/user/rebind-code`
- [ ] 实现 `/m/ecard/list`
- [ ] 实现 `/g/homework/list`
- [ ] 实现 CDN ZIP 下载
- [ ] 实现 ZIP Magic Number 校验
- [ ] 实现 ZIP 尾部密码生成
- [ ] 实现加密 ZIP 解压
- [ ] 实现 `content.json` 解析
- [ ] 实现缓存和失败重试
- [ ] 实现风险提示与日志输出

---

# 十九、端到端伪代码

```python
def main():
    # 宝贝先准备稳定机器码喵~
    device_code = load_or_create_device_code()

    phone = input("phone: ")
    password = input("password: ")

    try:
        token = login(phone, password, device_code)
    except DeviceBindRequired:
        token = bind_device(phone, password, device_code)

    parent_id = get_parent_account_id(token)
    save_auth(phone, password, token, parent_id, device_code)

    homework_response = get_homework_list(token, parent_id)
    for homework in homework_response.homeworks:
        print("作业:", homework.name)
        sections = []
        for content in homework.contents:
            zip_url = build_zip_url(homework_response.baseUrl, content.url)
            zip_path = download_zip(zip_url)
            check_zip_magic(zip_path)
            password = generate_zip_password(open(zip_path, "rb").read())
            extract_dir = unzip_with_password(zip_path, password)
            content_json = read_json(extract_dir / "content.json")
            section = parse_content_json(content_json, content.groupName)
            sections.append(section)
        show_answers(homework.name, sections)
```

---

*文档版本：2.0*
*基于当前 Fe 项目源码 `ETS100ApiClient.kt`、`ETS100AuthManager.kt`、`ReadScreen.kt`、`ZipPasswordGenerator.kt` 以及云端模式逻辑整理生成。*
