## 解析Mysbatis日志SQL

### 介绍

1. 解析Mysbatis日志SQL，将日志中的SQL语句和参数解析出来，方便查看日志中的SQL执行情况。

2. 系统误删除数据，可以通过日志中的SQL语句和参数恢复数据。

### 注意

1. 只能解析Mysbatis日志中的SQL语句和参数，其他日志格式不支持。
2. **不能解析参数中带"$"的语句**
3. 如果日志中的SQL语句和参数不是一一对应的，解析出来的参数可能会有误。
4. SQL语句在日志中的格式必须是一行，不能换行。且**参数日志在SQL语句日志后面**。

### 使用方法

```java

@Test
public void parseSQL() throws IOException {

    // files: 日志文件目录
    // new String[]{"UPDATE"}: 只解析UPDATE语句
    // T_EMC_EQUIPT: 需要关心的表名
    // \\[GUID.+==>\\s* ：sql与参数在日志中的相同前缀，用于识别sql对应的参数
    // ^\\d+-\\d+-\\d+\\s\\d+:\\d+:\\d+ ：获取日志时间的正则表达式,输出sql文件时会按照时间排序
    ConvertConfig config = new ConvertConfig("files",
            new String[]{"UPDATE"},
            "T_EMC_EQUIPT",
            "\\[GUID.+==>\\s*",
            "^\\d+-\\d+-\\d+\\s\\d+:\\d+:\\d+");
    // 解析日志
    ConvertUtils.parseSQL(config);
}
```

### 输出结果

输出文件在日志文件的子目录output下，文件有三种，内容如下：

- {time}-{tableName}.sql：解析结果
- {time}-{tableName}-time.log：解析过程中的日志：包含解析的SQL语句和参数，以及SQL语句的执行时间
-

{time}-{tableName}-checked-time.log：同一时间点（根据正则表达式解析出来的时间）内的日志，可能包含多条SQL，可能出现参数与SQL不对应的情况，或者多条sql有固定的前后顺序，不能随意调换顺序，所以需要人为检查