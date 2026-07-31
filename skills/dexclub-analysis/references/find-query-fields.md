# Find Query Field Reference

Use this reference when an exact `Find*Query` field, nested Matcher type, required property, default, or enum value is unclear. Use [find-queries.md](find-queries.md) for common composition recipes.

## Contents

- Public query roots
- Core matchers
- Collection and relationship matchers
- Annotation matchers
- Scalar and utility matchers
- Enum values

The tables describe the public JSON contract generated from `FindClassQuery`, `FindMethodQuery`, and `FindFieldQuery`. They intentionally exclude binding-only `searchIn*` fields and all `BatchFind*` types. Fields marked optional may be omitted; their serializer defaults are shown where applicable.

## Public Query Roots

<!-- schema-object:FindClassQuery -->
### `FindClassQuery`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `searchPackages` | string[] | no | `[]` |
| `excludePackages` | string[] | no | `[]` |
| `ignorePackagesCase` | boolean | no | `false` |
| `matcher` | `ClassMatcher` or null | no | null |
| `findFirst` | boolean | no | `false` |

<!-- schema-object:FindMethodQuery -->
### `FindMethodQuery`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `searchPackages` | string[] | no | `[]` |
| `excludePackages` | string[] | no | `[]` |
| `ignorePackagesCase` | boolean | no | `false` |
| `matcher` | `MethodMatcher` or null | no | null |
| `findFirst` | boolean | no | `false` |

<!-- schema-object:FindFieldQuery -->
### `FindFieldQuery`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `searchPackages` | string[] | no | `[]` |
| `excludePackages` | string[] | no | `[]` |
| `ignorePackagesCase` | boolean | no | `false` |
| `matcher` | `FieldMatcher` or null | no | null |
| `findFirst` | boolean | no | `false` |

## Core Matchers

<!-- schema-object:ClassMatcher -->
### `ClassMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `source` | `StringMatcher` or null | no | null |
| `className` | `StringMatcher` or null | no | null |
| `modifiers` | `AccessFlagsMatcher` or null | no | null |
| `superClass` | `ClassMatcher` or null | no | null |
| `interfaces` | `InterfacesMatcher` or null | no | null |
| `annotations` | `AnnotationsMatcher` or null | no | null |
| `fields` | `FieldsMatcher` or null | no | null |
| `methods` | `MethodsMatcher` or null | no | null |
| `usingStrings` | `StringMatcher`[] | no | `[]` |

<!-- schema-object:MethodMatcher -->
### `MethodMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `name` | `StringMatcher` or null | no | null |
| `modifiers` | `AccessFlagsMatcher` or null | no | null |
| `declaredClass` | `ClassMatcher` or null | no | null |
| `protoShorty` | string or null | no | null |
| `returnType` | `ClassMatcher` or null | no | null |
| `params` | `ParametersMatcher` or null | no | null |
| `annotations` | `AnnotationsMatcher` or null | no | null |
| `opCodes` | `OpCodesMatcher` or null | no | null |
| `usingStrings` | `StringMatcher`[] | no | `[]` |
| `usingFields` | `UsingFieldMatcher`[] | no | `[]` |
| `usingNumbers` | `NumberEncodeValueMatcher`[] | no | `[]` |
| `invokeMethods` | `MethodsMatcher` or null | no | null |
| `callerMethods` | `MethodsMatcher` or null | no | null |

<!-- schema-object:FieldMatcher -->
### `FieldMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `name` | `StringMatcher` or null | no | null |
| `modifiers` | `AccessFlagsMatcher` or null | no | null |
| `declaredClass` | `ClassMatcher` or null | no | null |
| `type` | `ClassMatcher` or null | no | null |
| `annotations` | `AnnotationsMatcher` or null | no | null |
| `readMethods` | `MethodsMatcher` or null | no | null |
| `writeMethods` | `MethodsMatcher` or null | no | null |

## Collection and Relationship Matchers

<!-- schema-object:InterfacesMatcher -->
### `InterfacesMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `interfaces` | `ClassMatcher`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:FieldsMatcher -->
### `FieldsMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `fields` | `FieldMatcher`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:MethodsMatcher -->
### `MethodsMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `methods` | `MethodMatcher`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:ParametersMatcher -->
### `ParametersMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `params` | (`ParameterMatcher` or null)[] | no | `[]` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:ParameterMatcher -->
### `ParameterMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `type` | `ClassMatcher` or null | no | null |
| `annotations` | `AnnotationsMatcher` or null | no | null |

<!-- schema-object:UsingFieldMatcher -->
### `UsingFieldMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `field` | `FieldMatcher` or null | no | null |
| `usingType` | `UsingType` | no | `Any` |

## Annotation Matchers

