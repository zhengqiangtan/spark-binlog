package org.apache.spark.sql.mlsql.sources.mysql.binlog.io;


import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.sql.mlsql.sources.mysql.binlog.MySQLCDCUtils;
import org.apache.spark.sql.mlsql.sources.mysql.binlog.RawBinlogEvent;
import org.apache.spark.sql.mlsql.sources.mysql.binlog.TableInfo;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class InsertRowsWriter extends AbstractEventWriter {
    private String timeZone;

    private Configuration hadoopConf;

    public InsertRowsWriter(String timeZone, Configuration hadoopConf) {
        this.timeZone = timeZone;
        this.hadoopConf = hadoopConf;
    }

    @Override
    public List<String> writeEvent(RawBinlogEvent event) {
        WriteRowsEventData data = event.getEvent().getData();
        List<String> items = new ArrayList<>();

        for (Serializable[] row : data.getRows()) {
            try {
                StringWriter writer = new StringWriter();
                startJson(writer, event);
                final BitSet bitSet = data.getIncludedColumns();
                writeRow(event.getTableInfo(), row, bitSet);
                endJson();
                items.add(writer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return items;
    }

    protected void writeRow(TableInfo tableInfo, Serializable[] row, BitSet includedColumns) throws IOException {

        jsonGenerator.writeArrayFieldStart("rows");
        int i = includedColumns.nextSetBit(0);
        jsonGenerator.writeStartObject();
        while (i != -1) {
            SchemaTool schemaTool = new SchemaTool(tableInfo.getSchema(), i,timeZone,hadoopConf);
            String columnName = schemaTool.getColumnNameByIndex();
            if (row[i] != null) {
                jsonGenerator.writeObjectField(columnName, MySQLCDCUtils.getWritableObject(schemaTool, row[i]));
            }
            i = includedColumns.nextSetBit(i + 1);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndArray();
    }
}
