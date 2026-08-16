# Hypixel Tools 1.8.9 Private

这是一个面向 Minecraft 1.8.9 的私人 JVM Agent 工具项目。项目主要用于在不依赖 Forge 的情况下，通过 `-javaagent` 启动参数加载功能，并在客户端内显示起床战争相关辅助信息。

> 注意：README 不记录任何 API Key。需要更换 Key 时请直接修改源码中的配置常量，或后续改成外部配置文件。

## 当前产物

主要输出目录：

```text
outputs\
```

正常完整版：

```text
outputs\fair-av-standalone-agent-1.8.9-fireball-gen-pro-esp.jar
```

正常轻量版：

```text
outputs\fireball-invis-standalone-agent-1.8.9.jar
```

保护判断无时间限制测试版，完整版：

```text
outputs\fair-av-standalone-agent-1.8.9-fireball-gen-pro-esp-prot-time-unlocked-test-v2.jar
```

保护判断无时间限制测试版，轻量版：

```text
outputs\fireball-invis-standalone-agent-1.8.9-prot-time-unlocked-test.jar
```

数据共享服务端：

```text
outputs\fireball-share-server.jar
```

## JVM 启动参数

完整版：

```text
-javaagent:outputs\fair-av-standalone-agent-1.8.9-fireball-gen-pro-esp.jar=appendBootstrap
```

轻量版：

```text
-javaagent:outputs\fireball-invis-standalone-agent-1.8.9.jar=appendBootstrap
```

测试版，完整版：

```text
-javaagent:outputs\fair-av-standalone-agent-1.8.9-fireball-gen-pro-esp-prot-time-unlocked-test-v2.jar=appendBootstrap
```

测试版，轻量版：

```text
-javaagent:outputs\fireball-invis-standalone-agent-1.8.9-prot-time-unlocked-test.jar=appendBootstrap
```

## 功能概览

完整版包含：

- 火球预测
- 隐身提示
- 家附近敌人提示
- 资源点检测
- Hypixel BedWars 数据查询
- 玩家聊天翻译
- 对方保护等级推算
- 队友数据共享客户端逻辑

轻量版保留核心功能，主要用于火球、隐身、保护推算等更轻的测试场景。

测试版只删除保护判断的时间限制，方便在自定义测试局里直接测试保护 1/2/3/4。正常版不建议用测试版时间逻辑。

## 保护判断时间窗

正常版当前时间窗：

| 阶段 | 时间 | 说明 |
|---|---:|---|
| 开局锁 0 | 床记录后 1:30 内 | 强制认为保护为 0 |
| 允许判断保护 2 | 床记录后 2:30 起 | 保护 2 参与样本判断 |
| 允许判断保护 3 | 床记录后 6:30 起 | 保护 3 优先参与样本判断 |

测试版中这三个时间限制都为 0，但仍然需要先检测到床，才会进入局内保护判断。

## 保护样本数据

截至当前整理，保护推算累计参考样本为：

```text
2322 条
```

其中：

- 历史旧日志：1986 条
- 当前新测试日志：336 条
- 无锋利样本：1687 条
- 锋利 1 样本：629 条
- 其他或未识别样本：6 条

当前锋利 1、非暴击规则范围：

| 套装 | 剑 | 保护 0 | 保护 1 | 保护 2 | 保护 3 |
|---|---:|---:|---:|---:|---:|
| 皮革套 | 木剑 | >=4.20 | 2.60-4.15 | 2.50-3.90 | 0.85-3.45 |
| 皮革套 | 石剑 | >=4.90 | 3.40-4.85 | 3.45-4.90 | 2.65-4.05 |
| 皮革套 | 铁剑 | >=5.60 | 4.80-5.55 | 3.95-5.60 | 3.25-4.55 |
| 皮革套 | 钻剑 | >=5.40 | 5.40-6.15 | 4.45-5.70 | 3.45-4.55 |
| 铁套 | 木剑 | >=2.40 | 2.50-3.25 | 1.65-3.05 | 0.75-2.75 |
| 铁套 | 石剑 | >=3.80 | 2.20-3.75 | 2.00-3.50 | 1.05-2.95 |
| 铁套 | 铁剑 | >=4.30 | 3.70-4.35 | 2.00-4.05 | 2.35-3.35 |
| 铁套 | 钻剑 | >=5.00 | 3.60-4.85 | 2.45-4.50 | 2.65-3.75 |
| 钻套 | 木剑 | >=2.80 | 2.30-2.85 | 1.10-2.60 | 0.95-2.35 |
| 钻套 | 石剑 | >=3.30 | 2.00-3.25 | 2.30-3.05 | 1.35-2.65 |
| 钻套 | 铁剑 | >=3.00 | 2.30-3.65 | 2.60-3.45 | 1.05-3.05 |
| 钻套 | 钻剑 | >=4.20 | 2.50-4.15 | 2.55-3.85 | 0.25-3.45 |

规则顺序会影响结果。当前逻辑是保护 3 优先于保护 2，保护 2 优先于保护 1，保护 0 使用最低伤害阈值。

## 数据共享服务端

服务端用于队友之间共享保护推算样本，也可以代理查询 Hypixel Stats 并做短期缓存。

构建：

```powershell
.\build-share-server.ps1
```

启动：

```powershell
java -jar .\outputs\fireball-share-server.jar
```

服务端会按局记录日志，并提供内网页面查看每局数据。客户端通过 TCP 连接服务端，不需要 HTTP 域名。

## 构建命令

构建完整版：

```powershell
.\build-agent.ps1
```

构建轻量版：

```powershell
.\build-fireball-invis-agent.ps1
```

构建翻译单独版：

```powershell
.\build-auto-translation-agent.ps1
```

构建共享服务端：

```powershell
.\build-share-server.ps1
```

如果输出 jar 正在被游戏占用，Windows 会提示无法覆盖。此时关闭客户端后重试，或用 `-OutputJarPath` 输出到一个新的 jar 文件名。

## 目录结构

```text
src\agent\java\              完整版 JVM Agent 源码
src\fireball_invis\java\     轻量版 JVM Agent 源码
src\translation_agent\java\  单独翻译 Agent 源码
src\share_server\java\       数据共享服务端源码
outputs\                     构建产物、日志、备份
backups\                     项目备份
build\                       临时编译目录
```

## 日志

客户端主要日志：

```text
outputs\fireballpredictor-agent-load.log
```

这个日志会记录：

- Agent 是否加载成功
- Minecraft 类 patch 情况
- Stats 查询状态
- 保护伤害样本
- 保护推算结果
- 数据共享上传和拉取状态

保护测试时优先看 `damage probe result` 和 `protection inferred` 两类日志。

## 使用备注

- 目标版本是 Minecraft 1.8.9。
- 主要加载方式是 `-javaagent`，不是 Forge Mod 加载。
- Lunar、PCL、原版启动器等客户端对 JVM 参数支持方式不同，启动前要确认参数确实生效。
- 测试版仅用于采样和调规则，正常对局建议使用正常版。
- API Key 不建议提交到公开仓库。
