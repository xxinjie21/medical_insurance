# 医保核销系统 API 接口测试案例

## 文档说明

本文档包含医保核销系统所有接口的完整测试案例，覆盖正常场景、异常场景、边界场景、权限验证和性能测试。

**系统信息：**
- 基础 URL：`http://localhost:8080`
- 内容类型：`application/json`
- 认证方式：JWT Token（部分接口需要）
- 测试环境：开发环境
- JDK 版本：17
- 数据库：MySQL 8.0+
- 缓存：Redis 7.0+

**测试数据准备：**
- 测试前需执行 `docs/int.sql` 初始化数据库
- 建议为不同角色创建测试账号
- 性能测试前请备份生产数据

**测试工具推荐：**
- Postman / Apifox（接口功能测试）
- JMeter（性能测试）
- curl（命令行快速测试）

---

## 一、用户管理接口 (UserController)

### 1.1 用户注册

**接口信息：**
- 接口名称：用户注册
- 请求路径：`/user/sign`
- 请求方法：POST
- 接口描述：支持患者、医院、医保局三种角色注册

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| password | String | 是 | 登录密码 |
| name | String | 是 | 姓名/医院名称/机构名称 |
| idCard | String | 否 | 身份证号（仅患者需要） |
| hospitalId | Long | 否 | 所属医院 ID（仅医院需要） |
| role | Integer | 是 | 角色：1-患者 2-医院 3-医保局 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-USER-001 | 患者注册 - 成功 | `{"password":"123456","name":"张三","idCard":"110101199001011234","role":1}` | 返回成功，用户 ID，密码 MD5 加密存储 | - | ⏳ |
| TC-USER-002 | 医院注册 - 成功 | `{"password":"hospital123","name":"北京市第一医院","role":2}` | 返回成功，医院 ID，自动生成医院编号 | - | ⏳ |
| TC-USER-003 | 医保局注册 - 成功 | `{"password":"bureau123","name":"北京市医保局","role":3}` | 返回成功，机构 ID | - | ⏳ |
| TC-USER-004 | 患者注册 - 身份证号重复 | `{"password":"123456","name":"李四","idCard":"110101199001011234","role":1}` | 返回失败，提示身份证已存在 | - | ⏳ |
| TC-USER-005 | 患者注册 - 缺少必填字段 | `{"password":"123456","role":1}` | 返回失败，提示姓名不能为空 | - | ⏳ |
| TC-USER-006 | 注册 - 密码为空 | `{"name":"王五","idCard":"110101199001015678","role":1}` | 返回失败，提示密码不能为空 | - | ⏳ |
| TC-USER-007 | 医院注册 - 缺少 hospitalId | `{"password":"123456","name":"医院","role":2}` | 返回成功（医院不需要 hospitalId） | - | ⏳ |
| TC-USER-008 | 患者注册 - 身份证号格式错误 | `{"password":"123456","name":"张三","idCard":"12345","role":1}` | 返回失败，提示身份证号格式不正确 | - | ⏳ |
| TC-USER-009 | 患者注册 - 角色无效 | `{"password":"123456","name":"张三","idCard":"110101199001011234","role":5}` | 返回失败，提示角色无效 | - | ⏳ |
| TC-USER-010 | 患者注册 - 姓名超长 | `{"password":"123456","name":"这是一段非常长的超过 50 个字符的姓名测试数据这是一段非常长的超过 50 个字符的姓名测试数据","idCard":"110101199001015678","role":1}` | 返回失败，提示姓名长度超限 | - | ⏳ |

**curl 示例：**
```bash
curl -X POST http://localhost:8080/user/sign \
  -H "Content-Type: application/json" \
  -d '{"password":"123456","name":"张三","idCard":"110101199001011234","role":1}'
```

---

### 1.2 用户登录

**接口信息：**
- 接口名称：用户登录
- 请求路径：`/user/login`
- 请求方法：POST
- 接口描述：用户登录获取访问令牌

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户 ID |
| password | String | 是 | 登录密码 |
| role | Integer | 是 | 角色：1-患者 2-医院 3-医保局 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-USER-011 | 患者登录 - 成功 | `{"userId":1,"password":"123456","role":1}` | 返回成功，包含 token、用户信息，token 有效期 30 天 | - | ⏳ |
| TC-USER-012 | 医院登录 - 成功 | `{"userId":2,"password":"hospital123","role":2}` | 返回成功，包含 token、医院信息 | - | ⏳ |
| TC-USER-013 | 医保局登录 - 成功 | `{"userId":3,"password":"bureau123","role":3}` | 返回成功，包含 token、机构信息 | - | ⏳ |
| TC-USER-014 | 登录 - 密码错误 | `{"userId":1,"password":"wrongpassword","role":1}` | 返回失败，提示密码错误 | - | ⏳ |
| TC-USER-015 | 登录 - 用户不存在 | `{"userId":999,"password":"123456","role":1}` | 返回失败，提示用户不存在 | - | ⏳ |
| TC-USER-016 | 登录 - 角色不匹配 | `{"userId":1,"password":"123456","role":2}` | 返回失败，提示角色不匹配 | - | ⏳ |
| TC-USER-017 | 登录 - 参数缺失 | `{"userId":1,"role":1}` | 返回失败，提示密码不能为空 | - | ⏳ |
| TC-USER-018 | 登录 - 密码 MD5 加密前 | `{"userId":1,"password":"123456","role":1}` | 返回成功（后端自动 MD5 加密比对） | - | ⏳ |
| TC-USER-019 | 登录 - 账号被禁用 | `{"userId":1,"password":"123456","role":1}` (已禁用账号) | 返回失败，提示账号异常 | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 1,
    "name": "张三",
    "idCard": "110101199001011234",
    "role": 1,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"password":"123456","role":1}'
