package com.walmartlabs.concord.plugins.ansible;

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

import com.walmartlabs.concord.common.ConfigurationUtils;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.ansible.ArgUtils.getMap;
import static com.walmartlabs.concord.plugins.ansible.ArgUtils.getString;

public class AnsibleConfig {

    private static final Logger log = LoggerFactory.getLogger(AnsibleConfig.class);

    private static final String CFG_FILE_NAME = "ansible.cfg";

    private final Path workDir;
    private final Path tmpDir;
    private final boolean debug;

    private Map<String, Map<String, Object>> cfg = new HashMap<>();

    public AnsibleConfig(Path workDir, Path tmpDir, boolean debug) {
        this.workDir = workDir;
        this.tmpDir = tmpDir;
        this.debug = debug;
    }

    public AnsibleConfig parse(Map<String, Object> args) {
        String s = getString(args, TaskParams.CONFIG_FILE_KEY);
        if (s != null) {
            Path provided = workDir.resolve(s);
            if (Files.exists(provided)) {
                log.info("Using the provided configuration file: {}", provided);
                this.cfg = loadFromFile(provided);
                return this;
            }
        }

        Map<String, Object> userCfg = getMap(args, TaskParams.CONFIG_KEY);

        this.cfg = makeAnsibleCfg(userCfg);

        return this;
    }

    public Path write() {
        StringBuilder b = new StringBuilder();

        for (Map.Entry<String, Map<String, Object>> c : cfg.entrySet()) {
            b = addCfgSection(b, c.getKey(), c.getValue());
        }

        if (debug) {
            log.info("Using the configuration: \n{}", b);
        }

        Path cfgPath = getConfigPath();
        try {
            Files.write(cfgPath, b.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (IOException e) {
            log.error("Configuration write {} error", CFG_FILE_NAME, e);
            throw new RuntimeException("Configuration write error: " + e.getMessage());
        }
        return workDir.relativize(cfgPath);
    }

    public AnsibleConfig enrichEnv(AnsibleEnv env) {
        env.get().put("ANSIBLE_CONFIG", workDir.relativize(getConfigPath()).toString());
        return this;
    }

    public ConfigSection getDefaults() {
        Map<String, Object> defaults = cfg.computeIfAbsent("defaults", s -> new HashMap<>());
        return new ConfigSection(defaults);
    }

    private Path getConfigPath() {
        return tmpDir.resolve(CFG_FILE_NAME);
    }

    private static Map<String, Object> makeDefaults() {
        Map<String, Object> m = new HashMap<>();

        // disable puppet / chef fact gathering, significant speed/performance increase - usually unneeded
        // may eventually need !hardware for AIX/HPUX or set at runtime, Ansible 2.4 fixes many broken facts
        m.put("gather_subset", "!facter,!ohai");

        // disable ssl host key checking by default
        m.put("host_key_checking", false);

        //SSH timeout, default is 10 seconds and too slow for stores
        m.put("timeout", "120");

        // use a shorter path to store temporary files
        m.put("remote_tmp", "/tmp/${USER}/ansible");

        return m;
    }

    private static Map<String, Map<String, Object>> makeAnsibleCfg(Map<String, Object> userCfg) {
        Map<String, Object> m = new HashMap<>();
        m.put("defaults", makeDefaults());
        m.put("ssh_connection", makeSshConnCfg());

        if (userCfg != null) {
            m = ConfigurationUtils.deepMerge(m, userCfg);
        }

        return assertCfg(m);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String,Object>> assertCfg(Map<String, Object> cfg) {
        Map<String, Map<String,Object>> result = new HashMap<>();

        for (Map.Entry<String, Object> c : cfg.entrySet()) {
            String k = c.getKey();
            Object v = c.getValue();
            if (!(v instanceof Map)) {
                throw new IllegalArgumentException("Invalid configuration. Expected a JSON object: " + k + ", got: " + v);
            }

            result.put(k, (Map<String, Object>) v);
        }

        return result;
    }

    private static Map<String, Object> makeSshConnCfg() {
        Map<String, Object> m = new HashMap<>();

        // Default pipelining to True for better overall performance, compatibility
        m.put("pipelining", "True");

        return m;
    }

    private static StringBuilder addCfgSection(StringBuilder b, String name, Map<String, Object> m) {
        if (m == null || m.isEmpty()) {
            return b;
        }

        b.append("[").append(name).append("]\n");
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }

            b.append(e.getKey()).append(" = ").append(v).append("\n");
        }
        return b;
    }

    private Map<String, Map<String,Object>> loadFromFile(Path file) {
        try {
            Ini ini = new Ini();
            ini.load(file.toFile());

            Map<String, Map<String, Object>> result = new HashMap<>();
            for (Map.Entry<String, Profile.Section> e : ini.entrySet()) {
                Map<String, Object> section = new HashMap<>();
                for (Map.Entry<String, String> s : e.getValue().entrySet()) {
                    section.put(s.getKey(), s.getValue());
                }
                result.put(e.getKey(), section);
            }
            return result;
        } catch (IOException e) {
            log.error("Configuration parse error: {}", e.getMessage());
            throw new RuntimeException("Configuration parse error " + file + ": " + e.getMessage());
        }
    }
}
