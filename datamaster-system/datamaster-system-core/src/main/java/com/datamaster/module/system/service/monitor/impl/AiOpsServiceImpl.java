package com.datamaster.module.system.service.monitor.impl;

import com.datamaster.common.utils.DateUtils;
import com.datamaster.config.web.domain.Server;
import com.datamaster.config.web.domain.server.SysFile;
import com.datamaster.module.system.controller.admin.monitor.vo.AiOpsReportRespVO;
import com.datamaster.module.system.controller.admin.monitor.vo.AiOpsReportRespVO.AiOpsFindingVO;
import com.datamaster.module.system.controller.admin.monitor.vo.AiOpsReportRespVO.AiOpsLogEventVO;
import com.datamaster.module.system.controller.admin.monitor.vo.AiOpsReportRespVO.AiOpsMetricVO;
import com.datamaster.module.system.service.monitor.IAiOpsService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Rule-based AI ops diagnosis.
 */
@Service
public class AiOpsServiceImpl implements IAiOpsService {

    private static final int MAX_LOG_FILES = 6;

    private static final int MAX_LOG_LINES = 80;

    private static final int MAX_LOG_EVENTS = 12;

    @Override
    public AiOpsReportRespVO diagnose() throws Exception {
        Server server = new Server();
        server.copyTo();

        AiOpsReportRespVO report = new AiOpsReportRespVO();
        report.setDiagnoseTime(DateUtils.getTime());
        report.setServer(server);

        addMetrics(report, server);
        scanLogs(report);
        summarize(report);
        return report;
    }

    private void addMetrics(AiOpsReportRespVO report, Server server) {
        double cpuUsage = 100D - server.getCpu().getFree();
        double memUsage = server.getMem().getUsage();
        double jvmUsage = server.getJvm().getUsage();

        report.getMetrics().add(new AiOpsMetricVO("CPU", formatPercent(cpuUsage), level(cpuUsage, 85D, 70D),
                "CPU 使用率 = 100 - 空闲率"));
        report.getMetrics().add(new AiOpsMetricVO("内存", formatPercent(memUsage), level(memUsage, 85D, 70D),
                "物理内存使用率"));
        report.getMetrics().add(new AiOpsMetricVO("JVM", formatPercent(jvmUsage), level(jvmUsage, 85D, 70D),
                "当前 Java 进程堆内存使用率"));

        addThresholdFinding(report, cpuUsage, 85D, 70D, "CPU 使用率偏高",
                "当前 CPU 使用率 " + formatPercent(cpuUsage),
                "排查高频接口、定时任务、ETL/采集任务是否集中运行，必要时扩容实例或调整调度窗口。");
        addThresholdFinding(report, memUsage, 85D, 70D, "系统内存压力偏高",
                "当前内存使用率 " + formatPercent(memUsage),
                "检查 Java 堆、缓存、批量任务和数据库连接池占用，必要时释放缓存或调整 JVM/容器内存。");
        addThresholdFinding(report, jvmUsage, 85D, 70D, "JVM 堆内存压力偏高",
                "当前 JVM 使用率 " + formatPercent(jvmUsage),
                "关注 Full GC、批量导入和大结果集查询，必要时调大 -Xmx 或优化内存对象生命周期。");

        if (server.getSysFiles() != null) {
            for (SysFile sysFile : server.getSysFiles()) {
                report.getMetrics().add(new AiOpsMetricVO("磁盘 " + sysFile.getDirName(),
                        formatPercent(sysFile.getUsage()), level(sysFile.getUsage(), 90D, 80D),
                        "可用 " + sysFile.getFree() + " / 总量 " + sysFile.getTotal()));
                addThresholdFinding(report, sysFile.getUsage(), 90D, 80D, "磁盘空间不足风险",
                        sysFile.getDirName() + " 已使用 " + formatPercent(sysFile.getUsage()),
                        "清理历史日志、上传临时文件、构建产物和备份文件，或扩容该挂载点。");
            }
        }
    }

