import { inject, injectable, named } from "inversify";
import pty from 'node-pty';
import { Model } from "../models/model.js";
import { Tasks } from "../tasks.js";
import { Agent } from "./agent.js";
import { Logger } from "winston";

@injectable()
export class ConsoleAgent implements Agent {
    private context: string = "";

    constructor(
        //@inject(StateMachine) readonly state: StateMachine<ResearchStates, { prompt: string }>,
        @inject(Model) @named(Tasks.LinuxConsoleSession) private model: Model,
        @inject(Logger) private logger: Logger
    ) { }

    private prompt(): string {
        return this.context;
    }

    private consoleAwaitsInput(data: string): boolean {
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

        process.stdout.write(this.context);

        ptyProcess.onData(async (data) => {
            process.stdout.write(data);
            this.context += data;
            this.context = this.context.slice(-5000);

            if (this.consoleAwaitsInput(this.context)) {
                this.logger.debug(this.context)

                // TODO : switch to streaming, cut on the first newline, and send each line immediately to the model
                for await (const chunk of this.model.generateText(this.context)) {
                    ptyProcess.write(chunk);
                }
            }
        });

        process.stdin.setRawMode(true);
        process.stdin.on('data', (data) => {
            ptyProcess.write(data.toString());
        });

        try {
            return await new Promise<void>((resolve, reject) => {
                ptyProcess.onExit(({ exitCode, signal }) => {
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
