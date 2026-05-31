const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const PG_HOST = '127.0.0.1';
const PG_PORT = 5432;
const PG_SUPERUSER = 'qdata';
const PG_SUPERPASSWORD = 'qdata';
const PG_USER = 'datamaster';
const PG_PASSWORD = 'datamaster';
const NEW_DB = 'data_master';
const SQL_FILE = path.join(__dirname, 'V1.5.0_PG.sql');
const CONFIG_FILE = path.join(__dirname, '..', '..', '..', 'datamaster-server', 'src', 'main', 'resources', 'application-dev.yml');

async function createDatabase() {
  console.log(`Connecting to PostgreSQL at ${PG_HOST}:${PG_PORT}...`);
  const client = new Client({
    host: PG_HOST, port: PG_PORT,
    user: PG_SUPERUSER, password: PG_SUPERPASSWORD,
    database: 'postgres',
  });
  await client.connect();
  console.log('Connected.');

  const res = await client.query(`SELECT 1 FROM pg_database WHERE datname = $1`, [NEW_DB]);
  if (res.rows.length === 0) {
    console.log(`Creating database "${NEW_DB}"...`);
    await client.query(`CREATE DATABASE "${NEW_DB}"`);
    console.log('Done.');
  } else {
    console.log(`Database "${NEW_DB}" already exists.`);
  }
  await client.end();
}

async function grantPermissions() {
  console.log('Granting permissions to datamaster user...');
  const client = new Client({
    host: PG_HOST, port: PG_PORT,
    user: PG_SUPERUSER, password: PG_SUPERPASSWORD,
    database: NEW_DB,
  });
  await client.connect();
  await client.query(`GRANT ALL ON SCHEMA public TO "${PG_USER}"`);
  await client.query(`GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "${PG_USER}"`);
  await client.query(`ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "${PG_USER}"`);
  await client.query(`ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "${PG_USER}"`);
  console.log('Permissions granted.');
  await client.end();
}

async function runSqlFile() {
  console.log(`Executing SQL file: ${SQL_FILE}`);
  const client = new Client({
    host: PG_HOST, port: PG_PORT,
    user: PG_SUPERUSER, password: PG_SUPERPASSWORD,
    database: NEW_DB,
  });
  await client.connect();

  const sql = fs.readFileSync(SQL_FILE, 'utf8');
  const statements = sql
    .split(/;\s*\n/)
    .map(s => s.trim())
    .filter(s => s.length > 0 && !s.startsWith('--'));

  console.log(`Found ${statements.length} SQL statements.`);

  let success = 0, failed = 0;
  for (let i = 0; i < statements.length; i++) {
    try {
      await client.query(statements[i]);
      success++;
      if (success % 50 === 0) process.stdout.write(`\r  Executed ${success}/${statements.length}...`);
    } catch (err) {
      failed++;
      if (failed <= 5) {
        console.error(`\nError #${failed} at stmt ${i + 1}: ${err.message.substring(0, 150)}`);
        console.error(`  SQL: ${statements[i].substring(0, 100)}...`);
      }
    }
  }
  console.log(`\nDone: ${success} succeeded, ${failed} failed.`);
  await client.end();
}

async function updateConfig() {
  console.log(`Updating config: ${CONFIG_FILE}`);
  let content = fs.readFileSync(CONFIG_FILE, 'utf8');
  const oldUrl = 'jdbc:postgresql://127.0.0.1:5432/datamaster?currentSchema=public';
  const newUrl = 'jdbc:postgresql://127.0.0.1:5432/data_master?currentSchema=public';
  if (content.includes(oldUrl)) {
    content = content.replace(oldUrl, newUrl);
    fs.writeFileSync(CONFIG_FILE, content, 'utf8');
    console.log('Config updated: datamaster → data_master');
  } else {
    console.log('URL pattern not found in config.');
  }
}

async function main() {
  try {
    await createDatabase();
    await grantPermissions();
    await runSqlFile();
    await updateConfig();
    console.log('\nAll done!');
  } catch (err) {
    console.error('Fatal error:', err.message);
    process.exit(1);
  }
}

main();
