#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${PAPER_JAR:?Set PAPER_JAR to a trusted Paper 26.3 server JAR}"
test -f target/PlayerProfiles-8.0.4.jar
test -f core/target/check-classpath.txt
mkdir -p .verification
paper_test_dir=$(mktemp -d "$PWD/.verification/paper-regression.XXXXXX")
mkdir -p "$paper_test_dir/classes" "$paper_test_dir/plugins"
javac -cp "target/PlayerProfiles-8.0.4.jar:$(cat core/target/check-classpath.txt)" -d "$paper_test_dir/classes" checks/runtime/Probe.java
jar cf "$paper_test_dir/plugins/PlayerProfilesProbe.jar" -C "$paper_test_dir/classes" . -C checks/runtime plugin.yml
cp target/PlayerProfiles-8.0.4.jar "$paper_test_dir/plugins/PlayerProfiles.jar"
cp "$PAPER_JAR" "$paper_test_dir/paper.jar"
cat > "$paper_test_dir/server.properties" <<'PROPERTIES'
server-ip=127.0.0.1
server-port=25639
online-mode=true
enable-query=false
enable-rcon=false
view-distance=2
simulation-distance=2
max-players=2
level-name=regression-world
level-type=minecraft:flat
generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"structure_overrides":[]}
generate-structures=false
allow-nether=false
PROPERTIES
cat > "$paper_test_dir/bukkit.yml" <<'YAML'
settings:
  allow-end: false
YAML
# Only a disposable localhost test server is started. The test plugin shuts it down.
printf 'eula=true\n' > "$paper_test_dir/eula.txt"
for phase in first restart; do
    (cd "$paper_test_dir" && timeout 300 java -XX:ActiveProcessorCount=2 -Xms128M -Xmx640M \
        -Dplayerprofiles.regression=true -Dplayerprofiles.restart="$([[ "$phase" == restart ]] && echo true || echo false)" \
        -Dterminal.jline=false -Dterminal.ansi=false -jar paper.jar --nogui --noconsole > "runtime-$phase.log" 2>&1)
    test -f "$paper_test_dir/probe-results.txt"
    if ! grep -Eq '^TOTAL: [0-9]+ checks passed$' "$paper_test_dir/probe-results.txt" || grep -q '^FAIL:' "$paper_test_dir/probe-results.txt"; then
        cat "$paper_test_dir/probe-results.txt"
        exit 1
    fi
    mv "$paper_test_dir/probe-results.txt" "$paper_test_dir/results-$phase.txt"
    cat "$paper_test_dir/results-$phase.txt"
done