```

---

### 1.3 用户登出

**接口信息：**
- 接口名称：用户登出
- 请求路径：`/user/loginout`
- 请求方法：POST
- 接口描述：用户登出，清除 token

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| token | String | 是 | 访问令牌 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-USER-021 | 登出 - 成功 | `token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` | 返回成功，提示已登出，Redis 中 token 被删除 | - | ⏳ |
| TC-USER-022 | 登出 - token 无效 | `token=invalid_token` | 返回成功（幂等性） | - | ⏳ |
| TC-USER-023 | 登出 - token 为空 | `token=` | 返回失败，提示 token 不能为空 | - | ⏳ |
| TC-USER-024 | 登出 - token 已过期 | `token=过期的 token` | 返回成功（幂等性） | - | ⏳ |
| TC-USER-025 | 登出 - 重复登出 | 同一 token 调用两次 | 第二次返回成功（幂等性） | - | ⏳ |

**curl 示例：**
```bash
curl -X POST "http://localhost:8080/user/loginout?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 二、医院管理接口 (HospitalController)

### 2.1 医院注册

**接口信息：**
- 接口名称：医院注册
- 请求路径：`/hospital/sign`
- 请求方法：POST
- 接口描述：医院机构注册账号

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 医院名称 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-HOSP-001 | 医院注册 - 成功 | `{"name":"上海市第二医院"}` | 返回成功，医院 ID | - | ⏳ |
| TC-HOSP-002 | 医院注册 - 名称重复 | `{"name":"北京市第一医院"}` | 返回失败，提示医院名称已存在 | - | ⏳ |
| TC-HOSP-003 | 医院注册 - 名称为空 | `{"name":""}` | 返回失败，提示医院名称不能为空 | - | ⏳ |

**curl 示例：**
```bash
curl -X POST http://localhost:8080/hospital/sign \
  -H "Content-Type: application/json" \
  -d '{"name":"上海市第二医院"}'
```

---

### 2.2 获取医院列表

**接口信息：**
- 接口名称：获取医院列表
- 请求路径：`/hospital/list`
- 请求方法：GET
- 接口描述：分页获取所有医院列表（患者/医保局可用）

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-HOSP-011 | 获取医院列表 - 第一页 | `GET /hospital/list?pageNum=1&pageSize=10` | 返回医院列表分页数据 | - | ⏳ |
| TC-HOSP-012 | 获取医院列表 - 第二页 | `GET /hospital/list?pageNum=2&pageSize=10` | 返回第二页数据 | - | ⏳ |
| TC-HOSP-013 | 获取医院列表 - 无数据 | `GET /hospital/list?pageNum=999&pageSize=10` | 返回空列表 | - | ⏳ |
| TC-HOSP-014 | 获取医院列表 - 默认分页 | `GET /hospital/list` | 返回第一页，每页 10 条 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/hospital/list?pageNum=1&pageSize=10"
```

---

### 2.3 查询本院患者列表

**接口信息：**
- 接口名称：查询本院患者列表
- 请求路径：`/hospital/patient/list`
- 请求方法：GET
- 接口描述：医院查询本院就诊的患者列表

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hospitalId | Long | 是 | 医院 ID（从请求头获取） |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-HOSP-021 | 查询本院患者 - 成功 | `GET /hospital/patient/list?pageNum=1&pageSize=10` (header: hospitalId=2) | 返回本院患者列表 | - | ⏳ |
| TC-HOSP-022 | 查询本院患者 - 无患者 | `GET /hospital/patient/list?pageNum=1&pageSize=10` (header: hospitalId=999) | 返回空列表 | - | ⏳ |
| TC-HOSP-023 | 查询本院患者 - 缺少医院 ID | `GET /hospital/patient/list` | 返回失败，提示缺少 hospitalId | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/hospital/patient/list?pageNum=1&pageSize=10" \
  -H "hospitalId: 2"
```

---

## 三、就诊管理接口 (VisitController)

### 3.1 新增就诊记录

