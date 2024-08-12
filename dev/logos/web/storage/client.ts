export interface Constructable<T> {
    new(...args: any[]): T;
}

export type EntityMutationRequest<Request, Entity> = {
    setId?: (id: Uint8Array) => Request,
    setEntity?: (entity: Entity) => Request
};

export type EntityMutationResponse = {
    getId_asU8: () => Uint8Array
};

export type EntityReadRequest = {};

export type EntityReadResponse<Entity> = {
    getResultsList: () => Entity[]
};
