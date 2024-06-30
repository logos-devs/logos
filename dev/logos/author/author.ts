import "reflect-metadata";

import { container } from "./author-module.js";
import { ConsoleAgent } from "./agents/console.js";

process.chdir(process.env.BUILD_WORKSPACE_DIRECTORY);

// gotta now "code how to code".
// the LLM needs to be able to invoke scripted prompt sequences that guide it through the process
// of writing a piece of software, adding a new feature, investigating a bug, etc.
// I need to flowchart these in lucidchart first. Then, for each task, the LLM needs a state machine
// for how to proceed through the task, and how to know when it's done.
// The state machine should be obvious and visible to the LLM via PS1 perhaps. Transitions should be
// commands the LLM invokes, and guards will give the LLM feedback on how to transtion.
// For example, an LLM should not be able to transition to complete if features remain that are
// unimplemented, or a bug should not be fixed if the test demonstrating the bug does not pass.
// 
// states should not reflect the state of the project or task. they should reflect the state of the agent.
// agents may need to return to previous states, and should at each state evaluate the level of completion
// of their task, and which transition of *their* state is most appropriate at the moment.

await container.get(ConsoleAgent).run().catch(
    (reason) => {
        console.error(reason)
        process.exit(1);
    }
);