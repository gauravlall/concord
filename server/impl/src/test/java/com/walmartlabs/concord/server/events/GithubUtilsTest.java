package com.walmartlabs.concord.server.events;

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

import org.junit.Test;

import static org.junit.Assert.*;

public class GithubUtilsTest {

    @Test
    public void testGetRefShortName() {
        assertRefShortName("master", "master");
        assertRefShortName("refs/heads/master", "master");
        assertRefShortName("refs/tags/master", "master");
        assertRefShortName("refs/heads/feature/boo", "feature/boo");
        assertRefShortName("refs/remotes/boo/HEAD", "boo");
    }

    @Test
    public void testRepoUrlMatcher() {
        assertRepoUrl("devtools/concord", "https://localhost/devtools/concord");
        assertRepoUrl("devtools/concord", "https://localhost/devtools/concord.git");
        assertRepoUrl("devtools/concord", "git@localhost:devtools/concord.git");
        assertRepoUrl("devtools/concord", "git@localhost:devtools/concord");
        assertRepoUrl("devtools/concord", "git@localhost:devtools/concord/");
        assertRepoUrl("devtools/concord", "git@localhost:devtools/concord.git/");
        assertRepoUrl("devtools/concord", "git@mydomain:devtools/concord.git/", "mydomain");

        assertNotRepoUrl("devtools/concord_new", "git@localhost:devtools/concord.git");
        assertNotRepoUrl("devtools/new_concord", "git@localhost:devtools/concord.git");
        assertNotRepoUrl("devtools/con", "git@localhost:devtools/concord.git");
    }

    @Test
    public void testParse() {
        assertRepositoryName("git@github.example.com:h1sammo/Anisble-GLS-AppInstall.git", "h1sammo/Anisble-GLS-AppInstall");
        assertRepositoryName("git+https://github.com/owner/name.git", "owner/name");
        assertRepositoryName("git://gh.pages.com/owner/name.git", "owner/name");
        assertRepositoryName("git://github.assemble.com/owner/name.git", "owner/name");
        assertRepositoryName("git://github.assemble.two.com/owner/name.git", "owner/name");
        assertRepositoryName("git://github.com/owner/name", "owner/name");
        assertRepositoryName("git@github.com/owner/name.git", "owner/name");
        assertRepositoryName("git@github.com:owner/name.git", "owner/name");
        assertRepositoryName("git@github.com:8080/owner/name.git", "owner/name");
        assertRepositoryName("github.com:owner/name.git", "owner/name");
        assertRepositoryName("github:owner/name", "owner/name");
        assertRepositoryName("http://github.com:8080/owner/name", "owner/name");
        assertRepositoryName("http://github.com/owner/name", "owner/name");
        assertRepositoryName("http://github.com/owner/name.git", "owner/name");
        assertRepositoryName("http://github.com/owner/name/tree", "owner/name");
        assertRepositoryName("http://github.com/owner/name/tree/master", "owner/name");
        assertRepositoryName("https://assemble@github.com/owner/name.git", "owner/name");
        assertRepositoryName("https://github.com/owner/name/blob/249b21a86400b38969cee3d5df6d2edf8813c137/README.md", "owner/name");
//        assertRepositoryName("git@github.com:owner/name.git#1.2.3", "owner/name");
    }

    private static void assertRepositoryName(String url, String expected) {
        String actual = GithubUtils.getRepositoryName(url);
        assertEquals(expected, actual);
    }

    private static void assertRefShortName(String ref, String expected) {
        String shortName = GithubUtils.getRefShortName(ref);
        assertEquals(expected, shortName);
    }

    private static void assertRepoUrl(String repoName, String url) {
        assertRepoUrl(repoName, url, "localhost");
    }

    private static void assertRepoUrl(String repoName, String url, String domain) {
        assertTrue(GithubUtils.isRepositoryUrl(repoName, url, domain));
    }

    private static void assertNotRepoUrl(String repoName, String url) {
        assertNotRepoUrl(repoName, url, "localhost");
    }

    private static void assertNotRepoUrl(String repoName, String url, String domain) {
        assertFalse(GithubUtils.isRepositoryUrl(repoName, url, domain));
    }
}
