# OpenCode PyCharm Plugin (MVP Skeleton)

该仓库现在包含一个基于 IntelliJ Platform 的 OpenCode PyCharm 插件实现骨架，按 PRD 的关键要求落地了最小可运行能力。

## 已实现能力
- **ACP 客户端基础连接**：WebSocket 连接、请求/通知发送、基础日志。
- **Cursor 风格快捷键**：
  - `Cmd/Ctrl + K`：内联指令浮层入口。
  - `Cmd/Ctrl + L`：打开 OpenCode 侧边聊天窗口。
  - `Tab`：接受 Ghost Text（MVP 骨架）。
  - `Cmd/Ctrl + Right`：逐词接受 Ghost Text（MVP 骨架）。
- **Tool Window 聊天 UI**：输入框、`@` mention 补全、Apply to Editor 触发。
- **ACP Host 基础能力**：
  - state sync 上报（文件、可见区域、光标/选区）；
  - 工作区读写接口入口（read/list/search）；
  - WorkspaceEdit 的 Replace/Insert/Delete 原子应用（单次命令）。
- **启动流程**：项目启动自动连接 ACP，并监听光标/文档变化。

## 项目结构
- `src/main/kotlin/com/opencode/pycharm/acp`：ACP 协议通信与模型。
- `src/main/kotlin/com/opencode/pycharm/actions`：快捷键动作。
- `src/main/kotlin/com/opencode/pycharm/service`：核心服务、状态同步、编辑应用。
- `src/main/kotlin/com/opencode/pycharm/ui`：Tool Window UI。
- `src/main/resources/META-INF/plugin.xml`：插件声明与扩展注册。

## 本地运行（开发模式）
1. 设置环境变量（示例）：`export OPENCODE_TOKEN=xxx`
2. 启动 IntelliJ 插件沙箱：`./gradlew runIde`
3. 在沙箱 IDE 中使用快捷键测试 OpenCode 行为。

## 备注
当前实现是 MVP skeleton，用于快速建立插件主干与 ACP/交互闭环，后续可继续补齐：
- 真正的流式渲染与 Inline Diff 视图；
- 更完整的 ACP 消息反序列化和响应路由；
- 更精细的 Undo/事务边界与跨文件一致性处理。
