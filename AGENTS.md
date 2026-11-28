# 仓库协作与提交规范

## 提交信息与发版日志语言

- 本仓库所有 `git commit` 提交信息（包括 Conventional Commits 的描述部分，例如 `ci(release): 新增 Telegram 频道发布与失败通知`）**必须使用简体中文**，禁止使用英文描述。
- 本仓库的 GitHub Release 更新日志、Telegram 频道发版通知文案及手动编写的更新日志，均**必须使用简体中文**输出（代码标识符、版本号、命令或专有名词除外）。
- 每次发布新版本前，应同步在 `app/src/main/java/com/av123/video/ui/screens/about/AboutScreen.kt` 的 `APP_CHANGELOG` 中补充对应版本的简体中文更新日志条目。

## Git 账号隔离与推送边界

- 本仓库提交作者必须保持为 `daweishimisi11 <360553218+daweishimisi11@users.noreply.github.com>`，严禁混用全局其他账号。
- 仅向 `origin`（`https://github.com/daweishimisi11/123.git`）推送分支或标签，禁止向 `upstream` 推送