**接口信息：**
- 接口名称：新增就诊记录
- 请求路径：`/visit/add`
- 请求方法：POST
- 接口描述：医院为患者新增就诊记录
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 患者 ID |
| hospitalId | Long | 是 | 医院 ID |
| type | Integer | 是 | 就诊类型：1-门诊 2-住院 |
| diagnosis | String | 是 | 诊断结果 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-VISIT-001 | 新增就诊 - 成功 | `{"userId":1,"hospitalId":2,"type":1,"diagnosis":"上呼吸道感染"}` | 返回成功，就诊 ID，包含医院名称、患者姓名 | - | ⏳ |
| TC-VISIT-002 | 新增就诊 - 患者不存在 | `{"userId":999,"hospitalId":2,"type":1,"diagnosis":"感冒"}` | 返回失败，提示患者不存在 | - | ⏳ |
| TC-VISIT-003 | 新增就诊 - 医院不存在 | `{"userId":1,"hospitalId":999,"type":1,"diagnosis":"感冒"}` | 返回失败，提示医院不存在 | - | ⏳ |
| TC-VISIT-004 | 新增就诊 - 缺少必填字段 | `{"userId":1,"hospitalId":2}` | 返回失败，提示诊断结果不能为空 | - | ⏳ |
| TC-VISIT-005 | 新增就诊 - 就诊类型无效 | `{"userId":1,"hospitalId":2,"type":3,"diagnosis":"感冒"}` | 返回失败，提示就诊类型无效 | - | ⏳ |
| TC-VISIT-006 | 新增就诊 - 诊断结果为空 | `{"userId":1,"hospitalId":2,"type":1,"diagnosis":""}` | 返回失败，提示诊断结果不能为空 | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "hospitalId": 2,
    "type": 1,
    "diagnosis": "上呼吸道感染",
    "status": 0,
    "hospitalName": "北京市第一医院",
    "userName": "张三"
  }
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8080/visit/add \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"hospitalId":2,"type":1,"diagnosis":"上呼吸道感染"}'
```

---

### 3.2 查询我的就诊记录

**接口信息：**
- 接口名称：查询我的就诊记录
- 请求路径：`/visit/my/list`
- 请求方法：GET
- 接口描述：患者查询自己的就诊记录（分页）
- 权限：PATIENT

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-VISIT-011 | 查询我的就诊 - 成功 | `GET /visit/my/list?pageNum=1&pageSize=10` | 返回患者就诊列表（MyBatis-Plus Page 对象） | - | ⏳ |
| TC-VISIT-012 | 查询我的就诊 - 无记录 | `GET /visit/my/list?pageNum=1&pageSize=10` (新用户) | 返回空列表 | - | ⏳ |
| TC-VISIT-013 | 查询我的就诊 - 默认分页 | `GET /visit/my/list` | 返回第一页，每页 10 条 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/visit/my/list?pageNum=1&pageSize=10"
```

---

### 3.3 医院查询就诊记录

**接口信息：**
- 接口名称：医院查询就诊记录
- 请求路径：`/visit/hospital/list`
- 请求方法：GET
- 接口描述：医院查询本院的就诊记录
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hospitalId | Long | 是 | 医院 ID |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-VISIT-021 | 医院查询就诊 - 成功 | `GET /visit/hospital/list?hospitalId=2&pageNum=1&pageSize=10` | 返回本院就诊记录列表 | - | ⏳ |
| TC-VISIT-022 | 医院查询就诊 - 无记录 | `GET /visit/hospital/list?hospitalId=999` | 返回空列表 | - | ⏳ |
| TC-VISIT-023 | 医院查询就诊 - 缺少医院 ID | `GET /visit/hospital/list` | 返回失败，提示医院 ID 不能为空 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/visit/hospital/list?hospitalId=2&pageNum=1&pageSize=10"
```

---

### 3.4 查询就诊记录详情

**接口信息：**
- 接口名称：查询就诊记录详情
- 请求路径：`/visit/{visitId}`
- 请求方法：GET
- 接口描述：根据 ID 查询就诊记录（带缓存）
- 权限：无

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| visitId | Long | 是 | 就诊 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-VISIT-031 | 查询就诊详情 - 成功 | `GET /visit/1` | 返回就诊记录详情 | - | ⏳ |
| TC-VISIT-032 | 查询就诊详情 - 缓存命中 | `GET /visit/1` (第二次访问) | 返回就诊记录，从 Redis 缓存获取 | - | ⏳ |
| TC-VISIT-033 | 查询就诊详情 - 记录不存在 | `GET /visit/999` | 返回失败，提示就诊记录不存在 | - | ⏳ |
| TC-VISIT-034 | 查询就诊详情 - 缺少 visitId | `GET /visit/` | 返回 404 错误 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET http://localhost:8080/visit/1
```

---

## 四、费用管理接口 (FeeController)

### 4.1 批量添加费用明细

**接口信息：**
- 接口名称：批量添加费用明细
- 请求路径：`/fee/batch/add`
- 请求方法：POST
- 接口描述：医院为就诊记录添加费用明细
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| visitId | Long | 是 | 就诊 ID |
| name | String | 是 | 费用项目名称 |
| price | BigDecimal | 是 | 单价 |
| num | Integer | 是 | 数量 |
| type | Integer | 是 | 费用类型：1-甲类 2-乙类 3-自费 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-FEE-001 | 批量添加费用 - 成功 | `[{"visitId":1,"name":"阿莫西林胶囊","type":1,"price":25.5,"num":2},{"visitId":1,"name":"血常规检查","type":2,"price":50,"num":1}]` | 返回成功，费用 ID 列表，包含总价，自动计算 total=price*num | - | ⏳ |
| TC-FEE-002 | 批量添加费用 - 就诊不存在 | `[{"visitId":999,"name":"阿莫西林","type":1,"price":25.5,"num":2}]` | 返回失败，提示就诊不存在 | - | ⏳ |
| TC-FEE-003 | 批量添加费用 - 价格为负 | `[{"visitId":1,"name":"测试","type":1,"price":-10,"num":1}]` | 返回失败，提示单价必须大于 0 | - | ⏳ |
| TC-FEE-004 | 批量添加费用 - 数量为零 | `[{"visitId":1,"name":"测试","type":1,"price":10,"num":0}]` | 返回失败，提示数量必须大于 0 | - | ⏳ |
| TC-FEE-005 | 批量添加费用 - 类型无效 | `[{"visitId":1,"name":"测试","type":4,"price":10,"num":1}]` | 返回失败，提示费用类型无效 | - | ⏳ |
| TC-FEE-006 | 批量添加费用 - 名称为空 | `[{"visitId":1,"name":"","type":1,"price":10,"num":1}]` | 返回失败，提示项目名称不能为空 | - | ⏳ |
| TC-FEE-007 | 批量添加费用 - 价格为 0 | `[{"visitId":1,"name":"免费项目","type":1,"price":0,"num":1}]` | 返回失败，提示单价必须大于 0 | - | ⏳ |
| TC-FEE-008 | 批量添加费用 - 数量过大 | `[{"visitId":1,"name":"测试","type":1,"price":10,"num":999999}]` | 返回成功（边界值测试） | - | ⏳ |
| TC-FEE-009 | 批量添加费用 - 价格精度 | `[{"visitId":1,"name":"测试","type":1,"price":10.123,"num":1}]` | 返回成功，保留 2 位小数 | - | ⏳ |
| TC-FEE-010 | 批量添加费用 - 名称超长 | `[{"visitId":1,"name":"这是一段非常长的超过 100 个字符的费用项目名称测试数据这是一段非常长的超过 100 个字符的费用项目名称测试数据","type":1,"price":10,"num":1}]` | 返回失败，提示名称长度超限 | - | ⏳ |
| TC-FEE-011 | 批量添加费用 - 空列表 | `[]` | 返回失败，提示费用明细不能为空 | - | ⏳ |
| TC-FEE-012 | 批量添加费用 - 并发添加 | 同一 visitId 同时提交 10 个请求 | 只有一个成功，其他返回"正在处理费用"（分布式锁） | - | ⏳ |
| TC-FEE-013 | 批量添加费用 - 已结算就诊 | `[{"visitId":1,"name":"测试","type":1,"price":10,"num":1}]` (已结算的就诊) | 返回失败，提示已结算不能添加费用 | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "visitId": 1,
      "name": "阿莫西林胶囊",
      "type": 1,
      "price": 25.50,
      "num": 2,
      "total": 51.00
    },
    {
      "id": 2,
      "visitId": 1,
      "name": "血常规检查",
      "type": 2,
      "price": 50.00,
      "num": 1,
      "total": 50.00
    }
  ]
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8080/fee/batch/add \
  -H "Content-Type: application/json" \
  -d '[{"visitId":1,"name":"阿莫西林胶囊","type":1,"price":25.5,"num":2},{"visitId":1,"name":"血常规检查","type":2,"price":50,"num":1}]'
