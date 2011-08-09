// Copyright (c) 2011 Cloudera, Inc. All rights reserved.

package com.cloudera.impala.datagenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

class TestDataGenerator {  
  // 2 years
  private static final int DEFAULT_NUM_PARTITIONS = 24;
  // 10 tuples per day of month
  private static final int DEFAULT_MAX_TUPLES_PER_PARTITION = 310;
  // arbitrary default value
  private static final int DEFAULT_END_YEAR = 2010;
  
  private static void GenerateAllTypesData(String dir, int numPartitions,
      int maxTuplesPerPartition) throws IOException {
    int numYears = Math.max((numPartitions / 12) - 1, 1);
    int startYear = Math.max(DEFAULT_END_YEAR - numYears, 0);
    GregorianCalendar date = new GregorianCalendar(startYear, Calendar.JANUARY, 1);
    GregorianCalendar endDate = new GregorianCalendar(DEFAULT_END_YEAR, Calendar.DECEMBER, 31);
    int months = 0;
    while (date.before(endDate) && months < numPartitions) {
      GregorianCalendar nextMonth = (GregorianCalendar) date.clone();
      nextMonth.add(Calendar.MONTH, 1);
      GenerateAllTypesPartition(dir, date, nextMonth, 10, maxTuplesPerPartition, false);
      date = nextMonth;
      ++months;
    }
  }

  private static void GenerateAllTypesAggData(String dir) throws IOException {
    int startYear = 2010;
    GregorianCalendar date = new GregorianCalendar(startYear, Calendar.JANUARY, 1);
    GregorianCalendar endDate = (GregorianCalendar) date.clone();
    endDate.add(Calendar.DAY_OF_MONTH, 10);
    while (date.before(endDate)) {
      GregorianCalendar nextDay = (GregorianCalendar) date.clone();
      nextDay.add(Calendar.DAY_OF_MONTH, 1);
      GenerateAllTypesPartition(dir, date, nextDay, 1000, 1000, true);
      date = nextDay;
    }
  }

  private static void GenerateAllTypesPartition(String dir, Calendar startDate,
      Calendar endDate, int intsPerDay, int maxTuplesPerPartition, boolean writeNulls)
      throws IOException {
    SimpleDateFormat filenameFormat = new SimpleDateFormat("yyMMdd");
    PrintWriter writer = new PrintWriter(new FileWriter(new File(new File(dir),
        filenameFormat.format(startDate.getTime()) + ".txt")));
    Calendar date = (Calendar) startDate.clone();
    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
    int id = 0;
    while (date.before(endDate) && id < maxTuplesPerPartition) {
      for (int int_col = 0; int_col < intsPerDay && id < maxTuplesPerPartition;
           ++int_col) {
        boolean bool_col = (id % 2 == 0 ? true : false);
        byte tinyint_col = (byte) (int_col % 10);
        short smallint_col = (short) (int_col % 100);
        long bigint_col = int_col * 10;
        float float_col = (float) (1.1 * int_col);
        //float float_col = (byte) (1.1 * int_col);
        double double_col = 10.1 * int_col;
        String date_string_col = df.format(date.getTime());
        String string_col = String.valueOf(int_col);
        writer.format("%d,%b,%s,%s,%s,%s,", id, bool_col,
            (writeNulls && tinyint_col == 0 ? "" : Byte.toString(tinyint_col)),
            (writeNulls && smallint_col == 0 ? "" : Short.toString(smallint_col)),
            (writeNulls && int_col == 0 ? "" : Integer.toString(int_col)),
            (writeNulls && bigint_col == 0 ? "" : Long.toString(bigint_col)));
        writer.format("%s,%s,'%s','%s'\n",
            (writeNulls && int_col == 0 ? "" : Float.toString(float_col)),
            (writeNulls && int_col == 0 ? "" : Double.toString(double_col)),
            date_string_col, string_col);
        ++id;
      }
      date.add(Calendar.DAY_OF_MONTH, 1);
    }
    writer.close();
  }

  /**
   * Generate some test data.
   * 
   * @param BaseOutputDirectory
   *          : Required base output folder of generated data files.
   * @throws Exception
   *           something bad happened
   */
  public static void main(String args[]) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: " + "TestDataGenerator BaseOutputDirectory");          
    }
    
    // Generate AllTypes
    String allTypesDirName = args[0] + "/AllTypes";
    File allTypesDir = new File(allTypesDirName);
    allTypesDir.mkdirs();    
    GenerateAllTypesData(allTypesDirName, DEFAULT_NUM_PARTITIONS,
        DEFAULT_MAX_TUPLES_PER_PARTITION);
    
    // Generate AllTypesSmall
    String allTypesSmallDirName = args[0] + "/AllTypesSmall";
    File allTypesSmallDir = new File(allTypesSmallDirName);
    allTypesSmallDir.mkdirs();
    GenerateAllTypesData(allTypesSmallDirName, 4, 25);

    // Generate AllTypesAgg
    String allTypesAggDirName = args[0] + "/AllTypesAgg";
    File allTypesAggDir = new File(allTypesAggDirName);
    allTypesAggDir.mkdirs();
    GenerateAllTypesAggData(allTypesAggDirName);
  }
}
