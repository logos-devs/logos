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


container.bind(Model).to(OpenAiModel)
    .whenTargetNamed(Tasks.LinuxConsoleSession);


const
    CONTEXT_LENGTH = 8192,
    PROMPT = 'author@logos: $ ';

let context = `# You are an AI agent which develops software automatically. You are connected to a real Linux terminal with a busybox environment which allows you to execute commands. You will use these commands to work on the project. You can only issue text commands to the terminal. You cannot issue special escape codes to the terminal, so please be careful not to run commands which will require you to hit modifier keys like ctrl-c or ctrl-d to return to the prompt. Always use cat with heredoc to create files. You do not have access to interactive text editors. We will add that capability to you later. The following is your terminal session. Use commands to explore the project and make changes as needed. Whenever you want to make plans or think out loud, or say something to the human user, you must write those statements as a shell comment by preceding every new line with # just like I have done with these instructions. DO NOT wrap your commands with backticks. Remember that in this session everything you write will be directly evaluated by the busybox ash shell. Good luck!

`;

interface Agent {
    run(): Promise<void>;
}

@injectable()
class ConsoleAgent implements Agent {
    @inject(Model) @named(Tasks.LinuxConsoleSession) private model: Model;

    async run(): Promise<void> {
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
            process.stdout.write(data);
            context += data;

            if (data.endsWith(PROMPT)) {
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
