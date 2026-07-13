export const REPORT_TEMPLATE_FORMAT = {
  templateCode: 'water_month_report',
  templateName: '月度取水许可分析报告',
  version: 1,
  description: '用于生成水资源取水许可月度分析报告',
  skillBinding: {
    skillId: null,
    skillCode: '',
    knowledgeSpace: 'datamaster-skills'
  },
  dataSchema: {
    required: [
      'title',
      'summary',
      'metrics.validPermitCount',
      'metrics.totalWaterAmount',
      'charts.permitTrend',
      'charts.areaWaterRanking',
      'tables.riskPermits',
      'insights'
    ],
    fields: [
      {
        key: 'title',
        label: '报告标题',
        type: 'string',
        required: true
      },
      {
        key: 'summary',
        label: '报告摘要',
        type: 'string',
        required: true
      },
      {
        key: 'metrics.validPermitCount',
        label: '有效许可证数量',
        type: 'number',
        format: 'integer',
        required: true
      },
      {
        key: 'metrics.totalWaterAmount',
        label: '总取水量',
        type: 'number',
        unit: '万立方米',
        format: 'decimal',
        required: true
      },
      {
        key: 'charts.permitTrend',
        label: '许可数量趋势',
        type: 'array',
        required: true,
        itemSchema: {
          month: 'string',
          count: 'number'
        }
      },
      {
        key: 'charts.areaWaterRanking',
        label: '区域取水量排名',
        type: 'array',
        required: true,
        itemSchema: {
          area: 'string',
          amount: 'number'
        }
      },
      {
        key: 'tables.riskPermits',
        label: '风险许可证明细',
        type: 'array',
        required: false,
        itemSchema: {
          permitNo: 'string',
          enterpriseName: 'string',
          riskReason: 'string'
        }
      },
      {
        key: 'insights',
        label: '分析结论',
        type: 'array',
        required: true,
        itemSchema: 'string'
      }
    ]
  },
  layout: {
    page: {
      width: 'A4',
      padding: '24px',
      background: '#f7f8fa'
    },
    sections: [
      {
        key: 'header',
        type: 'title',
        bind: 'title'
      },
      {
        key: 'summary',
        type: 'paragraph',
        bind: 'summary'
      },
      {
        key: 'coreMetrics',
        type: 'metricGrid',
        columns: 2,
        items: [
          {
            label: '有效许可证数量',
            bind: 'metrics.validPermitCount',
            format: 'integer'
          },
          {
            label: '总取水量',
            bind: 'metrics.totalWaterAmount',
            format: 'decimal',
            unit: '万立方米'
          }
        ]
      },
      {
        key: 'permitTrend',
        type: 'lineChart',
        title: '许可数量趋势',
        bind: 'charts.permitTrend',
        xField: 'month',
        yField: 'count'
      },
      {
        key: 'areaWaterRanking',
        type: 'barChart',
        title: '区域取水量排名',
        bind: 'charts.areaWaterRanking',
        xField: 'area',
        yField: 'amount'
      },
      {
        key: 'riskPermits',
        type: 'table',
        title: '风险许可证明细',
        bind: 'tables.riskPermits',
        columns: [
          { label: '许可证号', prop: 'permitNo' },
          { label: '企业名称', prop: 'enterpriseName' },
          { label: '风险原因', prop: 'riskReason' }
        ]
      },
      {
        key: 'insights',
        type: 'insightList',
        title: '分析结论',
        bind: 'insights'
      }
    ]
  },
  style: {
    theme: {
      primaryColor: '#165dff',
      successColor: '#00b42a',
      warningColor: '#ff7d00',
      textColor: '#1d2129',
      mutedTextColor: '#86909c',
      fontFamily: 'PingFang SC, Microsoft YaHei, sans-serif'
    },
    components: {
      title: {
        fontSize: '24px',
        fontWeight: 600,
        align: 'center',
        color: '#1d2129'
      },
      paragraph: {
        fontSize: '14px',
        lineHeight: 1.8,
        color: '#4e5969'
      },
      metricCard: {
        background: '#ffffff',
        border: '1px solid #e5e6eb',
        borderRadius: '6px'
      },
      chart: {
        height: '260px'
      },
      table: {
        size: 'small',
        border: true
      }
    }
  },
  prompt: {
    instruction: '请基于当前数据库和知识库，为该模板生成结构化报告数据。',
    outputFormat: 'json',
    returnSqlAllowed: true,
    sqlField: 'sql'
  }
}

export function buildReportTemplateFormatText(skill) {
  const template = JSON.parse(JSON.stringify(REPORT_TEMPLATE_FORMAT))
  template.skillBinding.skillId = skill?.id || null
  template.skillBinding.skillCode = skill?.skillCode || ''
  if (skill?.skillCode) {
    template.templateCode = `${skill.skillCode}_report_template`
  }
  if (skill?.skillName) {
    template.templateName = `${skill.skillName}报告模板`
  }
  return JSON.stringify(template, null, 2)
}
