package io.github.dexclub.codeview.treesitter.highlight

import io.github.dexclub.codeview.core.token.CodeTokenKind

/**
 * Maps tree-sitter highlight capture names (e.g. "@keyword", "@type.builtin")
 * to [CodeTokenKind] enum values.
 */
object TreeSitterTokenMapper {

    fun map(captureName: String): CodeTokenKind? = when (captureName) {
        // Keywords
        "keyword",
        "keyword.function",
        "keyword.return",
        "keyword.operator",
        "conditional",
        "repeat",
        "include",
        "exception",
        "storageclass" -> CodeTokenKind.Keyword

        "keyword.modifier",
        "type.qualifier" -> CodeTokenKind.KeywordModifier

        "type.builtin",
        "keyword.type" -> CodeTokenKind.KeywordType

        // Literals
        "string",
        "text.literal",
        "character" -> CodeTokenKind.StringLiteral

        "number",
        "float" -> CodeTokenKind.NumberLiteral

        "boolean" -> CodeTokenKind.BooleanLiteral

        "constant.builtin" -> CodeTokenKind.NullLiteral

        // Comments
        "comment",
        "comment.documentation",
        "spell" -> CodeTokenKind.Comment

        // Types
        "type",
        "constructor" -> CodeTokenKind.TypeName

        // Functions / methods
        "function",
        "function.method",
        "method",
        "method.call" -> CodeTokenKind.FunctionName

        // Variables
        "variable" -> CodeTokenKind.VariableName

        "variable.builtin" -> CodeTokenKind.Builtin

        // Properties / fields
        "property",
        "field" -> CodeTokenKind.PropertyName

        // Parameters
        "parameter",
        "parameter.builtin" -> CodeTokenKind.ParameterName

        // Constants
        "constant" -> CodeTokenKind.ConstantName

        // Labels
        "label" -> CodeTokenKind.LabelName

        // Operators
        "operator" -> CodeTokenKind.Operator

        // Punctuation
        "punctuation.bracket",
        "punctuation.delimiter",
        "punctuation.special" -> CodeTokenKind.Punctuation

        // Annotations / attributes
        "attribute" -> CodeTokenKind.Annotation

        // Namespaces
        "namespace" -> CodeTokenKind.Namespace

        // Escape sequences
        "string.escape",
        "character.special" -> CodeTokenKind.EscapeSequence

        // Interpolation / special string content
        "string.regex",
        "string.special",
        "text.uri",
        "text.underline" -> CodeTokenKind.Interpolation

        // Builtins
        "builtin",
        "function.builtin" -> CodeTokenKind.Builtin

        // Errors
        "error" -> CodeTokenKind.Invalid

        else -> null
    }
}
