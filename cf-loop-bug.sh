# Reproduction script for https://github.com/kelloggm/specimin/issues/272

## First attempt, doesn't repro with specimin main on 4/29/24 9:30am (crash reported, no crash observed)
#./gradlew run --args='--outputDirectory "/Users/mjk76/Research/code-review-verification/playground/cf-out" --root "/Users/mjk76/jsr308/checker-framework/framework/src/main/java" --targetFile "org/checkerframework/framework/util/typeinference8/types/AbstractType.java" --targetMethod "org.checkerframework.framework.util.typeinference8.types.AbstractType#getFunctionTypeParameterTypes()"'

## Attempt two
./gradlew run --args='--outputDirectory "/Users/mjk76/Research/code-review-verification/playground/cf-out" --root "/Users/mjk76/jsr308/checker-framework/framework/src/main/java" --targetFile "org/checkerframework/framework/util/Contract.java" --targetMethod "org.checkerframework.framework.util.Contract#viewpointAdaptDependentTypeAnnotation(GenericAnnotatedTypeFactory<?,?,?,?>, StringToJavaExpression, Tree)"'
