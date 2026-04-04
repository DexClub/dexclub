;; Minimal Smali highlights query for ktreesitter compatibility.
;;
;; The upstream query currently triggers Query initialization failure on JVM
;; through ktreesitter. Keep this version intentionally conservative so the
;; workspace can at least render stable baseline syntax colors.

; Types

(class_identifier) @type
(primitive_type) @type.builtin

; Methods / fields

(method_identifier) @method
(field_identifier) @field

; Registers / parameters

(parameter) @parameter
(variable) @variable.builtin

; Labels

[
  (label)
  (jmp_label)
] @label

; Operators / keywords

(opcode) @keyword.operator
(annotation_visibility) @storageclass
(access_modifier) @type.qualifier

; Literals

(string) @string
(escape_sequence) @string.escape
(character) @character

[
  (number)
  (float)
  (NaN)
  (Infinity)
] @number

(boolean) @boolean
(null) @constant.builtin

; Comments / errors

(comment) @comment
(ERROR) @error
