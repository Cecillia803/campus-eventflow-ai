# 接口契约 · 源码阶段

这是人类可读契约，不是HTTP路由配置。传输层、身份适配、存储实现均后置。
Java核心与TypeScript界面类型并不直接一一序列化；另一台电脑应在Adapter中显式映射，并做契约测试。

## 共享要求

1. Actor仅由可信认证Adapter构造，任何请求/模型给出的用户身份不得覆盖它。
2. 读取与写入都校验归属；管理菜单可见不代表有权限。
3. 写请求使用操作幂等键；相同用户＋键＋相同语义参数返回原始结果，不同参数冲突。
4. 请求结果回放优先于新的时间/版本校验；用户已经成功提交不能因为重试时过期变成失败。
5. 不记录或接受凭据；配置、认证/HTTP/DB Adapter不在本阶段实现。
6. 外部依赖缺失返回NOT_CONNECTED，不返回空列表、成功、默认用户或伪造AI回答。
7. 浏览器传输Adapter须校验响应结构、枚举、标识与版本；不得仅用TypeScript类型断言当运行时校验。
8. Java long标识/报名sequence传输为十进制字符串。界面version若继续用number，必须服务端及客户端共同限制在JS安全整数范围，否则统一改字符串。
9. 数据文字一律普通文本渲染。网页不执行来源HTML或模型Markdown内嵌HTML。
10. 所有清单接口应有分页/上限；当前除活动分页外，账户/后台数组为首版有限响应契约，正式Adapter必须限制条数并后续提供分页。禁止把全库一次返回。

## 前端调用与后端职责

| EventFlowClient调用 | 核心对应/后置处理 |
| --- | --- |
| search | ActivityQuery.Filter；from/untilExclusive表示举办日期筛选，左闭右开，以client.zone解释 |
| detail | 返回聚合详情、官方入口、独立演示场次、来源及修订；查询Adapter后置 |
| subscriptions / subscribe / unsubscribe | SubscriptionService；取消绑定expectedRevision，存储检查owner |
| notifications | 读取本人站内收件箱；NotificationDelivery负责持久去重，当前无实现 |
| registrations | 读取本人请求与报名记录；PENDING仅在真实异步请求Adapter有此状态时返回 |
| cancelRegistration | sessionId＋expectedSequence，映射RegistrationService.cancel，不只是按场次取消最新记录 |
| prepareDraft | sessionId＋ruleVersion＋commandKey，映射DraftService.prepare |
| confirmDraft | 服务端ConfirmationPolicy.Executor按当前用户和draftId执行或回放；不得前端传“已确认”状态字段替代用户操作接口 |
| openChat / chat | Conversations的持久Adapter创建/读取owner-scoped会话，创建也必须幂等 |
| ask | chatId＋expectedVersion＋messageId＋question；映射ReadOnlyAssistant.ask，zone来自可信请求设置 |
| admin | 来源/审核/修订/场次/报名/任务/审计；查询与权限Adapter后置 |
| adminAction | 仅允许模块许可动作；审核通过映射EditorialService；拒绝、任务重试是后置任务Adapter职责 |
| createSession | 后台输入无时区本地时间＋zone；Adapter必须处理不存在/重复的夏令时时刻，禁止直接猜成UTC |

### 活动查询

Query.text≤300；limit后置Adapter给出且必须≤100。首页首版无静态类别样例，输入条件从用户输入取得。
未注明时间的活动不能被当成“报名中”；按日期筛选时明确排除未知日期并在覆盖说明中说明。
稳定排序使用业务排序字段＋唯一活动身份，游标与查询条件绑定，防止条件改变后复用旧游标。

### 资料审核

ReviewField.key白名单：
title、summary、content、campus、category、organizer、eventTime、registrationTime、eligibility、registrationInstructions、notice。
每个字段有sourceId/sourceRevision/locator，不允许来源URL等自由文本当可信凭据。
界面只编辑值，不伪造证据位置；若修订确实需要换来源，另一台电脑应增加来源选择器并验证证据存在。
当前修订策略为非空字段增量更新，不实现字段删除；清除错误事实需要后续明确“删除/未知”的审计模型，不能用空串代替。
日期字段在该Module中保留原始语义文本；结构化日期解析与来源置信度核验在真实提取Adapter完成。

### 模拟报名

RegistrationStore.transact负责锁定/串行化场次、查幂等记录、纯规则计算、持久化状态/结果/事件意图。
整场次Session快照只是规则输入，不得照搬成全量DB读写的高并发实现。
原子写失败不得部分扣减；事件投递失败由后置可靠事件Adapter恢复。
REGISTER、CANCEL和CANCEL_SESSION属于不同命令语义；同幂等键跨操作重用必须冲突。
取消必须携带报名sequence；旧sequence即使用户再次报名也不允许取消新记录。
Session.version是状态并发版本；Draft.ruleVersion是业务规则版本，两者不能混为一谈。
候补资格按成功加入队列时判定；如果后续引入动态资格变化，必须新增递补前资格重核策略与跳过/退出记录，当前不宣称支持。

### 提醒

Subscription字段内为OR，不同字段之间为AND；不能全部为空。
站内提醒逻辑身份由subscriptionId＋subscriptionRevision＋activityId＋activityRevision＋kind组成。
ReminderPolicy.SEND只是允许投递的决定，不表示已发送。
投递实现必须再次核查当前版本、退订状态、来源新鲜度，并原子记录发送结果；截止提醒不发送给已过期活动。

### Agent

Planner仅可返回四种只读意图：FIND_ACTIVITIES、EXPLAIN_ACTIVITY、MY_REGISTRATIONS、CLARIFY。
所有模型调用、检索和会话存储为接口，当前无模型供应商绑定、API key或向量库。
Query日期边界与时区做基础校验；模型相对时间理解正确性仍需评测，不能由参数合法推导语义正确。
EvidenceGuard只检查引用身份存在，不证明回答语义真实；不能把它叫作完整事实核验。
Conversations.findOwnedExchange先回放已提交消息；appendExchange必须再次检查唯一消息身份并返回原始Reply，以覆盖并发重试。
消息语义包含question和zone。会话版本不一致时非重放请求拒绝，防止并发消息覆盖。
规划/模型执行在持久写之前，两个并发请求仍可能产生重复模型费用；后续可增加持久请求占位/配额，不在此处伪称“调用恰好一次”。
有限历史窗口由真实Conversations Adapter提供，不能把整个会话无限发给模型。
确认草稿不占库存；Executor须用稳定的用户＋draft身份驱动报名幂等，并处理跨服务协调，不能“先检查后分别写两库”。

## UI与错误

未接入／加载／空结果／已返回／错误为不同分支。
前端同一次用户操作在不确定失败时保留命令键，结果以服务端为准；重新加载页面后需要先查询记录。
业务错误由ClientFailure映射；HTTP Adapter必须统一把响应转换成该类，否则按依赖异常展示。
未知错误不展示原始异常/SQL/栈信息。
界面按钮的disabled只能改善体验，不构成服务端权限或重复提交防护。

