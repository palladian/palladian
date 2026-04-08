const fs = require('fs');

const commonJsPath = 'C:/Users/User/AppData/Local/Google/Chrome/User Data/Default/Extensions/fihnjjcciajhdojfnbdddfaoknhalnja/3.5.1_0/data/js/common.js';
const common5JsPath = 'C:/Users/User/AppData/Local/Google/Chrome/User Data/Default/Extensions/fihnjjcciajhdojfnbdddfaoknhalnja/3.5.1_0/data/js/common5.js';
const common3JsPath = 'C:/Users/User/AppData/Local/Google/Chrome/User Data/Default/Extensions/fihnjjcciajhdojfnbdddfaoknhalnja/3.5.1_0/data/js/common3.js';
const common6JsPath = 'C:/Users/User/AppData/Local/Google/Chrome/User Data/Default/Extensions/fihnjjcciajhdojfnbdddfaoknhalnja/3.5.1_0/data/js/common6.js';

let rules = {
    searchPairs: {}, // specific element removal rules
    hostRules: {},   // per-host logic (clicks, etc)
    cookies: {},     // cookies to set
    storage: {}      // local/session storage to set
};

const parseArgs = (str) => {
    try {
        return eval('[' + str + ']');
    } catch (e) {
        return null;
    }
};

// Process common.js (searchPairs)
try {
    if (fs.existsSync(commonJsPath)) {
        const commonContent = fs.readFileSync(commonJsPath, 'utf8');
        const match = commonContent.match(/let\s+searchPairs\s*=\s*(\{[\s\S]*?\});/);
        if (match) {
            const objStr = match[1];
            // Eval is safe here as we are parsing the extension's source code which is text
            // We trust the input files from the user's extension folder
            const searchPairs = eval('(' + objStr + ')');
            rules.searchPairs = searchPairs;
        }
    } else {
        console.log('common.js not found');
    }
} catch (e) {
    console.error('Error processing common.js', e);
}

// Process common3.js (Storage/Cookies)
try {
    if (fs.existsSync(common3JsPath)) {
        const content = fs.readFileSync(common3JsPath, 'utf8');
        const lines = content.split('\n');
        let currentHosts = [];

        for (const line of lines) {
            const caseMatch = line.match(/^\s*case\s+'([^']+)':/);
            if (caseMatch) {
                currentHosts.push(caseMatch[1]);
                continue;
            }

            if (currentHosts.length === 0) continue;

            const returnMatch = line.match(/^\s*return\s+(\{.*?\}|\[.*?\]);/);
            if (returnMatch) {
                try {
                    const val = eval('(' + returnMatch[1] + ')');
                    // console.log(`Found storage rule for ${currentHosts}:`, val);
                    const items = Array.isArray(val) ? val : [val];

                    currentHosts.forEach(host => {
                        rules.storage[host] = items;
                    });
                } catch (e) {
                }
                currentHosts = [];
            }

            if (line.match(/^\s*return\s+/) || line.match(/^\s*break;/)) {
                currentHosts = [];
            }
        }
    }
} catch (e) {
    console.error('Error processing common3.js', e);
}

// Process common6.js (Cookies simple)
try {
    if (fs.existsSync(common6JsPath)) {
        const content = fs.readFileSync(common6JsPath, 'utf8');
        const lines = content.split('\n');
        let currentHosts = [];

        for (const line of lines) {
            const caseMatch = line.match(/^\s*case\s+'([^']+)':/);
            if (caseMatch) {
                currentHosts.push(caseMatch[1]);
                continue;
            }

            if (currentHosts.length === 0) continue;

            const returnMatch = line.match(/^\s*return\s+(\[.*?\]);/);
            if (returnMatch) {
                try {
                    const val = eval('(' + returnMatch[1] + ')');
                    currentHosts.forEach(host => {
                        rules.cookies[host] = val;
                    });
                } catch (e) {
                }
                currentHosts = [];
            }

            if (line.match(/^\s*return\s+/) || line.match(/^\s*break;/)) {
                currentHosts = [];
            }
        }
    }
} catch (e) {
    console.error('Error processing common6.js', e);
}

// Process common5.js (Host Rules - selectors, chains, if/else)
try {
    if (fs.existsSync(common5JsPath)) {
        const common5Content = fs.readFileSync(common5JsPath, 'utf8');
        const lines = common5Content.split('\n');
        let currentHosts = [];

        for (const line of lines) {
            const caseMatch = line.match(/^\s*case\s+'([^']+)':/);
            if (caseMatch) {
                currentHosts.push(caseMatch[1]);
                continue;
            }

            if (currentHosts.length === 0) continue;

            // return 'selector';
            const returnStrMatch = line.match(/^\s*return\s+(['"])(.*?)\1;/);
            if (returnStrMatch) {
                const selector = returnStrMatch[2];
                currentHosts.forEach(host => {
                    rules.hostRules[host] = {type: 'selector', value: selector};
                });
                currentHosts = [];
                continue;
            }

            // return _chain(...)
            const returnChainMatch = line.match(/^\s*return\s+_chain\((.*?)\);/);
            if (returnChainMatch) {
                const args = parseArgs(returnChainMatch[1]);
                if (args) {
                    currentHosts.forEach(host => {
                        rules.hostRules[host] = {type: 'chain', values: args};
                    });
                }
                currentHosts = [];
                continue;
            }

            // return _if(cond, ...)
            const returnIfMatch = line.match(/^\s*return\s+_if\((.*?)\);/);
            if (returnIfMatch) {
                const args = parseArgs(returnIfMatch[1]);
                if (args && args.length >= 2) {
                    const condition = args[0];
                    const trueAction = args.slice(1);
                    currentHosts.forEach(host => {
                        rules.hostRules[host] = {type: 'if', condition: condition, trueAction: trueAction};
                    });
                }
                currentHosts = [];
                continue;
            }

            // return _if_else(cond, if_sel, else_sel)
            const returnIfElseMatch = line.match(/^\s*return\s+_if_else\((.*?)\);/);
            if (returnIfElseMatch) {
                const args = parseArgs(returnIfElseMatch[1]);
                if (args && args.length >= 3) {
                    const condition = args[0];
                    const trueAction = args[1];
                    const falseAction = args[2];
                    currentHosts.forEach(host => {
                        rules.hostRules[host] = {
                            type: 'if_else',
                            condition: condition,
                            trueAction: Array.isArray(trueAction) ? trueAction : [trueAction],
                            falseAction: Array.isArray(falseAction) ? falseAction : [falseAction]
                        };
                    });
                }
                currentHosts = [];
                continue;
            }

            if (line.match(/^\s*return\s+/) || line.match(/^\s*break;/)) {
                currentHosts = [];
            }
        }
    }
} catch (e) {
    console.error('Error processing common5.js', e);
}

// Manually adding adblock warnings as general rules if possible, or they might be in searchPairs?
// The user asked to remove ad-blocker warnings too.
// Common adblock warning text/classes:
// The "I don't care about cookies" extension might not strictly cover ad-blocker warnings, but
// let's look at searchPairs again.
// searchPairs has keys that are usually selectors or text? In common.js keys are DOM conditions/selectors.

fs.writeFileSync('cookie_rules.json', JSON.stringify(rules, null, 2));
console.log('cookie_rules.json generated with ' + Object.keys(rules.hostRules).length + ' host rules and ' + Object.keys(rules.searchPairs).length + ' search pairs.');