```

---

### 4.2 查询费用明细

**接口信息：**
- 接口名称：查询费用明细
- 请求路径：`/fee/listByVisitId`
- 请求方法：GET
- 接口描述：根据就诊 ID 查询费用明细列表
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| visitId | Long | 是 | 就诊 ID |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-FEE-011 | 查询费用 - 成功 | `GET /fee/listByVisitId?visitId=1` | 返回费用明细列表 | - | ⏳ |
| TC-FEE-012 | 查询费用 - 无费用 | `GET /fee/listByVisitId?visitId=999` | 返回空列表 | - | ⏳ |
| TC-FEE-013 | 查询费用 - 缺少 visitId | `GET /fee/listByVisitId` | 返回失败，提示 visitId 不能为空 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/fee/listByVisitId?visitId=1"
```

---

## 五、结算管理接口 (SettleController)

### 5.1 就诊结算

**接口信息：**
- 接口名称：就诊结算
- 请求路径：`/settle/calculate/{visitId}`
- 请求方法：POST
- 接口描述：对就诊记录进行医保结算计算
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| visitId | Long | 是 | 就诊 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-SETTLE-001 | 结算 - 成功 | `POST /settle/calculate/1` | 返回结算详情，包含总费用、报销金额、自付金额，自动计算：甲类 100% 报销、乙类 80% 报销、自费 0% 报销 | - | ⏳ |
| TC-SETTLE-002 | 结算 - 就诊不存在 | `POST /settle/calculate/999` | 返回失败，提示就诊不存在 | - | ⏳ |
| TC-SETTLE-003 | 结算 - 无费用 | `POST /settle/calculate/2` (无费用的就诊) | 返回失败，提示没有费用明细 | - | ⏳ |
| TC-SETTLE-004 | 结算 - 重复结算 | `POST /settle/calculate/1` (已结算的就诊) | 返回失败，提示已结算 | - | ⏳ |
| TC-SETTLE-005 | 结算 - 并发结算 | 同一就诊同时提交 10 个结算请求 | 只有一个成功，其他返回"正在结算中"（分布式锁） | - | ⏳ |
| TC-SETTLE-006 | 结算 - 费用为 0 | `POST /settle/calculate/3` (总费用为 0 的就诊) | 返回失败，提示总费用必须大于 0 | - | ⏳ |
| TC-SETTLE-007 | 结算 - 金额精度验证 | `POST /settle/calculate/1` (包含小数金额) | 返回成功，金额保留 2 位小数，四舍五入 | - | ⏳ |
| TC-SETTLE-008 | 结算 - 报销比例验证 | `POST /settle/calculate/1` (甲类 100 元，乙类 100 元，自费 100 元) | 返回成功，reimburseAmount=180 元（100+80+0） | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "visitId": 1,
    "totalAmount": 101.00,
    "reimburseAmount": 91.00,
    "selfPayAmount": 10.00,
    "status": 1
  }
}
```

**金额计算说明：**
- 甲类费用（type=1）：100% 报销
- 乙类费用（type=2）：80% 报销
- 自费费用（type=3）：0% 报销

**curl 示例：**
```bash
curl -X POST http://localhost:8080/settle/calculate/1
```

---

### 5.2 查询结算详情

**接口信息：**
- 接口名称：查询结算详情
- 请求路径：`/settle/detail/{visitId}`
- 请求方法：GET
- 接口描述：查询就诊的结算详情
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| visitId | Long | 是 | 就诊 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-SETTLE-011 | 查询结算 - 成功 | `GET /settle/detail/1` | 返回结算详情 | - | ⏳ |
| TC-SETTLE-012 | 查询结算 - 未结算 | `GET /settle/detail/2` (未结算的就诊) | 返回失败，提示未结算 | - | ⏳ |
| TC-SETTLE-013 | 查询结算 - 就诊不存在 | `GET /settle/detail/999` | 返回失败，提示就诊不存在 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET http://localhost:8080/settle/detail/1
```

