package com.dawnwang.utils;


import com.dawnwang.DTO.FieldType;
import com.dawnwang.DTO.ParamDTO;
import com.dawnwang.config.ConvertConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConvertUtils {


    public static boolean isSql(String text) {
        if (text == null || text.length() <= 0) {
            return false;
        }

        if (strNum(text, ": ") > 1) {
            return false;
        }

        if (!text.contains("?")) {
            return false;
        }

        final String tempText = fixSql(text).toUpperCase(Locale.ROOT);
        boolean select = tempText.contains("SELECT") && tempText.contains("FROM");
        boolean insert = tempText.contains("INSERT INTO") && tempText.contains("VALUES");
        boolean delete = tempText.contains("DELETE") && tempText.contains("FROM");
        boolean update = tempText.contains("UPDATE") && tempText.contains("SET");

        return select || insert || delete || update;
    }

    public static String getTypeBySQL(String text) {
        if (text == null || text.length() <= 0) {
            return null;
        }

        if (strNum(text, ": ") > 1) {
            return null;
        }

        if (!text.contains("?")) {
            return null;
        }

        final String tempText = fixSql(text).toUpperCase(Locale.ROOT);
        boolean select = tempText.contains("SELECT") && tempText.contains("FROM");
        boolean insert = tempText.contains("INSERT INTO") && tempText.contains("VALUES");
        boolean delete = tempText.contains("DELETE") && tempText.contains("FROM");
        boolean update = tempText.contains("UPDATE") && tempText.contains("SET");

        if (select) {
            return "SELECT";
        } else if (insert) {
            return "INSERT";
        } else if (delete) {
            return "DELETE";
        } else if (update) {
            return "UPDATE";
        }
        return null;
    }


    // exclude
    public static boolean excludeSql(String text, String[] excludes) {
        for (String exclude : excludes) {
            if (text.contains(exclude)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isParam(String text) {
        if (text == null || text.length() <= 0) {
            return false;
        }

        return strNum(text, ": ") <= 1;
    }

    public static int strNum(String text, String checkFlag) {
        String var = text;
        int res = 0;
        while (var.contains(checkFlag)) {
            var = var.substring(var.indexOf(checkFlag) + checkFlag.length()).trim();
            res++;
        }
        return res;
    }

    public static String fixSql(String text) {
        String paramFlag = ": ";
        if (text.contains(paramFlag)) {
            return text.substring(text.lastIndexOf(paramFlag) + 2).trim();
        } else {
            return text.trim();
        }
    }

    public static String fixParam(String text) {
        String paramFlag = ": ";
        if (text.contains(paramFlag)) {
            return text.substring(text.lastIndexOf(paramFlag) + 2).trim();
        } else {
            return text.trim();
        }
    }

    public static List<ParamDTO> getParamValues(String params) {
        String realParamStr = fixParam(params);
        //分隔符定义
        String regEx = "(\\(String\\)|\\(Long\\)|\\(BigDecimal\\)|\\(Date\\)|\\(Timestamp\\)|\\(LocalDate\\)|\\(LocalTime\\)|\\(LocalDateTime\\)),?";
        Pattern pattern = Pattern.compile(regEx);
        // 获取参数和对应的类型
        String[] split = pattern.split(realParamStr);
        Matcher matcher = pattern.matcher(realParamStr);
        List<ParamDTO> paramDTOS = new ArrayList<>();
        int i = 0;
        while (matcher.find()) {
            String group = matcher.group();
            ParamDTO paramDTO = new ParamDTO();
            String value = StringUtils.trim(split[i]);
            String type = StringUtils.substring(group, group.lastIndexOf("(") + 1, group.lastIndexOf(")"));
            paramDTO.setValue(StringUtils.trim(value));
            paramDTO.setType(type);
            paramDTOS.add(paramDTO);
            i++;
        }
        return paramDTOS;
    }


    public static String convert(String sql, String params) {
        if (!isSql(sql) || !isParam(params)) {
            return null;
        }

        sql = fixSql(sql);
        if (!sql.contains("?")) {
            return null;
        }
        List<ParamDTO> paramDTOList = getParamValues(params);

        for (ParamDTO paramDTO : paramDTOList) {
            String paramValue = paramDTO.getValue();
            String type = paramDTO.getType();

            if ("null".equals(paramValue)) {
                int index = sql.indexOf("?");
                int end = index;
                char c;
                int isNullFalg = 0;
                while (true) {
                    index--;
                    c = sql.charAt(index);
                    if (c != ' ') {
                        if (c == '=') {
                            char preChar = sql.charAt(index - 1);
                            if (preChar == '!') {
                                // is not null
                                isNullFalg = 1;
                                index--;
                            } else if (preChar == ':' || preChar == '<' || preChar == '>') {
                                // 正常赋值
                                isNullFalg = 0;
                            } else {
                                // is null
                                isNullFalg = 2;
                            }
                        }
                        break;
                    }
                }
                switch (isNullFalg) {
                    case 1:
                        // 这个?不转义，这方法还识别不了，这个有点坑
                        sql = sql.replaceFirst((sql.substring(index, end) + "\\?"), " is not null ");
                        break;
                    case 2:
                        sql = sql.replaceFirst((sql.substring(index, end) + "\\?"), " is null ");
                        break;
                    case 0:
                    default:
                        sql = sql.replaceFirst("\\?", paramValue);
                }

            } else {
                String defaultStr = FieldType.getDefaultValue(type);
                String finalParam = defaultStr + paramValue + defaultStr;
                if (paramValue.contains("\\")) {
                    paramValue = paramValue.replace("\\", "\\\\");
                }
                if (paramValue.contains("$")) {
                    paramValue = paramValue.replace("$", "\\\\$");
                }
                sql = sql.replaceFirst("\\?", finalParam);
            }

            if (!sql.contains("?")) {
                break;
            }

        }

        return sql;

    }

    public static void parseSQL(ConvertConfig config) throws IOException {
        File logFile = new File(config.getLogFilesDir());

        File[] files = logFile.listFiles();
        // 按文件名排序
        Arrays.sort(files, Comparator.comparing(File::getName));
        // 需要匹配的sql类型
        String[] sqlType = config.getSqlType();
        // 目标表名
        String targetTableName = config.getTargetTableName();
        // 匹配的前缀正则表达式
        String prefixRegex = config.getPrefixRegex();
        // 匹配的SQL时间正则表达式
        String regexTime = config.getRegexTime();

        // 解析结果
        Map<String, String> parseResultMap = new LinkedHashMap<>();
        // 解析结果-需要检查的 同一个sql不同时间
        List<String> checkedList = new ArrayList<>();
        // 解析
        ConvertUtils convertUtils = new ConvertUtils();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            List<String> list = FileUtils.readLines(file, "UTF-8");
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                String sql = list.get(i);
                // 排除不是sql
                if (!isSql(sql)) {
                    continue;
                }
                // 只处理关心的sql类型
                String typeBySQL = ConvertUtils.getTypeBySQL(sql);
                if (typeBySQL == null) {
                    continue;
                }
                if (sqlType.length > 0 && !Arrays.asList(sqlType).contains(typeBySQL)) {
                    continue;
                }

                // 排除不需要的表
                if (!StringUtils.contains(sql, targetTableName)) {
                    continue;
                }

                // 提取SQL的前缀，用来向下查找参数，参数的前缀和sql的前缀必须一致
                // 2024-01-31 11:44:04 [GUID:afebb01e60c3457696bda50061c6039e] INFO   DaoMapper.updateById ==>  Preparing: UPDATE T_EMC_EQUIPT SET `BRANDTOKEN` = ? WHERE ID = ?
                // 2024-01-31 11:44:04 [GUID:afebb01e60c3457696bda50061c6039e] INFO   DaoMapper.updateById ==> Parameters: 其他(String), 1067769506263936(Long)
                // 这里的前缀是[GUID:afebb01e60c3457696bda50061c6039e] INFO   DaoMapper.updateById ==>
                Pattern prefixPatten = Pattern.compile(prefixRegex);
                Matcher prefixPatcher = prefixPatten.matcher(sql);
                String prefix = "";
                if (prefixPatcher.find()) {
                    prefix = prefixPatcher.group();
                }

                // 提取时间，用于排序 解析结果
                Pattern timePatten = Pattern.compile(regexTime);
                Matcher timePatcher = timePatten.matcher(sql);
                String time = "";
                if (timePatcher.find()) {
                    time = timePatcher.group();
                }

                String param = "";

                // todo 这里的逻辑有点问题，如果有多个相同的sql，但是参数不同，这里会有问题
                // 获取sql中有多少个？
                int paramSize = StringUtils.countMatches(sql, "?");
                for (int j = i + 1; j < listSize; j++) {
                    String s1 = list.get(j);
                    if (s1.contains("Parameters:")
                            && s1.contains(StringUtils.trim(prefix))
                            && paramSize == ConvertUtils.getParamValues(s1).size()) {
                        param = s1;
                        break;
                    }
                }
                // 如果参数不为空，且不包含$，则进行转换,并且不包含$是因为转义的问题，这里不处理
                if (StringUtils.isNotBlank(sql) && StringUtils.isNotBlank(param) && !StringUtils.contains(param, "$")) {
                    String convert = ConvertUtils.convert(sql, param) + ";";
                    if (parseResultMap.containsKey(convert)) {
                        // time + convert
                        checkedList.add(parseResultMap.get(convert) + " " + convert);//old
                        checkedList.add(time + " " + convert);//new
                        checkedList.add("\n");//old
                    }
                    parseResultMap.put(convert, time);
                }
            }
        }
        // 根据时间排序
        Map<String, String> sortedMap = new LinkedHashMap<>();
        parseResultMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        // 排序输出
        ArrayList<String> sortedSqlList = new ArrayList<>();
        ArrayList<String> sortedTimeSqlList = new ArrayList<>();
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            String sql = entry.getKey();
            String time = entry.getValue();
            sortedSqlList.add(sql);
            sortedTimeSqlList.add(time + " " + sql);
        }

        // 创建数据输出目录
        File logFilesDir1 = new File(logFile, "output");
        if (!logFilesDir1.exists()) {
            logFilesDir1.mkdirs();
        }
        long currentTime = System.currentTimeMillis();
        File sqlFile = new File(logFilesDir1, currentTime + "-" + targetTableName + ".sql");
        File sqlTimeFile = new File(logFilesDir1, currentTime + "-" + targetTableName + "-time.log");
        File sqlCheckedFile = new File(logFilesDir1, currentTime + "-" + targetTableName + "-checked-time.log");
        FileUtils.writeLines(sqlFile, sortedSqlList);
        FileUtils.writeLines(sqlTimeFile, sortedTimeSqlList);
        FileUtils.writeLines(sqlCheckedFile, checkedList);
    }

}
