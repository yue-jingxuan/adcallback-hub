# Huida‑Callback‑Hub
> 通用投流回调网关（广告回调中台）

[![License](https://img.shields.io/badge/license-Apache2.0-blue.svg)](LICENSE)
[![SpringBoot](https://img.shields.io/badge/SpringBoot-3.x-green.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()

## 📖 项目介绍
一款面向短剧、电商、游戏投放场景的**通用广告回调网关**。
统一接收抖音、快手、穿山甲、百度等流量平台的转化回调，提供验签校验、异步重试、日志持久化、事件转发分发能力，解决多广告平台回调接入繁琐、回调丢失、重试困难的痛点。

> 开源免费，可私有化部署，支持二次开发与商业化定制。

## ✨ V1 已实现功能
- ✅ HTTP回调统一接收入口
- ✅ 回调原始请求日志全量落库
- ✅ 失败任务定时重试
- ✅ 多平台验签配置化管理
- ✅ TraceId 全链路追踪
- ✅ 请求异步处理，防止回调超时

## 🚧 开发规划（Roadmap）
- [x] 项目基础骨架搭建
- [ ] 回调接收核心接口开发
- [ ] 抖音/快手/穿山甲验签适配器
- [ ] MQ异步回调重试队列
- [ ] Redis幂等防重复回调
- [ ] 回调事件转发下游业务系统
- [ ] 简易后台管理面板（配置、日志查询、重试操作）

## 🛠 技术栈
- 后端框架：Spring‑Boot 3.x
- JDK：Java 21
- ORM：MyBatis‑Plus
- 数据库：MySQL 8.0
- 缓存：Redis
- 消息队列：RabbitMQ
- 部署：Docker

## 📦 快速启动
> 后续V1完成后补充完整启动文档

### 环境准备
- JDK 21
- MySQL >= 8.0
- Redis
- RabbitMQ

## 🤝 适用行业场景
- 短剧投放公司
- 电商信息流广告
- App推广、游戏买量
- 多渠道投放回调统一管理

## 📄 开源协议
本项目采用 Apache‑2.0 开源协议，可免费使用、修改。

## 💬 交流反馈
欢迎提交 Issue 反馈需求与Bug。
