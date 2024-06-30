import { injectable } from "inversify";


@injectable()
export abstract class Model {
    abstract generateText(prompt: string, model?: string): AsyncGenerator<string>;
}
export const GenerativeTextModelParams = {
    SystemPrompt: Symbol.for("GenerativeTextModelSystemPrompt")
};

