package com.dawnwang.utils;

import com.dawnwang.config.ConvertConfig;
import org.junit.Test;

import java.io.IOException;


public class ConvertUtilsTest {

    @Test
    public void parseSQL() throws IOException {
        ConvertConfig config = new ConvertConfig("files",
                new String[]{"UPDATE"},
                "T_EMC_EQUIPT",
                "\\[GUID.+==>\\s*",
                "^\\d+-\\d+-\\d+\\s\\d+:\\d+:\\d+");
        ConvertUtils.parseSQL(config);
    }
}