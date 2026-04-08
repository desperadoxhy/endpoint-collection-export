# Endpoint Collection Export

> 声明：`Endpoint Collection Export` 是一个第三方 IntelliJ IDEA 插件，可生成与 Bruno 兼容的 OpenCollection YAML 文件；与 Bruno 官方团队无隶属关系，也不是 Bruno 官方发布产品。

`Endpoint Collection Export` 是一个 IntelliJ IDEA 插件，用于把 Java Spring Boot Controller 直接导出为 API collection 文件，并生成与 Bruno 兼容的 OpenCollection YAML 结构。

当前实现不依赖 Bruno CLI，导出结果直接写入 OpenCollection YAML 结构。

## 许可说明

本项目采用 `Apache License 2.0` 开源许可。

- 允许阅读、复制、修改、重新发布和商用
- 分发时需保留版权声明与许可文本
- 具体约束以仓库根目录 `LICENSE` 为准

完整许可条款见仓库根目录的 `LICENSE` 文件。

## 功能特性

- 在 Spring Boot `controller` 上提供右键菜单：`导出到 Bruno`
- 支持在方法名上右键，只导出当前单个接口
- 支持导出当前 controller 下的全部接口
- 支持导出当前 controller 继承基类中已实现的 Spring 接口
- 基于注释、请求映射、入参注解、入参类型、返回类型生成 OpenCollection 请求文件
- 自动生成或维护：
  - 项目级 `opencollection.yml`
  - controller 级 `folder.yml`
  - workspace 根目录下的 `workspace.yml`
- 若目标文件已存在，则默认复用，不覆盖已有 API 文件
- 导出完成后，在 IDEA Run 窗口输出摘要和接口明细

## 适用范围

- Java 17
- IntelliJ IDEA 2023.3 及以上
- Spring Boot 2 / Spring MVC 原生注解

当前优先支持常见 Spring MVC 场景，不额外兼容第三方扩展注解。

## 使用方式

1. 在 IDEA 中安装插件
2. 打开 `Settings` -> `Tools` -> `Endpoint Collection Export`
3. 配置接口集合基础输出目录
4. 在 Spring Boot Controller 类名上右键，选择 `导出到 Bruno`
5. 或在某个方法名上右键，只导出当前方法对应接口

如果基础输出目录未配置，导出时会先弹出对话框要求输入或选择目录。

## 导出目录结构

假设基础输出目录配置为：

```text
/path/to/collection-workspace
```

导出 `example-service` 项目中的 `OrderController` 后，结果大致如下：

```text
/path/to/collection-workspace/
├── workspace.yml
└── example-service/
    ├── opencollection.yml
    └── OrderController/
        ├── folder.yml
        ├── GET-orders-id-OrderController.getById.yml
        └── POST-orders-OrderController.create.yml
```

其中：

- `workspace.yml` 负责登记当前 workspace 下的 collection 列表
- `opencollection.yml` 只在项目目录生成一次
- `folder.yml` 的 `info.name` 使用 controller 注释摘要
- 单个接口的 `.yml` 文件默认只新增，不覆盖已有文件

## 导出规则

- 接口名称优先取方法注释第一行
- 方法注释中的 HTML 标签会被移除
- controller 注释为空时，`folder.yml` 回退为 controller 类名
- 右键类名导出时，会包含本类和继承基类中的可见 Spring 接口
- 右键方法名导出时，仅导出当前方法

## 开发与打包

常用命令：

```bash
./gradlew build
./gradlew buildPlugin
```

插件打包产物位于：

```text
build/distributions/
```

## 项目说明

- 本项目与 Bruno 官方无隶属关系
- 插件 ID：`io.github.zerojehovah.endpointcollectionexport`
- 插件名称：`Endpoint Collection Export`
- 当前版本：`1.0.1`

如果后续需要扩展支持更多 Spring 注解或更复杂的继承/接口声明场景，可以继续在解析器和导出规则上迭代。
