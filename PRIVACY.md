# Privacy / 隐私说明

本应用不含广告、统计、远端更新或第三方网络 SDK。家长 PIN 以加盐哈希保存在本地私有存储；受管应用列表、计时进度、答题和休息状态保存在本机。无障碍服务识别前台应用包名，用于计时和显示题目，不上传屏幕内容或应用列表。

仅在用户点击辅助授权按钮时，通过本机回环地址连接传统 ADB。用户需开启调试并同意系统授权。本机 ADB 密钥位于应用私有、禁止备份目录；不打印或上传密钥。App 不申请通用安全设置写权限，辅助授权保留既有无障碍服务并在失败时尝试回滚。设备或系统进程终止可能使授权失败；请核对服务状态。完成授权后关闭 ADB 调试。

App does not collect analytics or send screen/app data to a server. Settings, salted PIN hashes, viewing time and pending challenges stay on-device. User-initiated legacy local ADB connects only to loopback; its key is private and excluded from backup. Android authorisation is required. Disable debugging after setup.

On the verified B3P firmware, local background protection queries and registers only this app's package name, label and version in the vendor's on-device process-protection provider. It does not change other applications' entries or upload this configuration. No additional general system-writing permission is requested.
