# 本机交付状态

日期：2026-09-15  
阶段：D1/D2源码编写与静态核对。未进入环境/数据/配置/运行阶段。

## 已编写
| 内容 | 源码 | 实施范围 |
| --- | --- | --- |
| 身份与错误 | contracts/Actor、Failure | 可信身份使用、归属与角色规则 |
| 活动时间/查询 | ActivityTime、ActivityQuery | 未知时间、取消延期、日期与分页输入 |
| 来源处理 | IngestionFlow、DuplicatePolicy | 流程状态、来源端口、保守重复判定 |
| 审核 | RevisionPolicy、EditorialService | 字段白名单、证据、人工保护、乐观版本、原子存储端口 |
| 报名候补 | RegistrationPolicy、RegistrationService、RegistrationStore | 无座位报名、取消、自动递补、管理取消、稳定命令契约 |
| 订阅提醒 | ReminderPolicy、SubscriptionService、NotificationDelivery | 订阅条件、归属、版本失效、发送判定及去重端口 |
| 只读Agent | AgentPorts、ReadOnlyAssistant、EvidenceGuard | 有限工具编排、证据身份、会话版本与消息重放 |
| 确认草稿 | DraftService、ConfirmationPolicy | 草稿生成编排、归属、期限与规则版本、执行端口 |
| 学生端页面 | web/src/pages | 活动列表/详情、订阅、通知、模拟报名、助手 |
| 管理端页面 | AdminPage | 来源/审核/修订/场次/记录/任务/审计视图、审核字段与创建场次表单 |
| 接入状态 | EventFlowClient、useResource、ResourcePanel | 缺连接、读取状态、安全失败、取消旧请求 |
| 文档 | docs、contracts/README | 研究快照、架构、契约、验收和交接 |

## 不在本机实现/验证
- 上述外部Adapter的实际实现、HTTP控制器、登录、DB、Redis、MQ、模型和向量库。
- SQL/迁移、业务数据、Mock、种子、测试夹具。
- pom/package/lock/env/Docker/CI或其他构建与运行配置。
- 源码编译、运行测试、启动网页、浏览器截图或压测。
- Git初始化、提交、远程仓库和上传。
- 分页后台大数据集、动态候补资格重核、修订字段删除等后续扩展。

## 静态检视记录
只通过文本阅读、文件枚举和搜索检查，不运行项目或测试：
- 修正Java record内同名text accessor与静态校验函数的名称歧义，改为限定调用。
- 报名变更结果采用Optional，系统递补不使用null状态假装用户结果。
- 取消绑定expectedSequence，防旧取消影响新报名。
- 同场次快照身份验证；管理取消独立权限。
- 聊天增加已提交消息重放和原子append返回原结果契约。
- UI无效筛选会作废旧请求，降低陈旧响应覆盖风险。
- 草稿过期/规则更新后清理旧草稿身份，允许重新准备。
- 页面输出普通文本；安全外链保护；未接入不呈现业务成功。
- client身份对象变更时App重建页面树，清理旧会话/请求的可见状态；同一对象内部更换身份仍被契约禁止。
- 管理创建成功后清空表单；规则版本冲突会重新读取详情，而不是继续使用陈旧版本准备草稿。
- 文件审计仅发现Java、TypeScript、Vue、CSS和Markdown；未发现构建/环境/SQL/二进制文件或Git/依赖目录。
- 静态检查不证明编译通过或运行可靠，剩余验收全部见ACCEPTANCE。

## 资料与技能影响
研究文档以快照形式复制到docs/research，保留原桌面文档；研究中的“尚未开发”是历史描述，不代表本次未编写代码。
模块设计技能用于集中纯规则与外部接入契约；前端构建技能用于组件、状态和布局组织。
用户限制优先：没有执行图像概念、浏览器启动/视觉验收，不宣称页面已达到视觉保真或生产可用。
新源代码根据产品方案编写，没有把上游完整实现或旧配置复制进来；后续若逐段复用上游代码需补相应许可/归属。
