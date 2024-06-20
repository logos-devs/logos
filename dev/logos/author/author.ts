import * as childProcess from 'child_process';
import {Ollama} from "ollama";
import * as path from 'path';
import fs from "fs";

process.chdir(process.env.BUILD_WORKSPACE_DIRECTORY);
const busyboxPath = process.argv[2];

console.log(busyboxPath);
process.exit(0);

const server: Ollama = new Ollama({host: "http://10.255.255.6:8085"});

async function generate(msg: string) {
    return await server.chat({
        model: 'llama3:70b-instruct-q5_K_M',
        messages: [
            {role: 'user', content: msg}
        ],
        options: {
            num_ctx: 8192
        }
    });
}

function execToString(cmd: string): string {
    return childProcess.execSync(cmd).toString();
}

function isMatchOrAncestor(queryTarget: string, targetToMatch: string): boolean {
    const queryParts = queryTarget.split(":")[0].split("/");
    const targetParts = targetToMatch.split(":")[0].split("/");

    if (queryTarget === targetToMatch) {
        return true;
    }

    if (queryParts.length < targetParts.length) {
        for (let i = 0; i < queryParts.length; i++) {
            if (queryParts[i] !== targetParts[i]) {
                return false;
            }
        }
        return true;
    }

    return false;
}

function contents(directoryPath: string) {
    let results = [];

    function walk(directory: string) {
        const files = fs.readdirSync(directory);

        files.forEach((file) => {
            const filePath = path.join(directory, file);
            const stats = fs.statSync(filePath);

            if (stats.isFile()) {
                results.push({name: filePath, type: 'F'});
            } else if (stats.isDirectory()) {
                results.push({name: filePath, type: 'D'});
                walk(filePath);
            }
        });
    }

    walk(directoryPath);
    return results;
}


const target = process.argv[2],
    prompt = execToString("bazel query 'kind(prompt, //...)'")
        .split("\n")
        .filter((queryTarget: string) => queryTarget && isMatchOrAncestor(queryTarget, target))
        .map((queryTarget: string) => {
            execToString(`bazel build ${queryTarget}`);
            return queryTarget;
        })
        .map((queryTarget: string) => {
            return fs.readFileSync(`bazel-bin/${queryTarget.replace(/^\/\//, '').replace(/:prompt$/, "/prompt.txt")}`, 'utf8').trim();
        })
        .join("\n"),

    moduleTarget = target.split(":")[0],
    directory = moduleTarget.replace(/^\/\//, ''),
    editor = `$ help
You are an AI agent which develops software automatically. Below is the description of the current bazel
module you are working on. You are connected to a terminal which allows you to execute commands. You will use these
commands to work on the project. You cannot talk to the user. You can only issue commands to the terminal. The following
is your terminal session. Please use commands to explore the project.

${prompt}
 
$ pwd
${directory}

$ bazel build ${moduleTarget}/...
${execToString(`bazel build ${moduleTarget}/... 2>&1`)}

$ ls
${contents(directory).map((item) =>
        `${item.type}:${item.name}`).join('\n')}

$ `;

console.log(editor);
console.log(await generate(editor));
// .forEach((queryTarget: string) => console.log(queryTarget));
