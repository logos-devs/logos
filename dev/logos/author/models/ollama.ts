import { injectable, inject } from "inversify";
import { Ollama } from "ollama";
import { Model } from "./model.js";
import { GenerativeTextModelParams } from "./model.js";

export enum OllamaModels {
    dolphin_llama3_8b_v2_9_q5_K_M = "dolphin-llama3:8b-v2.9-q5_K_M",
    llama3_8b_instruct_fp16 = "llama3:8b-instruct-fp16",
    llama3_70b_instruct_q5_K_M = "llama3:70b-instruct-q5_K_M"
}

export const OllamaParams = {
    Host: Symbol.for("OllamaHost"),
    ModelName: Symbol.for("OllamaModelName"),
};

@injectable()
export class OllamaServer extends Ollama {
    constructor(@inject(OllamaParams.Host) host: string) {
        super({ host });
    }
}

@injectable()
export class OllamaModel extends Model {
    private readonly server: OllamaServer;
    private readonly modelName: string;
    private readonly systemPrompt: string;

    constructor(
        @inject(OllamaServer) server: OllamaServer,
        @inject(OllamaParams.ModelName) modelName: string,
        @inject(GenerativeTextModelParams.SystemPrompt) systemPrompt: string
    ) {
        super();
        this.server = server;
        this.modelName = modelName;
        this.systemPrompt = systemPrompt;
    }

    override async *generateText(prompt: string): AsyncGenerator<string> {
        const stream = await this.server.chat({
            model: this.modelName,
            messages: [
                { role: 'system', content: this.systemPrompt },
                { role: 'user', content: prompt }
            ],
            stream: true,
            options: {}
        });

        for await (const chunk of stream) {
            yield chunk.message.content;
        }
    }
}