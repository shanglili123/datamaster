
import { BarChartOutlined, BulbOutlined, FundOutlined } from "@ant-design/icons-vue";

export const CHAT_TYPES = [
  {
    value: "chart",
    label: "智能图表",
    icon: BarChartOutlined,
    disabled: false,
  },
  {
    value: "smart",
    label: "智能问答",
    icon: BulbOutlined,
    disabled: true,
  },
  {
    value: "askData",
    label: "AI 问数",
    icon: FundOutlined,
    disabled: false,
  },
];

