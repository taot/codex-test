# 产品需求文档（PRD）：Opencode PyCharm 插件（Cursor 交互风格）

## 1. 产品概述

开发一款基于 IntelliJ Platform（面向 PyCharm 2023.x+）的 Opencode 插件，作为 Opencode Agent 的 IDE 前端 UI。插件需严格复刻 Cursor 的核心交互模式与快捷键体系，并通过 Agent Client Protocol（ACP）与 Opencode 后端建立双向通信。

### 1.1 产品目标
- 在 PyCharm 内提供接近 Cursor 的零学习成本 AI 辅助编程体验。
- 支持高频、低延迟、可撤销的代码生成与编辑流程。
- 以 ACP 标准协议实现 IDE 与 Agent 的解耦。

### 1.2 成功指标（KPI）
- 用户触发到 UI 首次反馈延迟 < 50ms（本地 UI 渲染侧）。
- ACP 能力覆盖率 100%（本 PRD 范围内列出的 capabilities）。
- AI 应用到编辑器后的整体撤销操作可通过 1 次 Cmd/Ctrl + Z 完整回滚。
- 兼容 PyCharm 2023.x 及以上版本。

---

## 2. 系统架构

插件在 ACP 体系中扮演 IDE Host，分为三层：

### 2.1 IDE UI 层（IntelliJ Platform SDK）
职责：
- 拦截编辑器快捷键与事件分发。
- 渲染内联输入、Ghost Text、Inline Diff、Tool Window。
- 管理 Tool Window 生命周期与上下文注入。
- 处理用户接受/拒绝 AI 变更的交互决策。

### 2.2 协议层（ACP Integration）
职责：
- 实现基于 JSON-RPC + WebSocket 的 ACP 通信。
- 序列化 IDE 状态（文件、光标、选区、可见范围等）并上报。
- 反序列化 Agent 指令（编辑、读取、搜索等）并路由执行。
- 支持流式分块响应（chunk）接收与增量渲染。

### 2.3 后端网关层（Opencode API Gateway）
职责：
- 鉴权（Token/OAuth/Session，按 Opencode API 规范落地）。
- 保持 WebSocket 长连接心跳（ping/pong）。
- 处理重连、退避策略、会话恢复。

---

## 3. 核心功能需求

## 3.1 Cursor 风格交互与快捷键体系

### 3.1.1 Cmd/Ctrl + K（内联生成/编辑）
**触发条件**
- 光标所在位置触发；
- 或有选中代码块时触发（对选区进行改写）。

**UI 行为**
- 在编辑器内渲染紧凑悬浮输入框（无原生窗口边框）。
- 输入自然语言指令后发起 ACP 请求。
- 接收响应后原位渲染 Inline Diff：
  - 删除行红色；
  - 新增行绿色。

**接受/拒绝**
- Cmd/Ctrl + Enter：Accept（应用变更）。
- Esc：Reject（丢弃变更并恢复原文）。

### 3.1.2 Cmd/Ctrl + L（全局上下文对话）
**触发条件**
- 打开右侧 Opencode Tool Window。
- 若存在编辑器选区，自动附加为上下文块。

**UI 行为**
- 对话输入框支持 `@` mention 自动补全：
  - 当前文件；
  - 指定目录；
  - 函数/符号；
  - 全 codebase。

**代码应用**
- 聊天代码块支持一键 `Apply to Editor`。
- 点击后通过 ACP WorkspaceEdit 指令执行变更。

### 3.1.3 Tab（智能补全 / Ghost Text）
**触发条件**
- 用户输入时后台静默请求补全。

**UI 行为**
- 光标后显示灰色斜体 Ghost Text。

**接受策略**
- Tab：全量采纳。
- Cmd/Ctrl + Right Arrow：逐词采纳。

---

## 3.2 ACP 能力实现（IDE Host Capabilities）

### 3.2.1 状态同步（State Sync）
插件需实时上报：
- 当前活动文件路径；
- 可见行范围（viewport）；
- 光标坐标与选区。

### 3.2.2 文件系统操作（FS Operations）
需实现 ACP 约定接口：
- `readFile`
- `listFiles`
- `searchWorkspace`

要求：
- 遵循 IDE 工作区权限边界；
- 对大目录查询提供分页或限流能力；
- 对搜索结果提供路径 + 行号定位信息。

