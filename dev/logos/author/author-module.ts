import { Container } from "inversify";
import { ConsoleAgent } from "./agents/console.js";
import { GenerativeTextModelParams, Model } from "./models/model.js";
import { OllamaModel, OllamaModels, OllamaParams, OllamaServer } from "./models/ollama.js";
import { Tasks } from "./tasks.js";
import winston, { Logger } from "winston";
import { OpenAiModel, OpenAiModels, OpenAiParams, OpenAiServer } from "./models/openai.js";

// skipBaseClassChecks allows wrapping external classes for injection without modification
export const container: Container = new Container({ skipBaseClassChecks: true });

// container.bind(Model).to(OpenAiModel)
//     .whenTargetNamed(Tasks.LinuxConsoleSession);

container.bind(Logger).toConstantValue(
    winston.createLogger({
        level: "debug",
        format: winston.format.combine(
            //winston.format.timestamp(),
            winston.format.simple()
        ),
        transports: [
            new winston.transports.File({ filename: "/tmp/logos.log" })
        ]
    })
);

container.bind(Model).to(OpenAiModel)
    .whenTargetNamed(Tasks.LinuxConsoleSession);
container.bind(OpenAiServer).toSelf();
container.bind(OpenAiParams.ApiKey).toConstantValue(process.env.OPENAI_API_KEY);
container.bind(OpenAiParams.ModelName).toConstantValue(OpenAiModels.gpt_4o);

container.bind(OllamaServer).toSelf();

container.bind(OllamaParams.ModelName)
    .toConstantValue(OllamaModels.llama3_70b_instruct_q5_K_M)
    .whenParentNamed(Tasks.LinuxConsoleSession);

container.bind(OllamaParams.Host)
    .toConstantValue("http://10.255.255.6:8085");

container.bind(GenerativeTextModelParams.SystemPrompt)
    .toConstantValue(`\
# You are an AI agent tasked with analyzing and critiquing software code. You have access to a Linux terminal with a
# bash shell. Your goal is to review the source code of a gRPC web framework called logos and provide a detailed
# critique.
# 
# Important:
# - Use only basic shell commands (cd, ls, cat, echo)
# - Never use multi-line shell commands. Only use one-liners.
# - Avoid any commands that could get the terminal stuck. You cannot use modifier keys, so you should not run any command which requires ctrl-d or ctrl-c to finish
# - Do not use text editors or commands requiring modifier keys
# - Write all non-command text as shell comments (preceded by #)
# - Do NOT use markdown formatting or code blocks
# - Use cat with heredoc to create or append to files
# 
# Instructions:
# 1. The first command you should run is: find /src/
# 2. Navigate to the project directory: cd /src/logos
# 3. Use cat to read each file in /src/logos one at time. Do this with individual shell commands, not a script.
# 4. After reading each file, append your analysis to critique.txt using cat with a heredoc. Only critique files that exist.
# 5. Focus on both framework code in /src/logos/dev and app code in /src/logos/apps
# 6. Provide specific, actionable advice about the source code
# 7. Be thorough in your analysis - there is no time limit
# 8. DO NOT hallucinate files which are not there. Verify that files exist using ls, and obey the output of ls completely.
# 
# Example of using cat with heredoc to append to critique.txt:
# cat << EOF >> critique.txt
# Your analysis text here
# Multiple lines are fine
# EOF
#
# Begin your analysis now. Remember to think carefully before executing any command.
# 
`).whenParentNamed(Tasks.LinuxConsoleSession);

container.bind(ConsoleAgent).toSelf();