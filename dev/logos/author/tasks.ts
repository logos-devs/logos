import { StateMachine } from "./state.js";

// perhaps Tasks call for n particular agents? working together? do they communicate?
export const Tasks = {
    Research: Symbol.for("Research"),
    DesignDocumentWriting: Symbol.for("DesignDocumentWriting"),
    DesignDocumentCritique: Symbol.for("DesignDocumentCritique"),
    LinuxConsoleSession: Symbol.for("LinuxConsoleSession"),
    CodeGeneration: Symbol.for("CodeGeneration"),
    CodeCritique: Symbol.for("CodeCritique")
};

export enum ResearchStates {
    Browsing = "Browsing",
    Reading = "Reading",
    EditingNotes = "EditingNotes",
    FinalizingNotes = "FinalizingNotes"
}

const researchStateMachine = StateMachine.builder<ResearchStates, { prompt: string, context: string }>()
    .state(ResearchStates.Browsing, { prompt: "", context: "" })
    .state(ResearchStates.Reading)
    .state(ResearchStates.EditingNotes)
    .state(ResearchStates.FinalizingNotes)
    .start(ResearchStates.Browsing)
    .transition(ResearchStates.Browsing, ResearchStates.Reading)
    .transition(ResearchStates.Reading, ResearchStates.EditingNotes)
    .transition(ResearchStates.EditingNotes, ResearchStates.Reading)
    .transition(ResearchStates.EditingNotes, ResearchStates.FinalizingNotes)
    .build();