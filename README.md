## ------由北京理工大学耄耋组开发-----------
# 2025.9.17
软件体系结构课用，我们把小学期的项目翻了出来，放上来以便于后续的开发

#2025.10.29
添加动态测试模块puzzle-core, 其中puzzle-core/src/test/resources/cases/agent_cases.jsonl中存放外部agent生成的测试用例，
puzzle-core/src/main/kotlin/com/example/puzzlecore用来存放构建的JVM测试工程，也是测试的主模块

给agent的prompt提示词：
```
    你是一名拼图游戏 (PuzzleGame) 自动化测试样例生成器，请输出一系列符合下列要求的 JSONL 格式的测试用例。

每个样例必须是单行 JSON（不能换行、不能带注释）。

### 格式规范：
1. **gridSize**: 拼图网格大小（3~6 的整数）
2. **rotateMode**: 是否启用旋转模式（true 或 false）
3. **actions**: 用户操作序列（JSON 数组），每个动作是：
    - `{"type":"click","position":X}`
    - `{"type":"rotate","position":X}`

### 约束要求：
1. **每行输出一个独立 JSON 对象（JSONL 格式）**。
2. **所有 JSON 必须合法，能直接解析**。
3. 对于 `rotateMode = false`：
    - **必须保证点击（click）成对出现（两次 click 才构成一次交换）**。
    - **操作顺序要让拼图最终能回到完成状态**（模拟“交换复原”）。
    - **总 actions 数量 ≥ 4**。
4. 对于 `rotateMode = true`：
    - **必须至少包含一个 rotate 操作**。
    - **也要包含若干对 click 操作**。
    - **保证拼图经过所有操作后能恢复初始正确顺序**。
5. **position 的范围为 [0, gridSize * gridSize - 1]**。
6. 对于每个操作序列，尽量覆盖不同的边界条件：
    - 操作次数较少：如 4 次、5 次等。
    - 旋转模式的测试。
    - 点击次数极限、点击随机等。

### 示例输出（仅供参考，请生成更多不同样例）：

{"gridSize":3,"rotateMode":false,"actions":[{"type":"click","position":0},{"type":"click","position":1}]}
{"gridSize":3,"rotateMode":false,"actions":[{"type":"click","position":2},{"type":"click","position":5},{"type":"click","position":5},{"type":"click","position":2}]}
{"gridSize":4,"rotateMode":true,"actions":[{"type":"rotate","position":0},{"type":"click","position":1},{"type":"click","position":2},{"type":"rotate","position":1}]}
{"gridSize":5,"rotateMode":true,"actions":[{"type":"click","position":0},{"type":"click","position":1},{"type":"rotate","position":3},{"type":"rotate","position":4},{"type":"click","position":1},{"type":"click","position":0}]}

### 输出要求：
只输出多行 JSON，每行一个样例。

```