<!-- schema-object:AnnotationsMatcher -->
### `AnnotationsMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `annotations` | `AnnotationMatcher`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:AnnotationMatcher -->
### `AnnotationMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `type` | `ClassMatcher` or null | no | null |
| `targetElementTypes` | `TargetElementTypesMatcher` or null | no | null |
| `policy` | `RetentionPolicyType` or null | no | null |
| `elements` | `AnnotationElementsMatcher` or null | no | null |
| `usingStrings` | `StringMatcher`[] | no | `[]` |

<!-- schema-object:AnnotationElementsMatcher -->
### `AnnotationElementsMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `elements` | `AnnotationElementMatcher`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:AnnotationElementMatcher -->
### `AnnotationElementMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `name` | `StringMatcher` or null | no | null |
| `value` | `AnnotationEncodeValueMatcher` or null | no | null |

<!-- schema-object:AnnotationEncodeValueMatcher -->
### `AnnotationEncodeValueMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `byteValue` | integer or null | no | null |
| `shortValue` | integer or null | no | null |
| `charValue` | string or null | no | null |
| `intValue` | integer or null | no | null |
| `longValue` | integer or null | no | null |
| `floatValue` | number or null | no | null |
| `doubleValue` | number or null | no | null |
| `stringValue` | `StringMatcher` or null | no | null |
| `classValue` | `ClassMatcher` or null | no | null |
| `methodValue` | `MethodMatcher` or null | no | null |
| `enumValue` | `FieldMatcher` or null | no | null |
| `arrayValue` | `AnnotationEncodeArrayMatcher` or null | no | null |
| `annotationValue` | `AnnotationMatcher` or null | no | null |
| `nullValue` | boolean | no | `false` |
| `boolValue` | boolean or null | no | null |

<!-- schema-object:AnnotationEncodeArrayMatcher -->
### `AnnotationEncodeArrayMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `values` | `AnnotationEncodeValueMatcher`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |
| `count` | `IntRangeValue` or null | no | null |

<!-- schema-object:TargetElementTypesMatcher -->
### `TargetElementTypesMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `types` | `TargetElementType`[] | no | `[]` |
| `matchType` | `MatchType` | no | `Contains` |

## Scalar and Utility Matchers

<!-- schema-object:StringMatcher -->
### `StringMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `value` | string | yes | none |
| `matchType` | `StringMatchType` | no | `Contains` |
| `ignoreCase` | boolean | no | `false` |

<!-- schema-object:AccessFlagsMatcher -->
### `AccessFlagsMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `modifiers` | integer | yes | none |
| `matchType` | `MatchType` | no | `Contains` |

<!-- schema-object:NumberEncodeValueMatcher -->
### `NumberEncodeValueMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `byteValue` | integer or null | no | null |
| `shortValue` | integer or null | no | null |
| `charValue` | string or null | no | null |
| `intValue` | integer or null | no | null |
| `longValue` | integer or null | no | null |
| `floatValue` | number or null | no | null |
| `doubleValue` | number or null | no | null |

Set exactly one numeric value field. For an Android resource ID, preserve its signed 32-bit bit pattern, convert hexadecimal notation to a decimal JSON number, and use `intValue`; do not substitute `longValue` for an unsigned-looking resId.

<!-- schema-object:OpCodesMatcher -->
### `OpCodesMatcher`

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `opCodes` | integer[] | no | `[]` |
| `opNames` | string[] | no | `[]` |
| `matchType` | `OpCodeMatchType` | no | `Contains` |
| `size` | `IntRangeValue` or null | no | null |

<!-- schema-object:IntRangeValue -->
### `IntRangeValue`

Use this JSON object for every Matcher field documented as `IntRangeValue`.

| JSON field | JSON type | Required | Default |
| --- | --- | --- | --- |
| `start` | integer | yes | none |
| `endInclusive` | integer | yes | none |

## Enum Values

Enum values are case-sensitive.

<!-- schema-enum:MatchType -->
### `MatchType`

| Value |
| --- |
| `Contains` |
| `Equals` |

<!-- schema-enum:StringMatchType -->
### `StringMatchType`

| Value |
| --- |
| `Contains` |
| `StartsWith` |
| `EndsWith` |
| `Equals` |
| `SimilarRegex` |

<!-- schema-enum:OpCodeMatchType -->
### `OpCodeMatchType`

| Value |
| --- |
| `Contains` |
| `StartsWith` |
| `EndsWith` |
| `Equals` |

<!-- schema-enum:RetentionPolicyType -->
### `RetentionPolicyType`

| Value |
| --- |
| `Source` |
| `Class` |
| `Runtime` |

<!-- schema-enum:TargetElementType -->
### `TargetElementType`

| Value |
| --- |
| `Type` |
| `Field` |
| `Method` |
| `Parameter` |
| `Constructor` |
| `LocalVariable` |
| `AnnotationType` |
| `Package` |
| `TypeParameter` |
| `TypeUse` |

<!-- schema-enum:UsingType -->
### `UsingType`

| Value |
| --- |
| `Any` |
| `Read` |
| `Write` |