---

## 六、申报批次管理接口 (BatchController)

### 6.1 创建申报批次

**接口信息：**
- 接口名称：创建申报批次
- 请求路径：`/batch/create/{hospitalId}`
- 请求方法：POST
- 接口描述：医院创建医保申报批次
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hospitalId | Long | 是 | 医院 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-BATCH-001 | 创建批次 - 成功 | `POST /batch/create/2` | 返回成功，批次 ID 和批次号，批次号格式：yyyyMMddHHmmss+hospitalId(6 位)+随机数 (6 位) | - | ⏳ |
| TC-BATCH-002 | 创建批次 - 医院不存在 | `POST /batch/create/999` | 返回失败，提示医院不存在 | - | ⏳ |
| TC-BATCH-003 | 创建批次 - 缺少医院 ID | `POST /batch/create/` | 返回 404 错误 | - | ⏳ |
| TC-BATCH-004 | 创建批次 - 并发创建 | 同一医院同时创建 10 个批次 | 全部成功，批次号不重复 | - | ⏳ |
| TC-BATCH-005 | 创建批次 - 批次号唯一性 | 快速创建两个批次 | 批次号不同（时间戳 + 随机数保证唯一） | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "hospitalId": 2,
    "batchNo": "202604271530450001123456",
    "settleCnt": 0,
    "totalAmt": 0.00,
    "status": 0
  }
}
```

**批次号生成规则：** `yyyyMMddHHmmss + hospitalId(6 位) + 随机数 (6 位)`

**curl 示例：**
```bash
curl -X POST http://localhost:8080/batch/create/2
```

---

### 6.2 添加结算单到批次

**接口信息：**
- 接口名称：添加结算单到批次
- 请求路径：`/batch/add-settle`
- 请求方法：POST
- 接口描述：将结算单添加到申报批次
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| batchId | Long | 是 | 批次 ID |
| settleId | Long | 是 | 结算单 ID |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-BATCH-011 | 添加结算单 - 成功 | `POST /batch/add-settle?batchId=1&settleId=1` | 返回成功，提示添加成功，更新批次总金额和总笔数 | - | ⏳ |
| TC-BATCH-012 | 添加结算单 - 批次不存在 | `POST /batch/add-settle?batchId=999&settleId=1` | 返回失败，提示批次不存在 | - | ⏳ |
| TC-BATCH-013 | 添加结算单 - 结算单不存在 | `POST /batch/add-settle?batchId=1&settleId=999` | 返回失败，提示结算单不存在 | - | ⏳ |
| TC-BATCH-014 | 添加结算单 - 已添加 | `POST /batch/add-settle?batchId=1&settleId=1` (重复添加) | 返回失败，提示已添加 | - | ⏳ |
| TC-BATCH-015 | 添加结算单 - 批次已完成 | `POST /batch/add-settle?batchId=1&settleId=2` (已完成批次) | 返回失败，提示批次已完成不能修改 | - | ⏳ |
| TC-BATCH-016 | 添加结算单 - 结算单已添加到其他批次 | `POST /batch/add-settle?batchId=2&settleId=1` (已添加的结算单) | 返回失败，提示结算单已添加到其他批次 | - | ⏳ |
| TC-BATCH-017 | 添加结算单 - 并发添加 | 同一结算单同时添加到不同批次 | 只有一个成功，其他返回失败（分布式锁） | - | ⏳ |
| TC-BATCH-018 | 添加结算单 - 批次金额更新 | 添加 100 元结算单到批次 | 批次总金额增加 100 元，settleCnt+1 | - | ⏳ |

**curl 示例：**
```bash
curl -X POST "http://localhost:8080/batch/add-settle?batchId=1&settleId=1"
```

---

### 6.3 查询批次详情

**接口信息：**
- 接口名称：查询批次详情
- 请求路径：`/batch/detail/{batchId}`
- 请求方法：GET
- 接口描述：查询申报批次详情及结算单列表
- 权限：HOSPITAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| batchId | Long | 是 | 批次 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-BATCH-021 | 查询批次 - 成功 | `GET /batch/detail/1` | 返回批次详情和结算单列表 | - | ⏳ |
| TC-BATCH-022 | 查询批次 - 批次不存在 | `GET /batch/detail/999` | 返回失败，提示批次不存在 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET http://localhost:8080/batch/detail/1
```

---

## 七、基金拨付接口 (PayController)

### 7.1 拨付批次款项

