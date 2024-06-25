import "reflect-metadata";

import pty from 'node-pty';
import {Container, injectable, inject, named} from "inversify";
import {Ollama} from "ollama";
import {OpenAI} from "openai";

process.chdir(process.env.BUILD_WORKSPACE_DIRECTORY);

const Tasks = {
    DesignDocumentWriting: Symbol.for("DesignDocumentWriting"),
    DesignDocumentCritique: Symbol.for("DesignDocumentCritique"),
    LinuxConsoleSession: Symbol.for("LinuxConsoleSession"),
    CodeGeneration: Symbol.for("CodeGeneration"),
    CodeCritique: Symbol.for("CodeCritique")
};

// skipBaseClassChecks allows wrapping external classes for injection without modification
const container: Container = new Container({skipBaseClassChecks: true});

@injectable()
abstract class Model {
    abstract generateText(prompt: string, model?: string): Promise<string>;
}

enum OpenAiModels {
    gpt_4o = "gpt-4o",
    gpt_4_turbo = "gpt-4-turbo",
    gpt_3_5_turbo = "gpt-3.5-turbo"
}

const OpenAiParams = {
    Host: Symbol.for("OpenAiHost"),
    ModelName: Symbol.for("OpenAiModelName"),
    ApiKey: Symbol.for("OpenAiAPIKey")
}

@injectable()
class OpenAiServer extends OpenAI {
    constructor(
        @inject(OpenAiParams.Host) host: string,
        @inject(OpenAiParams.ApiKey) apiKey: string
    ) {
        super({apiKey});
    }
}

@injectable()
class OpenAiModel extends Model {
    private readonly server: OpenAiServer;
    private readonly modelName: string;

    constructor(
        @inject(OpenAiServer) server: OpenAiServer,
        @inject(OpenAiParams.ModelName) modelName: string,
    ) {
        super();
        this.server = server;
        this.modelName = modelName;
    }

    async generateText(prompt: string): Promise<string> {
        return (await this.server.chat.completions.create({
            model: this.modelName,
            messages: [{role: "user", content: prompt}]
        })).choices[0].message.content;
    }
}

container.bind(OpenAiServer).toSelf();

container.bind(OpenAiParams.Host)
    .toConstantValue("http://10.255.255.6:8085");

container.bind(OpenAiParams.ApiKey)
    .toConstantValue(process.env.OPENAI_API_KEY);

container.bind(OpenAiParams.ModelName)
    .toConstantValue(OpenAiModels.gpt_4o)
    .whenParentNamed(Tasks.LinuxConsoleSession);


enum OllamaModels {
    llama3_8b_instruct_fp16 = "llama3:8b-instruct-fp16",
    llama3_70b_instruct_q5_K_M = "llama3:70b-instruct-q5_K_M"
}

const OllamaParams = {
    Host: Symbol.for("OllamaHost"),
    ModelName: Symbol.for("OllamaModelName"),
};

@injectable()
class OllamaServer extends Ollama {
    constructor(@inject(OllamaParams.Host) host: string) {
        super({host});
    }
}

@injectable()
class OllamaModel extends Model {
    private readonly server: OllamaServer;
    private readonly modelName: string;

    constructor(
        @inject(OllamaServer) server: OllamaServer,
        @inject(OllamaParams.ModelName) modelName: string,
    ) {
        super();
        this.server = server;
        this.modelName = modelName;
    }

    override async generateText(prompt: string): Promise<string> {
        return (await this.server.chat({
            model: this.modelName,
            messages: [{role: 'user', content: prompt}],
            options: {}
        })).message.content;
    }
}

container.bind(OllamaServer).toSelf();

container.bind(OllamaParams.ModelName)
    .toConstantValue(OllamaModels.llama3_70b_instruct_q5_K_M)
    .whenParentNamed(Tasks.LinuxConsoleSession);

container.bind(OllamaParams.Host)
    .toConstantValue("http://10.255.255.6:8085");


// container.bind(Model).to(OpenAiModel)
//     .whenTargetNamed(Tasks.LinuxConsoleSession);

container.bind(Model).to(OllamaModel)
    .whenTargetNamed(Tasks.LinuxConsoleSession);

let context = `# You are an AI agent tasked with analyzing and critiquing software code. You have access to a Linux terminal with a bash shell. Your goal is to review the source code of a gRPC web framework called logos and provide a detailed critique.

# Instructions:
# 1. Navigate to the project directory: cd /src/logos
# 2. Use ls to discover what files exist, and cat to read the file's contents
# 3. After reading each file, append your analysis to critique.txt using cat with a heredoc. Only critique files that exist.
# 4. Focus on both framework code in /src/logos/dev and app code in /src/logos/apps
# 5. Provide specific, actionable advice about the source code
# 6. Be thorough in your analysis - there is no time limit

# Important:
# - Use only basic shell commands (cd, ls, cat, echo)
# - Never use multi-line shell commands. Only use one-liners.
# - Avoid any commands that could get the terminal stuck. You cannot use modifier keys, so you should not run any command which requires ctrl-d or ctrl-c to finish
# - Do not use text editors or commands requiring modifier keys
# - Write all non-command text as shell comments (preceded by #)
# - Do NOT use markdown formatting or code blocks
# - Use cat with heredoc to create or append to files

# Example of using cat with heredoc to append to critique.txt:
# cat << EOF >> critique.txt
# Your analysis text here
# Multiple lines are fine
# EOF

# Begin your analysis now. Remember to think carefully before executing any command.
`;

interface Agent {
    run(): Promise<void>;
}

@injectable()
class ConsoleAgent implements Agent {
    @inject(Model) @named(Tasks.LinuxConsoleSession) private model: Model;

    private promptAwaitsInput(data: string): boolean {
        const
            lastNewlinePos = data.lastIndexOf('\n'),
            lastLine = lastNewlinePos >= 0 ? data.slice(lastNewlinePos + 1) : data;
        return /root@logos_[a-zA-Z0-9_]+:\S+ \$ $/.test(lastLine);
    }

    async run(): Promise<void> {
        const ptyProcess = pty.spawn("/bin/bash", [
            "-c",
            "bazel run --ui_event_filters=-info,-stdout,-stderr --noshow_progress //dev/logos/author:shell"
        ], {
            name: 'xterm',
            cols: 128,
            rows: 64,
            env: process.env
        });

        process.stdout.write(context);

        ptyProcess.onData((data) => {
            process.stdout.write(data);
            context += data;
            context = context.slice(-5000);

            //console.debug("data", {data});
            if (this.promptAwaitsInput(data)) {
                this.model.generateText(context).then((response) => {
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

        try {
            return await new Promise<void>((resolve, reject) => {
                ptyProcess.onExit(({exitCode, signal}) => {
                    console.log(`PTY process exited with code ${exitCode} and signal ${signal}`);
                    exitCode ? reject() : resolve();
                });

                console.log('PTY created. You can now interact with the shell.');
                console.log('Press Ctrl+C to exit.');
            });
        } finally {
            ptyProcess.kill();
            process.stdin.setRawMode(false);
            process.exit();
        }
    }
}


container.bind(ConsoleAgent).toSelf();

await container.get(ConsoleAgent).run()
    .catch((reason) =>
        console.error(reason));