    private void scanLogs(AiOpsReportRespVO report) {
        File logDir = new File("logs");
        if (!logDir.exists() || !logDir.isDirectory()) {
            report.getFindings().add(new AiOpsFindingVO("info", "未发现本地日志目录",
                    "当前工作目录下没有 logs 目录。", "如生产环境日志输出到外部目录，可在后续版本增加日志目录配置项。"));
            return;
        }

        File[] files = logDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });

        int scanned = 0;
        for (File file : files) {
            if (scanned >= MAX_LOG_FILES || !file.isFile() || !isLogFile(file)) {
                continue;
            }
            scanned++;
            appendLogEvents(report, file);
            if (report.getLogEvents().size() >= MAX_LOG_EVENTS) {
                break;
            }
        }

        if (!report.getLogEvents().isEmpty()) {
            report.getFindings().add(new AiOpsFindingVO("warn", "近期日志存在异常信号",
                    "扫描到 " + report.getLogEvents().size() + " 条 ERROR/WARN/Exception 片段。",
                    "优先处理最靠前的异常堆栈，结合操作日志和调度任务确认触发入口。"));
        }
    }

    private void appendLogEvents(AiOpsReportRespVO report, File file) {
        List<String> lines;
        try {
            lines = tailLines(file, MAX_LOG_LINES);
        } catch (IOException e) {
            report.getLogEvents().add(new AiOpsLogEventVO(file.getName(), "warn",
                    "日志读取失败：" + e.getMessage()));
            return;
        }

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            String level = detectLevel(line);
            if (level == null) {
                continue;
            }
            report.getLogEvents().add(new AiOpsLogEventVO(file.getName(), level, trim(line, 500)));
            if (report.getLogEvents().size() >= MAX_LOG_EVENTS) {
                return;
            }
        }
    }

    private List<String> tailLines(File file, int limit) throws IOException {
        LinkedList<String> lines = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > limit) {
                    lines.removeFirst();
                }
            }
        } finally {
            reader.close();
        }
        return lines;
    }

    private void summarize(AiOpsReportRespVO report) {
        int score = 100;
        int errorCount = 0;
        int warnCount = 0;
        for (AiOpsFindingVO finding : report.getFindings()) {
            if ("error".equals(finding.getLevel())) {
                score -= 25;
                errorCount++;
            } else if ("warn".equals(finding.getLevel())) {
                score -= 12;
                warnCount++;
            }
        }
        score = Math.max(0, score);
        report.setScore(score);

        if (errorCount > 0 || score < 60) {
            report.setStatus("error");
            report.setSummary("系统存在高风险项，建议立即处理。");
        } else if (warnCount > 0 || score < 85) {
            report.setStatus("warn");
            report.setSummary("系统整体可用，但存在需要关注的运维风险。");
        } else {
            report.setStatus("healthy");
            report.setSummary("系统关键指标正常，未发现明显异常。");
        }

        List<String> suggestions = new ArrayList<String>();
        suggestions.add("保留最近一次诊断结果，发布或升级后先执行一次 AI 运维巡检。");
        suggestions.add("出现 WARN/ERROR 时，先按日志片段定位模块，再结合调度、数据源和接口访问时间线排查。");
        suggestions.add("生产环境建议将日志目录、容器状态、DolphinScheduler 状态纳入诊断数据源。");
        report.setSuggestions(suggestions);
    }

    private void addThresholdFinding(AiOpsReportRespVO report, double value, double errorThreshold, double warnThreshold,
                                     String title, String detail, String suggestion) {
        if (value >= errorThreshold) {
            report.getFindings().add(new AiOpsFindingVO("error", title, detail, suggestion));
        } else if (value >= warnThreshold) {
            report.getFindings().add(new AiOpsFindingVO("warn", title, detail, suggestion));
        }
    }

    private String level(double value, double errorThreshold, double warnThreshold) {
        if (value >= errorThreshold) {
            return "error";
        }
        if (value >= warnThreshold) {
            return "warn";
        }
        return "normal";
    }

    private String detectLevel(String line) {
        if (line == null) {
            return null;
        }
        if (line.contains("ERROR") || line.contains("Exception") || line.contains("失败")) {
            return "error";
        }
        if (line.contains("WARN") || line.contains("告警") || line.contains("警告")) {
            return "warn";
        }
        return null;
    }

    private boolean isLogFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".log") || name.contains(".log.");
    }

    private String formatPercent(double value) {
        return String.format("%.2f%%", value);
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
