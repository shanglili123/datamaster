
import vue from "@vitejs/plugin-vue";

import createAutoImport from "./auto-import";
import createSvgIcon from "./svg-icon";
import createCompression from "./compression";
import createSetupExtend from "./setup-extend";
// import { visualizer } from "rollup-plugin-visualizer";

export default function createVitePlugins(viteEnv, isBuild = false) {
  const vitePlugins = [createDevHomeFallback(), vue()];
  vitePlugins.push(createAutoImport());
  vitePlugins.push(createSetupExtend());
  vitePlugins.push(createSvgIcon(isBuild));
  // vitePlugins.push(
  //   visualizer({
  //     gzipSize: true,
  //     brotliSize: true,
  //     filename: "err.html", // 默认在项目根目录下生成stats.html文件，可自定义
  //     open: true, //生成后自动打开浏览器查看
  //   }),
  // );
  isBuild && vitePlugins.push(...createCompression(viteEnv));
  return vitePlugins;
}

function createDevHomeFallback() {
  return {
    name: "datamaster-dev-home-fallback",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.method !== "GET" || !/^\/dev-api\/home(?:\?|$)/.test(req.url || "")) {
          next();
          return;
        }

        res.statusCode = 200;
        res.setHeader("Content-Type", "application/json;charset=utf-8");
        res.end(
          JSON.stringify({
            code: 200,
            msg: "操作成功",
            data: {
              integrationTaskTotal: 0,
              integrationTaskFailed: 0,
              developTaskTotal: 0,
              developTaskFailed: 0,
              apiCallTotal: 0,
              apiCallFailed: 0,
              tableRows: [],
            },
          }),
        );
      });
    },
  };
}

