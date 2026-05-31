

package com.datamaster.spark.etl.reader;

import com.alibaba.fastjson2.JSONObject;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import com.datamaster.spark.etl.utils.LogUtils;

import java.util.List;

/**
 * <P>
 * 用途:读数据
 * </p>
 *
 * @author: FXB
 * @create: 2025-04-21 13:34
 **/
public interface Reader {


    public static final String ETL_READER_ID_KEY = "etl:reader:id:";
    public static final String ETL_READER_DATE_KEY = "etl:reader:date:";

    Dataset<Row> read(SparkSession spark, JSONObject reader, List<String> readerColumns, LogUtils.Params logParams);

    String code();
}
