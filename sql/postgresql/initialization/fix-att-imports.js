const fs = require('fs');
const path = require('path');

const root = 'D:\\dev\\dataMaster\\datamaster-assets';

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const e of entries) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (e.name !== 'target') walk(full);
    } else if (e.name.endsWith('.java')) {
      fixFile(full);
    }
  }
}

let changed = 0;
function fixFile(file) {
  let content = fs.readFileSync(file, 'utf8');
  let orig = content;

  content = content.replace(/com\.datamaster\.module\.att\./g, 'com.datamaster.module.taxonomy.');
  content = content.replace(/\bIAttTagAssetRelApiService\b/g, 'ITaxonomyTagAssetRelApiService');
  content = content.replace(/\bIAttTagApiService\b/g, 'ITaxonomyTagApiService');
  content = content.replace(/\bIAttProjectApi\b/g, 'ITaxonomyProjectApi');
  content = content.replace(/\bAttTagAssetRelReqDTO\b/g, 'TaxonomyTagAssetRelReqDTO');
  content = content.replace(/\bAttTagAssetRelRespDTO\b/g, 'TaxonomyTagAssetRelRespDTO');
  content = content.replace(/\bAttProjectReqDTO\b/g, 'TaxonomyProjectReqDTO');
  content = content.replace(/\bAttProjectRespDTO\b/g, 'TaxonomyProjectRespDTO');

  content = content.replace(/com\.datamaster\.module\.model\.api\.service\.businessCategory\b/g, 'com.datamaster.module.modeling.api.service.businessCategory');
  content = content.replace(/com\.datamaster\.module\.model\.api\.service\.dataLayer\b/g, 'com.datamaster.module.modeling.api.service.dataLayer');
  content = content.replace(/com\.datamaster\.module\.model\.api\.service\.themeDomain\b/g, 'com.datamaster.module.modeling.api.service.themeDomain');

  if (content !== orig) {
    fs.writeFileSync(file, content, 'utf8');
    changed++;
  }
}

walk(root);
console.log(`Fixed ${changed} files.`);
