import { injectable, inject } from "inversify";
import { OpenAI } from "openai";
import { GenerativeTextModelParams, Model } from "./model.js";

export enum OpenAiModels {
    gpt_4o = "gpt-4o",
    gpt_4_turbo = "gpt-4-turbo",
    gpt_3_5_turbo = "gpt-3.5-turbo"
};

export const OpenAiParams = {
    ModelName: Symbol.for("OpenAiModelName"),
    ApiKey: Symbol.for("OpenAiAPIKey")
};

@injectable()
export class OpenAiServer extends OpenAI {
    constructor(
        @inject(OpenAiParams.ApiKey) apiKey: string
    ) {
        super({ apiKey });
    }
}

@injectable()
export class OpenAiModel extends Model {
    private readonly server: OpenAiServer;
    private readonly modelName: string;
    private readonly systemPrompt: string;

    constructor(
        @inject(OpenAiServer) server: OpenAiServer,
        @inject(OpenAiParams.ModelName) modelName: OpenAiModels,
        @inject(GenerativeTextModelParams.SystemPrompt) systemPrompt: string
    ) {
        super();
        this.server = server;
        this.modelName = modelName;
        this.systemPrompt = systemPrompt;
    }

    override async *generateText(prompt: string): AsyncGenerator<string> {
        const stream = await this.server.chat.completions.create({
            model: this.modelName,
            stream: true,
            messages: [
                { role: "system", content: this.systemPrompt },
                { role: "user", content: prompt }
            ]
        });

        try {
            for await (const chunk of stream) {
                yield chunk.choices[0]?.delta?.content || '';
            }
        }
        finally {
            stream.controller.abort();
        }
    }
}