package com.nmk.junitmcp.junit_platform;

import com.nmk.junitmcp.dto.CoverageResult;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CoverageAnalyzer {
    private final ExecFileLoader loader;
    private final String classesDirectory;

    public CoverageAnalyzer(ExecFileLoader loader, String classesDirectory) {
        this.loader = loader;
        this.classesDirectory = classesDirectory;
    }

    public Map<String, CoverageResult> analyze() throws IOException {
        Map<String, CoverageResult> results = new HashMap<>();

        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);

        // analyze compiled classes
        File classesDir = new File(classesDirectory);
        analyzer.analyzeAll(classesDir);

        for (IClassCoverage cc : coverageBuilder.getClasses()) {
            int covered = cc.getLineCounter().getCoveredCount();
            int total = cc.getLineCounter().getTotalCount();
            double percent = total == 0 ? 0.0 : (covered * 100.0 / total);

            CoverageResult cr = new CoverageResult();
            cr.setCoveredLines(covered);
            cr.setTotalLines(total);
            cr.setCoveragePercent(percent);

            results.put(cc.getName().replace("/", "."), cr);
        }

        return results;
    }
}
