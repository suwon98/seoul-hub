const fs = require('fs');
const path = require('path');

const NOTION_TOKEN = process.env.NOTION_TOKEN;
const DATABASE_ID = process.env.NOTION_DATABASE_ID;

if (!NOTION_TOKEN || !DATABASE_ID) {
    console.error("Error: NOTION_TOKEN or NOTION_DATABASE_ID environment variable is missing.");
    process.exit(1);
}

const jsonPath = path.join(__dirname, '../apps/backend/build/openapi.json');

if (!fs.existsSync(jsonPath)) {
    console.error("Warning: openapi.json does not exist. Skipping Notion sync.");
    process.exit(0);
}

const openapi = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

async function syncToNotion() {
    const paths = openapi.paths || {};
    console.log("Starting Notion API specification sync based on 5 standard columns...");

    for (const [pathKey, pathObj] of Object.entries(paths)) {
        for (const [method, operation] of Object.entries(pathObj)) {
            const summary = operation.summary || operation.description || "No description";

            // 엔드포인트 경로 규칙 및 자산 권한 검증을 통한 Auth 값 분기
            const isAdmin = pathKey.includes('admin') ||
                summary.includes('관리자') ||
                (operation.operationId || '').toLowerCase().includes('admin');
            const authValue = isAdmin ? "관리자" : "사용자";

            try {
                // 지정된 5개 컬럼 스펙에 맞추어 노션 데이터베이스 속성 매핑
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