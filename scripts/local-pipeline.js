const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Đảm bảo thư mục làm việc luôn là thư mục gốc của dự án (project root)
process.chdir(path.resolve(__dirname, '..'));

const COLLECTION_PATH = './tests/aidims_collection.json';
const ENV_PATH = './tests/environment.json';
const REPORT_PATH = './newman-report.json';

console.log(' [CI/CD Local] Bắt đầu quy trình tự động hóa...');

// Dọn dẹp file report cũ nếu có để tránh kết quả bị cũ (stale)
if (fs.existsSync(REPORT_PATH)) {
    try {
        fs.unlinkSync(REPORT_PATH);
    } catch (e) {
        console.warn('⚠️ Cảnh báo: Không thể xóa file report cũ:', e.message);
    }
}

// Dọn dẹp báo cáo unit test Maven cũ để tránh hiển thị lỗi của test case đã bị xóa/đổi tên
const surefireReportsDir = './aidims-backend/target/surefire-reports';
if (fs.existsSync(surefireReportsDir)) {
    try {
        if (fs.rmSync) {
            fs.rmSync(surefireReportsDir, { recursive: true, force: true });
        } else {
            fs.rmdirSync(surefireReportsDir, { recursive: true });
        }
    } catch (e) {
        console.warn('⚠️ Cảnh báo: Không thể xóa thư mục surefire-reports cũ:', e.message);
    }
}


// Helper to parse Maven Surefire reports for Backend Unit Test failures
function parseBackendUtFailures() {
    const reportsDir = './aidims-backend/target/surefire-reports';
    if (!fs.existsSync(reportsDir)) return '[]';

    let failures = [];
    try {
        const files = fs.readdirSync(reportsDir);
        for (const file of files) {
            if (file.endsWith('.txt')) {
                const filepath = path.join(reportsDir, file);
                const content = fs.readFileSync(filepath, 'utf8');
                const lines = content.split(/\r?\n/);
                for (let i = 0; i < lines.length; i++) {
                    const line = lines[i].trim();
                    if (line.startsWith('Tests run:')) continue; // Skip test set summary lines

                    if (line.includes('<<< FAILURE!') || line.includes('<<< ERROR!')) {
                        let testCase = line.split('<<<')[0].trim();
                        if (testCase.includes(' -- ')) {
                            testCase = testCase.split(' -- ')[0];
                        }
                        let errMsg = '';
                        if (i + 1 < lines.length) {
                            errMsg = lines[i + 1].trim();
                        }
                        failures.push({ testCase, errMsg });
                    }
                }
            }
        }
    } catch (e) {
        console.warn('⚠️ Cảnh báo: Không thể parse báo cáo test Backend:', e.message);
    }

    return JSON.stringify(failures);
}

// Helper to parse Jest console output for Frontend Unit Test failures
function parseFrontendUtFailures(error) {
    const output = (error.stdout ? error.stdout.toString() : '') + '\n' + (error.stderr ? error.stderr.toString() : '');
    if (!output.trim()) return '[]';

    const sections = output.split('●');
    if (sections.length <= 1) {
        const lines = output.split('\n');
        return JSON.stringify([{ testCase: 'TC-UT-FRONTEND', errMsg: lines.slice(-15).join('\n').trim() }]);
    }

    let failures = [];
    for (let i = 1; i < sections.length; i++) {
        const lines = sections[i].split('\n');
        const testCase = lines[0].trim();
        let errMsgLines = [];
        for (let j = 1; j < lines.length; j++) {
            const line = lines[j].trim();
            if (line.startsWith('at ')) break;
            if (line) {
                errMsgLines.push(line);
            }
        }
        failures.push({ testCase, errMsg: errMsgLines.join('\n') });
    }

    return JSON.stringify(failures);
}

// 0. Chạy Unit Tests cho cả Backend và Frontend
console.log(' Bước 0: Đang chạy Unit Tests...');
try {
    console.log(' - Đang chạy Backend Unit Tests...');
    const mvnCmd = process.platform === 'win32' ? 'mvnw.cmd clean test' : './mvnw clean test';
    const output = execSync(mvnCmd, { cwd: './aidims-backend', stdio: 'pipe' });
    console.log(output.toString());
    console.log(' - Backend Unit Tests: PASS.');
} catch (error) {
    if (error.stdout) process.stdout.write(error.stdout);
    if (error.stderr) process.stderr.write(error.stderr);
    console.error('  Lỗi: Backend Unit Tests thất bại. Quy trình dừng lại.');
    console.log('  Đang đồng bộ lỗi Backend Unit Test lên Jira...');
    const details = parseBackendUtFailures();
    try {
        execSync('node ./scripts/jira-sync.js', {
            stdio: 'inherit',
            env: { ...process.env, BACKEND_UT_STATUS: 'failed', BACKEND_UT_DETAILS: details }
        });
    } catch (syncError) {
        console.error('  Lỗi khi đồng bộ lên Jira:', syncError.message);
    }
    process.exit(1);
}

