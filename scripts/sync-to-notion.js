const fs = require('fs');
const path = require('path');

const NOTION_TOKEN = process.env.NOTION_TOKEN ? process.env.NOTION_TOKEN.trim() : "";
const RAW_DATABASE_ID = process.env.NOTION_DATABASE_ID ? process.env.NOTION_DATABASE_ID.trim() : "";

if (!NOTION_TOKEN || !RAW_DATABASE_ID) {
    console.error("Error: NOTION_TOKEN or NOTION_DATABASE_ID environment variable is missing.");
    process.exit(1);
}

const cleanId = RAW_DATABASE_ID.replace(/[^a-zA-Z0-9]/g, '');

if (cleanId.length !== 32) {
    console.error(`Error: 정제된 DATABASE_ID의 길이가 32자가 아닙니다. (현재 추출된 길이: ${cleanId.length}자)`);
    console.error(`GitHub Secrets에 저장된 값에 오타나 누락이 없는지 꼭 확인해 주세요. 추출된 값: ${cleanId}`);
    process.exit(1);
}

const DATABASE_ID = `${cleanId.substring(0, 8)}-${cleanId.substring(8, 12)}-${cleanId.substring(12, 16)}-${cleanId.substring(16, 20)}-${cleanId.substring(20)}`;

const jsonPath = path.join(__dirname, '../apps/backend/build/openapi.json');

if (!fs.existsSync(jsonPath)) {
    console.error("Warning: openapi.json does not exist. Skipping Notion sync.");
    process.exit(0);
}

const openapi = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

async function syncToNotion() {
    const paths = openapi.paths || {};
    console.log(`Starting Notion API specification sync to Database [${DATABASE_ID}]...`);

    for (const [pathKey, pathObj] of Object.entries(paths)) {
        for (const [method, operation] of Object.entries(pathObj)) {
            const summary = operation.summary || operation.description || "No description";

            const isAdmin = pathKey.includes('admin') ||
                summary.includes('관리자') ||
                (operation.operationId || '').toLowerCase().includes('admin');
            const authValue = isAdmin ? "관리자" : "사용자";

            try {
                const response = await fetch('https://api.notion.com/v1/pages', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${NOTION_TOKEN}`,
                        'Notion-Version': '2022-06-28',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        parent: { database_id: DATABASE_ID },
                        properties: {
                            'Entry Point': { title: [{ text: { content: pathKey } }] },
                            'Method': { select: { name: method.toUpperCase() } },
                            'Description': { rich_text: [{ text: { content: summary } }] },
                            'Done': { checkbox: false },
                            'Auth': { select: { name: authValue } }
                        }
                    })
                });

                if (!response.ok) {
                    const errLog = await response.json();
                    console.error(`[Failed] ${method.toUpperCase()} ${pathKey} sync error:`, errLog.message);
                } else {
                    console.log(`[Success] Notion sync completed: ${method.toUpperCase()} ${pathKey} [Auth: ${authValue}]`);
                }
            } catch (error) {
                console.error(`[Network Error] ${method.toUpperCase()} ${pathKey}:`, error.message);
            }
        }
    }
}

syncToNotion();