### 3.2.3 工作区编辑（Workspace Edits）
支持并正确执行：
- Replace
- Insert
- Delete

强约束：
- 单次 Agent 返回的所有编辑必须在 IntelliJ `UndoManager` 中合并为单一事务。
- 用户应可通过一次 Cmd/Ctrl + Z 撤销整个 AI 变更块。

---

## 3.3 渲染、性能与并发要求

### 3.3.1 流式响应（Streaming）
- 后端响应按 chunk 流式下发。
- UI 必须增量渲染，不得等待完整响应后再显示。

### 3.3.2 EDT 线程保护
- 网络 IO、Diff 计算、AST 分析必须在后台线程执行。
- 禁止阻塞 IntelliJ EDT。
- 所有 UI 更新通过 IntelliJ 推荐的 UI 调度机制回到 EDT。

### 3.3.3 性能指标
- 快捷键触发到浮层出现：P95 < 50ms。
- 流式首 token 到达后首帧渲染：P95 < 80ms。
- 10k 行文件中应用中等规模补丁不得引发明显卡顿（主线程无长阻塞）。

---

## 4. 实施阶段与验收标准

## 阶段 1：基础框架与 ACP 连通
**交付内容**
- PyCharm 插件脚手架与基础模块分层。
- ACP WebSocket/HTTP 客户端。
- Opencode 鉴权流程接入。
- ping/pong 心跳与自动重连。

**验收标准**
- 可稳定建立会话并维持连接。
- 断线后可自动重连并恢复基本可用状态。

## 阶段 2：核心 UI 与 Cursor 交互
**交付内容**
- Cmd/Ctrl + K 悬浮输入组件。
- Cmd/Ctrl + L Tool Window 对话面板。
- 选区自动上下文附加。
- `@` mention 自动补全基础能力。

**验收标准**
- 两组快捷键在 Windows/macOS 下行为一致。
- 对话与内联请求可正常发起并接收流式响应。

## 阶段 3：Inline Diff 与安全编辑落地
**交付内容**
- Inline Diff 高保真渲染。
- ACP WorkspaceEdit 全量解析与应用。
- 多行替换/插入/删除场景下 Undo 单事务合并。

**验收标准**
- 所有编辑操作可正确应用且可一次撤销。
- 多文件编辑路径下不出现错位写入。

## 最终验收（Release Gate）
- UI 延迟目标达成（关键路径 < 50ms）。
- PyCharm 2023.x+ 兼容性验证通过。
- ACP 协议能力覆盖率 100%。
- 核心交互（Cmd/Ctrl + K / Cmd/Ctrl + L / Tab）通过回归测试。

---

## 5. 非功能性要求

### 5.1 稳定性与容错
- 网络抖动时请求可重试，避免重复应用编辑。
- ACP 消息需具备请求 ID 与幂等处理机制。

### 5.2 安全与隐私
- 仅上传用户显式授权的上下文。
- 配置项支持关闭全局代码库索引/上传。
- 本地日志默认脱敏（文件路径、代码片段可配置掩码）。

### 5.3 可观测性
- 记录关键链路指标：请求时延、流式吞吐、编辑应用耗时、失败率。
- 提供调试开关导出 ACP 会话日志（脱敏）。

---

## 6. 风险与缓解策略

- **风险：快捷键冲突（IDE/系统级）**  
  缓解：提供可配置键位映射与冲突检测提示。

- **风险：大文件 Diff 导致性能下降**  
  缓解：增量 Diff + 分片计算 + 后台线程执行。

- **风险：流式中断导致 UI 状态不一致**  
  缓解：引入会话状态机（Pending/Streaming/Completed/Aborted）。

- **风险：WorkspaceEdit 跨文件事务一致性**  
  缓解：编辑前校验版本戳，失败时原子回滚并提示用户。

---

## 7. MVP 范围（首发）

MVP 必须包含：
- Cmd/Ctrl + K 内联编辑（含 Accept/Reject）。
- Cmd/Ctrl + L 侧边对话（含上下文附加、Apply to Editor）。
- Tab Ghost Text（全采纳 + 逐词采纳）。
- ACP：State Sync + FS Operations + Workspace Edits。
- 流式渲染与 Undo 单事务保障。

可延期能力（Post-MVP）：
- 更高级别符号索引与跨仓库上下文。
- 多代理协作模式。
- 团队级策略与审计控制台。
