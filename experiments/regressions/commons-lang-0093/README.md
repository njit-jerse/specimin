# commons-lang-0093: org.apache.commons.lang3.builder.ToStringBuilder#append(int[])

Specimin's output for this target compiled before the change and does not after
(new build: fails).

## Reproducing

```sh
git clone https://github.com/apache/commons-lang repo && git -C repo checkout e073d5dc20ab6e011a59eefda83bc3a43a83dc4b
./gradlew run -PskipCheckerFramework --args='--root "repo/src/main/java" --outputDirectory "out" --targetFile "org/apache/commons/lang3/builder/ToStringBuilder.java" --targetMethod "org.apache.commons.lang3.builder.ToStringBuilder#append(int[])"'
```

Then compile the result:

```sh
javac -classpath src/test/resources/shared/checker-qual-3.42.0.jar $(find out -name '*.java')
```

## Result

| | files emitted | Specimin exit | output compiles |
| --- | --- | --- | --- |
| before | 4 | ok | compiles |
| after | 4 | ok | fails |

## javac on the new output (first 40 lines; full log in javac.log)

```
Picked up JAVA_TOOL_OPTIONS: -Djava.net.preferIPv4Stack=true
/private/tmp/claude-501/-Users-mjk76-Research-specimin-specimin/331e32ce-858c-4205-9062-4095e580f651/scratchpad/full/out/new/commons-lang-0093/org/apache/commons/lang3/builder/ToStringBuilder.java:10: error: incompatible types: int[] cannot be converted to long[]
    style.append(buffer, null, array, null);
                               ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
1 error
```
