
import { Histogram, Opportunity, DataAnalysis } from "@element-plus/icons-vue";

export const CHAT_TYPES = [
  {
    value: "chart",
    label: "智能图表",
    icon: Histogram,
    disabled: false,
  },
  {
    value: "smart",
    label: "智能问答",
    icon: Opportunity,
    disabled: true,
  },
  {
    value: "askData",
    label: "AI 问数",
    icon: DataAnalysis,
    disabled: false,
  },
];