**接口信息：**
- 接口名称：拨付批次款项
- 请求路径：`/pay/pay-batch/{batchId}`
- 请求方法：POST
- 接口描述：医保局对申报批次进行基金拨付
- 权限：MEDICAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| batchId | Long | 是 | 批次 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-PAY-001 | 拨付 - 成功 | `POST /pay/pay-batch/1` | 返回成功，拨付记录 ID 和拨付信息，批次状态更新为已完成，生成拨付记录 | - | ⏳ |
| TC-PAY-002 | 拨付 - 批次不存在 | `POST /pay/pay-batch/999` | 返回失败，提示批次不存在 | - | ⏳ |
| TC-PAY-003 | 拨付 - 批次未申报 | `POST /pay/pay-batch/2` (状态为待申报的批次) | 返回失败，提示批次未申报 | - | ⏳ |
| TC-PAY-004 | 拨付 - 重复拨付 | `POST /pay/pay-batch/1` (已拨付的批次) | 返回失败，提示已拨付 | - | ⏳ |
| TC-PAY-005 | 拨付 - 并发拨付 | 同一批次同时提交 10 个拨付请求 | 只有一个成功，其他返回"正在处理"（分布式锁） | - | ⏳ |
| TC-PAY-006 | 拨付 - 金额验证 | `POST /pay/pay-batch/1` (批次总金额 10000 元) | 返回成功，拨付金额=10000 元 | - | ⏳ |
| TC-PAY-007 | 拨付 - 权限验证 | `POST /pay/pay-batch/1` (使用医院 token) | 返回失败，提示无权限（仅医保局可拨付） | - | ⏳ |
| TC-PAY-008 | 拨付 - 拨付时间记录 | `POST /pay/pay-batch/1` | 返回成功，payTime 为当前时间 | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "batchId": 1,
    "hospitalId": 2,
    "amount": 15000.00,
    "status": 1,
    "payTime": "2026-04-27T15:30:45"
  }
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8080/pay/pay-batch/1
```

---

### 7.2 查询拨付信息

**接口信息：**
- 接口名称：查询拨付信息
- 请求路径：`/pay/by-batch/{batchId}`
- 请求方法：GET
- 接口描述：根据批次 ID 查询拨付信息
- 权限：MEDICAL

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| batchId | Long | 是 | 批次 ID（路径参数） |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-PAY-011 | 查询拨付 - 成功 | `GET /pay/by-batch/1` | 返回拨付信息 | - | ⏳ |
| TC-PAY-012 | 查询拨付 - 未拨付 | `GET /pay/by-batch/2` (未拨付批次) | 返回失败，提示未拨付 | - | ⏳ |
| TC-PAY-013 | 查询拨付 - 批次不存在 | `GET /pay/by-batch/999` | 返回失败，提示批次不存在 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET http://localhost:8080/pay/by-batch/1
```

---

## 八、患者账户管理接口 (UserAccountController)

### 8.1 获取账户信息

**接口信息：**
- 接口名称：获取账户信息
- 请求路径：`/account/get`
- 请求方法：GET
- 接口描述：获取患者账户信息（余额、充值总额、消费总额）
- 权限：PATIENT

**请求参数：** 无（从 Token 获取当前用户 ID）

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-ACCOUNT-001 | 获取账户 - 成功 | `GET /account/get` | 返回账户信息，包含余额 | - | ⏳ |
| TC-ACCOUNT-002 | 获取账户 - 未登录 | `GET /account/get` (无 token) | 返回失败，提示登录已过期 | - | ⏳ |

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 1,
    "balance": 100.00,
    "totalRecharge": 500.00,
    "totalConsumption": 400.00
  }
}
```

**curl 示例：**
```bash
curl -X GET http://localhost:8080/account/get \
  -H "token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 8.2 账户充值

**接口信息：**
- 接口名称：账户充值
- 请求路径：`/account/recharge`
- 请求方法：POST
- 接口描述：患者账户充值
- 权限：PATIENT

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| amount | BigDecimal | 是 | 充值金额 |
| type | Integer | 是 | 充值类型：1-微信 2-支付宝 3-银行卡 4-现金 |
| remark | String | 否 | 备注 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-ACCOUNT-011 | 充值 - 成功 | `{"amount":100,"type":1,"remark":"微信充值"}` | 返回成功，提示充值成功，余额增加 100 元，生成充值记录 | - | ⏳ |
| TC-ACCOUNT-012 | 充值 - 金额为负 | `{"amount":-100,"type":1}` | 返回失败，提示充值金额必须大于 0 | - | ⏳ |
| TC-ACCOUNT-013 | 充值 - 金额为零 | `{"amount":0,"type":1}` | 返回失败，提示充值金额必须大于 0 | - | ⏳ |
| TC-ACCOUNT-014 | 充值 - 类型无效 | `{"amount":100,"type":5}` | 返回失败，提示充值类型无效 | - | ⏳ |
| TC-ACCOUNT-015 | 充值 - 重复提交 | 相同请求提交两次 | 第二次返回失败，提示操作正在进行中（分布式锁） | - | ⏳ |
| TC-ACCOUNT-016 | 充值 - 金额为小数 | `{"amount":100.567,"type":1}` | 返回成功，金额保留 2 位小数 | - | ⏳ |
| TC-ACCOUNT-017 | 充值 - 最大金额 | `{"amount":999999.99,"type":1}` | 返回成功（边界值测试） | - | ⏳ |
| TC-ACCOUNT-018 | 充值 - 备注为空 | `{"amount":100,"type":1,"remark":""}` | 返回成功，备注可为空 | - | ⏳ |
| TC-ACCOUNT-019 | 充值 - 备注超长 | `{"amount":100,"type":1,"remark":"这是一段非常长的超过 200 个字符的备注信息这是一段非常长的超过 200 个字符的备注信息"}` | 返回失败，提示备注长度超限 | - | ⏳ |
| TC-ACCOUNT-020 | 充值 - 并发充值 | 同一用户同时提交 10 个充值请求 | 依次处理，全部成功，余额正确累加 | - | ⏳ |

**curl 示例：**
```bash
curl -X POST http://localhost:8080/account/recharge \
  -H "Content-Type: application/json" \
  -H "token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{"amount":100,"type":1,"remark":"微信充值"}'
```

---

### 8.3 账户支付

