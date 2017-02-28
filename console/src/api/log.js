// @flow
import type {FetchRange} from "../types";
import {formatRangeHeader, parseRange, authHeader, defaultError} from "./common";

const offsetRange = (data: string, range: FetchRange) => {
    // if our data starts from the beginning, do nothing
    if (range.low !== undefined && range.low <= 0) {
        console.log("1");
        return {data, range};
    }

    // seek to the nearest newline
    const i = data.indexOf("\n");

    // no line breaks or a single line, do nothing
    if (i < 0 || i + 1 >= data.length) {
        return {data, range};
    }

    // trim data to a new line, add trimmed offest to the start of a range
    return {
        data: data.substr(i + 1),
        range: {...range, low: range.low + i + 1}
    };
};

const defaultFetchRange: FetchRange = {low: undefined, high: 2048};

export const fetchLog = (fileName: string, fetchRange: FetchRange = defaultFetchRange) => {
    const rangeHeader = formatRangeHeader(fetchRange);
    console.debug("API: fetchLog ['%s', %o] -> starting...", fileName, rangeHeader);
    return fetch(`/logs/${fileName}`, {headers: Object.assign({}, authHeader, rangeHeader)})
        .then(response => {
            if (!response.ok) {
                throw new defaultError(response);
            }

            const rangeHeader = response.headers.get("Content-Range");
            return response.text().then(data => {
                console.debug("API: fetchLog ['%s', %o] -> done, length: %d", fileName, fetchRange, data.length);
                return offsetRange(data, parseRange(rangeHeader));
            });
        });
};

