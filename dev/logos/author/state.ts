type Guard = () => boolean;

class State<StateName, StateProps> {
    constructor(
        public readonly name: StateName,
        public readonly props?: StateProps
    ) {}
}

class Transition<StateName, StateProps> {
    constructor(
        public readonly from: State<StateName, StateProps>,
        public readonly to: State<StateName, StateProps>,
        public readonly guard?: Guard
    ) {}
}

class InvalidTransitionError<StateName> extends Error {
    constructor(from: StateName, to: StateName) {
        super(`Invalid transition from ${from} to ${to}. Ensure the transition is defined.`);
        this.name = "InvalidTransitionError";
    }
}

class GuardConditionError<StateName> extends Error {
    constructor(from: StateName, to: StateName) {
        super(`Guard condition blocked transition from ${from} to ${to}. Check the guard conditions.`);
        this.name = "GuardConditionError";
    }
}

class MissingStateError<StateName> extends Error {
    constructor(stateName: StateName) {
        super(`State ${stateName} is not defined.`);
        this.name = "MissingStateError";
    }
}

class BuildAlreadyCalledError extends Error {
    constructor() {
        super("The build method has already been called. Cannot build the state machine multiple times.");
        this.name = "BuildAlreadyCalledError";
    }
}

export class StateMachine<StateName, StateProps> {
    private currentState: State<StateName, StateProps>;
    private readonly transitions: Map<StateName, readonly Transition<StateName, StateProps>[]> = new Map();

    public constructor(start: State<StateName, StateProps>, transitions: Transition<StateName, StateProps>[]) {
        this.currentState = start;
        this.transitions = this.buildTransitionMap(transitions);
    }

    private buildTransitionMap(transitions: Transition<StateName, StateProps>[]): Map<StateName, ReadonlyArray<Transition<StateName, StateProps>>> {
        const map = new Map<StateName, Transition<StateName, StateProps>[]>();
        transitions.forEach(transition => {
            if (!map.has(transition.from.name)) {
                map.set(transition.from.name, []);
            }
            map.get(transition.from.name)!.push(transition);
        });
        return new Map<StateName, ReadonlyArray<Transition<StateName, StateProps>>>(map);
    }

    public static builder<StateName, StateProps>(): StateMachineBuilder<StateName, StateProps> {
        return new StateMachineBuilder<StateName, StateProps>();
    }

    public getState(): State<StateName, StateProps> {
        return this.currentState;
    }

    public transition(toState: StateName): void {
        const possibleTransitions = this.transitions.get(this.currentState.name) || [];
        const transition = possibleTransitions.find(t => t.to.name === toState);

        if (!transition) {
            throw new InvalidTransitionError(this.currentState.name, toState);
        }

        if (transition.guard && !transition.guard()) {
            throw new GuardConditionError(this.currentState.name, transition.to.name);
        }

        this.currentState = transition.to;
    }
}

class StateMachineBuilder<StateName, StateProps> {
    private transitions: Transition<StateName, StateProps>[] = [];
    private startState?: State<StateName, StateProps>;
    private built = false;
    private states: Map<StateName, State<StateName, StateProps>> = new Map();

    public state(name: StateName, props?: StateProps): this {
        const state = new State(name, props);
        this.states.set(name, state);
        return this;
    }

    public start(name: StateName): this {
        const state = this.states.get(name);
        if (!state) {
            throw new MissingStateError(name);
        }
        if (this.startState) {
            throw new Error('Start state is already defined.');
        }
        this.startState = state;
        return this;
    }

    public transition(fromName: StateName, toName: StateName, guard?: Guard): this {
        if (this.built) {
            throw new Error('Cannot add transitions after the state machine is built.');
        }
        const fromState = this.states.get(fromName);
        const toState = this.states.get(toName);
        if (!fromState) {
            throw new MissingStateError(fromName);
        }
        if (!toState) {
            throw new MissingStateError(toName);
        }
        this.transitions.push(new Transition(fromState, toState, guard));
        return this;
    }

    public build(): StateMachine<StateName, StateProps> {
        if (this.built) {
            throw new BuildAlreadyCalledError();
        }
        if (!this.startState) {
            throw new MissingStateError('start state' as unknown as StateName);
        }
        this.built = true;
        return new StateMachine(this.startState, this.transitions);
    }
}

// // Example usage
// enum MyStateName {
//     Idle = "Idle",
//     Loading = "Loading",
//     Success = "Success",
//     Error = "Error",
// }

// interface MyStateProps {
//     description: string;
// }

// const stateMachine = StateMachine.builder<MyStateName, MyStateProps>()
//     .state(MyStateName.Idle, { description: "The system is idle." })
//     .state(MyStateName.Loading, { description: "The system is loading." })
//     .state(MyStateName.Success, { description: "The operation was successful." })
//     .state(MyStateName.Error, { description: "An error occurred." })
//     .start(MyStateName.Idle)
//     .transition(MyStateName.Idle, MyStateName.Loading)
//     .transition(MyStateName.Loading, MyStateName.Success)
//     .transition(MyStateName.Loading, MyStateName.Error)
//     .transition(MyStateName.Error, MyStateName.Idle)
//     .transition(MyStateName.Success, MyStateName.Idle)
//     .build();

// console.log(`Current State: ${stateMachine.getState().name}`); // Idle
// stateMachine.transition(MyStateName.Loading);
// stateMachine.transition(MyStateName.Success);
// stateMachine.transition(MyStateName.Idle);
// stateMachine.transition(MyStateName.Loading);
// stateMachine.transition(MyStateName.Error);
// stateMachine.transition(MyStateName.Idle);

// // Error handling example
// try {
//     stateMachine.transition(MyStateName.Loading); // Valid transition
//     stateMachine.transition(MyStateName.Error);   // This should throw an InvalidTransitionError
// } catch (error) {
//     if (error instanceof InvalidTransitionError || error instanceof GuardConditionError) {
//         console.error(error.message);
//     } else {
//         throw error;
//     }
// }
