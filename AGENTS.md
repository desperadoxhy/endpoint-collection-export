# AGENTS.md

## 项目简介

本项目是一个 IntelliJ IDEA 插件，当前目标是为 Java Spring Boot 项目提供“导出到 Bruno”的能力。

当前已确认的核心功能如下：

1. 在 Spring Boot 项目的 `controller` 中，支持右键菜单操作：`导出到 Bruno`。
2. 导出时，基于当前 `controller` 的注释、注解、入参类型、入参注解、出参类型，生成临时 OpenAPI 文档。
3. 通过本机 Bruno CLI 将生成的临时 OpenAPI 文档导入 Bruno。

后续当项目需求新增、变更或收敛时，必须同步更新本文件中的“项目简介”，确保 AGENTS.md 始终反映最新项目目标与范围。

## 开发约束

1. AGENTS.md 必须包含项目简要介绍；后续项目需求有新增、变更时，必须更新 AGENTS.md 的项目介绍。
2. AGENTS.md 必须包含项目开发约束；后续用户通过提示新增的约束，必须追加或更新到 AGENTS.md。
3. 当 AGENTS.md 更新时，必须自动执行一次 Git 提交，且该次提交内容仅包含 AGENTS.md 的变更。
4. 每完成一个功能开发或一次功能迭代，必须自动执行一次 Git 提交。
5. 只执行 Git 提交，不执行 Git 推送；提交完成后，必须输出可直接复制执行的 Git 推送命令。
6. Git 提交信息必须遵循 Angular 提交信息规范。

## Git 提交规则

1. AGENTS.md 变更提交应仅暂存并提交 `AGENTS.md`，不得混入其他文件。
2. 功能开发或迭代完成后的提交，应只包含本次功能相关变更，不得无故夹带无关修改。
3. Angular 提交信息格式遵循：`type(scope): subject`。
4. 推荐提交类型示例：
   - `docs(agents): ...` 用于 AGENTS.md 更新。
   - `feat(plugin): ...` 用于新增功能。
   - `fix(export): ...` 用于问题修复。
   - `refactor(parser): ...` 用于重构。
   - `test(plugin): ...` 用于测试补充。

## 执行约定

1. 每次完成提交后，输出对应的 Git 推送命令，但不实际执行推送。
2. 若当前任务只涉及 AGENTS.md，则本次提交只提交 AGENTS.md。
3. 若仓库中存在与当前任务无关的未提交变更，提交时应避免将其纳入本次提交。
