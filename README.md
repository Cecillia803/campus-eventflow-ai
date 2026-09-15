# Campus EventFlow · 校园活动智约

校园活动聚合、订阅提醒、有来源的智能问答与独立模拟报名。
本次交付为 **无环境阶段源代码**，不是可直接启动的应用。

仓库：[Cecillia803/campus-eventflow-ai](https://github.com/Cecillia803/campus-eventflow-ai)（私有）。
用户现已授权初始化Git并上传源码；下方及docs中的开发阶段记录保留为交付快照，运行验证状态不变。

## 从这里开始
- [交付状态](docs/DELIVERY.md)
- [架构与接入](docs/ARCHITECTURE.md)
- [接口契约](contracts/README.md)
- [下一台电脑交接](docs/HANDOFF.md)
- [验收场景](docs/ACCEPTANCE.md)
- [技术调研](docs/research/技术开发方案.md)

## 目录
- contracts：共享Java错误/身份类型，前后端契约说明。
- event-service：活动时间、修订审核、提醒、模拟报名纯规则与编排。
- agent-service：可信上下文、查询/检索、来源校验、会话归属和确认草稿规则。
- web：Vue/TypeScript页面源文件、状态处理与依赖注入接口。
- docs：调研快照、设计、边界与交接文档。

## 不包含
构建清单、依赖锁文件、环境配置、数据库脚本、Mock或种子数据、网络连接实现。
没有安装、构建、运行或联网接入业务系统。
因此不要直接执行旧项目的启动说明；本项目尚无启动命令。

源码使用Java records等语法（至少Java17语法级别）；最终JDK、Spring Boot与Spring AI版本在另一台电脑统一选择并验证。
前端源文件采用Vue 3＋TypeScript，没有安装这些依赖。