try {
    console.log(' - Đang chạy Frontend Unit Tests...');
    const output = execSync('npm test -- --watchAll=false', {
        cwd: './aidims-frontend',
        stdio: 'pipe',
        env: { ...process.env, CI: 'true' }
    });
    console.log(output.toString());
    console.log(' - Frontend Unit Tests: PASS.');
} catch (error) {
    if (error.stdout) process.stdout.write(error.stdout);
    if (error.stderr) process.stderr.write(error.stderr);
    console.error('  Lỗi: Frontend Unit Tests thất bại. Quy trình dừng lại.');
    console.log('  Đang đồng bộ lỗi Frontend Unit Test lên Jira...');
    const details = parseFrontendUtFailures(error);
    try {
        execSync('node ./scripts/jira-sync.js', {
            stdio: 'inherit',
            env: { ...process.env, BACKEND_UT_STATUS: 'passed', FRONTEND_UT_STATUS: 'failed', FRONTEND_UT_DETAILS: details }
        });
    } catch (syncError) {
        console.error('  Lỗi khi đồng bộ lên Jira:', syncError.message);
    }
    process.exit(1);
}

// 1. Kiểm tra các tệp tin cấu hình Postman
if (!fs.existsSync(COLLECTION_PATH)) {
    console.error(` Lỗi: Không tìm thấy file Postman Collection tại ${COLLECTION_PATH}`);
    process.exit(1);
}

// 2. Chạy test Postman bằng Newman
console.log(' Bước 1: Đang chạy API test từ Postman...');
let testSuccess = true;
try {
    const envParam = fs.existsSync(ENV_PATH) ? `-e ${ENV_PATH}` : '';
    execSync(`npx newman run ${COLLECTION_PATH} ${envParam} --reporters cli,json --reporter-json-export ${REPORT_PATH}`, { stdio: 'inherit' });
    console.log(' Bước 1: Tất cả testcases đều PASS.');
} catch (error) {
    console.log(' Bước 1: Phát hiện một số testcases bị thất bại (FAIL).');
    testSuccess = false;
}

// 3. Gọi Script đồng bộ kết quả lên Jira (Log Bug hoặc Auto-Close Bug)
console.log(' Bước 2: Đồng bộ kết quả lên Jira...');
try {
    execSync('node ./scripts/jira-sync.js', {
        stdio: 'inherit',
        env: { ...process.env, BACKEND_UT_STATUS: 'passed', FRONTEND_UT_STATUS: 'passed' }
    });
} catch (error) {
    console.error(' Lỗi khi đồng bộ lên Jira:', error.message);
    process.exit(1);
}

// 4. Quyết định đẩy code lên GitHub (Tự động commit/push khi kiểm thử thành công)
/*
if (testSuccess) {
    console.log(' Bước 3: Test thành công! Đang tiến hành tự động commit và push code...');
    try {
        // Lấy tên branch hiện tại
        const currentBranch = execSync('git rev-parse --abbrev-ref HEAD').toString().trim();
        console.log(` Branch hiện tại: ${currentBranch}`);

        // Chạy lệnh Git add, commit và push
        console.log(' Đang đóng gói (git add & commit)...');
        execSync('git add .', { stdio: 'inherit' });
        
        // Tránh lỗi nếu không có gì thay đổi để commit
        try {
            execSync('git commit -m "auto-commit: API and unit tests passed, syncing code to GitHub"', { stdio: 'inherit' });
        } catch (e) {
            console.log('ℹ Không có thay đổi nào mới để commit.');
        }

        console.log(` Đang đẩy code lên GitHub (git push origin ${currentBranch})...`);
        execSync(`git push origin ${currentBranch}`, { stdio: 'inherit' });
        console.log(' ĐÃ ĐẨY CODE LÊN GITHUB THÀNH CÔNG!');
    } catch (error) {
        console.error('Lỗi khi thực hiện các lệnh Git:', error.message);
        process.exit(1);
    }
} else {
    console.log(' QUY TRÌNH DỪNG LẠI: Phát hiện API test bị lỗi. Code KHÔNG được đẩy lên GitHub.');
    console.log(' Vui lòng kiểm tra lỗi trên bảng Jira, sửa code và chạy lại quy trình.');
    process.exit(1);
}*/
