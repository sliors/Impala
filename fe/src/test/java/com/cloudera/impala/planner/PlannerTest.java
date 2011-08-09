// Copyright (c) 2011 Cloudera, Inc. All rights reserved.

package com.cloudera.impala.planner;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.impala.analysis.AnalysisContext;
import com.cloudera.impala.catalog.Catalog;
import com.cloudera.impala.catalog.TestSchemaUtils;
import com.cloudera.impala.common.AnalysisException;
import com.cloudera.impala.common.NotImplementedException;
import com.cloudera.impala.testutil.TestFileParser;
import com.cloudera.impala.testutil.TestUtils;
import com.cloudera.impala.thrift.TPlan;

public class PlannerTest {
  private final static Logger LOG = LoggerFactory.getLogger(PlannerTest.class);

  private static Catalog catalog;
  private static AnalysisContext analysisCtxt;
  private final String testDir = "PlannerTest";

  @BeforeClass public static void setUp() throws Exception {
    HiveMetaStoreClient client = TestSchemaUtils.createClient();
    catalog = new Catalog(client);
    analysisCtxt = new AnalysisContext(catalog);
  }

  private void RunQuery(String query, ArrayList<String> expectedPlan) {
    try {
      LOG.info("running query " + query);
      AnalysisContext.AnalysisResult analysisResult = analysisCtxt.analyze(query);
      Planner planner = new Planner();
      PlanNode plan = planner.createPlan(analysisResult.selectStmt, analysisResult.analyzer);
      TPlan thriftPlan = plan.treeToThrift();
      LOG.info(thriftPlan.toString() + "\n");
      String result = TestUtils.compareOutput(plan.getExplainString().split("\n"), expectedPlan);
      if (!result.isEmpty()) {
        fail("query:\n" + query + "\n" + result);
      }
    } catch (AnalysisException e) {
      fail("analysis error: " + e.getMessage());
    } catch (NotImplementedException e) {
      fail("plan not implemented");
    }
  }

  private void RunUnimplementedQuery(String query) {
    try {
      AnalysisContext.AnalysisResult analysisResult = analysisCtxt.analyze(query);
      Planner planner = new Planner();
      PlanNode plan = planner.createPlan(analysisResult.selectStmt, analysisResult.analyzer);
      fail("query produced a plan\nquery=" + query + "\nplan=\n" + plan.getExplainString());
    } catch (AnalysisException e) {
      fail("analysis error: " + e.getMessage());
    } catch (NotImplementedException e) {
      // expected
    }
  }

  private void RunTests(String testCase) {
    String fileName = testDir + "/" + testCase + ".test";
    TestFileParser queryFileParser = new TestFileParser(fileName);
    queryFileParser.open();
    while (queryFileParser.hasNext()) {
      queryFileParser.next();
      String query = queryFileParser.getQuery();
      ArrayList<String> plan = queryFileParser.getExpectedResult(0);
      if (plan.size() > 0 && plan.get(0).toLowerCase().startsWith("not implemented")) {
        RunUnimplementedQuery(query);
      } else {
        RunQuery(query, plan);
      }
    }
    queryFileParser.close();
  }

  @Test public void Test() {
    RunTests("basic");
    RunTests("joins");
  }
}
