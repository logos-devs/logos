import pty from 'node-pty';
import {generateText} from 'ai';
import {createOllama} from 'ollama-ai-provider';


process.chdir(process.env.BUILD_WORKSPACE_DIRECTORY);

const
    provider = createOllama({baseURL: "http://10.255.255.6:8085"}),
    model = provider("llama3:70b-instruct-q5_K_M"),
    // model = provider("llama3:8b-instruct-fp16"),
    // model = provider('deepseek-coder-v2:16b-lite-instruct-fp16'),
    // CONTEXT_LENGTH = 8192,
    PROMPT = 'author@logos: $ ';

async function generate(msg: string) {
    return (await generateText({
        model: model,
        prompt: 'Write a vegetarian lasagna recipe for 4 people.',
    })).text;
}

let context = `# You are an AI agent which develops software automatically. You are connected to a real Linux terminal with a busybox environment which allows you to execute commands. You will use these commands to work on the project. You can only issue text commands to the terminal. You cannot issue special escape codes to the terminal, so please be careful not to run commands which will require you to hit modifier keys like ctrl-c or ctrl-d to return to the prompt. We will add that capability to you later. The following is your terminal session. Please use commands to explore the project and make changes as needed. Whenever you want to make plans or think out loud, or say something to the human user, you must write those statements as a shell comment by preceding every new line with # just like I have done with these instructions. DO NOT wrap your commands with backticks. Remember that in this session everything you write will be directly evaluated by the busybox ash shell. Good luck!

`;
const ptyProcess = pty.spawn("/bin/bash", [
    "-c",
    "bazel run --ui_event_filters=-info,-stdout,-stderr --noshow_progress //dev/logos/author:shell sh"
], {
    name: 'xterm',
    cols: 128,
    rows: 64,
    env: process.env
});

ptyProcess.onData((data) => {
    //console.log("onData: ", {data});
    process.stdout.write(data);
    context += data;
    if (data.endsWith(PROMPT)) {
        //console.log("endedWith!!!");
        generate(context).then((response) => {
            const cmd = response.trimEnd() + "\r";
            context += cmd;
            ptyProcess.write(cmd);
        });
    }
});

process.stdin.setRawMode(true);

process.stdin.on('data', (data) => {
    ptyProcess.write(data.toString());
});

await new Promise<void>((resolve, reject) => {
    ptyProcess.onExit(({exitCode, signal}) => {
        console.log(`PTY process exited with code ${exitCode} and signal ${signal}`);
        exitCode ? reject() : resolve();
    });

    console.log('PTY created. You can now interact with the shell.');
    console.log('Press Ctrl+C to exit.');
}).finally(() => {
    ptyProcess.kill();
    process.stdin.setRawMode(false);
    process.exit();
});
