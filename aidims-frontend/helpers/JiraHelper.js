import Helper from '@codeceptjs/helper';
import https from 'https';

class JiraHelper extends Helper {
  _init() {
    console.log('🔌 [Jira Helper] Initialized successfully!');
  }

  async _failed(test) {
    const error = test.err || {};
    const domain = process.env.JIRA_DOMAIN;
    const email = process.env.JIRA_EMAIL;
    const token = process.env.JIRA_TOKEN;
    const projectKey = process.env.JIRA_PROJECT_KEY;

    if (!domain || !email || !token || !projectKey) {
      console.warn('\n⚠️ [Jira Helper] Missing Jira credentials in .env file.');
      return;
    }

    const testCaseName = test.title;
    const summary = `[Bug-CI] Failure on E2E Test: ${testCaseName}`;
    const errorMsg = error.message || 'unknown';
    const errorStack = error.stack || 'unknown';
    const testFile = test.file || 'unknown';

    console.log(`\n🚨 [Jira Helper] Test failed: "${testCaseName}". Creating Jira Bug (ADF format)...`);

    const passOrFailText = `Fail\n\nActual Failure Details:\nError Message:\n${errorMsg}\n\nStack Trace:\n${errorStack}`;

    const body = {
      fields: {
        project: {
          key: projectKey
        },
        summary: summary,
        description: {
          type: 'doc',
          version: 1,
          content: [
            {
              type: 'paragraph',
              content: [
                {
                  type: 'text',
                  text: 'Test case ID*\n',
                  marks: [{ type: 'strong' }]
                },
                {
                  type: 'text',
                  text: 'TC-E2E-FRONTEND - ' + testCaseName + '\n\n'
                },
                {
                  type: 'text',
                  text: 'Test summary/Description*\n',
                  marks: [{ type: 'strong' }]
                },
                {
                  type: 'text',
                  text: `Automated E2E test failure detected in CI/CD pipeline for Frontend E2E tests (CodeceptJS) - ${testCaseName}.\n\n`
                },
                {
                  type: 'text',
                  text: 'Pre-condition\n',
                  marks: [{ type: 'strong' }]
                },
                {
                  type: 'text',
                  text: 'Frontend application is running and accessible.\n\n'
                },
                {
                  type: 'text',
                  text: 'Test steps*\n',
                  marks: [{ type: 'strong' }]
                },
                {
                  type: 'text',
                  text: `1. Run CodeceptJS E2E tests command in the frontend directory: npx codeceptjs run --steps\n2. Verify the E2E test scenario: "${testCaseName}" (in file: ${testFile}) executes and passes successfully.\n\n`
                },
                {
                  type: 'text',
                  text: 'Inputs (test data)\n',
                  marks: [{ type: 'strong' }]
                },
                {
                  type: 'text',
                  text: `- Testing Tool: CodeceptJS with Playwright\n- Command: npx codeceptjs run --steps\n- Test Scenario: ${testCaseName}\n- Test File: ${testFile}\n\n`
                },
                {
                  type: 'text',
                  text: 'Expected result*\n',
                  marks: [{ type: 'strong' }]
                },
                {
                  type: 'text',
                  text: `The E2E test scenario "${testCaseName}" should complete and pass without errors.\n\n`
                },
                {
                  type: 'text',
                  text: 'Pass/Fail\n',
                  marks: [{ type: 'strong' }, { type: 'textColor', attrs: { color: '#DE350B' } }]
                },
                {
                  type: 'text',
                  text: passOrFailText
                }
              ]
            }
          ]
        },
        issuetype: {
          name: 'Bug'
        }
      }
    };

    const cleanDomain = domain.replace(/\/$/, '');
    const hostname = cleanDomain.replace(/^https?:\/\//, '');
    const auth = Buffer.from(`${email}:${token}`).toString('base64');

    const options = {
      hostname: hostname,
      port: 443,
      path: '/rest/api/3/issue',
      method: 'POST',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Content-Length': Buffer.byteLength(JSON.stringify(body))
      }
    };

    return new Promise((resolve) => {
      const req = https.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const parsed = JSON.parse(data);
              console.log(`✅ [Jira Helper] Bug ticket created successfully: ${parsed.key}`);
              resolve(parsed);
            } catch (err) {
              console.error(`❌ [Jira Helper] Failed to parse response:`, err);
              resolve(null);
            }
          } else {
            console.error(`❌ [Jira Helper] Failed to create Jira issue: ${res.statusCode} - ${data}`);
            resolve(null);
          }
        });
      });

      req.on('error', (e) => {
        console.error('❌ [Jira Helper] Request to Jira failed:', e);
        resolve(null);
      });

      req.write(JSON.stringify(body));
      req.end();
    });
  }

  async _passed(test) {
    const domain = process.env.JIRA_DOMAIN;
    const email = process.env.JIRA_EMAIL;
    const token = process.env.JIRA_TOKEN;
    const projectKey = process.env.JIRA_PROJECT_KEY;

    if (!domain || !email || !token || !projectKey) {
      return;
    }

    const testCaseName = test.title;
    const bugSummary = `[Bug-CI] Failure on E2E Test: ${testCaseName}`;
    const cleanDomain = domain.replace(/\/$/, '');
    const hostname = cleanDomain.replace(/^https?:\/\//, '');
    const auth = Buffer.from(`${email}:${token}`).toString('base64');

    const searchJql = `project = "${projectKey}" AND status != "Done" AND status != "Resolved" AND status != "Closed" AND status != "Close"`;
    const encodedJql = encodeURIComponent(searchJql);

    const searchOptions = {
      hostname: hostname,
      port: 443,
      path: `/rest/api/3/search/jql?jql=${encodedJql}&maxResults=100&fields=summary,status`,
      method: 'GET',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Accept': 'application/json'
      }
    };

    const issues = await new Promise((resolve) => {
      const req = https.request(searchOptions, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const parsed = JSON.parse(data);
              resolve(parsed.issues || []);
            } catch (err) {
              console.error(`❌ [Jira Helper] Error parsing search JSON:`, err);
              resolve([]);
            }
          } else {
            console.error(`❌ [Jira Helper] Search API error: ${res.statusCode} - ${data}`);
            resolve([]);
          }
        });
      });
      req.on('error', (err) => {
        console.error(`❌ [Jira Helper] Search request error:`, err);
        resolve([]);
      });
      req.end();
    });

    const matchingIssue = issues.find(issue =>
      issue && issue.fields && issue.fields.summary === bugSummary
    );

    if (!matchingIssue) {
      return;
    }

    const issueKey = matchingIssue.key;
    console.log(`\n🔧 [Jira Helper] Test passed: "${testCaseName}". Found open Jira Bug: ${issueKey}. Resolving/Closing...`);

    const transitionsOptions = {
      hostname: hostname,
      port: 443,
      path: `/rest/api/3/issue/${issueKey}/transitions`,
      method: 'GET',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Accept': 'application/json'
      }
    };

    const transitions = await new Promise((resolve) => {
      const req = https.request(transitionsOptions, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const parsed = JSON.parse(data);
              resolve(parsed.transitions || []);
            } catch (err) {
              resolve([]);
            }
          } else {
            console.error(`❌ [Jira Helper] Transitions error: ${res.statusCode} - ${data}`);
            resolve([]);
          }
        });
      });
      req.on('error', () => resolve([]));
      req.end();
    });

    const doneTransition = transitions.find(t => {
      const name = (t.name || '').toLowerCase();
      return name === 'done' || name === 'resolved' || name === 'close' || name === 'closed';
    });

    if (!doneTransition) {
      console.warn(`⚠️ [Jira Helper] Could not find a suitable transition to close issue ${issueKey}`);
      return;
    }

    const transitionBody = {
      transition: { id: doneTransition.id }
    };

    const transitionOptions = {
      hostname: hostname,
      port: 443,
      path: `/rest/api/3/issue/${issueKey}/transitions`,
      method: 'POST',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Content-Length': Buffer.byteLength(JSON.stringify(transitionBody))
      }
    };

    await new Promise((resolve) => {
      const req = https.request(transitionOptions, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          resolve();
        });
      });
      req.on('error', () => resolve());
      req.write(JSON.stringify(transitionBody));
      req.end();
    });

    const commentBody = {
      body: {
        type: 'doc',
        version: 1,
        content: [{
          type: 'paragraph',
          content: [{
            type: 'text',
            text: `✅ CI/CD update: E2E test "${testCaseName}" has successfully passed in the latest run. This automated issue has been resolved.`
          }]
        }]
      }
    };

    const commentOptions = {
      hostname: hostname,
      port: 443,
      path: `/rest/api/3/issue/${issueKey}/comment`,
      method: 'POST',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Content-Length': Buffer.byteLength(JSON.stringify(commentBody))
      }
    };

    await new Promise((resolve) => {
      const req = https.request(commentOptions, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          console.log(`✅ [Jira Helper] Bug ticket ${issueKey} resolved and commented successfully.`);
          resolve();
        });
      });
      req.on('error', () => resolve());
      req.write(JSON.stringify(commentBody));
      req.end();
    });
  }
}

export default JiraHelper;
