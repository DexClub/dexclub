package io.github.dexclub.codeview.core.token

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public enum class CodeTokenKind {
    PlainText,
    Keyword,
    KeywordModifier,
    KeywordType,
    StringLiteral,
    NumberLiteral,
    BooleanLiteral,
    NullLiteral,
    Comment,
    TypeName,
    FunctionName,
    VariableName,
    PropertyName,
    ParameterName,
    ConstantName,
    LabelName,
    Operator,
    Punctuation,
    Annotation,
    Namespace,
    EscapeSequence,
    Interpolation,
    Builtin,
    Invalid,
}