**接口信息：**
- 接口名称：账户支付
- 请求路径：`/account/pay`
- 请求方法：POST
- 接口描述：患者使用账户余额支付就诊自付部分
- 权限：PATIENT

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| visitId | Long | 是 | 就诊 ID |
| remark | String | 否 | 备注 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-ACCOUNT-021 | 支付 - 成功 | `POST /account/pay?visitId=1&remark=就诊支付` | 返回成功，提示支付成功，余额减少 selfPayAmount 元，生成消费记录 | - | ⏳ |
| TC-ACCOUNT-022 | 支付 - 余额不足 | `POST /account/pay?visitId=1` (余额 < 自付金额) | 返回失败，提示账户余额不足 | - | ⏳ |
| TC-ACCOUNT-023 | 支付 - 就诊未结算 | `POST /account/pay?visitId=2` (未结算的就诊) | 返回失败，提示就诊未结算 | - | ⏳ |
| TC-ACCOUNT-024 | 支付 - 就诊不存在 | `POST /account/pay?visitId=999` | 返回失败，提示就诊不存在 | - | ⏳ |
| TC-ACCOUNT-025 | 支付 - 缺少 visitId | `POST /account/pay` | 返回失败，提示 visitId 不能为空 | - | ⏳ |
| TC-ACCOUNT-026 | 支付 - 重复支付 | `POST /account/pay?visitId=1` (已支付的就诊) | 返回失败，提示已支付 | - | ⏳ |
| TC-ACCOUNT-027 | 支付 - 安全验证 | `POST /account/pay?visitId=1` (使用他人 token) | 返回失败，只能给自己支付（userId 从 token 获取） | - | ⏳ |
| TC-ACCOUNT-028 | 支付 - 金额验证 | `POST /account/pay?visitId=1` (selfPayAmount=50.50) | 返回成功，扣除 50.50 元 | - | ⏳ |
| TC-ACCOUNT-029 | 支付 - 并发支付 | 同一就诊同时提交 10 个支付请求 | 只有一个成功，其他返回失败（幂等性控制） | - | ⏳ |
| TC-ACCOUNT-030 | 支付 - 余额刚好 | 余额=100 元，支付 100 元 | 返回成功，余额为 0 | - | ⏳ |

**安全控制说明：**
- ✅ 金额从结算单的 selfPay 字段获取，不是前端传入
- ✅ userId 从当前登录用户获取，不允许前端传
- ✅ 防止前端篡改金额或为他人支付

**curl 示例：**
```bash
curl -X POST "http://localhost:8080/account/pay?visitId=1&remark=就诊支付" \
  -H "token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 8.4 查询充值记录列表

**接口信息：**
- 接口名称：查询充值记录列表
- 请求路径：`/account/recharge/list`
- 请求方法：GET
- 接口描述：查询患者充值记录列表（分页）
- 权限：PATIENT

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户 ID |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-ACCOUNT-031 | 查询充值记录 - 成功 | `GET /account/recharge/list?userId=1&pageNum=1&pageSize=10` | 返回充值记录列表 | - | ⏳ |
| TC-ACCOUNT-032 | 查询充值记录 - 无记录 | `GET /account/recharge/list?userId=999` | 返回空列表 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/account/recharge/list?userId=1&pageNum=1&pageSize=10"
```

---

### 8.5 查询消费记录列表

**接口信息：**
- 接口名称：查询消费记录列表
- 请求路径：`/account/consumption/list`
- 请求方法：GET
- 接口描述：查询患者消费记录列表（分页）
- 权限：PATIENT

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户 ID |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**测试案例：**

| 案例编号 | 案例名称 | 请求数据 | 预期结果 | 实际结果 | 状态 |
|----------|----------|----------|----------|----------|------|
| TC-ACCOUNT-041 | 查询消费记录 - 成功 | `GET /account/consumption/list?userId=1&pageNum=1&pageSize=10` | 返回消费记录列表 | - | ⏳ |
| TC-ACCOUNT-042 | 查询消费记录 - 无记录 | `GET /account/consumption/list?userId=999` | 返回空列表 | - | ⏳ |

**curl 示例：**
```bash
curl -X GET "http://localhost:8080/account/consumption/list?userId=1&pageNum=1&pageSize=10"
```

---

## 九、完整业务流程测试

### 9.1 正常业务流程测试

**测试场景：** 患者就诊 → 费用录入 → 医保结算 → 患者充值 → 账户支付 → 医院申报 → 医保拨付

| 步骤 | 操作 | 接口 | 请求数据 | 预期结果 | 状态 |
|------|------|------|----------|----------|------|
| 1 | 患者注册 | POST /user/sign | `{"password":"123456","name":"张三","idCard":"110101199001011234","role":1}` | 患者 ID=1 | ⏳ |
| 2 | 医院注册 | POST /hospital/sign | `{"name":"北京市第一医院"}` | 医院 ID=2 | ⏳ |
| 3 | 患者登录 | POST /user/login | `{"userId":1,"password":"123456","role":1}` | 获取 token | ⏳ |
| 4 | 创建就诊 | POST /visit/add | `{"userId":1,"hospitalId":2,"type":1,"diagnosis":"上呼吸道感染"}` | 就诊 ID=1 | ⏳ |
| 5 | 添加费用 | POST /fee/batch/add | `[{"visitId":1,"name":"阿莫西林","type":1,"price":25.5,"num":2}]` | 费用 ID 列表 | ⏳ |
| 6 | 医保结算 | POST /settle/calculate/1 | - | 结算成功，selfPayAmount=10.20 | ⏳ |
| 7 | 查询结算 | GET /settle/detail/1 | - | 结算详情 | ⏳ |
| 8 | 患者充值 | POST /account/recharge | `{"amount":100,"type":1}` | 充值成功 | ⏳ |
| 9 | 查询账户 | GET /account/get | - | balance=100.00 | ⏳ |
| 10 | 账户支付 | POST /account/pay?visitId=1 | - | 支付成功 | ⏳ |
| 11 | 查询消费 | GET /account/consumption/list?userId=1 | - | 消费记录 | ⏳ |
| 12 | 创建批次 | POST /batch/create/2 | - | 批次 ID=1 | ⏳ |
| 13 | 添加结算单到批次 | POST /batch/add-settle?batchId=1&settleId=1 | - | 添加成功 | ⏳ |
| 14 | 查询批次 | GET /batch/detail/1 | - | 批次详情 | ⏳ |
| 15 | 医保拨付 | POST /pay/pay-batch/1 | - | 拨付成功 | ⏳ |
| 16 | 查询拨付 | GET /pay/by-batch/1 | - | 拨付详情 | ⏳ |

