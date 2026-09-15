# 校园活动平台：Spring AI 技术来源核查

核查日期：2026-09-15。仅查阅官方文档；未安装依赖、启动服务、调用模型或创建代码、配置及业务数据。以下官方 reference 页面核查时显示 Spring AI 2.0.1；未来实施必须先锁定实际项目版本，不能直接混用 1.x 与 2.x 示例。

## 六项官方依据

1. **工具调用**：模型仅提出工具调用请求及参数，实际执行由应用负责。可用 `@Tool` 暴露应用方法；`ToolContext` 可传递不发送给模型的用户、租户等上下文。2.0 的调用循环进入 Advisor 链，与 1.x 架构不同。[官方 Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)

2. **结构化输出**：`StructuredOutputConverter` 可将模型文本映射为 Java 对象，但文档明确它属于尽力转换，不保证模型遵守格式，并建议配合 schema 校验。因此“返回 DTO”不等于活动时间、地点等事实正确。[官方 Output Converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html)

3. **RAG**：`QuestionAnswerAdvisor` 支持基于已有 `VectorStore` 数据进行检索增强；模块化流程可使用 `RetrievalAugmentationAdvisor`。可按请求传递元数据过滤条件。它是检索与上下文编排能力，不是活动来源自动可靠的保证。[官方 Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)

4. **对话记忆**：官方要求每位用户、每段会话使用独立 conversation ID，并从服务端用户或会话派生；列举、删除也应限定当前用户所属会话。2.0.1 文档注明记忆 Advisor 缺少 `ChatMemory.CONVERSATION_ID` 时运行报错，没有默认 ID。[官方 Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)

5. **向量数据隔离**：共享索引需在写入时给每份文档添加分组元数据，并在搜索、删除时一致地应用对应过滤条件。`VectorStore` 本身面向整个索引，不能把“用了向量库”视为已完成用户隔离。[官方 Vector Databases — Partitioning a Shared Index](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_partitioning_a_shared_index)

6. **效果验证**：官方提供 `Evaluator`、`RelevancyEvaluator` 与事实核查示例；评估输入包含真实问题、上下文及模型回复。模型评估器本身也需要可调用的模型，不能替代对校园活动事实与边界场景的人工验收。[官方 Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html)

## 对校园活动聚合与 Agent 的设计推论

以下是基于上述机制的项目设计建议，不是框架自动提供的业务保证。

- 工具先限定为活动检索、详情、日程冲突查询等只读能力；若未来加入报名、取消报名或订阅，另设授权、用户确认、幂等与审计边界。当前不执行这些写操作。依据：工具执行归应用控制（来源 1）。
- 登录用户身份不能由模型填写的 `userId` 决定；服务端将可信身份传入工具上下文，数据库查询仍检查资源归属。工具结果通常会回传模型，需在返回前做字段最小化，不能误以为 `ToolContext` 的隔离同时覆盖所有结果（来源 1）。
- 公共活动资料与个人收藏、报名、会话应分开处理；conversation ID 不是授权凭证。向量查询与删除的学校、用户、可见性范围由服务端约束，而非接受模型自由决定的过滤条件（来源 4、5）。
- 时间、名额、报名状态等实时事实优先查业务数据；活动说明等非结构化资料可考虑 RAG。提取结果应保留来源与待核验状态，不能把结构化成功当作事实核验成功（来源 1—3、6）。

## 无环境阶段的交付与验证边界

在尚未具备 JDK、构建依赖、数据库、模型端点／凭证及合适样本时，最多只能编写或说明接口契约、DTO、状态流转、权限规则、异常回退、检索过滤逻辑和测试用例设计；当前任务仅交付本报告，不编写实现。这样的成果是设计或待验证实现，不应标成“AI 功能已实现并验证”。

没有实际运行环境，不能验证编译与集成兼容性；没有真实模型及代表性活动样本，不能验证工具选择与参数准确率、结构化提取准确率、RAG 召回与事实一致性、多轮记忆效果、延迟和费用。即使以后用 mock 验证了确定性业务逻辑，也不能据此声称模型效果已经通过验收。该边界是由真实生成与评估流程所需输入推导出的工程结论（来源 1—3、6）。

后续具备环境后，应分别验证：编译／接口契约、跨用户负向权限用例、实际模型与检索质量、真实来源和过期活动处理、失败回退与写操作确认。此处仅列验收方向，不创建或运行测试。
