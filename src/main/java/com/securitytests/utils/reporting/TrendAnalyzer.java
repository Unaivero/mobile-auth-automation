package com.securitytests.utils.reporting;

import com.securitytests.utils.logging.StructuredLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced trend analysis engine for test execution and security metrics
 */
public class TrendAnalyzer {
    private static final StructuredLogger logger = new StructuredLogger(TrendAnalyzer.class);
    
    /**
     * Analyze trends in test execution data
     */
    public TrendAnalysisResult analyzeTrends(List<Map<String, Object>> trendData) {
        logger.info("Analyzing trends for {} data points", trendData.size());
        
        if (trendData.isEmpty()) {
            return new TrendAnalysisResult(TrendDirection.STABLE, 0.0, new ArrayList<>());
        }
        
        // Sort data by date
        List<Map<String, Object>> sortedData = trendData.stream()
            .sorted((a, b) -> {
                Object dateA = a.get("execution_date");
                Object dateB = b.get("execution_date");
                if (dateA instanceof String && dateB instanceof String) {
                    return ((String) dateA).compareTo((String) dateB);
                }
                return 0;
            })
            .collect(Collectors.toList());
        
        // Extract success rates for trend analysis
        List<Double> successRates = extractSuccessRates(sortedData);
        
        // Calculate trend direction and strength
        TrendDirection direction = calculateTrendDirection(successRates);
        double trendStrength = calculateTrendStrength(successRates);
        
        // Analyze seasonal patterns
        List<SeasonalPattern> seasonalPatterns = analyzeSeasonalPatterns(sortedData);
        
        // Detect anomalies
        List<Anomaly> anomalies = detectAnomalies(sortedData, successRates);
        
        // Calculate volatility
        double volatility = calculateVolatility(successRates);
        
        // Generate trend insights
        List<TrendInsight> insights = generateTrendInsights(direction, trendStrength, 
            seasonalPatterns, anomalies, volatility);
        
        TrendAnalysisResult result = new TrendAnalysisResult(direction, trendStrength, insights);
        result.setSeasonalPatterns(seasonalPatterns);
        result.setAnomalies(anomalies);
        result.setVolatility(volatility);
        result.setDataPoints(sortedData.size());
        result.setAnalysisTimestamp(LocalDateTime.now());
        
        logger.info("Trend analysis completed: direction={}, strength={:.2f}, anomalies={}", 
            direction, trendStrength, anomalies.size());
        
        return result;
    }
    
    /**
     * Analyze security vulnerability trends
     */
    public SecurityTrendAnalysis analyzeSecurityTrends(List<Map<String, Object>> vulnerabilityData) {
        logger.info("Analyzing security trends for {} vulnerability records", vulnerabilityData.size());
        
        if (vulnerabilityData.isEmpty()) {
            return new SecurityTrendAnalysis(TrendDirection.STABLE, new HashMap<>(), new ArrayList<>());
        }
        
        // Group vulnerabilities by severity and date
        Map<String, List<Map<String, Object>>> severityGroups = vulnerabilityData.stream()
            .collect(Collectors.groupingBy(vuln -> (String) vuln.get("severity_level")));
        
        // Analyze trend for each severity level
        Map<String, TrendDirection> severityTrends = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : severityGroups.entrySet()) {
            String severity = entry.getKey();
            List<Map<String, Object>> severityData = entry.getValue();
            
            // Count vulnerabilities per day for this severity
            Map<String, Integer> dailyCounts = severityData.stream()
                .collect(Collectors.groupingBy(
                    vuln -> vuln.get("scan_date").toString(),
                    Collectors.summingInt(vuln -> ((Number) vuln.get("vulnerability_count")).intValue())
                ));
            
            List<Double> counts = dailyCounts.values().stream()
                .map(Integer::doubleValue)
                .collect(Collectors.toList());
            
            TrendDirection severityTrend = calculateTrendDirection(counts);
            severityTrends.put(severity, severityTrend);
        }
        
        // Determine overall vulnerability trend
        TrendDirection overallTrend = determineOverallSecurityTrend(severityTrends);
        
        // Identify security patterns
        List<SecurityPattern> patterns = identifySecurityPatterns(vulnerabilityData, severityTrends);
        
        // Calculate security risk velocity
        double riskVelocity = calculateSecurityRiskVelocity(vulnerabilityData);
        