---

### 9.2 异常场景测试

| 场景编号 | 场景描述 | 测试步骤 | 预期结果 | 状态 |
|----------|----------|----------|----------|------|
| SCENE-001 | 结算后修改费用 | 1. 结算成功<br>2. 尝试添加费用 | 添加失败 | ⏳ |
| SCENE-002 | 批次完成后添加结算单 | 1. 批次已完成<br>2. 添加结算单 | 添加失败，提示批次已完成 | ⏳ |
| SCENE-003 | 重复拨付 | 1. 拨付成功<br>2. 再次拨付 | 拨付失败，提示已拨付 | ⏳ |
| SCENE-004 | 重复结算 | 1. 结算成功<br>2. 再次结算 | 结算失败，提示已结算 | ⏳ |
| SCENE-005 | 余额不足支付 | 1. 充值 10 元<br>2. 支付 50 元 | 支付失败，提示余额不足 | ⏳ |
| SCENE-006 | 为他人充值 | 尝试使用他人 userId 充值 | 不允许，只能给自己充值 | ⏳ |
| SCENE-007 | 为他人支付 | 尝试使用他人 visitId 支付 | 支付失败，只能给自己支付 | ⏳ |

---

## 十、权限测试

### 10.1 角色权限验证

| 案例编号 | 接口 | 患者访问 | 医院访问 | 医保局访问 | 状态 |
|----------|------|----------|----------|------------|------|
| TC-PERM-001 | POST /visit/add | ❌ 403 | ✅ 成功 | ❌ 403 | ⏳ |
| TC-PERM-002 | GET /visit/my/list | ✅ 成功 | ❌ 403 | ❌ 403 | ⏳ |
| TC-PERM-003 | POST /fee/batch/add | ❌ 403 | ✅ 成功 | ❌ 403 | ⏳ |
| TC-PERM-004 | POST /settle/calculate/1 | ❌ 403 | ✅ 成功 | ❌ 403 | ⏳ |
| TC-PERM-005 | POST /batch/create/2 | ❌ 403 | ✅ 成功 | ❌ 403 | ⏳ |
| TC-PERM-006 | POST /pay/pay-batch/1 | ❌ 403 | ❌ 403 | ✅ 成功 | ⏳ |
| TC-PERM-007 | POST /account/recharge | ✅ 成功 | ❌ 403 | ❌ 403 | ⏳ |
| TC-PERM-008 | GET /hospital/list | ✅ 成功 | ✅ 成功 | ✅ 成功 | ⏳ |

---

## 十一、并发测试

### 11.1 并发场景测试

| 测试编号 | 测试场景 | 并发数 | 预期响应时间 | 预期成功率 | 状态 |
|----------|----------|--------|--------------|------------|------|
| PERF-001 | 并发登录 | 100 | < 1s | 100% | ⏳ |
| PERF-002 | 并发结算（同一就诊） | 10 | < 2s | 100%（分布式锁保证） | ⏳ |
| PERF-003 | 并发拨付（同一批次） | 10 | < 2s | 100%（分布式锁保证） | ⏳ |
| PERF-004 | 并发充值（同一用户） | 10 | < 1s | 100%（分布式锁保证） | ⏳ |
| PERF-005 | 并发添加结算单到批次 | 10 | < 2s | 100%（分布式锁保证） | ⏳ |

---

## 十二、测试状态说明

**状态标识：**
- ⏳ 待测试
- ✅ 测试通过
- ❌ 测试失败
- ⚠️ 部分通过

**测试环境：**
- 环境：开发环境
- 数据库：MySQL 8.0
- 缓存：Redis 7.0
- JDK 版本：17
- Spring Boot：2.7.18

**注意事项：**
1. 测试前请确保数据库已初始化（执行 int.sql）
2. 部分接口需要登录后获取 token，在请求头中添加 `token: xxx`
3. 测试数据请及时清理，避免影响后续测试
4. 性能测试请在独立环境进行
5. 并发测试验证分布式锁和幂等性控制的有效性

**数据清理建议：**
```sql
-- 清空测试数据（按顺序执行）
DELETE FROM consumption_record;
DELETE FROM recharge_record;
DELETE FROM user_account;
DELETE FROM pay;
DELETE FROM batch_item;
DELETE FROM batch;
DELETE FROM settle;
DELETE FROM fee;
DELETE FROM visit;
DELETE FROM user;
DELETE FROM hospital;
```

---

**文档版本：** 2.0.0  
**创建时间：** 2026-04-21  
**更新时间：** 2026-04-27  
**维护人员：** 医保核销系统开发团队
