/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import {
    ConcordId,
    ConcordKey,
    fetchJson,
    GenericOperationResult,
    OperationResult
} from '../../../common';

export interface RepositoryMeta {
    profiles?: string[];
    entryPoints?: string[];
}

export interface RepositoryEntry {
    id: ConcordId;
    name: ConcordKey;
    url: string;
    branch?: string;
    commitId?: string;
    path?: string;
    secretStoreType?: string;
    secretId: string;
    secretName: string;
    meta?: RepositoryMeta;
    disabled: boolean;
}

export interface EditRepositoryEntry {
    id?: ConcordId;
    name: ConcordKey;
    url: string;
    branch?: string;
    commitId?: string;
    path?: string;
    secretId: string;
    disabled: boolean;
}

export interface TriggerCfg {
    entryPoint: string;
}

export interface TriggerEntry {
    id: ConcordId;
    eventSource: ConcordKey;
    arguments?: object;
    conditions?: object;
    cfg: TriggerCfg;
}

export interface RepositoryValidationResponse {
    ok: boolean;
    result: OperationResult;
}

export const createOrUpdate = (
    orgName: ConcordKey,
    projectName: ConcordKey,
    entry: EditRepositoryEntry
): Promise<GenericOperationResult> => {
    const opts = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(entry)
    };

    return fetchJson(`/api/v1/org/${orgName}/project/${projectName}/repository`, opts);
};

export const deleteRepository = (
    orgName: ConcordKey,
    projectName: ConcordKey,
    repoName: ConcordKey
): Promise<GenericOperationResult> => {
    const opts = {
        method: 'DELETE'
    };

    return fetchJson(`/api/v1/org/${orgName}/project/${projectName}/repository/${repoName}`, opts);
};

export const refreshRepository = (
    orgName: ConcordKey,
    projectName: ConcordKey,
    repoName: ConcordKey,
    sync: boolean
): Promise<GenericOperationResult> => {
    const opts = {
        method: 'POST'
    };

    return fetchJson(
        `/api/v1/org/${orgName}/project/${projectName}/repository/${repoName}/refresh?sync=${sync}`,
        opts
    );
};

export const validateRepository = (
    orgName: ConcordKey,
    projectName: ConcordKey,
    repoName: ConcordKey
): Promise<RepositoryValidationResponse> => {
    const opts = {
        method: 'POST'
    };

    return fetchJson(
        `/api/v1/org/${orgName}/project/${projectName}/repository/${repoName}/validate`,
        opts
    );
};

export const listTriggers = (
    orgName: ConcordKey,
    projectName: ConcordKey,
    repoName: ConcordKey
): Promise<TriggerEntry[]> =>
    fetchJson(`/api/v1/org/${orgName}/project/${projectName}/repo/${repoName}/trigger`);
