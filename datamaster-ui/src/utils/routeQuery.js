export function parseRouteQuery(routeQuery) {
  if (!routeQuery) {
    return undefined;
  }
  if (typeof routeQuery === "object" && !Array.isArray(routeQuery)) {
    return routeQuery;
  }
  if (typeof routeQuery !== "string") {
    return undefined;
  }

  const candidates = [routeQuery, routeQuery.replace(/\\"/g, '"')];
  for (const candidate of candidates) {
    const parsed = parseRouteQueryCandidate(candidate);
    if (parsed) {
      return parsed;
    }
  }
  return undefined;
}

function parseRouteQueryCandidate(value) {
  let current = value;
  for (let i = 0; i < 2; i += 1) {
    try {
      current = JSON.parse(current);
    } catch {
      return undefined;
    }
    if (current && typeof current === "object" && !Array.isArray(current)) {
      return current;
    }
    if (typeof current !== "string") {
      return undefined;
    }
  }
  return undefined;
}
