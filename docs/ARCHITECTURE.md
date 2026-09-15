# 源码架构

## 当前实施决定
以已经讨论的Campus EventFlow名称创建独立桌面目录，不迁移旧环境骨架。
仅源码与Markdown文档。用户本次已授权D1/D2，不包含D3/D4。
采用纯业务Module与明确Interface；真实存储、身份、消息、模型等Adapter后置，不提供伪实现。

## 依赖方向
contracts ← event-service
contracts ← agent-service
web → EventFlowClient（由另一台电脑实现传输Adapter）
agent-service → ActivityTools / Retriever / Model / Conversations等接口
event-service → RegistrationStore等接口

不强迫Agent直接依赖业务后端内部类；共享字段由传输Adapter映射。
不引入Spring注解或HTTP框架，避免在版本/配置未确定时固化兼容性。

## 重要实现约定
- 所有业务时间来自调用方可信Clock；不使用机器时钟决定领域规则。
- 时间窗口为左闭右开；未知日期保持未知，日期精度需在来源模型中保留。
- 模拟报名一人一个有效记录，允许取消后重报并排至候补尾。
- 候补按服务端持久序号自动递补；满额或已有候补时不得插队。
- 使用整场次快照的不可变规则计算表达正确性基线，不是高吞吐存储布局。
- RegistrationStore必须原子完成去重、锁定快照、调用纯变更函数、持久化状态/结果/事件；没有任何内存Store。
- 当前只有领域事件意图；消息可靠投递实现后置。
- 图像生成、浏览器运行和视觉验收受用户范围限制，未执行；前端是源代码交付而非已验收视觉成品。

## 前端设计系统（源文件方案）
白色主表面、浅灰背景、深蓝文字、蓝色主操作；开放式标题区＋筛选栏＋列表，后台使用表格。
系统字体，不下载字体/图像。手机使用单列和可横向滚动导航。
常用组件：ResourcePanel、ActivityCard、各业务页面。
没有活动样例、演示账号或虚构统计。“未接入”与“空结果”不同。
客户端依赖由App的可选prop注入；缺失时显示未接入，绝不偷偷请求localhost。
没有应用挂载入口、HTML或构建配置，待另一台电脑创建。

