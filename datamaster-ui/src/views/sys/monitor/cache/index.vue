<template>
  <div class="app-container">
    <a-row :gutter="15">
      <a-col :span="24" class="card-box">
        <a-card>
          <template #title><MonitorOutlined style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">基本信息</span></template>
          <div class="monitor-table">
            <table cellspacing="0" style="width: 100%">
              <tbody>
                <tr>
                  <td ><div class="cell">Redis版本</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.redis_version }}</div></td>
                  <td ><div class="cell">运行模式</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.redis_mode == "standalone" ? "单机" : "集群" }}</div></td>
                  <td ><div class="cell">端口</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.tcp_port }}</div></td>
                  <td ><div class="cell">客户端数</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.connected_clients }}</div></td>
                </tr>
                <tr>
                  <td ><div class="cell">运行时间(天)</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.uptime_in_days }}</div></td>
                  <td ><div class="cell">使用内存</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.used_memory_human }}</div></td>
                  <td ><div class="cell">使用CPU</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ parseFloat(cache.info.used_cpu_user_children).toFixed(2) }}</div></td>
                  <td ><div class="cell">内存配置</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.maxmemory_human }}</div></td>
                </tr>
                <tr>
                  <td ><div class="cell">AOF是否开启</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.aof_enabled == "0" ? "否" : "是" }}</div></td>
                  <td ><div class="cell">RDB是否成功</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.rdb_last_bgsave_status }}</div></td>
                  <td ><div class="cell">Key数量</div></td>
                  <td ><div class="cell" v-if="cache.dbSize">{{ cache.dbSize }} </div></td>
                  <td ><div class="cell">网络入口/出口</div></td>
                  <td ><div class="cell" v-if="cache.info">{{ cache.info.instantaneous_input_kbps }}kps/{{cache.info.instantaneous_output_kbps}}kps</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </a-card>
      </a-col>

      <a-col :span="12" class="card-box bottom">
        <a-card>
          <template #title><PieChartOutlined style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">命令统计</span></template>
          <div class="monitor-table">
            <div ref="commandstats" style="height: 420px" />
          </div>
        </a-card>
      </a-col>

      <a-col :span="12" class="card-box bottom">
        <a-card>
          <template #title><DashboardOutlined style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">内存信息</span></template>
          <div class="monitor-table">
            <div ref="usedmemory" style="height: 420px" />
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup name="Cache">

import { getCache } from '@/api/system/monitor/cache.js';

import { MonitorOutlined, PieChartOutlined, DashboardOutlined } from '@ant-design/icons-vue';

import * as echarts from 'echarts';

const cache = ref([]);
const commandstats = ref(null);
const usedmemory = ref(null);
const { proxy } = getCurrentInstance();

function getList() {
  proxy.$modal.loading("正在加载缓存监控数据，请稍候！");
  getCache().then(response => {
    proxy.$modal.closeLoading();
    cache.value = response.data;

    const commandstatsIntance = echarts.init(commandstats.value, "macarons");
    commandstatsIntance.setOption({
      tooltip: {
        trigger: "item",
        formatter: "{a} <br/>{b} : {c} ({d}%)"
      },
      series: [
        {
          name: "命令",
          type: "pie",
          roseType: "radius",
          radius: [15, 95],
          center: ["50%", "38%"],
          data: response.data.commandStats,
          animationEasing: "cubicInOut",
          animationDuration: 1000
        }
      ]
    });
    const usedmemoryInstance = echarts.init(usedmemory.value, "macarons");
    usedmemoryInstance.setOption({
      tooltip: {
        formatter: "{b} <br/>{a} : " + cache.value.info.used_memory_human
      },
      series: [
        {
          name: "峰值",
          type: "gauge",
          min: 0,
          max: 1000,
          detail: {
            formatter: cache.value.info.used_memory_human
          },
          data: [
            {
              value: parseFloat(cache.value.info.used_memory_human),
              name: "内存消耗"
            }
          ]
        }
      ]
    })
    window.addEventListener("resize", () => {
      commandstatsIntance.resize();
      usedmemoryInstance.resize();
    });
  })
}

getList();
</script>
<style lang="scss" scoped>
.bottom {
  margin-bottom: 0;
}

.monitor-table {
  width: 100%;

  table {
    width: 100%;
    border-collapse: collapse;
  }

  th, td {
    border: 1px solid #f0f0f0;
    padding: 8px 12px;
    text-align: left;
    font-size: 13px;
    color: #3f4a5a;
  }

  thead th {
    background: #fafafa;
    font-weight: 600;
    color: #2f3a4a;
  }

  tbody tr:hover td {
    background: #f6faff;
  }

  .cell {
    line-height: 23px;
  }
}
</style>

