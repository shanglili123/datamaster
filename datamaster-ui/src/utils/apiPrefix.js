const OLD_TO_NEW_API_PREFIX = {
  da: "ast",
  dpp: "col",
  ds: "svc",
  dm: "mdl",
  mc: "cat",
  dg: "cat",
  dp: "std",
  att: "tax",
};

export function normalizeApiRequestUrl(url) {
  if (!url || typeof url !== "string" || !url.startsWith("/")) {
    return url;
  }

  return url.replace(/^\/(da|dpp|ds|dm|mc|dg|dp|att)(?=\/|$|\?)/, (match, prefix) => {
    return `/${OLD_TO_NEW_API_PREFIX[prefix]}`;
  });
}
