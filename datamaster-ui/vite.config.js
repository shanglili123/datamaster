
import { defineConfig, loadEnv } from "vite";
import path from "path";
import createVitePlugins from "./vite/plugins";

const API_PREFIX_COMPAT = {
  ast: "da",
  col: "dpp",
  svc: "ds",
  mdl: "dm",
  std: "dp",
  tax: "att",
};

const CATALOG_GOVERNANCE_API_PREFIXES = new Set([
  "dataCategory",
  "dataCategoryCat",
  "dataLevel",
  "standardsDesensitizeList",
  "desensitizeInterval",
  "desensitizeRules",
  "sensitiveLevel",
  "dataElemCat",
  "dataElem",
  "desensitizeUserRel",
  "desensitizeWhitelist",
]);

function rewriteDevApiPath(p) {
  const pathWithoutBase = p.replace(/^\/dev-api/, "");
  const catalogPath = pathWithoutBase.match(/^\/cat(?:\/([^/?#]+))?/);
  if (catalogPath) {
    const oldPrefix = CATALOG_GOVERNANCE_API_PREFIXES.has(catalogPath[1]) ? "dg" : "mc";
    return pathWithoutBase.replace(/^\/cat(?=\/|$|\?)/, `/${oldPrefix}`);
  }
  return pathWithoutBase.replace(
    /^\/(ast|col|svc|mdl|std|tax)(?=\/|$|\?)/,
    (match, prefix) => `/${API_PREFIX_COMPAT[prefix]}`,
  );
}

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd());
  const { VITE_APP_ENV } = env;
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.dataMaster.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.dataMaster.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === "production" ? "/" : "/",
    plugins: createVitePlugins(env, command === "build"),
    build: {
      rollupOptions: {
        input: {
          main: path.resolve(__dirname, "index.html"),
          // nested: path.resolve(__dirname, "login/index.html"),
        },
        output: {
          manualChunks(id) {
            if (id.includes("node_modules")) {
              return id
                .toString()
                .split("node_modules/")[1]
                .split("/")[0]
                .toString();
            }
          },
        },
      },
    },
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        "~": path.resolve(__dirname, "./"),
        // 设置别名
        "@": path.resolve(__dirname, "./src"),
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"],
    },
    // vite 相关配置
    server: {
      port: 81,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        "/dev-api": {
          target: "http://localhost:8080",
          changeOrigin: true,
          rewrite: rewriteDevApiPath,
        },
        "/dev-ai": {
          target: "http://localhost:8087",
          // target: "http://192.168.20.115:8080",
          // target: "https://dataMaster-pro.qiantong.tech/prod-api/",
          // target: "http://110.42.38.62:30001/prod-api/",
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-ai/, ""),
        },
        "/jmreport": {
          target: "http://localhost:8080",
          changeOrigin: true,
        },
        "/v3/api-docs": {
          target: "http://localhost:8080",
          changeOrigin: true,
          rewrite: (p) => p.replace("", ""),
        },
      },
    },
    //fix:error:stdin>:7356:1: warning: "@charset" must be the first rule in the file
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: "internal:charset-removal",
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === "charset") {
                  atRule.remove();
                }
              },
            },
          },
        ],
      },
    },
  };
});

