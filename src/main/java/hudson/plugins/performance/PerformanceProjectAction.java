package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.*;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.plugins.performance.PerformanceReportPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public final class PerformanceProjectAction implements Action {

  private static final String CONFIGURE_LINK = "configure";
  private static final String TRENDREPORT_LINK = "trendReport";
  private static final String TESTSUITE_LINK = "testsuiteReport";

  private static final String PLUGIN_NAME = "performance";


  private static final long serialVersionUID = 1L;

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(PerformanceProjectAction.class.getName());

  public final AbstractProject<?, ?> project;

  private transient List<String> performanceReportList;

  public String getDisplayName() {
    return Messages.ProjectAction_DisplayName();
  }

  public String getIconFileName() {
    return "graph.gif";
  }

  public String getUrlName() {
    return PLUGIN_NAME;
  }

  public PerformanceProjectAction(AbstractProject project) {
    this.project = project;
  }

  private JFreeChart createErrorsChart(CategoryDataset dataset) {

    final JFreeChart chart = ChartFactory.createLineChart(
        Messages.ProjectAction_PercentageOfErrors(), // chart title
        null, // unused
        "%", // range axis label
        dataset, // data
        PlotOrientation.VERTICAL, // orientation
        true, // include legend
        true, // tooltips
        false // urls
    );

    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    final LegendTitle legend = chart.getLegend();
    legend.setPosition(RectangleEdge.BOTTOM);

    chart.setBackgroundPaint(Color.white);

    final CategoryPlot plot = chart.getCategoryPlot();

    // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlinePaint(null);
    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.black);

    CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
    plot.setDomainAxis(domainAxis);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    domainAxis.setCategoryMargin(0.0);

    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    rangeAxis.setUpperBound(100);
    rangeAxis.setLowerBound(0);

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(4.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
  }

  private JFreeChart createThroughputChart(CategoryDataset dataset) {

    final JFreeChart chart = ChartFactory.createLineChart(
        Messages.ProjectAction_Throughput(), // chart title
        null, // unused
        "requests/sec", // range axis label
        dataset, // data
        PlotOrientation.VERTICAL, // orientation
        true, // include legend
        true, // tooltips
        false // urls
    );

    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    final LegendTitle legend = chart.getLegend();
    legend.setPosition(RectangleEdge.BOTTOM);

    chart.setBackgroundPaint(Color.white);

    final CategoryPlot plot = chart.getCategoryPlot();

    // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlinePaint(null);
    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.black);

    CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
    plot.setDomainAxis(domainAxis);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    domainAxis.setCategoryMargin(0.0);

    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
    rangeAxis.setAutoRange(true);
    //rangeAxis.setUpperBound(100);
    //rangeAxis.setLowerBound(0);

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(4.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
  }

  private JFreeChart createBytesTransferredChart(CategoryDataset dataset) {

    final JFreeChart chart = ChartFactory.createLineChart(
        Messages.ProjectAction_BytesTransferred(), // chart title
        null, // unused
        "KB", // range axis label
        dataset, // data
        PlotOrientation.VERTICAL, // orientation
        true, // include legend
        true, // tooltips
        false // urls
    );

    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    final LegendTitle legend = chart.getLegend();
    legend.setPosition(RectangleEdge.BOTTOM);

    chart.setBackgroundPaint(Color.white);

    final CategoryPlot plot = chart.getCategoryPlot();

    // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlinePaint(null);
    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.black);

    CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
    plot.setDomainAxis(domainAxis);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    domainAxis.setCategoryMargin(0.0);

    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
    rangeAxis.setAutoRange(true);
    //rangeAxis.setUpperBound(100);
    //rangeAxis.setLowerBound(0);

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(4.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
  }

  protected static JFreeChart createRespondingTimeChart(CategoryDataset dataset) {

    final JFreeChart chart = ChartFactory.createLineChart(
        Messages.ProjectAction_RespondingTime(), // charttitle
        null, // unused
        "ms", // range axis label
        dataset, // data
        PlotOrientation.VERTICAL, // orientation
        true, // include legend
        true, // tooltips
        false // urls
    );

    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    final LegendTitle legend = chart.getLegend();
    legend.setPosition(RectangleEdge.BOTTOM);

    chart.setBackgroundPaint(Color.white);

    final CategoryPlot plot = chart.getCategoryPlot();

    // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlinePaint(null);
    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.black);

    CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
    plot.setDomainAxis(domainAxis);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    domainAxis.setCategoryMargin(0.0);

    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(4.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
  }


    protected static JFreeChart createSummarizerChart (CategoryDataset dataset, String yAxis, String chartTitle) {

      final JFreeChart chart = ChartFactory.createBarChart(
          chartTitle, // chart title
          null, // unused
          yAxis, // range axis label
          dataset, // data
          PlotOrientation.VERTICAL, // orientation
          true, // include legend
          true, // tooltips
          true // urls
       );

       chart.setBackgroundPaint(Color.white);

       final CategoryPlot plot = chart.getCategoryPlot();

       plot.setBackgroundPaint(Color.WHITE);
       plot.setRangeGridlinesVisible(true);
       plot.setRangeGridlinePaint(Color.black);

       CategoryAxis domainAxis = plot.getDomainAxis();
       domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

       final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

       final BarRenderer renderer = (BarRenderer) plot.getRenderer();
           renderer.setDrawBarOutline(false);
           renderer.setBaseStroke(new BasicStroke(4.0f));
           renderer.setItemMargin(0);
           renderer.setMaximumBarWidth(0.05);

        
      return chart;
    }


  public void doErrorsGraph(StaplerRequest request, StaplerResponse response)
      throws IOException {
    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);
    String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
    if (performanceReportNameFile == null) {
      if (getPerformanceReportList().size() == 1) {
        performanceReportNameFile = getPerformanceReportList().get(0);
      } else {
        return;
      }
    }
    if (ChartUtil.awtProblemCause != null) {
      // not available. send out error message
      response.sendRedirect2(request.getContextPath() + "/images/headless.png");
      return;
    }
    DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderErrors = new DataSetBuilder<String, NumberOnlyBuildLabel>();
    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
    Range buildsLimits = getFirstAndLastBuild(request, builds);

    int nbBuildsToAnalyze = builds.size();
    for (AbstractBuild<?, ?> currentBuild :builds) {
      if (buildsLimits.in(nbBuildsToAnalyze)) {
    	
    	if (!buildsLimits.includedByStep(currentBuild.number)){
    		continue;
    	}  
    	  
        NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
        PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
        if (performanceBuildAction == null) {
          continue;
        }
        PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
            performanceReportNameFile);
        if (performanceReport == null) {
          nbBuildsToAnalyze--;
          continue;
        }
        dataSetBuilderErrors.add(performanceReport.errorPercent(),
            Messages.ProjectAction_Errors(), label);
      }
      nbBuildsToAnalyze--;
    }
    ChartUtil.generateGraph(request, response,
        createErrorsChart(dataSetBuilderErrors.build()), 400, 200);
  }

  	public void doRespondingTimeGraphPerTestCaseMode(StaplerRequest request,
  	      StaplerResponse response) throws IOException {
  		PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
  	    request.bindParameters(performanceReportPosition);
  	    String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
  	    if (performanceReportNameFile == null) {
  	      if (getPerformanceReportList().size() == 1) {
  	        performanceReportNameFile = getPerformanceReportList().get(0);
  	      } else {
  	        return;
  	      }
  	    }
  	    if (ChartUtil.awtProblemCause != null) {
  	      // not available. send out error message
  	      response.sendRedirect2(request.getContextPath() + "/images/headless.png");
  	      return;
  	    }
  	    DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
  	    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
  	    Range buildsLimits = getFirstAndLastBuild(request, builds);
  	    
  	    
  	    int nbBuildsToAnalyze = builds.size();
  	    
  	    for (AbstractBuild<?, ?> build : builds) {
  	       if (buildsLimits.in(nbBuildsToAnalyze)) {
  	        NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
  	        
  	        if (!buildsLimits.includedByStep(build.number)){
  	        	continue;
  	        }
  	        PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
  	        if (performanceBuildAction == null) {
  	          continue;
  	        }
  	        PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
  	            performanceReportNameFile);
  	        if (performanceReport == null) {
  	          nbBuildsToAnalyze--;
  	          continue;
  	        }
  	        
  	        List<HttpSample> allSamples = new ArrayList<HttpSample>();
  	        for (UriReport currentReport : performanceReport.getUriReportMap().values()) {
  	          allSamples.addAll(currentReport.getHttpSampleList());
  	        }
  	        Collections.sort(allSamples);
  	        for(HttpSample sample : allSamples){
  	        	if (sample.hasError()){
  	        		// we set duration as 0 for failed tests
  	        		dataSetBuilderAverage.add(0,
  	                        sample.getUri(), label);
  	        	}
  	        	else{
  	        	dataSetBuilderAverage.add(sample.getDuration(),
  	                    sample.getUri(), label);
  	        	}
  	        }
  	        
  	      }
  	      nbBuildsToAnalyze--;
  	    }
  	    ChartUtil.generateGraph(request, response,
  	        createRespondingTimeChart(dataSetBuilderAverage.build()), 600, 200);
  		
  	}
  
    public void doRespondingTimeGraph(StaplerRequest request,
      StaplerResponse response) throws IOException {
    	PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
          if (getPerformanceReportList().size() == 1) {
            performanceReportNameFile = getPerformanceReportList().get(0);
          } else {
            return;
          }
        }
        if (ChartUtil.awtProblemCause != null) {
          // not available. send out error message
          response.sendRedirect2(request.getContextPath() + "/images/headless.png");
          return;
        }
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (AbstractBuild<?, ?> build : builds) {
          if (buildsLimits.in(nbBuildsToAnalyze)) {
        	
        	if (!buildsLimits.includedByStep(build.number)){
          		continue;
          	}  
        	    
            NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
            PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
            if (performanceBuildAction == null) {
              continue;
            }
            PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
                performanceReportNameFile);
            if (performanceReport == null) {
              nbBuildsToAnalyze--;
              continue;
            }
            dataSetBuilderAverage.add(performanceReport.getMedian(),
                Messages.ProjectAction_Median(), label);
            dataSetBuilderAverage.add(performanceReport.getAverage(),
                Messages.ProjectAction_Average(), label);
            dataSetBuilderAverage.add(performanceReport.get90Line(),
                Messages.ProjectAction_Line90(), label);
          }
          nbBuildsToAnalyze--;
          continue;
        }
        ChartUtil.generateGraph(request, response,
            createRespondingTimeChart(dataSetBuilderAverage.build()), 400, 200);
  }
    
    public void doThroughputGraph(StaplerRequest request,
      StaplerResponse response) throws IOException {
    	PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
          if (getPerformanceReportList().size() == 1) {
            performanceReportNameFile = getPerformanceReportList().get(0);
          } else {
            return;
          }
        }
        if (ChartUtil.awtProblemCause != null) {
          // not available. send out error message
          response.sendRedirect2(request.getContextPath() + "/images/headless.png");
          return;
        }
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (AbstractBuild<?, ?> build : builds) {
          if (buildsLimits.in(nbBuildsToAnalyze)) {
        	
        	if (!buildsLimits.includedByStep(build.number)){
          		continue;
          	}  
        	    
            NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
            PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
            if (performanceBuildAction == null) {
              continue;
            }
            PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
                performanceReportNameFile);
            if (performanceReport == null) {
              nbBuildsToAnalyze--;
              continue;
            }
            dataSetBuilderAverage.add(performanceReport.getThrougput(), Messages.ProjectAction_Throughput(), label);
          }
          nbBuildsToAnalyze--;
          continue;
        }
        ChartUtil.generateGraph(request, response, createThroughputChart(dataSetBuilderAverage.build()), 400, 200);
  }

    public void doBytesTransferredGraph(StaplerRequest request, StaplerResponse response) throws IOException {
    	PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
          if (getPerformanceReportList().size() == 1) {
            performanceReportNameFile = getPerformanceReportList().get(0);
          } else {
            return;
          }
        }
        if (ChartUtil.awtProblemCause != null) {
          // not available. send out error message
          response.sendRedirect2(request.getContextPath() + "/images/headless.png");
          return;
        }
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (AbstractBuild<?, ?> build : builds) {
          if (buildsLimits.in(nbBuildsToAnalyze)) {
        	
        	if (!buildsLimits.includedByStep(build.number)){
          		continue;
          	}  
        	    
            NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
            PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
            if (performanceBuildAction == null) {
              continue;
            }
            PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
                performanceReportNameFile);
            if (performanceReport == null) {
              nbBuildsToAnalyze--;
              continue;
            }
            dataSetBuilderAverage.add(performanceReport.getAverageBytesTransferred()/1024.00,
            		Messages.ProjectAction_BytesTransferred(), label);
          }
          nbBuildsToAnalyze--;
          continue;
        }
        ChartUtil.generateGraph(request, response,
        		createBytesTransferredChart(dataSetBuilderAverage.build()), 400, 200);
  }

    public void doBytesTransferredGraphPerTestCase(StaplerRequest request, StaplerResponse response) throws IOException {
    	PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
          if (getPerformanceReportList().size() == 1) {
            performanceReportNameFile = getPerformanceReportList().get(0);
          } else {
            return;
          }
        }
        if (ChartUtil.awtProblemCause != null) {
          // not available. send out error message
          response.sendRedirect2(request.getContextPath() + "/images/headless.png");
          return;
        }
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (AbstractBuild<?, ?> build : builds) {
          if (buildsLimits.in(nbBuildsToAnalyze)) {
        	
        	if (!buildsLimits.includedByStep(build.number)){
          		continue;
          	}  
        	    
            NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
            PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
            if (performanceBuildAction == null) {
              continue;
            }
            PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
                performanceReportNameFile);
            if (performanceReport == null) {
              nbBuildsToAnalyze--;
              continue;
            }
            for (UriReport uriReport : performanceReport.getUriReportMap().values()) {
              dataSetBuilderAverage.add(uriReport.getAverageBytesTransferred()/1024.00,
            		   uriReport.getUri(), label);
            }
          }
          nbBuildsToAnalyze--;
          continue;
        }
        ChartUtil.generateGraph(request, response,
        		createBytesTransferredChart(dataSetBuilderAverage.build()), 400, 200);
  }

  public void doSummarizerGraph(StaplerRequest request,
                                StaplerResponse response) throws IOException {

        PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
      request.bindParameters(performanceReportPosition);
      String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
      if (performanceReportNameFile == null) {
        if (getPerformanceReportList().size() == 1) {
          performanceReportNameFile = getPerformanceReportList().get(0);
        } else {
          return;
        }
      }
      if (ChartUtil.awtProblemCause != null) {
        // not available. send out error message
        //response.sendRedirect2(request.getContextPath() + "/images/headless.png");
        return;
      }
      DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderSummarizer = new DataSetBuilder<String, NumberOnlyBuildLabel>();
      DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderSummarizerThroughput = new DataSetBuilder<String, NumberOnlyBuildLabel>();
      DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderSummarizerErrors = new DataSetBuilder<String, NumberOnlyBuildLabel>();
      
      List<?> builds = getProject().getBuilds();
      Range buildsLimits = getFirstAndLastBuild(request, builds);

      int nbBuildsToAnalyze = builds.size();
      for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
        AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
        if (buildsLimits.in(nbBuildsToAnalyze)) {
          NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
          PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
          if (performanceBuildAction == null) {
            continue;
          }
          PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
              performanceReportNameFile);


          if (performanceReport == null) {
            nbBuildsToAnalyze--;
            continue;
          }

          for (String key:performanceReport.getUriReportMap().keySet()) {
            // UriReport.getAverage() or UriReport.getHttpSampleList().get(0).getDuration is same thing as there would
            // be only one HttpSample when parsing Summarizer log
            Long methodAvg = performanceReport.getUriReportMap().get(key).getAverage();
            Long methodMin = performanceReport.getUriReportMap().get(key).getHttpSampleList().get(0).getSummarizerMin();
            Long methodMax = performanceReport.getUriReportMap().get(key).getHttpSampleList().get(0).getSummarizerMax();
            Double tp = performanceReport.getUriReportMap().get(key).getHttpSampleList().get(0).getThroughput();
            float methodErrors= (float) performanceReport.getUriReportMap().get(key).getHttpSampleList().get(0).getSummarizerErrors();
            dataSetBuilderSummarizer.add(methodAvg, "Avg", label);
            dataSetBuilderSummarizer.add(methodMin, "Min", label);
            dataSetBuilderSummarizer.add(methodMax, "Max", label);
            dataSetBuilderSummarizerThroughput.add(tp, "Throughput", label);
            dataSetBuilderSummarizerErrors.add(methodErrors, "%" + Messages.ProjectAction_Errors(), label);
          };
        }

       nbBuildsToAnalyze--;
      }

      
      String summarizerReportType = performanceReportPosition.getSummarizerReportType();
      if (summarizerReportType.equalsIgnoreCase("error")) {
        ChartUtil.generateGraph(request, response,
        createSummarizerChart(dataSetBuilderSummarizerErrors.build(),"%",Messages.ProjectAction_PercentageOfErrors()), 400, 200);
      } else if(summarizerReportType.equalsIgnoreCase("throughput")) {
        ChartUtil.generateGraph(request, response,
        createSummarizerChart(dataSetBuilderSummarizerThroughput.build(),"\\s",Messages.ProjectAction_Throughput()), 400, 200);
      } else {
        ChartUtil.generateGraph(request, response,
        createSummarizerChart(dataSetBuilderSummarizer.build(),"ms",Messages.ProjectAction_RespondingTime()), 400, 200);
      }

  }


  /**
   * <p>
   * give a list of two Integer : the smallest build to use and the biggest.
   * </p>
   * 
   * @param request
   * @param builds
   * @return outList
   */
  private Range getFirstAndLastBuild(StaplerRequest request, List<?> builds) {
    Range range = new Range();
    GraphConfigurationDetail graphConf = (GraphConfigurationDetail) createUserConfiguration(request);

    if (graphConf.isNone()) {
          return all(builds);
    }

    if (graphConf.isBuildCount()) {
      if (graphConf.getBuildCount() <= 0) {
          return all(builds);
      } else {
          int first = builds.size() - graphConf.getBuildCount();
          return new Range( first > 0 ? first + 1 : 1,
                            builds.size());
      }
    } else if (graphConf.isBuildNth()){
    	if (graphConf.getBuildStep() <= 0){
    		return all(builds);
    	} else {
    		return new Range(1, builds.size(), graphConf.getBuildStep());
    	}
    } else if (graphConf.isDate()) {
      if (graphConf.isDefaultDates()) {
          return all(builds);
      } else {
        int firstBuild = -1;
        int lastBuild = -1;
        int var = builds.size();
        GregorianCalendar firstDate = null;
        GregorianCalendar lastDate = null;
        try {
          firstDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getFirstDayCount());
          lastDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getLastDayCount());
          lastDate.set(GregorianCalendar.HOUR_OF_DAY, 23);
          lastDate.set(GregorianCalendar.MINUTE, 59);
          lastDate.set(GregorianCalendar.SECOND, 59);
        } catch (ParseException e) {
          LOGGER.log(Level.SEVERE, "Error during the manage of the Calendar", e);
        }
        for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
          AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
          GregorianCalendar buildDate = new GregorianCalendar();
          buildDate.setTime(currentBuild.getTimestamp().getTime());
          if (firstDate.getTime().before(buildDate.getTime())) {
            firstBuild = var;
          }
          if (lastBuild < 0 && lastDate.getTime().after(buildDate.getTime())) {
            lastBuild = var;
          }
          var--;
        }
        return new Range(firstBuild,lastBuild);
      }
    }
    throw new IllegalArgumentException("unsupported configType + " + graphConf.getConfigType());
  }

  public Range all(List<?> builds) {
      return new Range(1, builds.size());
  }

  public AbstractProject<?, ?> getProject() {
    return project;
  }

  public List<String> getPerformanceReportList() {
    this.performanceReportList = new ArrayList<String>(0);
    if (null == this.project) {
      return performanceReportList;
    }
    if (null == this.project.getSomeBuildWithWorkspace()) {
      return performanceReportList;
    }
    File file = new File(this.project.getSomeBuildWithWorkspace().getRootDir(),
        PerformanceReportMap.getPerformanceReportDirRelativePath());
    if (!file.isDirectory()) {
      return performanceReportList;
    }

    for (File entry : file.listFiles()) {
      if (entry.isDirectory()) {
        for (File e : entry.listFiles()) {
          this.performanceReportList.add(e.getName());
        }
      } else {
        this.performanceReportList.add(entry.getName());
      }
        
    }

    Collections.sort(performanceReportList);

    return this.performanceReportList;
  }

  public void setPerformanceReportList(List<String> performanceReportList) {
    this.performanceReportList = performanceReportList;
  }

  public boolean isTrendVisibleOnProjectDashboard() {
    if (getPerformanceReportList() != null
        && getPerformanceReportList().size() == 1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns the graph configuration for this project.
   * 
   * @param link
   *            not used
   * @param request
   *            Stapler request
   * @param response
   *            Stapler response
   * @return the dynamic result of the analysis (detail page).
   */
  public Object getDynamic(final String link, final StaplerRequest request,
      final StaplerResponse response) {
    if (CONFIGURE_LINK.equals(link)) {
      return createUserConfiguration(request);
    } else if (TRENDREPORT_LINK.equals(link)) {
      return createTrendReport(request);
    } else if (TESTSUITE_LINK.equals(link)) {
    	return createTestsuiteReport(request, response); 
    } else {
      return null;
    }
  }

  /**
   * Creates a view to configure the trend graph for the current user.
   * 
   * @param request
   *            Stapler request
   * @return a view to configure the trend graph for the current user
   */
  private Object createUserConfiguration(final StaplerRequest request) {
    GraphConfigurationDetail graph = new GraphConfigurationDetail(project,
        PLUGIN_NAME, request);
    return graph;
  }

  /**
   * Creates a view to configure the trend graph for the current user.
   * 
   * @param request
   *            Stapler request
   * @return a view to configure the trend graph for the current user
   */
  private Object createTrendReport(final StaplerRequest request) {
    String filename = getTrendReportFilename(request);
    CategoryDataset dataSet = getTrendReportData(request, filename).build();
    TrendReportDetail report = new TrendReportDetail(project, PLUGIN_NAME,
        request, filename, dataSet);
    return report;
  }

  private Object createTestsuiteReport(final StaplerRequest request, final StaplerResponse response){
	  String filename = getTestSuiteReportFilename(request);
	  	    
	  List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
	  Range buildsLimits = getFirstAndLastBuild(request, builds);
	  
	  TestSuiteReportDetail report = new TestSuiteReportDetail(project, PLUGIN_NAME,
        request, filename, buildsLimits);
	 	  
	  return report;
  }
  
  private String getTrendReportFilename(final StaplerRequest request) {
    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);
    return performanceReportPosition.getPerformanceReportPosition();
  }
  
  private String getTestSuiteReportFilename(final StaplerRequest request) {
	    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
	    request.bindParameters(performanceReportPosition);
	    return performanceReportPosition.getPerformanceReportPosition();
	  }

  private DataSetBuilder getTrendReportData(final StaplerRequest request,
      String performanceReportNameFile) {

    DataSetBuilder<String, NumberOnlyBuildLabel> dataSet = new DataSetBuilder<String, NumberOnlyBuildLabel>();
    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
    Range buildsLimits = getFirstAndLastBuild(request, builds);

    int nbBuildsToAnalyze = builds.size();
    for (AbstractBuild<?, ?> currentBuild : builds) {
      if (buildsLimits.in(nbBuildsToAnalyze)) {
        NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
        PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
        if (performanceBuildAction == null) {
          continue;
        }
        PerformanceReport report = null;
        report = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
            performanceReportNameFile);
        if (report == null) {
          nbBuildsToAnalyze--;
          continue;
        }
        dataSet.add(Math.round(report.getAverage()),
            Messages.ProjectAction_Average(), label);
        dataSet.add(Math.round(report.getMedian()),
            Messages.ProjectAction_Median(), label);
        dataSet.add(Math.round(report.get90Line()),
            Messages.ProjectAction_Line90(), label);
        dataSet.add(Math.round(report.getMin()),
            Messages.ProjectAction_Minimum(), label);
        dataSet.add(Math.round(report.getMax()),
            Messages.ProjectAction_Maximum(), label);
        dataSet.add(Math.round(report.errorPercent()),
            Messages.ProjectAction_PercentageOfErrors(), label);
        dataSet.add(Math.round(report.countErrors()),
            Messages.ProjectAction_Errors(), label);
      }
      nbBuildsToAnalyze--;
    }
    return dataSet;
  }
  public boolean ifSummarizerParserUsed(String filename) {

      boolean b = false;
      String  fileExt="";

      List<PerformanceReportParser> list =  project.getPublishersList().get(PerformancePublisher.class).getParsers();

      for ( int i=0; i < list.size(); i++) {
           if (list.get(i).getDescriptor().getDisplayName()=="JmeterSummarizer") {
              fileExt = list.get(i).glob;
              String parts[] = fileExt.split("\\s*[;:,]+\\s*");
              for (String path : parts) {
                if (filename.endsWith(path.substring(5))) {
                    b=true;
                }    
              }
           }
      }

   return b;
  }
  
  public boolean ifModePerformancePerTestCaseUsed(){
	  return project.getPublishersList().get(PerformancePublisher.class).isModePerformancePerTestCase();
  }


  public static class Range {

      public int first;

      public int last;
      
      public int step;

      private Range() {
      }
      
      public Range(int first, int last) {
          this.first = first;
          this.last = last;
          this.step = 1;
      }
      
      public Range(int first, int last, int step){
    	  this(first, last);
    	  this.step = step;
      }

      public boolean in(int nbBuildsToAnalyze) {
          return nbBuildsToAnalyze <= last
              && first <= nbBuildsToAnalyze;
      }
      
      public boolean includedByStep(int buildNumber){
    	  if (buildNumber % step == 0) {
    		  return true;
    	  }
    	  return false;
      }
      
  }
}