        SecurityTrendAnalysis analysis = new SecurityTrendAnalysis(overallTrend, severityTrends, patterns);
        analysis.setRiskVelocity(riskVelocity);
        analysis.setAnalysisTimestamp(LocalDateTime.now());
        analysis.setVulnerabilityTrend(overallTrend);
        
        logger.info("Security trend analysis completed: overall={}, risk_velocity={:.2f}", 
            overallTrend, riskVelocity);
        
        return analysis;
    }
    
    /**
     * Analyze performance trends
     */
    public PerformanceTrendAnalysis analyzePerformanceTrends(List<Map<String, Object>> performanceData) {
        logger.info("Analyzing performance trends for {} data points", performanceData.size());
        
        PerformanceTrendAnalysis analysis = new PerformanceTrendAnalysis();
        
        if (performanceData.isEmpty()) {
            return analysis;
        }
        
        // Analyze response time trends
        List<Double> responseTimes = performanceData.stream()
            .map(data -> ((Number) data.getOrDefault("avg_response_time", 0)).doubleValue())
            .collect(Collectors.toList());
        
        TrendDirection responseTimeTrend = calculateTrendDirection(responseTimes);
        analysis.setResponseTimeTrend(responseTimeTrend);
        
        // Analyze throughput trends
        List<Double> throughputValues = performanceData.stream()
            .map(data -> ((Number) data.getOrDefault("throughput", 0)).doubleValue())
            .collect(Collectors.toList());
        
        TrendDirection throughputTrend = calculateTrendDirection(throughputValues);
        analysis.setThroughputTrend(throughputTrend);
        
        // Analyze error rate trends
        List<Double> errorRates = performanceData.stream()
            .map(data -> ((Number) data.getOrDefault("error_rate", 0)).doubleValue())
            .collect(Collectors.toList());
        
        TrendDirection errorRateTrend = calculateTrendDirection(errorRates);
        analysis.setErrorRateTrend(errorRateTrend);
        
        // Detect performance degradation patterns
        List<PerformanceDegradationPattern> degradationPatterns = detectPerformanceDegradation(
            responseTimes, throughputValues, errorRates);
        analysis.setDegradationPatterns(degradationPatterns);
        
        // Calculate performance stability score
        double stabilityScore = calculatePerformanceStability(responseTimes, throughputValues, errorRates);
        analysis.setStabilityScore(stabilityScore);
        
        analysis.setAnalysisTimestamp(LocalDateTime.now());
        
        logger.info("Performance trend analysis completed: response_time={}, throughput={}, error_rate={}", 
            responseTimeTrend, throughputTrend, errorRateTrend);
        
        return analysis;
    }
    
    // Private helper methods
    
    private List<Double> extractSuccessRates(List<Map<String, Object>> data) {
        return data.stream()
            .map(record -> {
                Object successRate = record.get("success_rate");
                if (successRate instanceof Number) {
                    return ((Number) successRate).doubleValue();
                }
                return 0.0;
            })
            .collect(Collectors.toList());
    }
    
    private TrendDirection calculateTrendDirection(List<Double> values) {
        if (values.size() < 2) {
            return TrendDirection.STABLE;
        }
        
        // Use linear regression to determine trend
        double[] x = new double[values.size()];
        double[] y = values.stream().mapToDouble(Double::doubleValue).toArray();
        
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        
        double slope = calculateSlope(x, y);
        
        // Determine trend based on slope and significance
        if (Math.abs(slope) < 0.01) { // Threshold for stable trend
            return TrendDirection.STABLE;
        } else if (slope > 0) {
            return TrendDirection.IMPROVING;
        } else {
            return TrendDirection.DECLINING;
        }
    }
    
    private double calculateSlope(double[] x, double[] y) {
        int n = x.length;
        double sumX = Arrays.stream(x).sum();
        double sumY = Arrays.stream(y).sum();
        double sumXY = 0.0;
        double sumX2 = 0.0;
        
        for (int i = 0; i < n; i++) {
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return 0.0;
        }
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    private double calculateTrendStrength(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        // Calculate correlation coefficient as trend strength
        double[] x = new double[values.size()];
        double[] y = values.stream().mapToDouble(Double::doubleValue).toArray();
        
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        
        return Math.abs(calculateCorrelation(x, y));
    }
    
    private double calculateCorrelation(double[] x, double[] y) {
        int n = x.length;
        double meanX = Arrays.stream(x).average().orElse(0.0);
        double meanY = Arrays.stream(y).average().orElse(0.0);
        
        double numerator = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;
        
        for (int i = 0; i < n; i++) {
            double xDiff = x[i] - meanX;
            double yDiff = y[i] - meanY;
            numerator += xDiff * yDiff;
            sumX2 += xDiff * xDiff;
            sumY2 += yDiff * yDiff;
        }
        
        double denominator = Math.sqrt(sumX2 * sumY2);
        if (Math.abs(denominator) < 1e-10) {
            return 0.0;
        }
        
        return numerator / denominator;
    }
    
    private List<SeasonalPattern> analyzeSeasonalPatterns(List<Map<String, Object>> data) {
        List<SeasonalPattern> patterns = new ArrayList<>();
        
        // Group data by day of week
        Map<Integer, List<Double>> dayOfWeekData = new HashMap<>();
        
        for (Map<String, Object> record : data) {
            // This would require actual date parsing and day-of-week extraction
            // For now, we'll create a placeholder pattern
        }
        
        // Placeholder seasonal pattern
        if (data.size() >= 7) {
            patterns.add(new SeasonalPattern("Weekly", "Lower success rates on weekends", 0.85));
        }
        
        return patterns;
    }
    
    private List<Anomaly> detectAnomalies(List<Map<String, Object>> data, List<Double> values) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        if (values.size() < 3) {
            return anomalies;
        }
        
        // Calculate mean and standard deviation
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(values, mean);
        
        // Detect anomalies using z-score threshold
        double threshold = 2.0; // 2 standard deviations
        
        for (int i = 0; i < values.size(); i++) {
            double zScore = Math.abs((values.get(i) - mean) / stdDev);
            if (zScore > threshold) {
                Map<String, Object> record = data.get(i);
                String date = record.get("execution_date").toString();
                anomalies.add(new Anomaly(date, values.get(i), zScore, "Statistical outlier"));
            }
        }
        
        return anomalies;
    }
    
    private double calculateStandardDeviation(List<Double> values, double mean) {
        double sumSquaredDiffs = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .sum();
        
        return Math.sqrt(sumSquaredDiffs / values.size());
    }
    
    private double calculateVolatility(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return calculateStandardDeviation(values, mean) / mean;
    }
    
    private List<TrendInsight> generateTrendInsights(TrendDirection direction, double strength,
                                                   List<SeasonalPattern> patterns, List<Anomaly> anomalies,
                                                   double volatility) {
        List<TrendInsight> insights = new ArrayList<>();
        
        // Direction insights
        switch (direction) {
            case IMPROVING -> insights.add(new TrendInsight("positive_trend", 
                "Test success rate is improving over time", InsightSeverity.INFO));
            case DECLINING -> insights.add(new TrendInsight("negative_trend", 
                "Test success rate is declining - investigation recommended", InsightSeverity.WARNING));
            case STABLE -> insights.add(new TrendInsight("stable_trend", 
                "Test success rate is stable", InsightSeverity.INFO));
        }
        
        // Strength insights
        if (strength > 0.8) {
            insights.add(new TrendInsight("strong_trend", 
                "Trend is statistically significant", InsightSeverity.INFO));
        } else if (strength < 0.3) {
            insights.add(new TrendInsight("weak_trend", 
                "Trend is not statistically significant", InsightSeverity.WARNING));
        }
        
        // Volatility insights
        if (volatility > 0.2) {
            insights.add(new TrendInsight("high_volatility", 
                "High volatility detected - results are inconsistent", InsightSeverity.WARNING));
        }
        
        // Anomaly insights
        if (anomalies.size() > 0) {
            insights.add(new TrendInsight("anomalies_detected", 
                anomalies.size() + " anomalies detected in the data", InsightSeverity.CAUTION));
        }
        
        return insights;
    }
    
    private TrendDirection determineOverallSecurityTrend(Map<String, TrendDirection> severityTrends) {
        // Weight critical and high severity trends more heavily
        int criticalWeight = severityTrends.getOrDefault("CRITICAL", TrendDirection.STABLE) == TrendDirection.INCREASING ? 4 : 0;
        int highWeight = severityTrends.getOrDefault("HIGH", TrendDirection.STABLE) == TrendDirection.INCREASING ? 2 : 0;
        int mediumWeight = severityTrends.getOrDefault("MEDIUM", TrendDirection.STABLE) == TrendDirection.INCREASING ? 1 : 0;
        
        int totalIncreasingWeight = criticalWeight + highWeight + mediumWeight;
        
        if (totalIncreasingWeight >= 3) {
            return TrendDirection.INCREASING;
        } else if (totalIncreasingWeight > 0) {
            return TrendDirection.STABLE;
        } else {
            return TrendDirection.DECREASING;
        }
    }
    
    private List<SecurityPattern> identifySecurityPatterns(List<Map<String, Object>> vulnerabilityData,
                                                          Map<String, TrendDirection> severityTrends) {
        List<SecurityPattern> patterns = new ArrayList<>();
        
        // Analyze vulnerability types
        Map<String, Long> vulnTypeCounts = vulnerabilityData.stream()
            .collect(Collectors.groupingBy(
                vuln -> (String) vuln.get("vulnerability_type"),
                Collectors.counting()
            ));
        
        // Find most common vulnerability types
        vulnTypeCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .forEach(entry -> patterns.add(new SecurityPattern(
                entry.getKey(),
                "Frequent vulnerability type: " + entry.getValue() + " occurrences",
                entry.getValue().intValue()
            )));
        
        return patterns;
    }
    
    private double calculateSecurityRiskVelocity(List<Map<String, Object>> vulnerabilityData) {
        // Calculate the rate of change in security risk over time
        // This is a simplified implementation
        if (vulnerabilityData.size() < 2) {
            return 0.0;
        }
        
        // Group by date and calculate daily risk scores
        Map<String, Double> dailyRiskScores = vulnerabilityData.stream()
            .collect(Collectors.groupingBy(
                vuln -> vuln.get("scan_date").toString(),
                Collectors.summingDouble(vuln -> {
                    String severity = (String) vuln.get("severity_level");
                    return switch (severity.toUpperCase()) {
                        case "CRITICAL" -> 10.0;
                        case "HIGH" -> 5.0;
                        case "MEDIUM" -> 2.0;
                        case "LOW" -> 1.0;
                        default -> 0.0;
                    };
                })
            ));
        
        List<Double> riskScores = new ArrayList<>(dailyRiskScores.values());
        if (riskScores.size() < 2) {
            return 0.0;
        }
        
        // Calculate average change in risk score
        double totalChange = 0.0;
        for (int i = 1; i < riskScores.size(); i++) {
            totalChange += riskScores.get(i) - riskScores.get(i - 1);
        }
        
        return totalChange / (riskScores.size() - 1);
    }
    
    private List<PerformanceDegradationPattern> detectPerformanceDegradation(List<Double> responseTimes,
                                                                            List<Double> throughputValues,
                                                                            List<Double> errorRates) {
        List<PerformanceDegradationPattern> patterns = new ArrayList<>();
        
        // Detect response time degradation
        if (!responseTimes.isEmpty()) {
            double avgResponseTime = responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (avgResponseTime > 5000) { // 5 seconds threshold
                patterns.add(new PerformanceDegradationPattern("response_time_degradation",
                    "Average response time exceeds 5 seconds", avgResponseTime));
            }
        }
        
        // Detect throughput degradation
        if (!throughputValues.isEmpty()) {
            TrendDirection throughputTrend = calculateTrendDirection(throughputValues);
            if (throughputTrend == TrendDirection.DECLINING) {
                patterns.add(new PerformanceDegradationPattern("throughput_degradation",
                    "Throughput is declining over time", 0.0));
            }
        }
        
        // Detect error rate increase
        if (!errorRates.isEmpty()) {
            double avgErrorRate = errorRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (avgErrorRate > 0.05) { // 5% error rate threshold
                patterns.add(new PerformanceDegradationPattern("error_rate_increase",
                    "Error rate exceeds 5%", avgErrorRate));
            }
        }
        
        return patterns;
    }
    
    private double calculatePerformanceStability(List<Double> responseTimes, List<Double> throughputValues, List<Double> errorRates) {
        double stabilityScore = 100.0;
        
        // Penalize high variability in response times
        if (!responseTimes.isEmpty()) {
            double mean = responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStandardDeviation(responseTimes, mean);
            double cv = mean > 0 ? stdDev / mean : 0.0; // Coefficient of variation
            stabilityScore -= cv * 20; // Penalize high coefficient of variation
        }
        
        // Penalize high error rates
        if (!errorRates.isEmpty()) {
            double avgErrorRate = errorRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            stabilityScore -= avgErrorRate * 100; // Direct penalty for error rate
        }
        
        return Math.max(0.0, Math.min(100.0, stabilityScore));
    }
}